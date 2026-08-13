package az.simplexs.simplexs.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LegacyUpgradingPasswordEncoderTests {
    private final LegacyUpgradingPasswordEncoder encoder = new LegacyUpgradingPasswordEncoder();

    @Test
    void acceptsLegacyPasswordAndRequestsUpgrade() {
        assertThat(encoder.matches("legacy-secret", "legacy-secret")).isTrue();
        assertThat(encoder.matches("wrong", "legacy-secret")).isFalse();
        assertThat(encoder.upgradeEncoding("legacy-secret")).isTrue();
    }

    @Test
    void createsAndValidatesBcryptPassword() {
        String encoded = encoder.encode("strong-secret");
        assertThat(encoded).startsWith("{bcrypt}");
        assertThat(encoder.matches("strong-secret", encoded)).isTrue();
        assertThat(encoder.upgradeEncoding(encoded)).isFalse();
    }
}
