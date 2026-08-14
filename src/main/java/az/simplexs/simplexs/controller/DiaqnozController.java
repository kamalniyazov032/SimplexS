package az.simplexs.simplexs.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import az.simplexs.simplexs.repository.diaqnoz.DiaqnozRepository;

@Controller
public class DiaqnozController {
    private final DiaqnozRepository repository;

    public DiaqnozController(DiaqnozRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/diaqnozlar")
    public String list(@RequestParam(required = false) Long sistemId,
            @RequestParam(required = false) Long parentId,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "hamisi") String secim,
            @RequestParam(defaultValue = "aktiv") String status,
            @RequestParam(defaultValue = "hamisi") String tip,
            @RequestParam(defaultValue = "hamisi") String cins,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size,
            Model model) {
        var sistemler = repository.sistemler();
        if (sistemId == null && !sistemler.isEmpty()) sistemId = sistemler.getFirst().id();
        size = size == 100 ? 100 : 50;
        Boolean secileBiler = triState(secim, "secile_biler", "secile_bilmez");
        Boolean aktiv = triState(status, "aktiv", "passiv");
        Boolean kateqoriyadir = triState(tip, "kateqoriya", "diaqnoz");
        long total = repository.count(sistemId, parentId, secileBiler, aktiv, q, kateqoriyadir, cins);
        int totalPages = Math.max(1, (int) Math.ceil((double) total / size));
        page = Math.max(1, Math.min(page, totalPages));

        model.addAttribute("pageTitle", "Diaqnozlar");
        model.addAttribute("activeMenuGroup", "adminPanel");
        model.addAttribute("activeMenu", "diaqnozlar");
        model.addAttribute("sistemler", sistemler);
        model.addAttribute("diaqnozlar", repository.find(sistemId, parentId, secileBiler, aktiv,
                q, kateqoriyadir, cins, size, (page - 1) * size));
        model.addAttribute("selectedSistemId", sistemId);
        model.addAttribute("parentId", parentId);
        model.addAttribute("q", q);
        model.addAttribute("secim", secim);
        model.addAttribute("status", status);
        model.addAttribute("tip", tip);
        model.addAttribute("cins", cins);
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", size);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalCount", total);
        model.addAttribute("filterApplied", hasText(q) || parentId != null || !"hamisi".equals(secim)
                || !"aktiv".equals(status) || !"hamisi".equals(tip) || !"hamisi".equals(cins));
        return "pages/diaqnozlar";
    }

    private Boolean triState(String value, String trueValue, String falseValue) {
        if (trueValue.equals(value)) return true;
        if (falseValue.equals(value)) return false;
        return null;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
