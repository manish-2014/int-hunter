package org.madladlabs.classz.extractors;

import javassist.CtClass;
import javassist.CtField;
import javassist.Modifier;
import javassist.bytecode.*;
import javassist.bytecode.annotation.Annotation;
import javassist.bytecode.annotation.MemberValue;
import javassist.bytecode.annotation.StringMemberValue;
import org.madladlabs.classz.model.Finding;
import org.madladlabs.classz.reporting.IFindingWriter;
import org.madladlabs.classz.spi.IExtractor;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
/**
 * Detects primitive {@code int} and boxed {@code java.lang.Integer} fields on
 * JPA entity classes, supporting both Java EE (javax.persistence.*) and
 * Jakarta EE (jakarta.persistence.*) packages.
 */
public class HibernateIntFieldExtractor implements IExtractor {

    private static final Logger logger = LogManager.getLogger(HibernateIntFieldExtractor.class);
    /* ─────────────────────────────────────────────────────────────── */

    // Fully qualified annotation names we care about
    private static final String[] ENTITY_ANN  = {
            "javax.persistence.Entity",   "jakarta.persistence.Entity"
    };
    private static final String[] TABLE_ANN   = {
            "javax.persistence.Table",    "jakarta.persistence.Table"
    };
    private static final String[] COLUMN_ANN  = {
            "javax.persistence.Column",   "jakarta.persistence.Column"
    };

    /* ─────────────────────────────────────────────────────────────── */

    @Override
    public String name() {
        return "HibernateIntField";
    }

    @Override
    public void process(CtClass ctClass, IFindingWriter writer) throws Exception {
        if (!isAnnotatedWith(ctClass, ENTITY_ANN)) return;   // not an entity

        String tableName = resolveTableName(ctClass);

        for (CtField field : ctClass.getDeclaredFields()) {
            if (isConstant(field)) continue;                 // skip static finals
            if (!isIntLike(field)) continue;                 // only int/Integer

            String columnName = resolveColumnName(field);
            String javaType   = field.getType().getName();   // "int" or "java.lang.Integer"

            writer.accept(new Finding(
                    "HibernateIntField",
                    ctClass.getName(),
                    field.getName(),          // methodName slot repurposed for field
                    -1,
                    null, null,
                    tableName,
                    columnName,
                    javaType
            ));
        }
    }

    /* ───────────────────────── helper methods ───────────────────── */

    private boolean isAnnotatedWith(CtClass cc, String... fqcnCandidates) {
        return getAnnotation(cc.getClassFile(), fqcnCandidates) != null;
    }

    private String resolveTableName(CtClass cc) {
        Annotation a = getAnnotation(cc.getClassFile(), TABLE_ANN);
        if (a == null) return null;
        MemberValue mv = a.getMemberValue("name");
        return (mv instanceof StringMemberValue)
                ? ((StringMemberValue) mv).getValue()
                : null;
    }

    private boolean isIntLike(CtField f) throws Exception {
        String t = f.getType().getName();
        return "int".equals(t) || "java.lang.Integer".equals(t);
    }

    private boolean isConstant(CtField f) {
        int mod = f.getModifiers();
        return Modifier.isStatic(mod) && Modifier.isFinal(mod);
    }

    private String resolveColumnName(CtField f) {
        Annotation col = getAnnotation(f.getFieldInfo(), COLUMN_ANN);
        if (col == null) return null;
        MemberValue mv = col.getMemberValue("name");
        return (mv instanceof StringMemberValue)
                ? ((StringMemberValue) mv).getValue()
                : null;
    }

    /* ───────────────────── generic annotation fetch ─────────────── */

    private Annotation getAnnotation(AttributeInfo ai, String... fqcnCandidates) {
        if (!(ai instanceof AnnotationsAttribute)) return null;
        AnnotationsAttribute attr = (AnnotationsAttribute) ai;
        for (String fqcn : fqcnCandidates) {
            Annotation ann = attr.getAnnotation(fqcn);
            if (ann != null) return ann;
        }
        return null;
    }

    private Annotation getAnnotation(FieldInfo fi, String... fqcnCandidates) {
        AnnotationsAttribute vis =
                (AnnotationsAttribute) fi.getAttribute(AnnotationsAttribute.visibleTag);
        if (vis != null) {
            Annotation ann = getAnnotation(vis, fqcnCandidates);
            if (ann != null) return ann;
        }
        AnnotationsAttribute inv =
                (AnnotationsAttribute) fi.getAttribute(AnnotationsAttribute.invisibleTag);
        return inv == null ? null : getAnnotation(inv, fqcnCandidates);
    }

    private Annotation getAnnotation(ClassFile cf, String... fqcnCandidates) {
        AnnotationsAttribute vis =
                (AnnotationsAttribute) cf.getAttribute(AnnotationsAttribute.visibleTag);
        if (vis != null) {
            Annotation ann = getAnnotation(vis, fqcnCandidates);
            if (ann != null) return ann;
        }
        AnnotationsAttribute inv =
                (AnnotationsAttribute) cf.getAttribute(AnnotationsAttribute.invisibleTag);
        return inv == null ? null : getAnnotation(inv, fqcnCandidates);
    }
}
