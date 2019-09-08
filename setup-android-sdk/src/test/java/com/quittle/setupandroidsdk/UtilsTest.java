package com.quittle.setupandroidsdk;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import static org.junit.jupiter.api.Assertions.*;

class UtilsTest {
    private static final String CLASS_NAME = "com.quittle.setupandroidsdk.Data";
    private static final String FIELD_NAME = "CONSTANT";
    private static final Class<?> CLASS = String.class;

    @Test
    void verifyConstants() {
        assertEquals(CLASS_NAME, Data.class.getName());
        assertDoesNotThrow(() -> Data.class.getDeclaredField(FIELD_NAME));
        assertSame(CLASS, Data.CONSTANT.getClass());
    }

    @Test
    @SuppressFBWarnings("ES_COMPARING_STRINGS_WITH_EQ")
    void testGetConstantViaReflection_happyCase() {
        assertSame(Data.CONSTANT, Utils.getConstantViaReflection(CLASS_NAME, FIELD_NAME, CLASS).get());
    }

    @Test
    void testGetConstantViaReflection_wrongType() {
        assertEquals(Optional.empty(), Utils.getConstantViaReflection(CLASS_NAME, FIELD_NAME, Map.class));
    }

    @Test
    void testGetConstantViaReflection_unknownClassAndField() {
        assertEquals(Optional.empty(), Utils.getConstantViaReflection("com.fake.package.ClassName", FIELD_NAME, CLASS));
        assertEquals(Optional.empty(), Utils.getConstantViaReflection(CLASS_NAME, "MADE_UP_FIELD", CLASS));
    }

    @Test
    void testGetConstantViaReflection_privateConstant() {
        assertEquals(Optional.empty(), Utils.getConstantViaReflection(CLASS_NAME, "PRIVATE_CONSTANT", CLASS));
    }
}