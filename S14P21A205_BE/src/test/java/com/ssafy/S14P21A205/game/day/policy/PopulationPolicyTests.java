package com.ssafy.S14P21A205.game.day.policy;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PopulationPolicyTests {

    private final PopulationPolicy populationPolicy = new PopulationPolicy(null, null);

    @Test
    void resolvePopulationLevelMapsConfiguredRanges() {
        assertThat(populationPolicy.resolvePopulationLevel(20)).isEqualTo(PopulationLevel.VERY_CROWDED);
        assertThat(populationPolicy.resolvePopulationLevel(17)).isEqualTo(PopulationLevel.VERY_CROWDED);
        assertThat(populationPolicy.resolvePopulationLevel(16)).isEqualTo(PopulationLevel.CROWDED);
        assertThat(populationPolicy.resolvePopulationLevel(13)).isEqualTo(PopulationLevel.CROWDED);
        assertThat(populationPolicy.resolvePopulationLevel(12)).isEqualTo(PopulationLevel.NORMAL);
        assertThat(populationPolicy.resolvePopulationLevel(9)).isEqualTo(PopulationLevel.NORMAL);
        assertThat(populationPolicy.resolvePopulationLevel(8)).isEqualTo(PopulationLevel.RELAXED);
        assertThat(populationPolicy.resolvePopulationLevel(5)).isEqualTo(PopulationLevel.RELAXED);
        assertThat(populationPolicy.resolvePopulationLevel(4)).isEqualTo(PopulationLevel.VERY_RELAXED);
        assertThat(populationPolicy.resolvePopulationLevel(0)).isEqualTo(PopulationLevel.VERY_RELAXED);
    }

    @Test
    void resolvePopulationLabelClampsOutOfRangeValues() {
        assertThat(populationPolicy.resolvePopulationLevel(null)).isEqualTo(PopulationLevel.VERY_RELAXED);
        assertThat(populationPolicy.resolvePopulationLevel(-3)).isEqualTo(PopulationLevel.VERY_RELAXED);
        assertThat(populationPolicy.resolvePopulationLevel(99)).isEqualTo(PopulationLevel.VERY_CROWDED);
        assertThat(populationPolicy.resolvePopulationLabel(20)).isEqualTo(PopulationLevel.VERY_CROWDED.label());
    }
}