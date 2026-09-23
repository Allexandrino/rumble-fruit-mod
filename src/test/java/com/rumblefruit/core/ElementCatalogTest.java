package com.rumblefruit.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// the elemental catalog: five elements, stable ids, wrapping lookup, cycling
class ElementCatalogTest {

    @Test
    void thereAreExactlyFiveElements() {
        assertEquals(5, ElementCatalog.COUNT);
        assertEquals(5, ElementCatalog.values().length);
    }

    @Test
    void everyElementHasAUniqueIdAndColor() {
        for (ElementCatalog a : ElementCatalog.values()) {
            for (ElementCatalog b : ElementCatalog.values()) {
                if (a != b) {
                    assertNotEquals(a.id(), b.id());
                    assertNotEquals(a.color(), b.color());
                    assertNotEquals(a.key(), b.key());
                }
            }
        }
    }

    @Test
    void lookupByIdWrapsAroundBothEnds() {
        assertEquals(ElementCatalog.LIGHTNING, ElementCatalog.byId(0));
        assertEquals(ElementCatalog.NATURE, ElementCatalog.byId(4));
        assertEquals(ElementCatalog.LIGHTNING, ElementCatalog.byId(5));
        assertEquals(ElementCatalog.INFERNO, ElementCatalog.byId(6));
        assertEquals(ElementCatalog.NATURE, ElementCatalog.byId(-1));
        assertEquals(ElementCatalog.VOID, ElementCatalog.byId(-3));
    }

    @Test
    void theCycleWalksThroughEveryElementAndBack() {
        ElementCatalog e = ElementCatalog.LIGHTNING;
        assertEquals(ElementCatalog.INFERNO, e.next());
        assertEquals(ElementCatalog.VOID, e.next().next());
        assertEquals(ElementCatalog.FROST, e.next().next().next());
        assertEquals(ElementCatalog.NATURE, e.next().next().next().next());
        assertEquals(ElementCatalog.LIGHTNING, e.next().next().next().next().next());
    }

    @Test
    void onlyLightningIsLightning() {
        assertTrue(ElementCatalog.LIGHTNING.isLightning());
        for (ElementCatalog e : ElementCatalog.values()) {
            assertEquals(e == ElementCatalog.LIGHTNING, e.isLightning());
        }
    }
}
