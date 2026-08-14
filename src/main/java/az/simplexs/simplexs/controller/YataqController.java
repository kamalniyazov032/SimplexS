package az.simplexs.simplexs.controller;

import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import az.simplexs.simplexs.repository.bina.BinaRepository;
import az.simplexs.simplexs.repository.sobe.SobeRepository;
import az.simplexs.simplexs.repository.yataq.YataqRepository;
import az.simplexs.simplexs.security.AuthenticatedPersonal;
import jakarta.servlet.http.HttpSession;

@Controller
public class YataqController {
    private final YataqRepository repository;
    private final BinaRepository binaRepository;
    private final SobeRepository sobeRepository;

    public YataqController(YataqRepository repository, BinaRepository binaRepository,
            SobeRepository sobeRepository) {
        this.repository = repository;
        this.binaRepository = binaRepository;
        this.sobeRepository = sobeRepository;
    }

    @GetMapping("/yataqlar")
    public String list(@RequestParam(required = false) Long binaId,
            @RequestParam(required = false) Long mertebeId,
            @RequestParam(required = false) Long palataId,
            @RequestParam(required = false) Long sobeId,
            @RequestParam(defaultValue = "aktiv") String status,
            Model model, HttpSession session) {
        Long klinikaId = klinikaId(session);
        var binalar = binaRepository.findByKlinikaId(klinikaId).stream()
                .filter(b -> Boolean.TRUE.equals(b.aktiv())).toList();
        if (binaId == null && !binalar.isEmpty()) binaId = binalar.getFirst().binaId();
        Long selectedBinaId = binaId;
        var mertebeler = repository.mertebeler(klinikaId, binaId, statusBoolean(status));
        if (mertebeId == null && !mertebeler.isEmpty()) mertebeId = mertebeler.getFirst().id();
        Long selectedMertebeId = mertebeId;
        var palatalar = mertebeId == null ? java.util.List.<az.simplexs.simplexs.dto.yataq.Palata>of()
                : repository.palatalar(klinikaId, mertebeId, statusBoolean(status));
        if (palataId == null && !palatalar.isEmpty()) palataId = palatalar.getFirst().id();
        Long selectedPalataId = palataId;
        var yataqlar = repository.yataqlar(klinikaId, mertebeId, palataId, sobeId, statusBoolean(status));

        model.addAttribute("pageTitle", "Yataqlar");
        model.addAttribute("activeMenuGroup", "adminPanel");
        model.addAttribute("activeMenu", "yataqlar");
        model.addAttribute("binalar", binalar);
        model.addAttribute("mertebeler", mertebeler);
        model.addAttribute("palatalar", palatalar);
        model.addAttribute("yataqlar", yataqlar);
        model.addAttribute("sobeler", sobeRepository.findByKlinikaId(klinikaId).stream()
                .filter(s -> Boolean.TRUE.equals(s.aktiv())).toList());
        model.addAttribute("selectedBinaId", binaId);
        model.addAttribute("selectedMertebeId", mertebeId);
        model.addAttribute("selectedPalataId", palataId);
        model.addAttribute("selectedSobeId", sobeId);
        model.addAttribute("selectedMertebe", mertebeler.stream().filter(x -> x.id().equals(selectedMertebeId)).findFirst().orElse(null));
        model.addAttribute("selectedPalata", palatalar.stream().filter(x -> x.id().equals(selectedPalataId)).findFirst().orElse(null));
        model.addAttribute("status", status);
        model.addAttribute("filterApplied", sobeId != null || !"aktiv".equals(status));
        return "pages/yataqlar";
    }

    @PostMapping("/yataqlar/mertebe/yeni")
    public String mertebeYarat(@RequestParam Long binaId, @RequestParam Integer mertebeNo,
            @RequestParam String ad, HttpSession session,
            @AuthenticationPrincipal AuthenticatedPersonal personal, RedirectAttributes attributes) {
        Long klinikaId = klinikaId(session);
        if (!binaBelongs(klinikaId, binaId)) return error(attributes, "Bina seçilmiş klinikaya aid deyil.", binaId, null, null);
        flash(repository.mertebeYarat(klinikaId, binaId, mertebeNo, ad, personal.personalId()), attributes);
        return redirect(binaId, null, null);
    }

    @PostMapping("/yataqlar/mertebe/yenile")
    public String mertebeYenile(@RequestParam Long mertebeId, @RequestParam Long binaId,
            @RequestParam Integer mertebeNo, @RequestParam String ad,
            @RequestParam(defaultValue = "false") boolean aktiv, HttpSession session,
            @AuthenticationPrincipal AuthenticatedPersonal personal, RedirectAttributes attributes) {
        if (!mertebeBelongs(klinikaId(session), mertebeId)) return error(attributes, "Mərtəbə seçilmiş klinikaya aid deyil.", binaId, null, null);
        flash(repository.mertebeYenile(mertebeId, mertebeNo, ad, aktiv, personal.personalId()), attributes);
        return redirect(binaId, mertebeId, null);
    }

