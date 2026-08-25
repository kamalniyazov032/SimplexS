package az.simplexs.simplexs.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import az.simplexs.simplexs.repository.personal.PersonalRepository;
import az.simplexs.simplexs.repository.qiymet.QiymetRepository;
import az.simplexs.simplexs.repository.teskilat.TeskilatRepository;
import az.simplexs.simplexs.repository.xidmet.XidmetRepository;
import az.simplexs.simplexs.security.AuthenticatedPersonal;
import jakarta.servlet.http.HttpSession;

@Controller
public class QiymetController {
    private final QiymetRepository repo;
    private final XidmetRepository xidmetRepo;
    private final TeskilatRepository teskilatRepo;
    private final PersonalRepository personalRepo;

    public QiymetController(QiymetRepository repo, XidmetRepository xidmetRepo,
            TeskilatRepository teskilatRepo, PersonalRepository personalRepo) {
        this.repo = repo;
        this.xidmetRepo = xidmetRepo;
        this.teskilatRepo = teskilatRepo;
        this.personalRepo = personalRepo;
    }

    @GetMapping("/xidmetQiymetleri")
    public String list(@RequestParam(required = false) Long basliqId,
            @RequestParam(required = false) Long qrupId,
            @RequestParam(defaultValue = "aktiv") String qrupStatus,
            @RequestParam(defaultValue = "aktiv") String cedvelStatus,
            Model model, HttpSession session) {
        Long klinikaId = klinikaId(session);
        var basliqlar = repo.basliqlar(klinikaId, null);
        if (basliqId == null && !basliqlar.isEmpty()) basliqId = basliqlar.getFirst().id();
        Long selectedBasliqId = basliqId;
        var qruplar = repo.qruplar(klinikaId, basliqId, statusBoolean(qrupStatus));
        if (qrupId == null && !qruplar.isEmpty()) qrupId = qruplar.getFirst().id();
        Long selectedQrupId = qrupId;
        var cedveller = qrupId == null ? List.of()
                : repo.cedveller(klinikaId, qrupId, statusBoolean(cedvelStatus), null);

        model.addAttribute("pageTitle", "Xidmət qiymətləri");
        model.addAttribute("activeMenuGroup", "adminPanel");
        model.addAttribute("activeMenu", "xidmetQiymetleri");
        model.addAttribute("basliqlar", basliqlar);
        model.addAttribute("qruplar", qruplar);
        model.addAttribute("cedveller", cedveller);
        model.addAttribute("klonCedveller", repo.cedveller(klinikaId, null, null, null));
        model.addAttribute("selectedBasliqId", basliqId);
        model.addAttribute("selectedQrupId", qrupId);
        model.addAttribute("selectedBasliq", basliqlar.stream().filter(x -> x.id().equals(selectedBasliqId)).findFirst().orElse(null));
        model.addAttribute("selectedQrup", qruplar.stream().filter(x -> x.id().equals(selectedQrupId)).findFirst().orElse(null));
        model.addAttribute("qrupStatus", qrupStatus);
        model.addAttribute("cedvelStatus", cedvelStatus);
        model.addAttribute("filterApplied", !"aktiv".equals(qrupStatus) || !"aktiv".equals(cedvelStatus));
        model.addAttribute("qrupTeskilatIds", repo.qruplarinTeskilatIdleri(qruplar.stream().map(x -> x.id()).toList()));
        model.addAttribute("teskilatlar", teskilatRepo.siyahi(klinikaId).stream()
                .filter(t -> Boolean.TRUE.equals(t.aktiv())).toList());
        return "pages/xidmetQiymetleri";
    }

