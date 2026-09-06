package az.simplexs.simplexs.controller;

import az.simplexs.simplexs.controller.ambulator.AmbulatorController;
import az.simplexs.simplexs.repository.ambulator.AmbulatorRepository;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import org.springframework.ui.ExtendedModelMap;
import java.time.LocalDate;
import java.time.ZoneId;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class AmbulatorDateFilterTests {
    @Test
    void initialPageFiltersRowsAndCountByToday() {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Baku"));
        check(null, null, today, today);
    }

    @Test
    void userSelectedRangeIsPreserved() {
        LocalDate from = LocalDate.of(2026, 8, 1), to = LocalDate.of(2026, 8, 31);
        check(from, to, from, to);
        check(from, null, from, null);
    }

    private void check(LocalDate from, LocalDate to, LocalDate expectedFrom, LocalDate expectedTo) {
        var repo = mock(AmbulatorRepository.class);
        var session = mock(HttpSession.class);
        when(session.getAttribute(KlinikaController.SELECTED_KLINIKA_ID)).thenReturn(1L);
        var controller = new AmbulatorController(repo, mock(MessageSource.class));
        var model = new ExtendedModelMap();
        controller.list(null, null, null, from, to, null, "aktiv", null, null, null, model, session);
        verify(repo).gelisler(1L, null, null, null, expectedFrom, expectedTo, null, true, null, null, 101);
        verify(repo).gelisSayi(1L, null, null, null, expectedFrom, expectedTo, null, true, null);
        assertEquals(expectedFrom, model.get("tarixBaslama"));
        assertEquals(expectedTo, model.get("tarixBitme"));
    }
}
