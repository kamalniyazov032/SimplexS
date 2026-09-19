package az.simplexs.simplexs.controller;

import az.simplexs.simplexs.repository.eczaxana.*;
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

    public QaimeController(AnbarKontekstRepository context,
                           QaimeRepository repo,
                           QaimeService service,
                           AccessService access,
                           MessageSource messages) {
        this.context = context;
        this.repo = repo;
        this.service = service;
        this.access = access;
        this.messages = messages;
    }

    private Scope scope(Authentication auth,
                        HttpSession session,
                        Long warehouse,
                        int year) {

        Long clinic = (Long) session.getAttribute(
                KlinikaController.SELECTED_KLINIKA_ID
        );

        if (auth == null
                || !(auth.getPrincipal() instanceof AuthenticatedPersonal personal)
                || clinic == null
                || !access.hasClinic(auth, clinic))
            throw new Rejected(403, "denied");

        return new Scope(
                clinic,
                personal.personalId(),
                warehouse,
                year
        );
    }

    @GetMapping
    public String page(Authentication auth,
                       HttpSession session,
                       Model model) {

        var s = scope(auth, session, null, 0);

        service.authorize(s.clinic(), s.personal());

        model.addAttribute("pageTitle", message("title"));
        model.addAttribute(
                "warehouses",
                context.warehouses(s.clinic(), s.personal())
        );

        return "pages/eczaxana/qaimeler";
    }

    @GetMapping("/iller")
    @ResponseBody
    public Object years(@RequestParam Long anbarId,
                        Authentication auth,
                        HttpSession session) {

        var s = scope(auth, session, anbarId, 0);

        service.warehouse(
                s.clinic(),
                s.personal(),
                s.warehouse()
        );

        return context.years(s.clinic(), s.warehouse());
    }

    @GetMapping("/secimler")
    @ResponseBody
    public Object options(@RequestParam Long anbarId,
                          Authentication auth,
                          HttpSession session) {

        var s = scope(auth, session, anbarId, 0);

        service.warehouse(
                s.clinic(),
                s.personal(),
                s.warehouse()
        );

        return repo.options(s.clinic(), s.warehouse());
    }

    @GetMapping("/materiallar")
    @ResponseBody
    public Object materials(@RequestParam Long anbarId,
                            @RequestParam Long qrupId,
                            Authentication auth,
                            HttpSession session) {

        var s = scope(auth, session, anbarId, 0);

        service.warehouse(
                s.clinic(),
                s.personal(),
                s.warehouse()
        );

        return repo.catalog(s.warehouse(), qrupId);
    }

    @GetMapping("/vahidler")
    @ResponseBody
    public Object units(@RequestParam Long anbarId,
                        @RequestParam int il,
                        @RequestParam(required = false) Long qrupId,
                        @RequestParam(required = false) Long qaimeId,
                        @RequestParam Long materialId,
                        Authentication auth,
                        HttpSession session) {

        var s = scope(auth, session, anbarId, il);

        service.warehouse(
                s.clinic(),
                s.personal(),
                s.warehouse()
        );

        List<Map<String, Object>> materials;

        if (qaimeId != null) {
            service.validate(s);
            service.invoice(s, qaimeId);
            materials = repo.materials(s.warehouse(), qaimeId);
        } else {
            materials = repo.catalog(s.warehouse(), qrupId);
        }

        boolean exists = materials.stream().anyMatch(m ->
                m.get("material_id") instanceof Number n
                        && n.longValue() == materialId
        );

        if (!exists)
            throw new Rejected(404, "notFound");

        return repo.units(materialId);
    }

    @GetMapping("/siyahi")
    @ResponseBody
    public Object list(@RequestParam Long anbarId,
                       @RequestParam int il,
                       @RequestParam(defaultValue = "1") int page,
                       @RequestParam(defaultValue = "25") int size,
                       @RequestParam Map<String, String> filter,
                       Authentication auth,
                       HttpSession session) {

        var s = scope(auth, session, anbarId, il);

        service.validate(s);

        if (page < 1
                || page > 100000
                || !Set.of(10, 25, 50, 100).contains(size))
            throw new Rejected(400, "invalidInput");

        String direction = filter.getOrDefault("direction", "");

        if (!Set.of("", "G", "C").contains(direction))
            throw new Rejected(400, "invalidInput");

        try {
            var from = filter.getOrDefault("from", "").isBlank()
                    ? null
                    : java.time.LocalDate.parse(filter.get("from"));

            var to = filter.getOrDefault("to", "").isBlank()
                    ? null
                    : java.time.LocalDate.parse(filter.get("to"));

            if (from != null && to != null && from.isAfter(to))
                throw new Rejected(400, "invalidDate");

        } catch (java.time.format.DateTimeParseException e) {
            throw new Rejected(400, "invalidDate");
        }

        return repo.list(
                s.clinic(),
                s.warehouse(),
                s.year(),
                filter,
                page,
                size
        );
    }

    @GetMapping("/{id}/detal")
    @ResponseBody
    public Object detail(@PathVariable Long id,
                         @RequestParam Long anbarId,
                         @RequestParam int il,
                         Authentication auth,
                         HttpSession session) {

        return service.details(
                scope(auth, session, anbarId, il),
                id
        );
    }

    @PostMapping("/yeni")
    @ResponseBody
    public Object create(@RequestParam Long anbarId,
                         @RequestParam int il,
                         @RequestBody Map<String, Object> data,
                         Authentication auth,
                         HttpSession session) {

        return success(service.create(
                scope(auth, session, anbarId, il),
                data
        ));
    }

    @PostMapping("/{id}/yenile")
    @ResponseBody
    public Object update(@PathVariable Long id,
                         @RequestParam Long anbarId,
                         @RequestParam int il,
                         @RequestBody Map<String, Object> data,
                         Authentication auth,
                         HttpSession session) {

        return success(service.update(
                scope(auth, session, anbarId, il),
                id,
                data
        ));
    }

    @PostMapping("/{id}/material/elave")
    @ResponseBody
    public Object addMaterial(@PathVariable Long id,
                              @RequestParam Long anbarId,
                              @RequestParam int il,
                              @RequestBody Map<String, Object> data,
                              Authentication auth,
                              HttpSession session) {

        return success(service.addMaterial(
                scope(auth, session, anbarId, il),
                id,
                data
        ));
    }

    @PostMapping("/{id}/material/{materialId}/yenile")
    @ResponseBody
    public Object updateMaterial(@PathVariable Long id,
                                 @PathVariable Long materialId,
                                 @RequestParam Long anbarId,
                                 @RequestParam int il,
                                 @RequestBody Map<String, Object> data,
                                 Authentication auth,
                                 HttpSession session) {

        return success(service.updateMaterial(
                scope(auth, session, anbarId, il),
                id,
                materialId,
                data
        ));
    }

    @PostMapping("/{id}/material/{materialId}/sil")
    @ResponseBody
    public Object deleteMaterial(@PathVariable Long id,
                                 @PathVariable Long materialId,
                                 @RequestParam Long anbarId,
                                 @RequestParam int il,
                                 Authentication auth,
                                 HttpSession session) {

        return success(service.deleteMaterial(
                scope(auth, session, anbarId, il),
                id,
                materialId
        ));
    }

    private Map<String, Object> success(Map<String, Object> result) {
        var response = new LinkedHashMap<>(result);
        response.putIfAbsent("mesaj", message("saved"));
        return response;
    }

    private String message(String key) {
        var locale = LocaleContextHolder.getLocale();

        return messages.getMessage(
                "qaime." + key,
                null,
                messages.getMessage(
                        "qaime.saveFailed",
                        null,
                        locale
                ),
                locale
        );
    }

    @ExceptionHandler(Rejected.class)
    public ResponseEntity<Map<String, Object>> rejected(Rejected e) {
        if (e.result != null)
            return ResponseEntity.status(e.status).body(e.result);

        return ResponseEntity
                .status(e.status)
                .body(Map.of("message", message(e.getMessage())));
    }

    @ExceptionHandler({
            DataAccessException.class,
            org.springframework.http.converter.HttpMessageNotReadableException.class,
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,
            org.springframework.web.bind.MissingServletRequestParameterException.class
    })
    public ResponseEntity<Map<String, String>> failure(Exception e) {

        boolean db = e instanceof DataAccessException;

        return ResponseEntity
                .status(db ? 503 : 400)
                .body(Map.of(
                        "message",
                        db
                                ? ((DataAccessException) e).getMostSpecificCause().getMessage()
                                : message("invalidInput")
                ));
    }
}