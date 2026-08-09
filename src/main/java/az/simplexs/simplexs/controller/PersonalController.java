package az.simplexs.simplexs.controller;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import az.simplexs.simplexs.dto.personal.PersonalListItem;
import az.simplexs.simplexs.dto.personal.PersonalRol;
import az.simplexs.simplexs.dto.rol.Rol;
import az.simplexs.simplexs.repository.personal.PersonalRepository;
import az.simplexs.simplexs.repository.rol.RolRepository;
import az.simplexs.simplexs.repository.vezife.VezifeRepository;
import jakarta.servlet.http.HttpSession;

@Controller
public class PersonalController {
    private static final Locale AZ_LOCALE = Locale.forLanguageTag("az");

    private final PersonalRepository repo;
    private final VezifeRepository vezife;
    private final RolRepository rol;

    public PersonalController(PersonalRepository repo, VezifeRepository vezife, RolRepository rol) {
        this.repo = repo;
        this.vezife = vezife;
        this.rol = rol;
    }

    @GetMapping("/emekdash")
    public String list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long vezifeId,
            @RequestParam(required = false) String personalTipi,
            @RequestParam(required = false) String status,
            Model model,
            HttpSession session) {
        Long klinikaId = (Long) session.getAttribute(KlinikaController.SELECTED_KLINIKA_ID);
        List<PersonalListItem> allPersonals = repo.find(klinikaId);
        List<PersonalListItem> filteredPersonals = allPersonals.stream()
                .filter(personal -> matchesQuery(personal, q))
                .filter(personal -> vezifeId == null || vezifeId.equals(personal.vezifeId()))
                .filter(personal -> matchesType(personal, personalTipi))
                .filter(personal -> matchesStatus(personal, status))
                .toList();
        List<Rol> activeRoles = rol.findAll().stream().filter(r -> Boolean.TRUE.equals(r.aktiv())).toList();
        Map<Long, List<PersonalRol>> personalRoles = new LinkedHashMap<>(repo.rolesByPersonal(
                filteredPersonals.stream().map(PersonalListItem::personalId).toList()));
        filteredPersonals.forEach(personal -> personalRoles.putIfAbsent(personal.personalId(), List.of()));
        Map<Long, List<Rol>> availableRoles = filteredPersonals.stream().collect(Collectors.toMap(
                PersonalListItem::personalId,
                personal -> {
                    Set<Long> assignedRoleIds = personalRoles.getOrDefault(personal.personalId(), List.of()).stream()
                            .map(PersonalRol::rolId).collect(Collectors.toSet());
                    return activeRoles.stream().filter(role -> !assignedRoleIds.contains(role.rolId())).toList();
                }));

        model.addAttribute("pageTitle", "Əməkdaş və Həkim");
        model.addAttribute("activeMenuGroup", "adminPanel");
        model.addAttribute("activeMenu", "emekdash");
        model.addAttribute("personallar", filteredPersonals);
        model.addAttribute("personalCount", filteredPersonals.size());
        model.addAttribute("allPersonalCount", allPersonals.size());
        model.addAttribute("hekimCount", allPersonals.stream().filter(p -> Boolean.TRUE.equals(p.hekimdir())).count());
        model.addAttribute("emekdashCount", allPersonals.stream().filter(p -> !Boolean.TRUE.equals(p.hekimdir())).count());
        model.addAttribute("activePersonalCount", allPersonals.stream().filter(p -> Boolean.TRUE.equals(p.personalAktiv())
                && Boolean.TRUE.equals(p.klinikaElagesiAktiv())).count());
        model.addAttribute("filterApplied", hasText(q) || vezifeId != null || hasText(personalTipi) || hasText(status));
        model.addAttribute("q", q);
        model.addAttribute("selectedVezifeId", vezifeId);
        model.addAttribute("selectedPersonalTipi", personalTipi);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("personalRoles", personalRoles);
        model.addAttribute("availableRoles", availableRoles);
        model.addAttribute("vezifeler", vezife.findAll().stream().filter(v -> Boolean.TRUE.equals(v.aktiv())).toList());
        return "pages/emekdash";
    }

    @PostMapping("/emekdash/yeni")
    public String create(@RequestParam Long vezifeId, @RequestParam String ad, @RequestParam String soyad,
            @RequestParam(defaultValue = "false") boolean hekimdir,
            HttpSession session, RedirectAttributes attributes) {
        Long klinikaId = (Long) session.getAttribute(KlinikaController.SELECTED_KLINIKA_ID);
        repo.create(klinikaId, vezifeId, ad, soyad, hekimdir);
        attributes.addFlashAttribute("successMessage", "Personal yaradıldı.");
        return "redirect:/emekdash";
    }

    @PostMapping("/emekdash/rol")
    public String role(@RequestParam Long personalId, @RequestParam Long rolId,
            @RequestParam(defaultValue = "true") boolean elaveEdilsin,
            @RequestParam(defaultValue = "false") boolean esasRol, RedirectAttributes attributes) {
        repo.role(personalId, rolId, elaveEdilsin, esasRol);
        attributes.addFlashAttribute("successMessage", "Personalın rolu yeniləndi.");
        return "redirect:/emekdash";
    }

    private boolean matchesQuery(PersonalListItem personal, String query) {
        if (!hasText(query)) {
            return true;
        }
        String normalized = query.trim().toLowerCase(AZ_LOCALE);
        return contains(personal.personalKodu(), normalized)
                || contains(personal.tamAd(), normalized)
                || contains(personal.ad(), normalized)
                || contains(personal.soyad(), normalized)
                || contains(personal.ataAdi(), normalized)
                || contains(personal.mobilNomre(), normalized)
                || contains(personal.daxiliNomre(), normalized)
                || contains(personal.isNomresi(), normalized)
                || contains(personal.email(), normalized);
    }

    private boolean matchesType(PersonalListItem personal, String personalTipi) {
        if (!hasText(personalTipi)) {
            return true;
        }
        return "hekim".equals(personalTipi) == Boolean.TRUE.equals(personal.hekimdir());
    }

    private boolean matchesStatus(PersonalListItem personal, String status) {
        if (!hasText(status)) {
            return true;
        }
        boolean active = Boolean.TRUE.equals(personal.personalAktiv())
                && Boolean.TRUE.equals(personal.klinikaElagesiAktiv());
        return "aktiv".equals(status) == active;
    }

    private boolean contains(String value, String normalizedQuery) {
        return value != null && value.toLowerCase(AZ_LOCALE).contains(normalizedQuery);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
