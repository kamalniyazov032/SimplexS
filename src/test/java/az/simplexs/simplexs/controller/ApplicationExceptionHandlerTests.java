package az.simplexs.simplexs.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;

import org.junit.jupiter.api.Test;

class ApplicationExceptionHandlerTests {
    @Test
    void findsDeepestRootCause() {
        var databaseCause = new SQLException("real database cause");
        var exception = new IllegalStateException("wrapper", databaseCause);

        assertThat(ApplicationExceptionHandler.rootCause(exception)).isSameAs(databaseCause);
    }

    @Test
    void findsFirstApplicationFrameInsteadOfFrameworkFrame() {
        var exception = new IllegalStateException("failure");
        exception.setStackTrace(new StackTraceElement[] {
            new StackTraceElement("org.springframework.web.DispatcherServlet", "service", "DispatcherServlet.java", 1),
            new StackTraceElement("az.simplexs.simplexs.repository.xidmet.XidmetRepository", "paketler", "XidmetRepository.java", 146)
        });

        var location = ApplicationExceptionHandler.applicationLocation(exception);

        assertThat(location.className()).isEqualTo("XidmetRepository");
        assertThat(location.methodName()).isEqualTo("paketler");
    }
}
