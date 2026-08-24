package az.simplexs.simplexs.controller;

import java.util.*;
import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.context.*;
import org.springframework.context.i18n.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import az.simplexs.simplexs.dto.xeste.XesteForm;
import az.simplexs.simplexs.repository.teskilat.TeskilatRepository;
import az.simplexs.simplexs.repository.xeste.XesteRepository;
import az.simplexs.simplexs.security.AuthenticatedPersonal;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/xeste-qeydiyyati")
public class XesteQeydiyyatiController {
    private static final int DEFAULT_SIZE = 25;
    private static final Set<Integer> ALLOWED_SIZES = Set.of(25, 50, 100);
    private final XesteRepository repo;
    private final TeskilatRepository teskilatRepo;
    private final MessageSource messages;

    public XesteQeydiyyatiController(XesteRepository repo, TeskilatRepository teskilatRepo, MessageSource messages) {
        this.repo = repo;
        this.teskilatRepo = teskilatRepo;
        this.messages = messages;
    }

    @GetMapping("/siyahi")
    public String siyahi(@RequestParam(required = false) String q, @RequestParam(required = false) String xesteKodu, @RequestParam(required = false) Long vesiqeNovuId, @RequestParam(defaultValue = "aktiv") String status, @RequestParam(required = false) Long teskilatId, @RequestParam(required = false) Long cinsId, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dogumBaslangic, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dogumBitme, @RequestParam(required = false) Long olkeId, @RequestParam(required = false) Long seherId, @RequestParam(required = false) Long cursor, @RequestParam(defaultValue = "25") int size, Model m, HttpSession s) {
        size = ALLOWED_SIZES.contains(size) ? size : DEFAULT_SIZE;
        Boolean aktiv = status.isBlank() ? null : !"passiv".equals(status);
        Long k = klinika(s);
        List<az.simplexs.simplexs.dto.xeste.Xeste> rows = repo.siyahi(k, aktiv, q, xesteKodu, vesiqeNovuId, teskilatId, cinsId, dogumBaslangic, dogumBitme, olkeId, seherId, cursor, size + 1);
        boolean hasNext = rows.size() > size;
        List<az.simplexs.simplexs.dto.xeste.Xeste> xesteler = hasNext ? rows.subList(0, size) : rows;
        Long nextCursor = hasNext ? xesteler.getLast().id() : null;
        m.addAttribute("pageTitle", msg("patients.patient_list"));
        m.addAttribute("activeMenuGroup", "pasientQebulu");
        m.addAttribute("activeMenu", "xesteQeydiyyati");
        m.addAttribute("xesteler", xesteler);
        m.addAttribute("q", q);
        m.addAttribute("xesteKodu", xesteKodu);
        m.addAttribute("selectedVesiqeNovuId", vesiqeNovuId);
        m.addAttribute("selectedStatus", status);
        m.addAttribute("selectedTeskilatId", teskilatId);
        m.addAttribute("selectedCinsId", cinsId);
        m.addAttribute("dogumBaslangic", dogumBaslangic);
        m.addAttribute("dogumBitme", dogumBitme);
        m.addAttribute("selectedOlkeId", olkeId);
        m.addAttribute("selectedSeherId", seherId);
        m.addAttribute("selectedSize", size);
        m.addAttribute("hasNext", hasNext);
        m.addAttribute("nextCursor", nextCursor);
        m.addAttribute("count", xesteler.size());
        lookups(m, k);
        return "pages/pasienQebulu/xesteQeydiyyati";
    }

    @PostMapping("/yeni")
    public String yarat(@ModelAttribute XesteForm form, @RequestParam(required = false) String returnTo, HttpSession s, @AuthenticationPrincipal AuthenticatedPersonal p, RedirectAttributes f) {
        Map<String, Object> r = repo.yarat(klinika(s), form, p.personalId());
        result(r, f, msg("patients.created"));
        if ("ambulator".equals(returnTo) && success(r) && r.get("xeste_id") instanceof Number id)
            return "redirect:/ambulatorQebul/yeni?xesteId=" + id.longValue();
        return back();
    }

    @PostMapping("/yenile")
    public String yenile(@ModelAttribute XesteForm form, HttpSession s, @AuthenticationPrincipal AuthenticatedPersonal p, RedirectAttributes f) {
        result(repo.yenile(klinika(s), form, p.personalId()), f, msg("patients.updated"));
        return back();
    }

    @GetMapping({"", "/yeni"})
    public String yeni(@RequestParam(required = false) String returnTo, Model m, HttpSession s) {
        formBase(m, s, null);
        m.addAttribute("returnTo", returnTo);
        return "pages/pasienQebulu/xesteFormu";
    }

    @GetMapping("/{id}")
    public String redakte(@PathVariable Long id, Model m, HttpSession s) {
        formBase(m, s, repo.tap(klinika(s), id));
        return "pages/pasienQebulu/xesteFormu";
    }

    private void formBase(Model m, HttpSession s, az.simplexs.simplexs.dto.xeste.Xeste x) {
        m.addAttribute("pageTitle", msg(x == null ? "patients.new" : "patients.edit"));
        m.addAttribute("activeMenuGroup", "pasientQebulu");
        m.addAttribute("activeMenu", "xesteQeydiyyati");
        m.addAttribute("xeste", x);
        lookups(m, klinika(s));
    }

    private void lookups(Model m, Long k) {
        m.addAttribute("teskilatlar", teskilatRepo.siyahi(k).stream().filter(x -> Boolean.TRUE.equals(x.aktiv())).toList());
        m.addAttribute("vesiqeNovleri", repo.vesiqeNovleri());
        m.addAttribute("cinsler", repo.cinsler());
        m.addAttribute("aileVeziyyetleri", repo.aileVeziyyetleri());
        m.addAttribute("tehsiller", repo.tehsiller());
        m.addAttribute("qanQruplari", repo.qanQruplari());
        m.addAttribute("olkeler", repo.olkeler());
        m.addAttribute("seherler", repo.seherler());
    }

    private Long klinika(HttpSession s) {
        return (Long) s.getAttribute(KlinikaController.SELECTED_KLINIKA_ID);
    }

    private String back() {
        return "redirect:/xeste-qeydiyyati/siyahi";
    }

    private String msg(String k) {
        return messages.getMessage(k, null, LocaleContextHolder.getLocale());
    }

    private boolean success(Map<String, Object> r) {
        String status = String.valueOf(r.getOrDefault("status_kodu", ""));
        return status.toUpperCase().contains("UGUR") || "1".equals(status);
    }

    private void result(Map<String, Object> r, RedirectAttributes f, String fallback) {
        String message = String.valueOf(r.getOrDefault("mesaj", fallback));
        f.addFlashAttribute(success(r) ? "successMessage" : "errorMessage", message);
    }
}
