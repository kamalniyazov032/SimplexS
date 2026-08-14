package az.simplexs.simplexs.repository.modul;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

class ModulRepositoryTests {
    @Test
    void delegatesHierarchyValidationToDatabaseFunction() {
        var jdbc = mock(NamedParameterJdbcTemplate.class);
        when(jdbc.queryForMap(anyString(), any(MapSqlParameterSource.class)))
                .thenReturn(Map.of("status_kodu","XETA","mesaj","Xəta"));
        var repository = new ModulRepository(jdbc);
        var result = repository.update(7L, 1L, 7L, "Modul", null, null, 1, true, true);
        assertThat(result.get("status_kodu")).isEqualTo("XETA");
    }
}