    @PostMapping("/yataqlar/palata/yeni")
    public String palataYarat(@RequestParam Long binaId, @RequestParam Long mertebeId,
            @RequestParam String otaqNomresi, @RequestParam String ad,
            @RequestParam(required = false) String aciqlama, HttpSession session,
            @AuthenticationPrincipal AuthenticatedPersonal personal, RedirectAttributes attributes) {
        Long klinikaId = klinikaId(session);
        if (!mertebeBelongs(klinikaId, mertebeId)) return error(attributes, "Mərtəbə seçilmiş klinikaya aid deyil.", binaId, null, null);
        flash(repository.palataYarat(klinikaId, mertebeId, otaqNomresi, ad, aciqlama,
                personal.personalId()), attributes);
        return redirect(binaId, mertebeId, null);
    }

    @PostMapping("/yataqlar/palata/yenile")
    public String palataYenile(@RequestParam Long palataId, @RequestParam Long binaId,
            @RequestParam Long mertebeId, @RequestParam String otaqNomresi, @RequestParam String ad,
            @RequestParam(required = false) String aciqlama, @RequestParam(defaultValue = "false") boolean aktiv,
            HttpSession session, @AuthenticationPrincipal AuthenticatedPersonal personal,
            RedirectAttributes attributes) {
        if (!palataBelongs(klinikaId(session), palataId)) return error(attributes, "Palata seçilmiş klinikaya aid deyil.", binaId, mertebeId, null);
        flash(repository.palataYenile(palataId, otaqNomresi, ad, aciqlama, aktiv,
                personal.personalId()), attributes);
        return redirect(binaId, mertebeId, palataId);
    }

    @PostMapping("/yataqlar/yeni")
    public String yataqYarat(@RequestParam Long binaId, @RequestParam Long mertebeId,
            @RequestParam Long palataId, @RequestParam Long sobeId, @RequestParam String kod,
            @RequestParam String ad, @RequestParam(required = false) String aciqlama,
            HttpSession session, @AuthenticationPrincipal AuthenticatedPersonal personal,
            RedirectAttributes attributes) {
        Long klinikaId = klinikaId(session);
        if (!palataBelongs(klinikaId, palataId)) return error(attributes, "Palata seçilmiş klinikaya aid deyil.", binaId, mertebeId, null);
        flash(repository.yataqYarat(klinikaId, palataId, sobeId, kod, ad, aciqlama,
                personal.personalId()), attributes);
        return redirect(binaId, mertebeId, palataId);
    }

    @PostMapping("/yataqlar/yenile")
    public String yataqYenile(@RequestParam Long yataqId, @RequestParam Long binaId,
            @RequestParam Long mertebeId, @RequestParam Long palataId, @RequestParam Long sobeId,
            @RequestParam String kod, @RequestParam String ad,
            @RequestParam(defaultValue = "false") boolean aktiv, HttpSession session,
            @AuthenticationPrincipal AuthenticatedPersonal personal, RedirectAttributes attributes) {
        if (!yataqBelongs(klinikaId(session), yataqId)) return error(attributes, "Yataq seçilmiş klinikaya aid deyil.", binaId, mertebeId, palataId);
        flash(repository.yataqYenile(yataqId, sobeId, kod, ad, aktiv,
                personal.personalId()), attributes);
        return redirect(binaId, mertebeId, palataId);
    }

    private boolean binaBelongs(Long klinikaId, Long id) { return binaRepository.findByKlinikaId(klinikaId).stream().anyMatch(x -> x.binaId().equals(id)); }
    private boolean mertebeBelongs(Long klinikaId, Long id) { return repository.mertebeler(klinikaId, null, null).stream().anyMatch(x -> x.id().equals(id)); }
    private boolean palataBelongs(Long klinikaId, Long id) { return repository.palatalar(klinikaId, null, null).stream().anyMatch(x -> x.id().equals(id)); }
    private boolean yataqBelongs(Long klinikaId, Long id) { return repository.yataqlar(klinikaId, null, null, null, null).stream().anyMatch(x -> x.id().equals(id)); }
    private Long klinikaId(HttpSession session) { return (Long) session.getAttribute(KlinikaController.SELECTED_KLINIKA_ID); }
    private Boolean statusBoolean(String status) { return "hamisi".equals(status) ? null : !"passiv".equals(status); }

    private void flash(Map<String, Object> result, RedirectAttributes attributes) {
        String status = String.valueOf(result.getOrDefault("status_kodu", ""));
        attributes.addFlashAttribute(status.toUpperCase().contains("UGUR") || status.equals("1")
                ? "successMessage" : "errorMessage", String.valueOf(result.getOrDefault("mesaj", "Əməliyyat tamamlanmadı.")));
    }

    private String error(RedirectAttributes attributes, String message, Long binaId, Long mertebeId, Long palataId) {
        attributes.addFlashAttribute("errorMessage", message);
        return redirect(binaId, mertebeId, palataId);
    }

    private String redirect(Long binaId, Long mertebeId, Long palataId) {
        StringBuilder url = new StringBuilder("redirect:/yataqlar");
        if (binaId != null) url.append("?binaId=").append(binaId);
        if (mertebeId != null) url.append("&mertebeId=").append(mertebeId);
        if (palataId != null) url.append("&palataId=").append(palataId);
        return url.toString();
    }
}
