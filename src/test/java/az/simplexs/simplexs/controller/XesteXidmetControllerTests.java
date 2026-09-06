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
}
