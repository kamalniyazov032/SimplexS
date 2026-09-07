package az.simplexs.simplexs.repository.qiymet;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.aop.framework.ProxyFactory;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

class QiymetRepositoryTests {
    @Test
    void sendsJsonToNewFunctionAndRollsBackFailure() {
        var jdbc = mock(NamedParameterJdbcTemplate.class);
        when(jdbc.queryForList(any(String.class), any(MapSqlParameterSource.class)))
                .thenReturn(List.of(Map.of("status_kodu", "PAY_MELUMATI_YANLISDIR")));
        var manager = mock(PlatformTransactionManager.class);
        var status = mock(TransactionStatus.class);
        when(manager.getTransaction(any(TransactionDefinition.class))).thenReturn(status);
        var proxy = new ProxyFactory(new QiymetRepository(jdbc));
        proxy.addAdvice(new TransactionInterceptor(manager, new AnnotationTransactionAttributeSource()));
        var repository = (QiymetRepository) proxy.getProxy();
        String json = "[{\"xidmet_id\":10,\"qiymet\":50,\"edv_aktivdir\":false,\"xeste_payi\":30,\"sigorta_payi\":70,\"xeste_endirim\":10,\"sigorta_endirim\":5}]";
        repository.qiymetleriSaxla(4L, json, 17L);
        var sql = ArgumentCaptor.forClass(String.class);
        var params = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbc).queryForList(sql.capture(), params.capture());
        assertThat(sql.getValue()).contains("fn_xidmet_qiymetlerini_toplu_yenile", "p_xidmetler=>CAST(:json AS jsonb)");
        assertThat(params.getValue().getValue("json")).isEqualTo(json);
        assertThat(params.getValue().getValue("cedvel")).isEqualTo(4L);
        assertThat(params.getValue().getValue("personal")).isEqualTo(17L);
        verify(status).setRollbackOnly();
    }
}
