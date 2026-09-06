package az.simplexs.simplexs.controller;

import az.simplexs.simplexs.repository.ambulator.AmbulatorRepository;
import az.simplexs.simplexs.repository.xestexidmet.XesteXidmetRepository;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import java.util.NoSuchElementException;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class XesteXidmetControllerTests {
    @Test
    void inaccessibleVisitCannotListOrCancelServices() {
        var repo = mock(XesteXidmetRepository.class);
        var visits = mock(AmbulatorRepository.class);
        var session = mock(HttpSession.class);
        when(session.getAttribute(KlinikaController.SELECTED_KLINIKA_ID)).thenReturn(2L);
        when(visits.gelis(2L, 17L)).thenThrow(new NoSuchElementException());
        var controller = new XesteXidmetController(repo, visits, mock(MessageSource.class));
        assertThrows(NoSuchElementException.class, () -> controller.siyahi(17L, null, session));
        assertThrows(NoSuchElementException.class, () -> controller.legv(17L, 99L, session, null));
        verifyNoInteractions(repo);
    }

    @Test
    void packageStatusUsesAuthenticatedPersonalAndChecksVisitOwnership() {
        var repo = mock(XesteXidmetRepository.class);
        var visits = mock(AmbulatorRepository.class);
        var session = mock(HttpSession.class);
        when(session.getAttribute(KlinikaController.SELECTED_KLINIKA_ID)).thenReturn(2L);
        var personal = new az.simplexs.simplexs.security.AuthenticatedPersonal(17L, "test", "", "Test", true, java.util.List.of());
        var controller = new XesteXidmetController(repo, visits, mock(MessageSource.class));
        when(repo.secilmisXidmetler(20L)).thenReturn(java.util.List.of(java.util.Map.of("xeste_xidmet_id", 3L)));
        when(repo.paketStatusunuYenile(3L, true, 17L)).thenReturn(java.util.Map.of("status_kodu", "UGURLU"));
        controller.paketStatusu(20L, 3L, true, session, personal);
        verify(visits).gelis(2L, 20L);
        verify(repo).paketStatusunuYenile(3L, true, 17L);
        controller.paketStatusu(20L, 3L, false, session, personal);
        verify(repo).paketStatusunuYenile(3L, false, 17L);
        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> controller.paketStatusu(20L, 99L, true, session, personal));
        verify(repo, never()).paketStatusunuYenile(eq(99L), anyBoolean(), anyLong());
    }
}
