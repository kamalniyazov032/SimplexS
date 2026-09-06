package az.simplexs.simplexs.controller;

import java.util.List;
import java.util.Map;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import az.simplexs.simplexs.repository.ambulator.AmbulatorRepository;
import az.simplexs.simplexs.repository.xestexidmet.XesteXidmetRepository;
import az.simplexs.simplexs.security.AuthenticatedPersonal;
import jakarta.servlet.http.HttpSession;

@Controller
@org.springframework.web.bind.annotation.RequestMapping("/xeste-xidmetleri")
public class XesteXidmetController {
    private final XesteXidmetRepository repo;
    private final AmbulatorRepository ambulatorRepo;
    private final MessageSource messages;

    public XesteXidmetController(XesteXidmetRepository repo, AmbulatorRepository ambulatorRepo, MessageSource messages) {
        this.repo = repo;
        this.ambulatorRepo = ambulatorRepo;
        this.messages = messages;
    }

    @GetMapping("/{gelisId}")
    public String sehife(@PathVariable Long gelisId, Model model, @RequestParam(required = false) Long istekId, HttpSession session) {
        var gelis = ambulatorRepo.gelis(klinikaId(session), gelisId);
        model.addAttribute("pageTitle", msg("patient_services.title"));
        model.addAttribute("activeMenuGroup", "pasientQebulu");
        model.addAttribute("activeMenu", "ambulatorQebul");
        model.addAttribute("gelis", gelis);
        model.addAttribute("istekId", istekId);
        model.addAttribute("qruplar", repo.qruplar(klinikaId(session)));
        model.addAttribute("isteyenHekimler", repo.isteyenHekimler(gelisId));
        model.addAttribute("gonderenHekim", repo.gonderenHekim(gelisId));
        model.addAttribute("bugun", java.time.LocalDate.now(java.time.ZoneId.of("Asia/Baku")));
        return "pages/pasienQebulu/xesteXidmetleri";
    }

    @GetMapping("/{gelisId}/kataloq") @ResponseBody
    public ResponseEntity<Map<String, Object>> kataloq(@PathVariable Long gelisId,
            @RequestParam(defaultValue = "XIDMET") String nov,
            @RequestParam(required = false) Long istekId,
            @RequestParam(required = false) java.time.LocalDate tarix,
            @RequestParam(required = false) Long qrupId,
            @RequestParam(required = false) Long secimId,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page, HttpSession session) {
        try {
            ambulatorRepo.gelis(klinikaId(session), gelisId);
            int offset = Math.max(0, page) * 100;
            List<Map<String, Object>> rows;
            if ("PAKET".equals(nov)) rows = repo.paketler(gelisId, q, offset);
            else if ("RUTIN".equals(nov)) rows = secimId == null ? repo.rutinler(klinikaId(session), q, offset) : repo.rutinTerkibi(gelisId, secimId, tarix == null ? java.time.LocalDate.now(java.time.ZoneId.of("Asia/Baku")) : tarix);
            else rows = repo.xidmetler(gelisId, qrupId, q, offset, istekId, klinikaId(session));
            boolean more = secimId == null && rows.size() > 100;
            return ResponseEntity.ok(Map.of("items", more ? rows.subList(0, 100) : rows, "hasMore", more));
        } catch (DataAccessException exception) {
            return ResponseEntity.unprocessableContent().body(Map.of(
                    "message", databaseMessage(exception), "type", "DATABASE_ERROR"));
        }
    }

    @GetMapping("/{gelisId}/xidmet/{xidmetId}/sobeler") @ResponseBody
    public List<Map<String, Object>> sobeler(@PathVariable Long gelisId, @PathVariable Long xidmetId, HttpSession session) {
        ambulatorRepo.gelis(klinikaId(session), gelisId);
        return repo.sobeler(gelisId, xidmetId);
    }

    @GetMapping("/{gelisId}/xidmet/{xidmetId}/sobe/{sobeId}/hekimler") @ResponseBody
    public List<Map<String, Object>> hekimler(@PathVariable Long gelisId, @PathVariable Long xidmetId, @PathVariable Long sobeId, @RequestParam(required = false) java.time.LocalDate tarix, HttpSession session) {
        ambulatorRepo.gelis(klinikaId(session), gelisId);
        return repo.sobeHekimleri(gelisId, xidmetId, sobeId, tarix == null
                ? java.time.LocalDate.now(java.time.ZoneId.of("Asia/Baku")) : tarix);
    }

