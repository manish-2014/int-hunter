package org.madladlabs.classz.extractors;

import javassist.CtClass;
import javassist.CtMethod;
import javassist.bytecode.*;
import org.madladlabs.classz.model.Finding;
import org.madladlabs.classz.reporting.IFindingWriter;
import org.madladlabs.classz.spi.IExtractor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.*;

/**
 * Extractor that detects the use of PreparedStatement setter methods (like setInt, setString, etc.)
 * in Java bytecode. This is useful for identifying parameterized queries which are generally
 * safer against SQL injection attacks.
 *
 * <h2>How it works:</h2>
 * <ol>
 *   <li>Scans bytecode for SQL string literals (INSERT, UPDATE, DELETE, SELECT, etc.)</li>
 *   <li>Tracks when these SQL strings are passed to Connection.prepareStatement()</li>
 *   <li>Monitors subsequent method calls to find setter invocations on the PreparedStatement</li>
 *   <li>Reports findings when setter methods are detected for modifying SQL statements</li>
 * </ol>
 *
 * <h2>Bytecode Analysis Strategy:</h2>
 * <p>The extractor performs a two-phase scan:</p>
 * <ul>
 *   <li><b>Phase 1:</b> Detect SQL strings and prepareStatement calls</li>
 *   <li><b>Phase 2:</b> Track PreparedStatement usage and setter method invocations</li>
 * </ul>
 *
 * @author madladlabs
 */
public class PreparedStmtExtractor implements IExtractor {

    private static final Logger logger = LogManager.getLogger(PreparedStmtExtractor.class);
    /**
     * Constant pool tag for String constants in Java bytecode
     */
    private static final int CONSTANT_String = 8;



    @Override
    public String name() {
        return "PreparedStatement";
    }

