package az.simplexs.simplexs.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import az.simplexs.simplexs.repository.sobe.SobeRepository;
import jakarta.servlet.http.HttpSession;

@Controller
public class SobeController {
    private final SobeRepository repository;

    public SobeController(SobeRepository repository) { this.repository = repository; }

    @GetMapping("/shobe")
    public String list(Model model, HttpSession session) {
        Long klinikaId = klinikaId(session);
        model.addAttribute("pageTitle", "Şöbələr");
        model.addAttribute("activeMenuGroup", "adminPanel");
        model.addAttribute("activeMenu", "shobe");
        var sobeler = repository.findByKlinikaId(klinikaId);
        model.addAttribute("sobeler", sobeler);
        model.addAttribute("aktivSobeSayi", sobeler.stream().filter(s -> Boolean.TRUE.equals(s.aktiv())).count());
        model.addAttribute("passivSobeSayi", sobeler.stream().filter(s -> !Boolean.TRUE.equals(s.aktiv())).count());
        model.addAttribute("sobeTipleri", repository.findSobeTipleri());
        model.addAttribute("hekimSecimQaydalari", repository.findHekimSecimQaydalari());
        model.addAttribute("cinsler", repository.findCinsler());
        model.addAttribute("personallar", repository.findPersonallar(klinikaId));
        return "pages/shobe";
    }

    @PostMapping("/shobe/yeni")
    public String create(@RequestParam String ad, @RequestParam Long sobeTipiId,
                         @RequestParam(required = false) Long hekimSecimQaydasiId,
                         @RequestParam(required = false) Long sobeMudiriPersonalId,
                         @RequestParam(required = false) Long boyukTibbBacisiPersonalId,
                         @RequestParam(required = false) Long cinsId,
                         HttpSession session, RedirectAttributes attributes) {
        Long klinikaId = klinikaId(session);
        if (klinikaId == null) return error(attributes, "Əvvəlcə klinika seçin.");
        if (sobeMudiriPersonalId != null && sobeMudiriPersonalId.equals(boyukTibbBacisiPersonalId)) {
            return error(attributes, "Şöbə müdiri və böyük tibb bacısı eyni personal ola bilməz.");
        }
        var result = repository.create(klinikaId, ad, sobeTipiId, hekimSecimQaydasiId,
            sobeMudiriPersonalId, boyukTibbBacisiPersonalId, cinsId, null);
        message(result.ugurludur(), result.mesaj(), attributes);
        return "redirect:/shobe";
    }

    @PostMapping("/shobe/yenile")
    public String update(@RequestParam Long sobeId,
                         @RequestParam(required = false) Long hekimSecimQaydasiId,
                         @RequestParam(required = false) Long sobeMudiriPersonalId,
                         @RequestParam(required = false) Long boyukTibbBacisiPersonalId,
                         @RequestParam(required = false) Long cinsId,
                         @RequestParam(defaultValue = "false") boolean aktiv,
                         HttpSession session, RedirectAttributes attributes) {
        Long klinikaId = klinikaId(session);
        boolean belongs = repository.findByKlinikaId(klinikaId).stream().anyMatch(s -> s.sobeId().equals(sobeId));
        if (!belongs) return error(attributes, "Şöbə seçilmiş klinikaya aid deyil.");
        var result = repository.update(sobeId, hekimSecimQaydasiId, sobeMudiriPersonalId,
            boyukTibbBacisiPersonalId, cinsId, aktiv, null);
        message(result.ugurludur(), result.mesaj(), attributes);
        return "redirect:/shobe";
    }

    private Long klinikaId(HttpSession session) { return (Long) session.getAttribute(KlinikaController.SELECTED_KLINIKA_ID); }
    private String error(RedirectAttributes a, String m) { a.addFlashAttribute("errorMessage", m); return "redirect:/shobe"; }
    private void message(boolean success, String text, RedirectAttributes a) {
        a.addFlashAttribute(success ? "successMessage" : "errorMessage",
            text == null || text.isBlank() ? "Əməliyyat zamanı naməlum xəta baş verdi." : text);
    }
}
