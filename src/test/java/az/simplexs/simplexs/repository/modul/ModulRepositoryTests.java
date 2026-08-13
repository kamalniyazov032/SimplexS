package az.simplexs.simplexs.repository.modul;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

class ModulRepositoryTests {
    @Test
    void rejectsModuleAsItsOwnParentWithoutDatabaseCall() {
        var jdbc = mock(NamedParameterJdbcTemplate.class);
        var repository = new ModulRepository(jdbc);
        var result = repository.update(7L, 7L, "Modul", null, null, 1, true, true);
        assertThat(result.get("status_kodu")).isEqualTo("XETA");
        verifyNoInteractions(jdbc);
    }
}
