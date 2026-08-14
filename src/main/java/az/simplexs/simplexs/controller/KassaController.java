package az.simplexs.simplexs.controller;

import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import az.simplexs.simplexs.repository.kassa.KassaRepository;
import az.simplexs.simplexs.security.AuthenticatedPersonal;
import jakarta.servlet.http.HttpSession;

@Controller
public class KassaController {
    private final KassaRepository repository;

    public KassaController(KassaRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/kassalar")
    public String list(@RequestParam(defaultValue = "aktiv") String status,
            Model model, HttpSession session) {
        Long klinikaId = klinikaId(session);
        var kassalar = repository.findAll(klinikaId, statusBoolean(status));
        model.addAttribute("pageTitle", "Kassalar");
        model.addAttribute("activeMenuGroup", "adminPanel");
        model.addAttribute("activeMenu", "kassalar");
        model.addAttribute("kassalar", kassalar);
        model.addAttribute("status", status);
        model.addAttribute("aktivKassaSayi", kassalar.stream().filter(k -> Boolean.TRUE.equals(k.aktiv())).count());
        model.addAttribute("passivKassaSayi", kassalar.stream().filter(k -> !Boolean.TRUE.equals(k.aktiv())).count());
        return "pages/kassalar";
    }

    @PostMapping("/kassalar/yenile")
    public String update(@RequestParam Long kassaId, @RequestParam String kod,
            @RequestParam String ad, @RequestParam(required = false) String aciqlama,
            @RequestParam(defaultValue = "false") boolean aktiv, HttpSession session,
            @AuthenticationPrincipal AuthenticatedPersonal personal, RedirectAttributes attributes) {
        Long klinikaId = klinikaId(session);
        boolean belongs = repository.findAll(klinikaId, null).stream().anyMatch(k -> k.id().equals(kassaId));
        if (!belongs) {
            attributes.addFlashAttribute("errorMessage", "Kassa seçilmiş klinikaya aid deyil.");
            return "redirect:/kassalar";
        }
        flash(repository.update(kassaId, kod, ad, aciqlama, aktiv, personal.personalId()), attributes);
        return "redirect:/kassalar";
    }

    private Long klinikaId(HttpSession session) {
        return (Long) session.getAttribute(KlinikaController.SELECTED_KLINIKA_ID);
    }

    private Boolean statusBoolean(String status) {
        return "hamisi".equals(status) ? null : !"passiv".equals(status);
    }

    private void flash(Map<String, Object> result, RedirectAttributes attributes) {
        String status = String.valueOf(result.getOrDefault("status_kodu", ""));
        attributes.addFlashAttribute(status.toUpperCase().contains("UGUR") || status.equals("1")
                ? "successMessage" : "errorMessage",
                String.valueOf(result.getOrDefault("mesaj", "Kassa yenilənə bilmədi.")));
    }
}