    @PostMapping("/{gelisId}/yarat")
    public String yarat(@PathVariable Long gelisId, @RequestParam String xidmetlerJson,
            @RequestParam(required = false) String aciqlama, @AuthenticationPrincipal AuthenticatedPersonal personal,
            @RequestParam(required = false) Long istekId, HttpSession session, RedirectAttributes redirect) {
        ambulatorRepo.gelis(klinikaId(session), gelisId);
        Map<String, Object> result = istekId != null ? repo.yenile(gelisId, istekId, xidmetlerJson, personal.personalId()) : repo.yarat(gelisId, xidmetlerJson, aciqlama, personal.personalId());
        String status = String.valueOf(result.getOrDefault("status_kodu", ""));
        String key = status.toUpperCase().contains("UGUR") ? "successMessage" : "errorMessage";
        redirect.addFlashAttribute(key, result.getOrDefault("mesaj", msg("patient_services.save_error")));
        return "redirect:/xeste-xidmetleri/" + gelisId + "/hamisi";
    }


    @GetMapping("/{gelisId}/hamisi")
    public String hamisi(@PathVariable Long gelisId, HttpSession session, Model model) {
        model.addAttribute("gelis", ambulatorRepo.gelis(klinikaId(session), gelisId));
        model.addAttribute("pageTitle", msg("patient_services.all"));
        return "pages/pasienQebulu/butunXidmetler";
    }
    @GetMapping("/{gelisId}/siyahi") @ResponseBody
    public List<Map<String, Object>> siyahi(@PathVariable Long gelisId, @RequestParam(required = false) Long istekId, HttpSession session) {
        ambulatorRepo.gelis(klinikaId(session), gelisId);
        return istekId == null ? repo.secilmisXidmetler(gelisId) : repo.redakte(gelisId, istekId);
    }
    @PostMapping("/{gelisId}/hazirla") @ResponseBody
    public Map<String, Object> hazirla(@PathVariable Long gelisId, @org.springframework.web.bind.annotation.RequestBody Map<String, Object> row,
            @RequestParam(required = false) Long istekId, HttpSession session, @AuthenticationPrincipal AuthenticatedPersonal personal) {
        ambulatorRepo.gelis(klinikaId(session), gelisId);
        return repo.hazirla(gelisId, row, istekId, personal.personalId());
    }
    @PostMapping("/{gelisId}/legv/{id}") @ResponseBody
    public Map<String, Object> legv(@PathVariable Long gelisId, @PathVariable Long id, HttpSession session, @AuthenticationPrincipal AuthenticatedPersonal personal) {
        ambulatorRepo.gelis(klinikaId(session), gelisId);
        if (repo.secilmisXidmetler(gelisId).stream().noneMatch(row -> String.valueOf(id).equals(String.valueOf(row.get("xeste_xidmet_id")))))
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND);
        return repo.legv(id, personal.personalId());
    }

    @PostMapping("/{gelisId}/paket-statusu/{id}") @ResponseBody
    public ResponseEntity<Map<String, Object>> paketStatusu(@PathVariable Long gelisId, @PathVariable Long id,
            @RequestParam boolean paketDaxildir, HttpSession session,
            @AuthenticationPrincipal AuthenticatedPersonal personal) {
        ambulatorRepo.gelis(klinikaId(session), gelisId);
        if (repo.secilmisXidmetler(gelisId).stream().noneMatch(row -> String.valueOf(id).equals(String.valueOf(row.get("xeste_xidmet_id")))))
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND);
        try {
            return ResponseEntity.ok(repo.paketStatusunuYenile(id, paketDaxildir, personal.personalId()));
        } catch (DataAccessException exception) {
            return ResponseEntity.unprocessableContent().body(Map.of("mesaj", databaseMessage(exception)));
        }
    }

    private Long klinikaId(HttpSession session) {
        return (Long) session.getAttribute(KlinikaController.SELECTED_KLINIKA_ID);
    }
    private String msg(String key) { return messages.getMessage(key, null, LocaleContextHolder.getLocale()); }

    private String databaseMessage(Throwable error) {
        Throwable root = error;
        while (root.getCause() != null) root = root.getCause();
        String message = root.getMessage();
        if (message == null || message.isBlank()) return msg("patient_services.load_error");
        String firstLine = message.replace('\r', '\n').lines()
                .map(line -> line.trim()).filter(line -> !line.isBlank()).findFirst().orElse("");
        if (firstLine.startsWith("ERROR:")) firstLine = firstLine.substring("ERROR:".length()).trim();
        return firstLine.isBlank() ? msg("patient_services.load_error") : firstLine;
    }
}
