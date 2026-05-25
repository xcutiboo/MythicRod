package io.xcutiboo.mythicrod.paper.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class StringFormattingTest {

    @Test
    void formatsBiomeCategoryNamesWithoutLeakingUnderscores() {
        assertEquals("Ocean Biome", StringFormatting.formatCategoryName("biome_ocean"));
        assertEquals("Mushroom Fields Biome", StringFormatting.formatCategoryName("biome_mushroom_fields"));
    }

    @Test
    void formatsOrdinaryCategoryNamesAsTitleCase() {
        assertEquals("Global", StringFormatting.formatCategoryName("global"));
        assertEquals("Event Loot", StringFormatting.formatCategoryName("event_loot"));
    }
}
