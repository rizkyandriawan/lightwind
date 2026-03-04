package dev.kakrizky.lightwind.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class BeanUtil {

    private BeanUtil() {}

    /**
     * Copies only non-null properties from source to target (for PATCH/merge semantics).
     * Null values in source are treated as "not set" and skipped.
     */
    public static void copyNonNullProperties(Object source, Object target, String... ignoreProperties) {
        if (source == null || target == null) return;

        Set<String> ignoreSet = new HashSet<>(Arrays.asList(ignoreProperties));

        Class<?> targetClass = target.getClass();
        while (targetClass != null && targetClass != Object.class) {
            for (Field targetField : targetClass.getDeclaredFields()) {
                String name = targetField.getName();
                if (ignoreSet.contains(name)) continue;

                try {
                    Object value = getFieldValue(source, name, targetField.getType());
                    if (value != null) {
                        setFieldValue(target, name, targetField, value);
                    }
                } catch (Exception ignored) {
                }
            }
            targetClass = targetClass.getSuperclass();
        }
    }

    public static void copyProperties(Object source, Object target, String... ignoreProperties) {
        if (source == null || target == null) return;

        Set<String> ignoreSet = new HashSet<>(Arrays.asList(ignoreProperties));

        Class<?> targetClass = target.getClass();
        while (targetClass != null && targetClass != Object.class) {
            for (Field targetField : targetClass.getDeclaredFields()) {
                String name = targetField.getName();
                if (ignoreSet.contains(name)) continue;

                try {
                    Object value = getFieldValue(source, name, targetField.getType());
                    if (value != null || hasField(source.getClass(), name)) {
                        setFieldValue(target, name, targetField, value);
                    }
                } catch (Exception ignored) {
                }
            }
            targetClass = targetClass.getSuperclass();
        }
    }

    private static Object getFieldValue(Object source, String fieldName, Class<?> expectedType) {
        // Try getter first
        Method getter = findGetter(source.getClass(), fieldName);
        if (getter != null) {
            try {
                return getter.invoke(source);
            } catch (Exception ignored) {
            }
        }

        // Fall back to direct field access
        Field field = findField(source.getClass(), fieldName);
        if (field != null) {
            try {
                field.setAccessible(true);
                return field.get(source);
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private static void setFieldValue(Object target, String fieldName, Field targetField, Object value) {
        // Try setter first (important for Hibernate dirty tracking)
        Method setter = findSetter(target.getClass(), fieldName, targetField.getType());
        if (setter != null) {
            try {
                setter.invoke(target, value);
                return;
            } catch (Exception ignored) {
            }
        }

        // Fall back to direct field access
        try {
            targetField.setAccessible(true);
            if (value == null || targetField.getType().isAssignableFrom(value.getClass())) {
                targetField.set(target, value);
            }
        } catch (Exception ignored) {
        }
    }

    private static boolean hasField(Class<?> clazz, String name) {
        return findField(clazz, name) != null;
    }

    private static Method findGetter(Class<?> clazz, String fieldName) {
        String capitalized = Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            try {
                return current.getMethod("get" + capitalized);
            } catch (NoSuchMethodException e) {
                try {
                    return current.getMethod("is" + capitalized);
                } catch (NoSuchMethodException e2) {
                    current = current.getSuperclass();
                }
            }
        }
        return null;
    }

    private static Method findSetter(Class<?> clazz, String fieldName, Class<?> fieldType) {
        String capitalized = Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            try {
                return current.getMethod("set" + capitalized, fieldType);
            } catch (NoSuchMethodException e) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private static Field findField(Class<?> clazz, String name) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        return null;
    }
}
