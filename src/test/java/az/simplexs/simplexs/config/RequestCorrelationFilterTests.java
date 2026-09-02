package az.simplexs.simplexs.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestCorrelationFilterTests {
    @Test
    void exposesOneRequestIdAndAlwaysClearsMdc() throws Exception {
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        var observedId = new AtomicReference<String>();

        new RequestCorrelationFilter().doFilter(request, response, (req, res) ->
                observedId.set(MDC.get(RequestCorrelationFilter.MDC_KEY)));

        assertThat(observedId.get()).hasSize(12).matches("[0-9A-F]+");
        assertThat(request.getAttribute(RequestCorrelationFilter.REQUEST_ATTRIBUTE)).isEqualTo(observedId.get());
        assertThat(response.getHeader(RequestCorrelationFilter.RESPONSE_HEADER)).isEqualTo(observedId.get());
        assertThat(MDC.get(RequestCorrelationFilter.MDC_KEY)).isNull();
    }
}
