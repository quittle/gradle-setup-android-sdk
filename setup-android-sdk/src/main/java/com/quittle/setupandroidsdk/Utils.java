package com.quittle.setupandroidsdk;

import java.util.Optional;

/**
 * Contains utility methods
 */
final class Utils {
    /**
     * Attempts to grab a constant field from a class via reflection
     * @param <ConstantType> The type of the constant.
     * @param className The fully qualified name of the class the constant is on
     * @param fieldName The constant name
     * @param clazz The type of the constant
     * @return An optional containing the value of the constant if it exists, otherwise returns an
     *         empty optional. Never returns null.
     */
    @SuppressWarnings("unchecked")
    static <ConstantType> Optional<ConstantType> getConstantViaReflection(
            final String className, final String fieldName, final Class<ConstantType> clazz) {
        try {
            final Object o = Class.forName(className).getDeclaredField(fieldName).get(null);
            if (clazz.isInstance(o)) {
                return Optional.of((ConstantType) o);
            } else {
                return Optional.empty();
            }
        } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException e) {
            return Optional.empty();
        }
    }

    private Utils() {}
}
