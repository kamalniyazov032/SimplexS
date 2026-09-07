package az.simplexs.simplexs.repository.xidmet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

class XidmetRepositoryTests {
    @Test
    void packageListUsesTheFourParameterFunction() {
        var jdbc = mock(NamedParameterJdbcTemplate.class);
        var repository = new XidmetRepository(jdbc);

        repository.paketler(1L, true, null);

        var sql = ArgumentCaptor.forClass(String.class);
        verify(jdbc).query(sql.capture(), any(MapSqlParameterSource.class), org.mockito.ArgumentMatchers.<org.springframework.jdbc.core.RowMapper<Object>>any());
        assertThat(sql.getValue())
                .contains("CAST(NULL AS bigint)")
                .contains("CAST(:aktiv AS boolean)")
                .doesNotContain("CAST(:dil AS varchar)")
                .doesNotContain("CAST(NULL AS integer)");
    }
    @Test
    void creationPassesDecimalPriceToDatabaseFunction() {
        var jdbc = mock(NamedParameterJdbcTemplate.class);
        var repository = new XidmetRepository(jdbc);
        var price = new java.math.BigDecimal("25.75");
        org.mockito.Mockito.when(jdbc.queryForList(any(String.class), any(MapSqlParameterSource.class)))
                .thenReturn(java.util.List.of(java.util.Map.of("status_kodu", "UGURLU", "xidmet_id", 9L)));

        repository.xidmetYarat(1L, "LAB001", "Service", price, 12L, 4L, 2L,
                null, null, null, null, false, true);

        var sql = ArgumentCaptor.forClass(String.class);
        var parameters = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        org.mockito.Mockito.verify(jdbc, org.mockito.Mockito.times(2))
                .queryForList(sql.capture(), parameters.capture());
        assertThat(sql.getAllValues().getFirst()).contains("fn_xidmet_yarat", "p_qiymet=>:qiymet");
        assertThat(parameters.getAllValues().getFirst().getValue("qiymet")).isEqualTo(price);
    }
}
