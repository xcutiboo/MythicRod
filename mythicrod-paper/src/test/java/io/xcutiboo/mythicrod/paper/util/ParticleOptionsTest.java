package io.xcutiboo.mythicrod.paper.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.Particle;
import org.junit.jupiter.api.Test;

class ParticleOptionsTest {

    @Test
    void suggestedParticlesAreValidPaperParticleNames() {
        for (String particleName : ParticleOptions.suggestedNames()) {
            assertTrue(ParticleOptions.isConfigurableParticleName(particleName), particleName);
        }
    }

    @Test
    void exposesEveryPaperParticleNameForCommandSuggestions() {
        assertTrue(ParticleOptions.configurableNames().contains("SPLASH"));
        assertTrue(ParticleOptions.configurableNames().contains("HAPPY_VILLAGER"));
        assertEquals(ParticleOptions.configurableNames().stream().sorted().toList(), ParticleOptions.configurableNames());
    }

    @Test
    void everyCurrentPaperParticleHasSafeConfiguredDataHandling() {
        for (Particle particle : Particle.values()) {
            assertTrue(ParticleOptions.isConfigurableParticleName(particle.name()), particle.name());
        }
    }

    @Test
    void normalizesCommandInputBeforeValidation() {
        assertTrue(ParticleOptions.isConfigurableParticleName(" happy_villager "));
        assertFalse(ParticleOptions.isConfigurableParticleName("not_a_particle"));
    }

    @Test
    void cyclesUnknownValuesBackToFirstSuggestion() {
        assertEquals("SPLASH", ParticleOptions.nextSuggested("not_a_particle"));
    }
}