    /**
     * Processes a Java class to detect PreparedStatement setter usage.
     *
     * <h3>How setInt Detection Works:</h3>
     *
     * <p>Consider this Java code:</p>
     * <pre>
     * String sql = "INSERT INTO users (id, name) VALUES (?, ?)";
     * PreparedStatement stmt = conn.prepareStatement(sql);
     * stmt.setInt(1, userId);
     * stmt.setString(2, userName);
     * </pre>
     *
     * <p>This compiles to bytecode like:</p>
     * <pre>
     * LDC "INSERT INTO users (id, name) VALUES (?, ?)"  // Load SQL string
     * ASTORE 2                                           // Store in local var 2
     * ALOAD 1                                            // Load connection
     * ALOAD 2                                            // Load SQL string
     * INVOKEINTERFACE java/sql/Connection.prepareStatement
     * ASTORE 3                                           // Store PreparedStatement in var 3
     * ALOAD 3                                            // Load PreparedStatement
     * ICONST_1                                           // Push parameter index 1
     * ILOAD 4                                            // Load userId
     * INVOKEVIRTUAL java/sql/PreparedStatement.setInt   // Call setInt
     * </pre>
     *
     * <p>The detection algorithm:</p>
     * <ol>
     *   <li>When we see LDC with a SQL string, we remember it</li>
     *   <li>When we see INVOKEINTERFACE/INVOKEVIRTUAL calling prepareStatement, we know
     *       the SQL is being prepared</li>
     *   <li>We track which local variable stores the PreparedStatement (via ASTORE)</li>
     *   <li>We scan forward for INVOKEVIRTUAL calls to setInt, setString, etc.</li>
     *   <li>When found, we create a Finding to report this usage</li>
     * </ol>
     *
     * @param ctClass The Java class to analyze
     * @param writer The writer to report findings to
     * @throws Exception if bytecode analysis fails
     */
    @Override
    public void process(CtClass ctClass, IFindingWriter writer) throws Exception {
        logger.info("Processing class: " + ctClass.getName());

        if (earlyReturnIfNoJDBCPrepOrCallable(ctClass)) {
            logger.info("Skipping " + ctClass.getName() +
                    " – no Prepared Statement or CallableStatement references found.");
            return;            // nothing Springy here – skip expensive work
        }

        for (CtMethod method : ctClass.getDeclaredMethods()) {
            logger.info("  Method: " + method.getName());

            MethodInfo methodInfo = method.getMethodInfo();
            CodeAttribute codeAttr = methodInfo.getCodeAttribute();
            if (codeAttr == null) {
                logger.info("    Skipped: No CodeAttribute.");
                continue;
            }

            ConstPool constPool = methodInfo.getConstPool();
            CodeIterator ci = codeAttr.iterator();
            LineNumberAttribute lineAttr = (LineNumberAttribute) codeAttr.getAttribute(LineNumberAttribute.tag);

            // Track the most recently seen SQL string
            String recentSqlLiteral = null;

            // Track which local variable holds the PreparedStatement
            int preparedStmtVarIndex = -1;

            while (ci.hasNext()) {
                int index = ci.next();
                int opcode = ci.byteAt(index);

                // Phase 1: Detect SQL string constants
                if (opcode == Opcode.LDC || opcode == Opcode.LDC_W) {
                    int cpIndex = (opcode == Opcode.LDC) ? ci.byteAt(index + 1) : ci.u16bitAt(index + 1);
                    if (constPool.getTag(cpIndex) == CONSTANT_String) {
                        String str = constPool.getStringInfo(cpIndex);
                        if (isSqlString(str)) {
                            recentSqlLiteral = str;
                            logger.info("    Found SQL string: " + str);
                        }
                    }
                }

                // Phase 2: Detect Connection.prepareStatement call
                if ((opcode == Opcode.INVOKEVIRTUAL || opcode == Opcode.INVOKEINTERFACE) && recentSqlLiteral != null) {
                    int cpIndex = ci.u16bitAt(index + 1);

                    String className = "";
                    String methodName = "";

                    // Extract class and method names based on invocation type
                    if (opcode == Opcode.INVOKEVIRTUAL) {
                        className = constPool.getMethodrefClassName(cpIndex);
                        methodName = constPool.getMethodrefName(cpIndex);
                    } else if (opcode == Opcode.INVOKEINTERFACE) {
                        className = constPool.getInterfaceMethodrefClassName(cpIndex);
                        methodName = constPool.getInterfaceMethodrefName(cpIndex);
                    }

                    logger.info("    Checking method call: " + className + "." + methodName);

                    // Check if this is a prepareStatement call
                    if (methodName.equals("prepareStatement") &&
                            (className.equals("java.sql.Connection") || className.endsWith("Connection"))) {

                        String currentSql = recentSqlLiteral;
                        logger.info("    Matched prepareStatement with SQL: " + currentSql);

                        // Track where the PreparedStatement is stored
                        if (ci.hasNext()) {
                            int nextIndex = ci.lookAhead();
                            if (ci.byteAt(nextIndex) == Opcode.ASTORE) {
                                preparedStmtVarIndex = ci.byteAt(nextIndex + 1);
                                logger.info("    PreparedStatement stored in local var: " + preparedStmtVarIndex);
                            } else if (ci.byteAt(nextIndex) >= Opcode.ASTORE_0 &&
                                    ci.byteAt(nextIndex) <= Opcode.ASTORE_3) {
                                preparedStmtVarIndex = ci.byteAt(nextIndex) - Opcode.ASTORE_0;
                                logger.info("    PreparedStatement stored in local var: " + preparedStmtVarIndex);
                            }
                        }

                        // Scan forward to find setter method calls
                        scanForSetterMethods(codeAttr, constPool, index + 3, currentSql,
                                preparedStmtVarIndex, lineAttr, ctClass, method, writer);

                        // Reset for next potential prepareStatement
                        recentSqlLiteral = null;
                        preparedStmtVarIndex = -1;
                    }
                }
            }
        }
    }

    /**
     * Scans bytecode starting from a given position to find setter method invocations
     * on a PreparedStatement object.
     *
     * @param codeAttr The code attribute containing the bytecode
     * @param constPool The constant pool for resolving method references
     * @param startPos The position to start scanning from
     * @param sql The SQL statement being prepared
     * @param preparedStmtVar The local variable index holding the PreparedStatement
     * @param lineAttr Line number attribute for source line mapping
     * @param ctClass The class being analyzed
     * @param method The method being analyzed
     * @param writer The findings writer
     */
    private void scanForSetterMethods(CodeAttribute codeAttr, ConstPool constPool, int startPos,
                                      String sql, int preparedStmtVar, LineNumberAttribute lineAttr,
                                      CtClass ctClass, CtMethod method, IFindingWriter writer) {
        CodeIterator follow = codeAttr.iterator();
        follow.move(startPos);
        boolean foundSetter = false;

        while (follow.hasNext()) {
            try {
                int lookaheadIndex = follow.next();
                int op = follow.byteAt(lookaheadIndex);

                // Check for method invocations
                if ((op == Opcode.INVOKEVIRTUAL || op == Opcode.INVOKEINTERFACE)) {
                    int cp = follow.u16bitAt(lookaheadIndex + 1);
                    String cname = "";
                    String mname = "";

                    if (op == Opcode.INVOKEVIRTUAL) {
                        cname = constPool.getMethodrefClassName(cp);
                        mname = constPool.getMethodrefName(cp);
                    } else {
                        cname = constPool.getInterfaceMethodrefClassName(cp);
                        mname = constPool.getInterfaceMethodrefName(cp);
                    }

                    logger.info("      Checking method: " + cname + "." + mname);

                    // Check if this is a setter method on PreparedStatement
                    if (isPreparedStatementSetter(cname, mname)) {
                        if (isModifyingStatement(sql)) {
                            int lineNumber = (lineAttr != null) ? lineAttr.toLineNumber(lookaheadIndex) : -1;
                            logger.info("      Found setter: " + cname + "." + mname + " at line " + lineNumber);
                            writer.accept(new Finding(
                                    "PreparedStatement",
                                    ctClass.getName(),
                                    method.getName(),
                                    lineNumber,
                                    trimSql(sql),
                                    null, null, null, null
                            ));
                            foundSetter = true;
                            break;
                        }
                    }
                }

                // Stop scanning at method exit points
                if (isReturnOpcode(op)) {
                    break;
                }
            } catch (Exception e) {
                // Handle any bytecode reading errors
                break;
            }
        }

        if (!foundSetter) {
            logger.info("    No matching setter found after prepareStatement.");
        }
    }

