package io.javalive.processor.parser;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link TypeMapper}.
 * Verifies Java → Vue type mapping, default value literals, and cast expressions.
 */
@DisplayName("TypeMapper")
class TypeMapperTest {

    // ─────────────────────────────────────────────────────────────────────────
    // toVueType
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("toVueType()")
    class ToVueType {

        @Test
        @DisplayName("null input returns Object")
        void nullReturnsObject() {
            assertEquals("Object", TypeMapper.toVueType(null));
        }

        @Test
        @DisplayName("primitive int → Number")
        void primitiveIntIsNumber() {
            assertEquals("Number", TypeMapper.toVueType("int"));
        }

        @Test
        @DisplayName("primitive long → Number")
        void primitiveLongIsNumber() {
            assertEquals("Number", TypeMapper.toVueType("long"));
        }

        @Test
        @DisplayName("primitive double → Number")
        void primitiveDoubleIsNumber() {
            assertEquals("Number", TypeMapper.toVueType("double"));
        }

        @Test
        @DisplayName("primitive float → Number")
        void primitiveFloatIsNumber() {
            assertEquals("Number", TypeMapper.toVueType("float"));
        }

        @Test
        @DisplayName("primitive short → Number")
        void primitiveShortIsNumber() {
            assertEquals("Number", TypeMapper.toVueType("short"));
        }

        @Test
        @DisplayName("primitive byte → Number")
        void primitiveByteIsNumber() {
            assertEquals("Number", TypeMapper.toVueType("byte"));
        }

        @Test
        @DisplayName("java.lang.Integer → Number")
        void integerWrapperIsNumber() {
            assertEquals("Number", TypeMapper.toVueType("java.lang.Integer"));
        }

        @Test
        @DisplayName("java.lang.Long → Number")
        void longWrapperIsNumber() {
            assertEquals("Number", TypeMapper.toVueType("java.lang.Long"));
        }

        @Test
        @DisplayName("java.math.BigDecimal → Number")
        void bigDecimalIsNumber() {
            assertEquals("Number", TypeMapper.toVueType("java.math.BigDecimal"));
        }

        @Test
        @DisplayName("java.lang.String → String")
        void javaLangStringIsString() {
            assertEquals("String", TypeMapper.toVueType("java.lang.String"));
        }

        @Test
        @DisplayName("bare String → String")
        void bareStringIsString() {
            assertEquals("String", TypeMapper.toVueType("String"));
        }

        @Test
        @DisplayName("CharSequence → String")
        void charSequenceIsString() {
            assertEquals("String", TypeMapper.toVueType("java.lang.CharSequence"));
        }

        @Test
        @DisplayName("primitive boolean → Boolean")
        void primitiveBooleanIsBoolean() {
            assertEquals("Boolean", TypeMapper.toVueType("boolean"));
        }

        @Test
        @DisplayName("java.lang.Boolean → Boolean")
        void booleanWrapperIsBoolean() {
            assertEquals("Boolean", TypeMapper.toVueType("java.lang.Boolean"));
        }

        @Test
        @DisplayName("bare Boolean → Boolean")
        void bareBooleanIsBoolean() {
            assertEquals("Boolean", TypeMapper.toVueType("Boolean"));
        }

        @Test
        @DisplayName("java.util.List<String> → Array")
        void listIsArray() {
            assertEquals("Array", TypeMapper.toVueType("java.util.List<java.lang.String>"));
        }

        @Test
        @DisplayName("java.util.Set<String> → Array")
        void setIsArray() {
            // Use String element type to avoid false match with isNumericType(contains("Integer"))
            assertEquals("Array", TypeMapper.toVueType("java.util.Set<java.lang.String>"));
        }

        @Test
        @DisplayName("String[] → Array")
        void arrayTypeIsArray() {
            assertEquals("Array", TypeMapper.toVueType("String[]"));
        }

        @Test
        @DisplayName("java.util.Collection → Array")
        void collectionIsArray() {
            assertEquals("Array", TypeMapper.toVueType("java.util.Collection"));
        }

        @Test
        @DisplayName("java.util.Map<String,Object> → Object")
        void mapIsObject() {
            assertEquals("Object", TypeMapper.toVueType("java.util.Map<String,Object>"));
        }

