package az.simplexs.simplexs.security;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AccessServiceMenuCacheTests {
    private final NamedParameterJdbcTemplate jdbc = mock(NamedParameterJdbcTemplate.class);
    private final AtomicLong time = new AtomicLong();
    private final AccessService access = new AccessService(jdbc, mock(MessageSource.class), time::get);

    @AfterEach
    void resetLocale() { LocaleContextHolder.resetLocaleContext(); }

    private Authentication user(long id) {
        return new UsernamePasswordAuthenticationToken(
                new AuthenticatedPersonal(id, "user" + id, "", "", true, List.of()), "", List.of());
    }

    @Test
    void repeatedRequestsReuseMenuUntilTenMinutesAfterLoad() {
        var user = user(1);
        access.menuSystems(user, 2L);
        time.set(Duration.ofMinutes(9).toNanos());
        access.menuSystems(user, 2L);
        access.menu(user, 2L);
        verify(jdbc, times(1)).query(anyString(), any(SqlParameterSource.class), any(RowMapper.class));
        time.set(Duration.ofMinutes(10).toNanos());
        access.menuSystems(user, 2L);
        verify(jdbc, times(2)).query(anyString(), any(SqlParameterSource.class), any(RowMapper.class));
    }

    @Test
    void cacheSeparatesUsersClinicsAndLanguages() {
        LocaleContextHolder.setLocale(Locale.forLanguageTag("az"));
        access.menuSystems(user(1), 2L);
        access.menuSystems(user(2), 2L);
        access.menuSystems(user(1), 3L);
        LocaleContextHolder.setLocale(Locale.ENGLISH);
        access.menuSystems(user(1), 2L);
        LocaleContextHolder.setLocale(Locale.forLanguageTag("az"));
        access.menuSystems(user(1), 2L);
        verify(jdbc, times(4)).query(anyString(), any(SqlParameterSource.class), any(RowMapper.class));
    }

    @Test
    void missingUserOrClinicDoesNotQueryDatabase() {
        access.menuSystems(null, 2L);
        access.menuSystems(user(1), null);
        verifyNoInteractions(jdbc);
    }
}
