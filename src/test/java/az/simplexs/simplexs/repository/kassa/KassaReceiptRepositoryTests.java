package az.simplexs.simplexs.repository.kassa;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class KassaReceiptRepositoryTests {
    @Test void filtersReceiptStatusInsteadOfActiveFlagBeforePagination() {
        var jdbc=mock(NamedParameterJdbcTemplate.class);
        new KassaEmeliyyatRepository(jdbc).allReceipts(1L,7L,"",null,null,"","","LEGV_EDILIB",2);
        var sql=ArgumentCaptor.forClass(String.class);
        var params=ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbc).queryForList(sql.capture(),params.capture());
        assertThat(sql.getValue()).contains(":status='' OR status=:status", "LIMIT 101 OFFSET :offset").doesNotContain(":active");
        assertThat(params.getValue().getValue("status")).isEqualTo("LEGV_EDILIB");
        assertThat(params.getValue().getValue("offset")).isEqualTo(100L);
    }
    @Test void totalsUseEveryFilteredRowAndSignedAmountsIncludingReversals() {
        var jdbc=mock(NamedParameterJdbcTemplate.class);
        new KassaEmeliyyatRepository(jdbc).receiptTotals(1L,7L,"A17",null,null,"","","");
        var sql=ArgumentCaptor.forClass(String.class);
        var params=ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbc).queryForMap(sql.capture(),params.capture());
        assertThat(sql.getValue()).contains("WHEN istiqamet='GIRIS' THEN mebleg", "WHEN istiqamet='CIXIS' THEN -mebleg",
                ":status='' OR status=:status", "protokol_kodu", "COALESCE(SUM")
                .doesNotContain("LIMIT", "OFFSET", "status='TAMAMLANIB'");
        assertThat(params.getValue().getValue("query")).isEqualTo("A17");
        assertThat(params.getValue().getValue("status")).isEqualTo("");
    }
}
