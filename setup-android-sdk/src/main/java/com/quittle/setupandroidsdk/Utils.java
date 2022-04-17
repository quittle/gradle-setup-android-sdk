package com.quittle.setupandroidsdk;

import java.io.File;
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

    /**
     * Loops over the input, returning the first file that exists.
     * @param files The files to check the existence of.
     * @return The first file in the input that exists or {@code null} if none of the files exist.
     */
    public static File getFirstFileThatExists(final File... files) {
        for (final File file : files) {
            if (file.exists()) {
                return file;
            }
        }
        return null;
    }

    private Utils() {}
}
