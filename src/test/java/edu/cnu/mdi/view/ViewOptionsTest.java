package edu.cnu.mdi.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

import edu.cnu.mdi.util.PropertyUtils;

class ViewOptionsTest {

    @Test
    void builderProducesDefensiveImmutableSnapshot() {
        ViewPropertiesBuilder builder = new ViewPropertiesBuilder()
                .title("First")
                .width(640)
                .useContainer(false);
        ViewOptions options = builder.buildOptions();
        builder.title("Second");

        var firstCopy = options.toProperties();
        assertEquals("First", firstCopy.get(PropertyUtils.TITLE));
        assertEquals(640, firstCopy.get(PropertyUtils.WIDTH));
        assertEquals(false, firstCopy.get(PropertyUtils.USECONTAINER));

        firstCopy.put(PropertyUtils.TITLE, "Mutated");
        assertEquals("First", options.toProperties().get(PropertyUtils.TITLE));
        assertFalse(options.toProperties().isEmpty());
    }
}