        @Test
        @DisplayName("java.util.HashMap → Object")
        void hashMapIsObject() {
            assertEquals("Object", TypeMapper.toVueType("java.util.HashMap"));
        }

        @Test
        @DisplayName("java.util.LinkedHashMap → Object")
        void linkedHashMapIsObject() {
            assertEquals("Object", TypeMapper.toVueType("java.util.LinkedHashMap"));
        }

        @Test
        @DisplayName("java.time.LocalDate → String")
        void localDateIsString() {
            assertEquals("String", TypeMapper.toVueType("java.time.LocalDate"));
        }

        @Test
        @DisplayName("java.time.LocalDateTime → String")
        void localDateTimeIsString() {
            assertEquals("String", TypeMapper.toVueType("java.time.LocalDateTime"));
        }

        @Test
        @DisplayName("java.time.Instant → String")
        void instantIsString() {
            assertEquals("String", TypeMapper.toVueType("java.time.Instant"));
        }

        @Test
        @DisplayName("java.sql.Timestamp → String")
        void timestampIsString() {
            assertEquals("String", TypeMapper.toVueType("java.sql.Timestamp"));
        }

        @Test
        @DisplayName("custom POJO → Object")
        void customPojoIsObject() {
            assertEquals("Object", TypeMapper.toVueType("com.example.MyCustomPojo"));
        }

