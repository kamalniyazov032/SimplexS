package az.simplexs.simplexs.repository.kassa;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class KassaAdvanceRepositoryTests {
    @Test void fullNameSearchUsesFirstWordAndMatchesEveryWordAcrossFields() {
        var jdbc=mock(NamedParameterJdbcTemplate.class);
        new KassaEmeliyyatRepository(jdbc).advancePatients(1L,null," Kamal  Niyazov ",null,null,1);
        var sql=ArgumentCaptor.forClass(String.class);
        var params=ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbc).queryForList(sql.capture(),params.capture());
        assertThat(params.getValue().getValue("query")).isEqualTo("Kamal");
        assertThat(params.getValue().getValue("term1")).isEqualTo("Niyazov");
        assertThat(sql.getValue()).contains("fn_xeste_avanslar_ucun_siyahisi",":term0",":term1","WHERE aktiv=true","LIMIT 101 OFFSET :offset").doesNotContain("Kamal");
    }
    @Test void searchTreatsWildcardCharactersAsLiteralCardText() {
        var jdbc=mock(NamedParameterJdbcTemplate.class);
        new KassaEmeliyyatRepository(jdbc).advancePatients(1L,"AMBULATOR","A%_01",null,null,1);
        var params=ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbc).queryForList(anyString(),params.capture());
        assertThat(params.getValue().getValue("query")).isEqualTo("A\\%\\_01");
        assertThat(params.getValue().getValue("term0")).isEqualTo("A%_01");
    }
    @Test void paginationSkipsPreviouslyDisplayedRowsAndFetchesOneExtra() {
        var jdbc=mock(NamedParameterJdbcTemplate.class);
        new KassaEmeliyyatRepository(jdbc).advancePatients(1L,null,"Kamal",null,null,3);
        var params=ArgumentCaptor.forClass(MapSqlParameterSource.class);
        var sql=ArgumentCaptor.forClass(String.class);
        verify(jdbc).queryForList(sql.capture(),params.capture());
        assertThat(params.getValue().getValue("offset")).isEqualTo(200L);
        assertThat(sql.getValue()).contains("gelis_id DESC LIMIT 101 OFFSET :offset");
    }
}
