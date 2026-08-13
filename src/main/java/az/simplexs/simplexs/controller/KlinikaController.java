package az.simplexs.simplexs.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.core.Authentication;

import az.simplexs.simplexs.dto.klinika.KlinikaListItem;
import az.simplexs.simplexs.repository.klinika.KlinikaRepository;
import az.simplexs.simplexs.security.AccessService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.DispatcherType;

@Controller
public class KlinikaController {
    public static final String SELECTED_KLINIKA_ID = "selectedKlinikaId";

    private final KlinikaRepository klinikaRepository;
    private final AccessService accessService;

    public KlinikaController(KlinikaRepository klinikaRepository, AccessService accessService) {
        this.klinikaRepository = klinikaRepository;
        this.accessService = accessService;
    }

    @PostMapping("/klinika-sec")
    public String select(
        @RequestParam Long klinikaId,
        @RequestParam(defaultValue = "/dashboard") String redirectUrl,
        HttpSession session,
        Authentication authentication
    ) {
        boolean isActiveClinic = klinikaRepository.findAll().stream()
            .anyMatch(klinika -> Boolean.TRUE.equals(klinika.aktiv()) && klinika.klinikaId().equals(klinikaId))
            && accessService.hasClinic(authentication, klinikaId);

        if (isActiveClinic) {
            session.setAttribute(SELECTED_KLINIKA_ID, klinikaId);
        }

        String safeRedirect = redirectUrl.startsWith("/") && !redirectUrl.startsWith("//")
            ? redirectUrl
            : "/dashboard";
        return "redirect:" + safeRedirect;
    }
}

@ControllerAdvice
class KlinikaHeaderAdvice {
    private final KlinikaRepository klinikaRepository;
    private final AccessService accessService;

    KlinikaHeaderAdvice(KlinikaRepository klinikaRepository, AccessService accessService) {
        this.klinikaRepository = klinikaRepository;
        this.accessService = accessService;
    }

    @ModelAttribute
    void addKlinikalar(Model model, HttpServletRequest request, HttpSession session, Authentication authentication) {
        if ("/login".equals(request.getRequestURI()) || "/error".equals(request.getRequestURI())
                || request.getDispatcherType() == DispatcherType.ERROR) {
            return;
        }

        List<Long> icazeliKlinikaIdleri = accessService.clinicIds(authentication);
        List<KlinikaListItem> aktivKlinikalar = klinikaRepository.findAll().stream()
            .filter(klinika -> Boolean.TRUE.equals(klinika.aktiv()) && icazeliKlinikaIdleri.contains(klinika.klinikaId()))
            .toList();

        Long selectedId = (Long) session.getAttribute(KlinikaController.SELECTED_KLINIKA_ID);
        Long sessionSelectedId = selectedId;
        boolean selectionIsValid = sessionSelectedId != null && aktivKlinikalar.stream()
            .anyMatch(klinika -> klinika.klinikaId().equals(sessionSelectedId));

        if (!selectionIsValid) {
            selectedId = aktivKlinikalar.isEmpty() ? null : aktivKlinikalar.getFirst().klinikaId();
            if (selectedId != null) {
                session.setAttribute(KlinikaController.SELECTED_KLINIKA_ID, selectedId);
            } else {
                session.removeAttribute(KlinikaController.SELECTED_KLINIKA_ID);
            }
        }

        model.addAttribute("klinikalar", aktivKlinikalar);
        model.addAttribute("selectedKlinikaId", selectedId);
        model.addAttribute("currentRequestUri", request.getRequestURI());
        model.addAttribute("userModules", accessService.menu(authentication, selectedId));
    }
}
