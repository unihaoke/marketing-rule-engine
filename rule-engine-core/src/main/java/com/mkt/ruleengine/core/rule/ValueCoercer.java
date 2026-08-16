package com.mkt.ruleengine.core.rule;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 值强转工具：将叶子条件阈值/字段值按 {@link ValueType} 归一化，统一比较语义。
 */
public final class ValueCoercer {

    private static final DateTimeFormatter[] ISO_FORMATTERS = {
            DateTimeFormatter.ISO_OFFSET_DATE_TIME,
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ISO_INSTANT,
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd")
    };

    private ValueCoercer() {
    }

    /** 按目标类型强转 */
    public static Object coerce(Object raw, ValueType target) {
        if (raw == null) {
            return null;
        }
        return switch (target) {
            case STRING -> String.valueOf(raw);
            case NUMBER -> toBigDecimal(raw);
            case BOOLEAN -> toBoolean(raw);
            case DATETIME -> toEpochMillis(raw);
            case LIST -> toList(raw);
        };
    }

    public static BigDecimal toBigDecimal(Object raw) {
        if (raw instanceof BigDecimal bd) {
            return bd;
        }
        if (raw instanceof Number n) {
            return new BigDecimal(n.toString());
        }
        if (raw instanceof Boolean b) {
            return b ? BigDecimal.ONE : BigDecimal.ZERO;
        }
        if (raw instanceof String s) {
            String t = s.trim();
            if (t.isEmpty()) {
                return null;
            }
            return new BigDecimal(t);
        }
        throw new IllegalArgumentException("cannot coerce to NUMBER: " + raw);
    }

    public static Boolean toBoolean(Object raw) {
        if (raw instanceof Boolean b) {
            return b;
        }
        if (raw instanceof Number n) {
            return n.doubleValue() != 0;
        }
        if (raw instanceof String s) {
            String t = s.trim().toLowerCase();
            return "true".equals(t) || "1".equals(t) || "yes".equals(t);
        }
        return null;
    }

    /** 任意日期表示 → 纪元毫秒 */
    public static Long toEpochMillis(Object raw) {
        if (raw instanceof Number n) {
            return n.longValue();
        }
        if (raw instanceof Instant i) {
            return i.toEpochMilli();
        }
        if (raw instanceof java.util.Date d) {
            return d.getTime();
        }
        if (raw instanceof LocalDateTime ldt) {
            return ldt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        }
        if (raw instanceof OffsetDateTime odt) {
            return odt.toInstant().toEpochMilli();
        }
        if (raw instanceof String s) {
            String t = s.trim();
            if (t.isEmpty()) {
                return null;
            }
            // 纯数字视为毫秒时间戳
            if (t.matches("\\d{10,13}")) {
                return Long.parseLong(t);
            }
            for (DateTimeFormatter fmt : ISO_FORMATTERS) {
                try {
                    if (fmt == DateTimeFormatter.ISO_INSTANT) {
                        return Instant.parse(t).toEpochMilli();
                    }
                    if (fmt == DateTimeFormatter.ISO_OFFSET_DATE_TIME) {
                        return OffsetDateTime.parse(t, fmt).toInstant().toEpochMilli();
                    }
                    return LocalDateTime.parse(t, fmt).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                } catch (Exception ignored) {
                    // try next
                }
            }
            throw new IllegalArgumentException("cannot parse DATETIME: " + raw);
        }
        throw new IllegalArgumentException("cannot coerce to DATETIME: " + raw);
    }

    @SuppressWarnings("unchecked")
    public static List<Object> toList(Object raw) {
        if (raw instanceof List<?> list) {
            return new ArrayList<>(list);
        }
        if (raw instanceof Collection<?> col) {
            return new ArrayList<>(col);
        }
        if (raw instanceof Object[] arr) {
            return new ArrayList<>(List.of(arr));
        }
        if (raw instanceof String s) {
            String t = s.trim();
            if (t.startsWith("[") && t.endsWith("]")) {
                t = t.substring(1, t.length() - 1);
            }
            if (t.isEmpty()) {
                return new ArrayList<>();
            }
            List<Object> result = new ArrayList<>();
            for (String part : t.split(",")) {
                result.add(part.trim());
            }
            return result;
        }
        return new ArrayList<>(List.of(raw));
    }

    /** 值相等比较（数字按 BigDecimal，其余按 toString） */
    public static boolean valueEquals(Object a, Object b) {
        if (a == null || b == null) {
            return a == b;
        }
        if (a instanceof Number na && b instanceof Number nb) {
            return toBigDecimal(na).compareTo(toBigDecimal(nb)) == 0;
        }
        return String.valueOf(a).equals(String.valueOf(b));
    }

    /** 比较两个值（数字/日期/字符串），返回 -1/0/1 */
    public static int compare(Object a, Object b) {
        if (a instanceof Number na && b instanceof Number nb) {
            return toBigDecimal(na).compareTo(toBigDecimal(nb));
        }
        if (a instanceof Boolean ba && b instanceof Boolean bb) {
            return Boolean.compare(ba, bb);
        }
        return String.valueOf(a).compareTo(String.valueOf(b));
    }

    /** 是否可比较（数值或同为数值/字符串形态） */
    public static boolean isComparable(Object a, Object b) {
        return (a instanceof Number && b instanceof Number)
                || (!(a instanceof Number) && !(b instanceof Number) && !(a instanceof Boolean) && !(b instanceof Boolean));
    }

    /** Map/List 转字符串（调试/日志用） */
    public static String stringify(Object value) {
        if (value instanceof Map<?, ?> || value instanceof Collection<?>) {
            return String.valueOf(value);
        }
        return String.valueOf(value);
    }
}
