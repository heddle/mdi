package edu.cnu.mdi.view;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

class JsonViewTest {

    @Test
    void defaultConstructorPolicyIsInitiallyHidden() {
        assertFalse(JsonView.DEFAULT_VISIBLE);
    }
}
