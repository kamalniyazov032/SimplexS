package az.simplexs.simplexs.repository.xidmet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

class XidmetRepositoryTests {
    @Test
    void packageListUsesTheFourParameterFunction() {
        var jdbc = mock(NamedParameterJdbcTemplate.class);
        var repository = new XidmetRepository(jdbc);

        repository.paketler(1L, true, null);

        var sql = ArgumentCaptor.forClass(String.class);
        verify(jdbc).query(sql.capture(), any(MapSqlParameterSource.class), any(RowMapper.class));
        assertThat(sql.getValue())
                .contains("CAST(NULL AS bigint)")
                .contains("CAST(:aktiv AS boolean)")
                .doesNotContain("CAST(:dil AS varchar)")
                .doesNotContain("CAST(NULL AS integer)");
    }
}
