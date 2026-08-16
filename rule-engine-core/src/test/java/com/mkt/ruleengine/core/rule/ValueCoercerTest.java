package com.mkt.ruleengine.core.rule;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 值强转测试。
 */
class ValueCoercerTest {

    @Test
    void numberCoercion() {
        assertEquals(new BigDecimal("100"), ValueCoercer.toBigDecimal("100"));
        assertEquals(new BigDecimal("100.5"), ValueCoercer.toBigDecimal(100.5));
    }

    @Test
    void datetimeCoercion() {
        long expected = LocalDateTime.parse("2024-06-01T12:00:00")
                .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        assertEquals(expected, ValueCoercer.toEpochMillis("2024-06-01 12:00:00"));
        assertEquals(expected, ValueCoercer.toEpochMillis("2024-06-01T12:00:00"));
    }

    @Test
    void listCoercion() {
        assertEquals(3, ValueCoercer.toList("[a, b, c]").size());
        assertEquals(2, ValueCoercer.toList(List.of(1, 2)).size());
    }

    @Test
    void valueEqualsIgnoresNumericType() {
        assertTrue(ValueCoercer.valueEquals(100, 100L));
        assertTrue(ValueCoercer.valueEquals(100, "100"));
        assertTrue(ValueCoercer.valueEquals(new BigDecimal("1.0"), 1));
    }
}