    @GetMapping("/xidmetQiymetleri/cedvel/{cedvelId}")
    public String tarifler(@PathVariable Long cedvelId,
            @RequestParam(required = false) Long xidmetQrupuId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String qiymetStatus,
            @RequestParam(required = false) String hekimQiymetStatus,
            @RequestParam(required = false) Long hekimPersonalId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "100") int size,
            Model model, HttpSession session) {
        Long klinikaId = klinikaId(session);
        var cedvel = repo.cedvel(klinikaId, cedvelId);
        var hekimler = personalRepo.find(klinikaId).stream()
                .filter(p -> Boolean.TRUE.equals(p.hekimdir()) && Boolean.TRUE.equals(p.personalAktiv())
                        && Boolean.TRUE.equals(p.klinikaElagesiAktiv()))
                .toList();
        Long selectedHekimId = hekimPersonalId;
        if (selectedHekimId != null && hekimler.stream().noneMatch(h -> h.personalId().equals(selectedHekimId))) {
            hekimPersonalId = null;
        }
        size = Math.max(20, Math.min(size, 100));
        long total = repo.xidmetSayi(cedvelId, xidmetQrupuId, q, qiymetStatus,
                hekimQiymetStatus, hekimPersonalId);
        int totalPages = Math.max(1, (int) Math.ceil((double) total / size));
        page = Math.max(1, Math.min(page, totalPages));
        var hekimQiymetleri = repo.hekimQiymetleri(cedvelId, null, null);
        var hekimQiymetSaylari = hekimQiymetleri.stream().collect(java.util.stream.Collectors.groupingBy(
                        az.simplexs.simplexs.dto.qiymet.HekimXidmetQiymeti::xidmetId,
                        java.util.stream.Collectors.counting()));

        model.addAttribute("pageTitle", cedvel.qrupAdi());
        model.addAttribute("activeMenuGroup", "adminPanel");
        model.addAttribute("activeMenu", "xidmetQiymetleri");
        model.addAttribute("cedvel", cedvel);
        model.addAttribute("xidmetQruplari", hierarchyOrder(xidmetRepo.qruplar(klinikaId)));
        model.addAttribute("xidmetler", repo.xidmetler(cedvelId, xidmetQrupuId, q, qiymetStatus,
                hekimQiymetStatus, hekimPersonalId, size, (page - 1) * size));
        model.addAttribute("hekimQiymetleri", hekimQiymetleri);
        model.addAttribute("hekimQiymetSaylari", hekimQiymetSaylari);
        model.addAttribute("hekimler", hekimler);
        model.addAttribute("selectedXidmetQrupuId", xidmetQrupuId);
        model.addAttribute("q", q);
        model.addAttribute("qiymetStatus", qiymetStatus);
        model.addAttribute("hekimQiymetStatus", hekimQiymetStatus);
        model.addAttribute("selectedHekimPersonalId", hekimPersonalId);
        model.addAttribute("filterApplied", xidmetQrupuId != null
                || (q != null && !q.isBlank()) || (qiymetStatus != null && !qiymetStatus.isBlank())
                || (hekimQiymetStatus != null && !hekimQiymetStatus.isBlank()) || hekimPersonalId != null);
        model.addAttribute("totalCount", total);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", size);
        return "pages/xidmetQiymetTarifleri";
    }

    private List<az.simplexs.simplexs.dto.xidmet.XidmetQrupu> hierarchyOrder(
            List<az.simplexs.simplexs.dto.xidmet.XidmetQrupu> groups) {
        Comparator<az.simplexs.simplexs.dto.xidmet.XidmetQrupu> order = Comparator
                .comparing(az.simplexs.simplexs.dto.xidmet.XidmetQrupu::siraNo,
                        Comparator.nullsLast(Integer::compareTo))
                .thenComparing(az.simplexs.simplexs.dto.xidmet.XidmetQrupu::ad,
                        String.CASE_INSENSITIVE_ORDER);
        var children = new HashMap<Long, List<az.simplexs.simplexs.dto.xidmet.XidmetQrupu>>();
        var ids = new HashSet<Long>();
        groups.forEach(group -> ids.add(group.id()));
        groups.forEach(group -> children.computeIfAbsent(group.parentId(), ignored -> new ArrayList<>()).add(group));
        children.values().forEach(list -> list.sort(order));
        var result = new ArrayList<az.simplexs.simplexs.dto.xidmet.XidmetQrupu>();
        var visited = new HashSet<Long>();
        groups.stream().filter(group -> group.parentId() == null || !ids.contains(group.parentId()))
                .sorted(order).forEach(group -> appendGroup(group, children, visited, result));
        groups.stream().filter(group -> !visited.contains(group.id())).sorted(order)
                .forEach(group -> appendGroup(group, children, visited, result));
        return result;
    }

    private void appendGroup(az.simplexs.simplexs.dto.xidmet.XidmetQrupu group,
            Map<Long, List<az.simplexs.simplexs.dto.xidmet.XidmetQrupu>> children, HashSet<Long> visited,
            List<az.simplexs.simplexs.dto.xidmet.XidmetQrupu> result) {
        if (!visited.add(group.id())) return;
        result.add(group);
        children.getOrDefault(group.id(), List.of())
                .forEach(child -> appendGroup(child, children, visited, result));
    }

    @PostMapping("/xidmetQiymetleri/basliq/yeni")
    public String basliqYarat(@RequestParam String ad, @RequestParam(required = false) String aciqlama,
            HttpSession session, @AuthenticationPrincipal AuthenticatedPersonal personal, RedirectAttributes a) {
        flash(repo.basliqYarat(klinikaId(session), ad, aciqlama, personal.personalId()), a, "Qiymət başlığı yaradıldı.");
        return redirect(null, null);
    }

    @PostMapping("/xidmetQiymetleri/basliq/yenile")
    public String basliqYenile(@RequestParam Long id, @RequestParam String ad,
            @RequestParam(required = false) String aciqlama, @RequestParam(defaultValue = "false") boolean aktiv,
            @AuthenticationPrincipal AuthenticatedPersonal personal, RedirectAttributes a) {
        flash(repo.basliqYenile(id, ad, aciqlama, aktiv, personal.personalId()), a, "Qiymət başlığı yeniləndi.");
        return redirect(id, null);
    }

    @PostMapping("/xidmetQiymetleri/qrup/yeni")
    public String qrupYarat(@RequestParam Long basliqId, @RequestParam String ad,
            @RequestParam(required = false) String aciqlama, @RequestParam(required = false) List<Long> teskilatIds,
            @RequestParam(defaultValue = "false") boolean standartdir, HttpSession session,
            @AuthenticationPrincipal AuthenticatedPersonal personal, RedirectAttributes a) {
        flash(repo.qrupYarat(klinikaId(session), basliqId, ad, aciqlama, standartdir, teskilatIds,
                personal.personalId()), a, "Qiymət qrupu yaradıldı.");
        return redirect(basliqId, null);
    }

    @PostMapping("/xidmetQiymetleri/qrup/yenile")
    public String qrupYenile(@RequestParam Long id, @RequestParam Long basliqId, @RequestParam String ad,
            @RequestParam(required = false) String aciqlama, @RequestParam(required = false) List<Long> teskilatIds,
            @RequestParam(defaultValue = "false") boolean standartdir,
            @RequestParam(defaultValue = "false") boolean aktiv,
            @AuthenticationPrincipal AuthenticatedPersonal personal, RedirectAttributes a) {
        flash(repo.qrupYenile(id, ad, aciqlama, standartdir, aktiv, teskilatIds, personal.personalId()),
                a, "Qiymət qrupu yeniləndi.");
        return redirect(basliqId, id);
    }

    @PostMapping("/xidmetQiymetleri/cedvel/yeni")
    public String cedvelYarat(@RequestParam Long basliqId, @RequestParam Long qrupId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baslamaTarixi,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate bitmeTarixi,
            @RequestParam(required = false) BigDecimal xestePayi,
            @RequestParam(required = false) BigDecimal xesteEndirimi,
            @RequestParam(required = false) BigDecimal sigortaEndirimi,
            @RequestParam Long menbeCedvelId,
            HttpSession session, @AuthenticationPrincipal AuthenticatedPersonal personal, RedirectAttributes a) {
        flash(repo.cedvelYaratVeKlonla(klinikaId(session), qrupId, baslamaTarixi, bitmeTarixi, xestePayi,
                xesteEndirimi, sigortaEndirimi, menbeCedvelId, personal.personalId()), a,
                menbeCedvelId == null ? "Qiymət cədvəli yaradıldı." : "Qiymət cədvəli klonlanaraq yaradıldı.");
        return redirect(basliqId, qrupId);
    }

    @PostMapping("/xidmetQiymetleri/cedvel/yenile")
    public String cedvelYenile(@RequestParam Long id, @RequestParam Long basliqId, @RequestParam Long qrupId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baslamaTarixi,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate bitmeTarixi,
            @RequestParam(required = false) BigDecimal xestePayi,
            @RequestParam(required = false) BigDecimal xesteEndirimi,
            @RequestParam(required = false) BigDecimal sigortaEndirimi,
            @RequestParam(defaultValue = "false") boolean aktiv,
            @AuthenticationPrincipal AuthenticatedPersonal personal, RedirectAttributes a) {
        flash(repo.cedvelYenile(id, baslamaTarixi, bitmeTarixi, xestePayi, xesteEndirimi,
                sigortaEndirimi, aktiv, personal.personalId()), a, "Qiymət cədvəli yeniləndi.");
        return redirect(basliqId, qrupId);
    }

    @PostMapping("/xidmetQiymetleri/qiymetler")
    public String qiymetler(@RequestParam Long cedvelId, @RequestParam String qiymetlerJson,
            @AuthenticationPrincipal AuthenticatedPersonal personal, RedirectAttributes a) {
        if (!validJsonArray(qiymetlerJson)) a.addFlashAttribute("errorMessage", "Göndərilən qiymət məlumatları düzgün deyil.");
        else flash(repo.qiymetleriSaxla(cedvelId, qiymetlerJson, personal.personalId()), a,
                "Dəyişdirilmiş xidmət qiymətləri yadda saxlanıldı.");
        return tarifRedirect(cedvelId);
    }

    @PostMapping("/xidmetQiymetleri/qiymetler/toplu")
    public String topluYenile(@RequestParam Long cedvelId, @RequestParam List<Long> xidmetIds,
            @RequestParam(required = false) BigDecimal xestePayi,
            @RequestParam(required = false) BigDecimal sigortaPayi,
            @RequestParam(required = false) BigDecimal xesteEndirimi,
            @RequestParam(required = false) BigDecimal sigortaEndirimi,
            @AuthenticationPrincipal AuthenticatedPersonal personal, RedirectAttributes a) {
        flash(repo.qiymetleriTopluYenile(cedvelId, xidmetIds, xestePayi, sigortaPayi,
                xesteEndirimi, sigortaEndirimi, personal.personalId()), a, "Xidmətlər toplu yeniləndi.");
        return tarifRedirect(cedvelId);
    }

    @PostMapping("/xidmetQiymetleri/hekim-qiymeti")
    public String hekimQiymeti(@RequestParam Long cedvelId, @RequestParam Long xidmetId,
            @RequestParam List<Long> hekimPersonalIds, @RequestParam BigDecimal qiymet,
            @RequestParam(defaultValue = "false") boolean aktiv, HttpSession session,
            @AuthenticationPrincipal AuthenticatedPersonal personal, RedirectAttributes a) {
        flash(repo.hekimQiymetleriniTopluSaxla(klinikaId(session), cedvelId, xidmetId, hekimPersonalIds,
                qiymet, aktiv, personal.personalId()), a, "Həkimə özəl qiymət yadda saxlanıldı.");
        return tarifRedirect(cedvelId);
    }

    private Long klinikaId(HttpSession session) {
        return (Long) session.getAttribute(KlinikaController.SELECTED_KLINIKA_ID);
    }

    private boolean validJsonArray(String json) {
        return json != null && json.length() <= 1_000_000 && json.trim().startsWith("[") && json.trim().endsWith("]");
    }

    private void flash(Map<String, Object> result, RedirectAttributes attributes, String fallback) {
        String status = String.valueOf(result.getOrDefault("status_kodu", ""));
        attributes.addFlashAttribute(status.toUpperCase().contains("UGUR") || status.equals("1")
                ? "successMessage" : "errorMessage", String.valueOf(result.getOrDefault("mesaj", fallback)));
    }

    private String redirect(Long basliqId, Long qrupId) {
        String query = basliqId == null ? "" : "?basliqId=" + basliqId + (qrupId == null ? "" : "&qrupId=" + qrupId);
        return "redirect:/xidmetQiymetleri" + query;
    }

    private String tarifRedirect(Long cedvelId) {
        return "redirect:/xidmetQiymetleri/cedvel/" + cedvelId;
    }

    private Boolean statusBoolean(String status) {
        return "hamisi".equals(status) ? null : !"passiv".equals(status);
    }
}
