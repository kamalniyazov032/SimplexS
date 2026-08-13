package az.simplexs.simplexs.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import az.simplexs.simplexs.dto.klinika.KlinikaListItem;
import az.simplexs.simplexs.repository.klinika.KlinikaRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.DispatcherType;

@Controller
public class KlinikaController {
    public static final String SELECTED_KLINIKA_ID = "selectedKlinikaId";

    private final KlinikaRepository klinikaRepository;

    public KlinikaController(KlinikaRepository klinikaRepository) {
        this.klinikaRepository = klinikaRepository;
    }

    @PostMapping("/klinika-sec")
    public String select(
        @RequestParam Long klinikaId,
        @RequestParam(defaultValue = "/dashboard") String redirectUrl,
        HttpSession session
    ) {
        boolean isActiveClinic = klinikaRepository.findAll().stream()
            .anyMatch(klinika -> Boolean.TRUE.equals(klinika.aktiv()) && klinika.klinikaId().equals(klinikaId));

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

    KlinikaHeaderAdvice(KlinikaRepository klinikaRepository) {
        this.klinikaRepository = klinikaRepository;
    }

    @ModelAttribute
    void addKlinikalar(Model model, HttpServletRequest request, HttpSession session) {
        if ("/login".equals(request.getRequestURI()) || "/error".equals(request.getRequestURI())
                || request.getDispatcherType() == DispatcherType.ERROR) {
            return;
        }

        List<KlinikaListItem> aktivKlinikalar = klinikaRepository.findAll().stream()
            .filter(klinika -> Boolean.TRUE.equals(klinika.aktiv()))
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
    }
}
