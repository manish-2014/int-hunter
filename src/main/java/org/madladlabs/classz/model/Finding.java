package org.madladlabs.classz.model;

public class Finding {

    private final String type;
    private final String className;
    private final String methodName;
    private final int bytecodeLine;
    private final String sqlSnippet;
    private final Integer paramIndex;
    private final String table;
    private final String column;
    private final String javaType;

    public Finding(String type, String className, String methodName, int bytecodeLine,
                   String sqlSnippet, Integer paramIndex, String table, String column, String javaType) {
        this.type = type;
        this.className = className;
        this.methodName = methodName;
        this.bytecodeLine = bytecodeLine;
        this.sqlSnippet = sqlSnippet;
        this.paramIndex = paramIndex;
        this.table = table;
        this.column = column;
        this.javaType = javaType;
    }

    public String getType() {
        return type;
    }

    public String getClassName() {
        return className;
    }

    public String getMethodName() {
        return methodName;
    }

    public int getBytecodeLine() {
        return bytecodeLine;
    }

    public String getSqlSnippet() {
        return sqlSnippet;
    }

    public Integer getParamIndex() {
        return paramIndex;
    }

    public String getTable() {
        return table;
    }

    public String getColumn() {
        return column;
    }

    public String getJavaType() {
        return javaType;
    }

    @Override
    public String toString() {
        return "Finding{" +
                "type='" + type + '\'' +
                ", className='" + className + '\'' +
                ", methodName='" + methodName + '\'' +
                ", bytecodeLine=" + bytecodeLine +
                ", sqlSnippet='" + sqlSnippet + '\'' +
                ", paramIndex=" + paramIndex +
                ", table='" + table + '\'' +
                ", column='" + column + '\'' +
                ", javaType='" + javaType + '\'' +
                '}';
    }
}