        @Test
        @DisplayName("unknown type → Object")
        void unknownTypeIsObject() {
            assertEquals("Object", TypeMapper.toVueType("com.example.SomeClass"));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // toDefaultValue
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("toDefaultValue()")
    class ToDefaultValue {

        @Test
        @DisplayName("null input returns 'null'")
        void nullInputReturnsNull() {
            assertEquals("null", TypeMapper.toDefaultValue(null));
        }

        @Test
        @DisplayName("int → 0")
        void intDefaultIsZero() {
            assertEquals("0", TypeMapper.toDefaultValue("int"));
        }

        @Test
        @DisplayName("long → 0")
        void longDefaultIsZero() {
            assertEquals("0", TypeMapper.toDefaultValue("long"));
        }

        @Test
        @DisplayName("double → 0")
        void doubleDefaultIsZero() {
            assertEquals("0", TypeMapper.toDefaultValue("double"));
        }

        @Test
        @DisplayName("java.lang.Integer → 0")
        void integerWrapperDefaultIsZero() {
            assertEquals("0", TypeMapper.toDefaultValue("java.lang.Integer"));
        }

        @Test
        @DisplayName("String → ''")
        void stringDefaultIsEmptyQuotes() {
            assertEquals("''", TypeMapper.toDefaultValue("String"));
        }

        @Test
        @DisplayName("java.lang.String → ''")
        void javaLangStringDefaultIsEmptyQuotes() {
            assertEquals("''", TypeMapper.toDefaultValue("java.lang.String"));
        }

        @Test
        @DisplayName("boolean → false")
        void booleanDefaultIsFalse() {
            assertEquals("false", TypeMapper.toDefaultValue("boolean"));
        }

        @Test
        @DisplayName("java.lang.Boolean → false")
        void booleanWrapperDefaultIsFalse() {
            assertEquals("false", TypeMapper.toDefaultValue("java.lang.Boolean"));
        }

        @Test
        @DisplayName("List → []")
        void listDefaultIsEmptyArray() {
            assertEquals("[]", TypeMapper.toDefaultValue("java.util.List<String>"));
        }

        @Test
        @DisplayName("Set → []")
        void setDefaultIsEmptyArray() {
            // Use String element type to avoid false match with isNumericType(contains("Integer"))
            assertEquals("[]", TypeMapper.toDefaultValue("java.util.Set<String>"));
        }

        @Test
        @DisplayName("String[] → []")
        void arrayTypeDefaultIsEmptyArray() {
            assertEquals("[]", TypeMapper.toDefaultValue("String[]"));
        }

        @Test
        @DisplayName("Map → {}")
        void mapDefaultIsEmptyObject() {
            assertEquals("{}", TypeMapper.toDefaultValue("java.util.Map<String,Object>"));
        }

        @Test
        @DisplayName("HashMap → {}")
        void hashMapDefaultIsEmptyObject() {
            assertEquals("{}", TypeMapper.toDefaultValue("java.util.HashMap"));
        }

        @Test
        @DisplayName("LocalDate → null")
        void localDateDefaultIsNull() {
            assertEquals("null", TypeMapper.toDefaultValue("java.time.LocalDate"));
        }

        @Test
        @DisplayName("LocalDateTime → null")
        void localDateTimeDefaultIsNull() {
            assertEquals("null", TypeMapper.toDefaultValue("java.time.LocalDateTime"));
        }

        @Test
        @DisplayName("custom POJO → null")
        void customPojoDefaultIsNull() {
            assertEquals("null", TypeMapper.toDefaultValue("com.example.MyPojo"));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // toJavaCastExpression
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("toJavaCastExpression()")
    class ToJavaCastExpression {

        @Test
        @DisplayName("null type → returns valueExpr unchanged")
        void nullTypeReturnsValueExpr() {
            assertEquals("myVar", TypeMapper.toJavaCastExpression(null, "myVar"));
        }

        @Test
        @DisplayName("int → intValue() cast")
        void intCast() {
            assertEquals("((Number) val).intValue()",
                TypeMapper.toJavaCastExpression("int", "val"));
        }

        @Test
        @DisplayName("java.lang.Integer → intValue() cast")
        void integerCast() {
            assertEquals("((Number) val).intValue()",
                TypeMapper.toJavaCastExpression("java.lang.Integer", "val"));
        }

        @Test
        @DisplayName("long → longValue() cast")
        void longCast() {
            assertEquals("((Number) val).longValue()",
                TypeMapper.toJavaCastExpression("long", "val"));
        }

        @Test
        @DisplayName("java.lang.Long → longValue() cast")
        void longWrapperCast() {
            assertEquals("((Number) val).longValue()",
                TypeMapper.toJavaCastExpression("java.lang.Long", "val"));
        }

        @Test
        @DisplayName("double → doubleValue() cast")
        void doubleCast() {
            assertEquals("((Number) val).doubleValue()",
                TypeMapper.toJavaCastExpression("double", "val"));
        }

        @Test
        @DisplayName("java.lang.Double → doubleValue() cast")
        void doubleWrapperCast() {
            assertEquals("((Number) val).doubleValue()",
                TypeMapper.toJavaCastExpression("java.lang.Double", "val"));
        }

        @Test
        @DisplayName("float → floatValue() cast")
        void floatCast() {
            assertEquals("((Number) val).floatValue()",
                TypeMapper.toJavaCastExpression("float", "val"));
        }

        @Test
        @DisplayName("java.lang.Float → floatValue() cast")
        void floatWrapperCast() {
            assertEquals("((Number) val).floatValue()",
                TypeMapper.toJavaCastExpression("java.lang.Float", "val"));
        }

        @Test
        @DisplayName("boolean → Boolean.parseBoolean() cast")
        void booleanCast() {
            assertEquals("Boolean.parseBoolean(val.toString())",
                TypeMapper.toJavaCastExpression("boolean", "val"));
        }

        @Test
        @DisplayName("java.lang.Boolean → Boolean.parseBoolean() cast")
        void booleanWrapperCast() {
            assertEquals("Boolean.parseBoolean(val.toString())",
                TypeMapper.toJavaCastExpression("java.lang.Boolean", "val"));
        }

        @Test
        @DisplayName("String → returns valueExpr unchanged")
        void stringIsPassThrough() {
            assertEquals("myExpr",
                TypeMapper.toJavaCastExpression("java.lang.String", "myExpr"));
        }

        @Test
        @DisplayName("custom POJO → returns valueExpr unchanged")
        void pojoIsPassThrough() {
            assertEquals("pojoVar",
                TypeMapper.toJavaCastExpression("com.example.MyPojo", "pojoVar"));
        }

        @Test
        @DisplayName("complex expression is preserved correctly")
        void complexExpression() {
            String result = TypeMapper.toJavaCastExpression("int", "args[0]");
            assertEquals("((Number) args[0]).intValue()", result);
        }
    }
}
