package org.madladlabs.classz.extractors;


import javassist.CtClass;
import javassist.CtMethod;
import javassist.bytecode.*;
import org.madladlabs.classz.model.Finding;
import org.madladlabs.classz.reporting.IFindingWriter;
import org.madladlabs.classz.spi.IExtractor;

import java.util.Locale;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Detects integer values passed to Spring's JdbcTemplate / NamedParameterJdbcTemplate update(...) methods.
 *
 * Strategy (single pass over the byte-code):
 *   1. Remember SQL-looking string constants (for nicer reporting).
 *   2. Record the bytecode offset of the most-recent Integer.valueOf(int) boxing call.
 *   3. When we hit an INVOKE* of *.update(...):
 *        – If that boxing call is "nearby" (<= BOXING_DISTANCE bytes back), we emit a Finding.
 */
public class JdbcTemplateIntExtractor implements IExtractor {

    private static final Logger logger = LogManager.getLogger(JdbcTemplateIntExtractor.class);

    private static final int CONSTANT_String = 8;
    private static final int BOXING_DISTANCE = 200; // bytes between Integer.valueOf and update call

    private static final String JDBC_TEMPLATE =
            "org.springframework.jdbc.core.JdbcTemplate";
    private static final String NAMED_TEMPLATE =
            "org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate";

    @Override
    public String name() {
        return "JdbcTemplateInt";
    }

    @Override
    public void process(CtClass ctClass, IFindingWriter writer) throws Exception {


        /* >>> EARLY EXIT <<< */

        if (earlyReturnIfNotSpring(ctClass)) {
            logger.info("Skipping " + ctClass.getName() +
                    " – no Spring JdbcTemplate or NamedParameterJdbcTemplate references found.");
            return;            // nothing Springy here – skip expensive work
        }


        for (CtMethod method : ctClass.getDeclaredMethods()) {
            MethodInfo mi = method.getMethodInfo();
            CodeAttribute ca = mi.getCodeAttribute();
            if (ca == null) continue;

            ConstPool cp   = mi.getConstPool();
            CodeIterator it = ca.iterator();
            LineNumberAttribute lines =
                    (LineNumberAttribute) ca.getAttribute(LineNumberAttribute.tag);

            String recentSql = null;     // last SQL literal encountered
            int    lastBox   = -1;       // bytecode index of last Integer.valueOf(int)

            while (it.hasNext()) {
                int idx = it.next();
                int op  = it.byteAt(idx);

                /* --- Phase 1: capture SQL literals ---------------------------------------- */
                if (op == Opcode.LDC || op == Opcode.LDC_W) {
                    int cpIdx = (op == Opcode.LDC)
                            ? it.byteAt(idx + 1)
                            : it.u16bitAt(idx + 1);
                    if (cp.getTag(cpIdx) == CONSTANT_String) {
                        String s = cp.getStringInfo(cpIdx);
                        if (looksLikeSql(s)) recentSql = s;
                    }
                }

                /* --- Phase 2: detect Integer boxing --------------------------------------- */
                if (op == Opcode.INVOKESTATIC) {
                    int cpIdx  = it.u16bitAt(idx + 1);
                    String cls = cp.getMethodrefClassName(cpIdx);
                    String m   = cp.getMethodrefName(cpIdx);
                    String sig = cp.getMethodrefType(cpIdx);

                    if ("java.lang.Integer".equals(cls)
                            && "valueOf".equals(m)
                            && "(I)Ljava/lang/Integer;".equals(sig)) {
                        lastBox = idx;          // remember where boxing happened
                    }
                }

                /* --- Phase 3: look for *.update(...) ------------------------------------- */
                if (op == Opcode.INVOKEVIRTUAL || op == Opcode.INVOKEINTERFACE) {
                    int cpIdx;
                    String cls;
                    String m;

                    if (op == Opcode.INVOKEVIRTUAL) {
                        cpIdx = it.u16bitAt(idx + 1);
                        cls   = cp.getMethodrefClassName(cpIdx);
                        m     = cp.getMethodrefName(cpIdx);
                    } else {
                        cpIdx = it.u16bitAt(idx + 1);
                        cls   = cp.getInterfaceMethodrefClassName(cpIdx);
                        m     = cp.getInterfaceMethodrefName(cpIdx);
                    }

                    if ("update".equals(m) &&
                            (JDBC_TEMPLATE.equals(cls)
                                    || NAMED_TEMPLATE.equals(cls)
                                    || cls.endsWith("JdbcTemplate"))) {

                        boolean intSeen = lastBox != -1
                                && (idx - lastBox) <= BOXING_DISTANCE;

                        if (intSeen) {
                            int line = (lines != null) ? lines.toLineNumber(idx) : -1;
                            writer.accept(new Finding(
                                    "JdbcTemplateInt",
                                    ctClass.getName(),
                                    method.getName(),
                                    line,
                                    recentSql != null ? cleanSql(recentSql) : null,
                                    null, null, null, null
                            ));
                        }

                        /* reset trackers for the next update call in the same method */
                        recentSql = null;
                        lastBox   = -1;
                    }
                }
            }
        }
    }

    /** @return true if the extractor should stop processing this class */
    /**
     * Returns true when the class does *not* reference JdbcTemplate / NamedParameterJdbcTemplate
     * and we can safely skip byte-code analysis.
     */
    private boolean earlyReturnIfNotSpring(CtClass ctClass) {
        ConstPool cp = ctClass.getClassFile().getConstPool();

        for (int i = 1; i < cp.getSize(); i++) {
            if (cp.getTag(i) == ConstPool.CONST_Class) {
                // Constant-pool stores the internal form; make sure it’s dot-notation before comparing.
                String internal = cp.getClassInfo(i);          // e.g. "org.springframework.jdbc.core.JdbcTemplate"
                String dotName  = internal.replace('/', '.');  // just in case slash form appears

                if (JDBC_TEMPLATE.equals(dotName) ||
                        NAMED_TEMPLATE.equals(dotName) ||
                        dotName.endsWith(".JdbcTemplate") ||
                        dotName.endsWith(".NamedParameterJdbcTemplate")) {
                    return false;              // relevant Spring reference found → KEEP processing
                }
            }
        }
        return true;                           // no match → skip this class
    }



    /* --------------------------------------------------------------------- */
    private boolean looksLikeSql(String s) {
        if (s == null || s.length() < 6) return false;
        String l = s.trim().toLowerCase(Locale.ROOT);
        return l.startsWith("select") || l.startsWith("insert")
                || l.startsWith("update") || l.startsWith("delete")
                || l.startsWith("merge")  || l.startsWith("call");
    }

    private String cleanSql(String sql) {
        return sql.replaceAll("(?s)/\\*.*?\\*/", "")       // block comments
                .replaceAll("--.*?(\\r?\\n|$)", "")      // line comments
                .trim();
    }
}