    /**
     * Checks if a given class and method name combination represents a PreparedStatement
     * setter method.
     *
     * @param className The fully qualified class name (using dots)
     * @param methodName The method name
     * @return true if this is a setter method we're interested in
     */
    private boolean isPreparedStatementSetter(String className, String methodName) {
        return (className.equals("java.sql.PreparedStatement") ||
                className.equals("java.sql.CallableStatement")) &&
                (methodName.equals("setInt") || methodName.equals("setLong") ||
                        methodName.equals("setString") || methodName.equals("setDouble") ||
                        methodName.equals("setFloat") || methodName.equals("setBoolean") ||
                        methodName.equals("setDate") || methodName.equals("setTimestamp") ||
                        methodName.equals("setBigDecimal") || methodName.equals("setBytes") ||
                        methodName.equals("setObject"));
    }

    /**
     * Checks if an opcode represents a return instruction.
     *
     * @param opcode The bytecode opcode
     * @return true if this is a return instruction
     */
    private boolean isReturnOpcode(int opcode) {
        return opcode == Opcode.RETURN || opcode == Opcode.ARETURN ||
                opcode == Opcode.IRETURN || opcode == Opcode.LRETURN ||
                opcode == Opcode.FRETURN || opcode == Opcode.DRETURN;
    }

    /**
     * Determines if a string is likely to be a SQL statement based on common SQL keywords.
     *
     * @param str The string to check
     * @return true if the string appears to be SQL
     */
    private boolean isSqlString(String str) {
        if (str == null || str.length() < 10) return false;
        String lower = str.toLowerCase().trim();
        return lower.startsWith("select") || lower.startsWith("insert") ||
                lower.startsWith("update") || lower.startsWith("delete") ||
                lower.startsWith("merge") || lower.startsWith("call");
    }

    /**
     * Checks if a SQL statement is a data-modifying statement (INSERT, UPDATE, DELETE).
     *
     * @param sql The SQL statement
     * @return true if this is a modifying statement
     */
    private boolean isModifyingStatement(String sql) {
        String trimmed = trimSql(sql).toLowerCase(Locale.ROOT);
        return trimmed.startsWith("insert") || trimmed.startsWith("update") || trimmed.startsWith("delete");
    }

    /**
     * Removes comments and trims whitespace from a SQL statement.
     *
     * @param sql The SQL statement to clean
     * @return The cleaned SQL statement
     */
    private String trimSql(String sql) {
        return sql.replaceAll("(?s)/\\*.*?\\*/", "")  // Remove block comments
                .replaceAll("--.*?(\r?\n|$)", "")     // Remove line comments
                .trim();
    }
    private static final String JDBC_PREPARED = "java.sql.PreparedStatement";

    private static final String JDBC_CALLATBLE =
            "java.sql.CallableStatement";
    private boolean earlyReturnIfNoJDBCPrepOrCallable(CtClass ctClass) {
        ConstPool cp = ctClass.getClassFile().getConstPool();

        for (int i = 1; i < cp.getSize(); i++) {
            if (cp.getTag(i) == ConstPool.CONST_Class) {
                // Constant-pool stores the internal form; make sure it’s dot-notation before comparing.
                String internal = cp.getClassInfo(i);          // e.g. "org.springframework.jdbc.core.JdbcTemplate"
                String dotName  = internal.replace('/', '.');  // just in case slash form appears

                if (JDBC_PREPARED.equals(dotName) ||
                        JDBC_CALLATBLE.equals(dotName) ||
                        dotName.endsWith(".PreparedStatement") ||
                        dotName.endsWith(".CallableStatement")) {
                    return false;              // relevant Spring reference found → KEEP processing
                }
            }
        }
        return true;                           // no match → skip this class
    }
}