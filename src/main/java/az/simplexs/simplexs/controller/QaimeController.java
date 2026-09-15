package az.simplexs.simplexs.controller;

import az.simplexs.simplexs.repository.eczaxana.*;
import az.simplexs.simplexs.repository.eczaxana.QaimeRepository.Operation;
import az.simplexs.simplexs.security.*;
import az.simplexs.simplexs.service.eczaxana.QaimeService;
import az.simplexs.simplexs.service.eczaxana.QaimeService.*;
import jakarta.servlet.http.HttpSession;

import java.util.*;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/eczaxana/qaimeler")
public class QaimeController {
    private final AnbarKontekstRepository context;
    private final QaimeRepository repo;
    private final QaimeService service;
    private final AccessService access;
    private final MessageSource messages;

    public QaimeController(AnbarKontekstRepository context, QaimeRepository repo, QaimeService service, AccessService access, MessageSource messages) {
        this.context = context;
        this.repo = repo;
        this.service = service;
        this.access = access;
        this.messages = messages;
    }

    private Scope scope(Authentication auth, HttpSession session, Long warehouse, int year) {
        Long clinic = (Long) session.getAttribute(KlinikaController.SELECTED_KLINIKA_ID);
        if (auth == null || !(auth.getPrincipal() instanceof AuthenticatedPersonal personal) || clinic == null || !access.hasClinic(auth, clinic))
            throw new Rejected(403, "denied");
        return new Scope(clinic, personal.personalId(), warehouse, year);
    }

    @GetMapping
    public String page(Authentication auth, HttpSession session, Model model) {
        var s = scope(auth, session, null, 0);
        service.authorize(s.clinic(), s.personal());
        model.addAttribute("pageTitle", message("title"));
        model.addAttribute("warehouses", context.warehouses(s.clinic(), s.personal()));
        return "pages/eczaxana/qaimeler";
    }

    @GetMapping("/iller")
    @ResponseBody
    public Object years(@RequestParam Long anbarId, Authentication auth, HttpSession session) {
        var s = scope(auth, session, anbarId, 0);
        service.warehouse(s.clinic(), s.personal(), anbarId);
        return context.years(s.clinic(), anbarId);
    }

    @GetMapping("/secimler")
    @ResponseBody
    public Object options(@RequestParam Long anbarId, Authentication auth, HttpSession session) {
        var s = scope(auth, session, anbarId, 0);
        service.warehouse(s.clinic(), s.personal(), anbarId);
        return repo.options(s.clinic(), anbarId);
    }

    @GetMapping("/materiallar")
    @ResponseBody
    public Object catalog(@RequestParam Long anbarId, @RequestParam Long qrupId, Authentication auth, HttpSession session) {
        var s = scope(auth, session, anbarId, 0);
        service.warehouse(s.clinic(), s.personal(), anbarId);
        return repo.catalog(s.clinic(), anbarId, qrupId);
    }

    @GetMapping("/siyahi")
    @ResponseBody
    public Object list(@RequestParam Long anbarId, @RequestParam int il, @RequestParam(defaultValue = "1") int page,
                       @RequestParam(defaultValue = "25") int size, @RequestParam Map<String, String> filter, Authentication auth, HttpSession session) {
        var s = scope(auth, session, anbarId, il);
        service.validate(s);
        if (page < 1 || page > 100000 || !Set.of(10, 25, 50, 100).contains(size))
            throw new Rejected(400, "invalidInput");
        String direction = filter.getOrDefault("direction", "");
        if (!Set.of("", "G", "C").contains(direction)) throw new Rejected(400, "invalidInput");
        try {
            java.time.LocalDate from = filter.getOrDefault("from", "").isBlank() ? null : java.time.LocalDate.parse(filter.get("from"));
            java.time.LocalDate to = filter.getOrDefault("to", "").isBlank() ? null : java.time.LocalDate.parse(filter.get("to"));
            if (from != null && to != null && from.isAfter(to)) throw new Rejected(400, "invalidDate");
        } catch (java.time.format.DateTimeParseException e) {
            throw new Rejected(400, "invalidDate");
        }
        return repo.list(s.clinic(), anbarId, il, filter, page, size);
    }

    @GetMapping("/{id}/detal")
    @ResponseBody
    public Object detail(@PathVariable Long id, @RequestParam Long anbarId, @RequestParam int il, Authentication auth, HttpSession session) {
        return service.details(scope(auth, session, anbarId, il), id);
    }

    @PostMapping("/hazirla")
    @ResponseBody
    public Object prepare(@RequestParam Long anbarId, @RequestParam int il, @RequestBody Map<String, Object> data, Authentication auth, HttpSession session) {
        return write(scope(auth, session, anbarId, il), Operation.PREPARE, null, null, data);
    }

    @PostMapping("/yeni")
    @ResponseBody
    public Object create(@RequestParam Long anbarId, @RequestParam int il, @RequestBody Map<String, Object> data, Authentication auth, HttpSession session) {
        return write(scope(auth, session, anbarId, il), Operation.CREATE, null, null, data);
    }

    @PostMapping("/{id}/yenile")
    @ResponseBody
    public Object update(@PathVariable Long id, @RequestParam Long anbarId, @RequestParam int il, @RequestBody Map<String, Object> data, Authentication auth, HttpSession session) {
        return write(scope(auth, session, anbarId, il), Operation.UPDATE, id, null, data);
    }

    @PostMapping("/{id}/material/elave")
    @ResponseBody
    public Object add(@PathVariable Long id, @RequestParam Long anbarId, @RequestParam int il, @RequestBody Map<String, Object> data, Authentication auth, HttpSession session) {
        return write(scope(auth, session, anbarId, il), Operation.ADD_MATERIAL, id, null, data);
    }

    @PostMapping("/{id}/material/{materialId}/yenile")
    @ResponseBody
    public Object editMaterial(@PathVariable Long id, @PathVariable Long materialId, @RequestParam Long anbarId, @RequestParam int il, @RequestBody Map<String, Object> data, Authentication auth, HttpSession session) {
        return write(scope(auth, session, anbarId, il), Operation.UPDATE_MATERIAL, id, materialId, data);
    }

    @PostMapping("/{id}/material/{materialId}/sil")
    @ResponseBody
    public Object delete(@PathVariable Long id, @PathVariable Long materialId, @RequestParam Long anbarId, @RequestParam int il, Authentication auth, HttpSession session) {
        return write(scope(auth, session, anbarId, il), Operation.DELETE_MATERIAL, id, materialId, Map.of());
    }

    private Object write(Scope s, Operation op, Long id, Long materialId, Map<String, Object> data) {
        var result = new LinkedHashMap<>(service.write(s, op, id, materialId, data));
        result.put("mesaj", message("saved"));
        return result;
    }

    private String message(String key) {
        return messages.getMessage("qaime." + key, null, messages.getMessage("qaime.saveFailed", null, LocaleContextHolder.getLocale()), LocaleContextHolder.getLocale());
    }

    @ExceptionHandler(Rejected.class)
    public ResponseEntity<Map<String, String>> rejected(Rejected e) {
        return ResponseEntity.status(e.status).body(Map.of("message", message(e.getMessage())));
    }

    @ExceptionHandler({DataAccessException.class, org.springframework.http.converter.HttpMessageNotReadableException.class, org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class, org.springframework.web.bind.MissingServletRequestParameterException.class})
    public ResponseEntity<Map<String, String>> failure(Exception e) {
        return ResponseEntity.status(e instanceof DataAccessException ? 503 : 400).body(Map.of("message", message(e instanceof DataAccessException ? "loadFailed" : "invalidInput")));
    }
}
