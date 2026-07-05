package io.javalive.processor.parser;

/**
 * Utility class for mapping Java types to their Vue / JavaScript equivalents.
 *
 * <p>Used by {@link ClassParser} and {@link FieldParser} to convert
 * Java field types into Vue prop types, default values, and JSON serialization hints.
 *
 * @author JavaLive Team
 * @since 1.0.0
 */
public final class TypeMapper {

    private TypeMapper() {} // utility class

    /**
     * Maps a fully qualified Java type string to the Vue constructor type.
     *
     * <p>Examples:
     * <ul>
     *   <li>{@code "int"} → {@code "Number"}</li>
     *   <li>{@code "java.lang.String"} → {@code "String"}</li>
     *   <li>{@code "java.util.List<com.example.User>"} → {@code "Array"}</li>
     *   <li>{@code "java.util.Map<String,Object>"} → {@code "Object"}</li>
     *   <li>{@code "com.example.MyPojo"} → {@code "Object"}</li>
     * </ul>
     *
     * @param javaType the fully qualified Java type
     * @return Vue constructor type string
     */
    public static String toVueType(String javaType) {
        if (javaType == null) return "Object";

        // Numeric primitives and their wrappers
        if (isNumericType(javaType)) return "Number";

        // String
        if (isStringType(javaType)) return "String";

        // Boolean
        if (isBooleanType(javaType)) return "Boolean";

        // Collections → Array
        if (isCollectionType(javaType)) return "Array";

        // Maps → Object
        if (isMapType(javaType)) return "Object";

        // Dates → String (ISO format)
        if (isDateType(javaType)) return "String";

        // Default: custom POJOs → Object
        return "Object";
    }

    /**
     * Returns the JavaScript default value literal for a given Java type.
     *
     * <p>Examples:
     * <ul>
     *   <li>{@code "int"} → {@code "0"}</li>
     *   <li>{@code "String"} → {@code "''"}</li>
     *   <li>{@code "boolean"} → {@code "false"}</li>
     *   <li>{@code "List<T>"} → {@code "[]"}</li>
     *   <li>{@code "Map<K,V>"} → {@code "{}"}</li>
     *   <li>custom POJO → {@code "null"}</li>
     * </ul>
     *
     * @param javaType the fully qualified Java type
     * @return JavaScript default value literal
     */
    public static String toDefaultValue(String javaType) {
        if (javaType == null) return "null";

        if (isNumericType(javaType)) return "0";
        if (isStringType(javaType)) return "''";
        if (isBooleanType(javaType)) return "false";
        if (isCollectionType(javaType)) return "[]";
        if (isMapType(javaType)) return "{}";
        if (isDateType(javaType)) return "null";

        return "null";
    }

    /**
     * Returns the Java-to-JavaScript conversion snippet used in the
     * MethodDispatcher to cast values coming over the wire (JSON numbers etc.)
     * back to their correct Java type.
     *
     * @param javaType  the target Java type
     * @param valueExpr the JavaScript expression holding the value
     * @return cast expression
     */
    public static String toJavaCastExpression(String javaType, String valueExpr) {
        if (javaType == null) return valueExpr;
        switch (javaType) {
            case "int":
            case "java.lang.Integer": return "((Number) " + valueExpr + ").intValue()";
            case "long":
            case "java.lang.Long":    return "((Number) " + valueExpr + ").longValue()";
            case "double":
            case "java.lang.Double":  return "((Number) " + valueExpr + ").doubleValue()";
            case "float":
            case "java.lang.Float":   return "((Number) " + valueExpr + ").floatValue()";
            case "boolean":
            case "java.lang.Boolean": return "Boolean.parseBoolean(" + valueExpr + ".toString())";
            default: return valueExpr;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private type-check helpers
    // ─────────────────────────────────────────────────────────────────────────

    private static boolean isNumericType(String t) {
        return t.equals("int") || t.equals("long") || t.equals("double") || t.equals("float")
            || t.equals("short") || t.equals("byte")
            || t.contains("Integer") || t.contains("Long") || t.contains("Double")
            || t.contains("Float") || t.contains("Short") || t.contains("Byte")
            || t.contains("BigDecimal") || t.contains("BigInteger");
    }

    private static boolean isStringType(String t) {
        return t.equals("java.lang.String") || t.equals("String")
            || t.contains("CharSequence") || t.contains("StringBuilder");
    }

    private static boolean isBooleanType(String t) {
        return t.equals("boolean") || t.equals("java.lang.Boolean") || t.equals("Boolean");
    }

    private static boolean isCollectionType(String t) {
        return t.contains("List") || t.contains("Set") || t.contains("Collection")
            || t.contains("Queue") || t.contains("Deque") || t.endsWith("[]");
    }

    private static boolean isMapType(String t) {
        return t.contains("Map") || t.contains("HashMap") || t.contains("LinkedHashMap")
            || t.contains("TreeMap") || t.contains("Hashtable");
    }

    private static boolean isDateType(String t) {
        return t.contains("LocalDate") || t.contains("LocalDateTime")
            || t.contains("ZonedDateTime") || t.contains("Instant")
            || t.contains("java.util.Date") || t.contains("java.sql.Date")
            || t.contains("Timestamp");
    }
}
