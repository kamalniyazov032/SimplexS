package az.simplexs.simplexs.controller;

import java.util.List;
import java.util.LinkedHashMap;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.servlet.http.HttpSession;

import az.simplexs.simplexs.dto.rol.Rol;
import az.simplexs.simplexs.dto.rol.RolSelahiyyet;
import az.simplexs.simplexs.repository.rol.RolRepository;
import az.simplexs.simplexs.repository.personal.PersonalRepository;

@Controller
public class RolController {
    private final RolRepository rolRepository;
    private final PersonalRepository personalRepository;

    public RolController(RolRepository rolRepository, PersonalRepository personalRepository) {
        this.rolRepository = rolRepository;
        this.personalRepository = personalRepository;
    }

    @GetMapping("/rollar")
    public String rollar(@RequestParam(required = false) Long rolId, @RequestParam(required = false) String status, Model model, HttpSession session) {
        model.addAttribute("pageTitle", "Rollar və səlahiyyətlər");
        model.addAttribute("activeMenuGroup", "adminPanel");
        model.addAttribute("activeMenu", "rollar");
        model.addAttribute("selectedRol", new Rol(null, null, "", null, false, 0, false, null));
            String selectedStatus = status == null ? "aktiv" : status;
            var rollar = rolRepository.findAll((Long) session.getAttribute(KlinikaController.SELECTED_KLINIKA_ID)).stream()
                .filter(rol -> selectedStatus.isBlank() || ("aktiv".equals(selectedStatus) == Boolean.TRUE.equals(rol.aktiv())))
                .toList();
            model.addAttribute("rollar", rollar);
            Long selectedRolId = rolId != null && rollar.stream().anyMatch(rol -> rol.rolId().equals(rolId))
                ? rolId : (rollar.isEmpty() ? null : rollar.getFirst().rolId());
            model.addAttribute("selectedRolId", selectedRolId);
            model.addAttribute("selectedRoleStatus", selectedStatus);
            if (selectedRolId != null) {
                model.addAttribute("selectedRol", rollar.stream()
                    .filter(rol -> rol.rolId().equals(selectedRolId))
                    .findFirst()
                    .orElse(new Rol(null, null, "", null, false, 0, false, null)));
                model.addAttribute("modullar", rolRepository.findModullar(selectedRolId));
                var selahiyyetler = rolRepository.findSelahiyyetler(selectedRolId);
                var selahiyyetQruplari = new LinkedHashMap<Long, List<RolSelahiyyet>>();
                for (var selahiyyet : selahiyyetler) {
                    selahiyyetQruplari.computeIfAbsent(selahiyyet.modulId(), ignored -> new java.util.ArrayList<>())
                        .add(selahiyyet);
                }
                model.addAttribute("selahiyyetler", selahiyyetler);
                model.addAttribute("selahiyyetQruplari", selahiyyetQruplari);
                model.addAttribute("rolaBagliPersonallar", personalRepository.findByRole(selectedRolId, false));
            }
        return "pages/rollar";
    }

    @PostMapping("/rollar/yeni")
    public String create(@RequestParam String ad, @RequestParam(required = false) String aciqlama,
                         @RequestParam(defaultValue = "false") boolean sistemRoludur,
                         HttpSession session, RedirectAttributes attributes) {
        rolRepository.create((Long) session.getAttribute(KlinikaController.SELECTED_KLINIKA_ID), ad, aciqlama, sistemRoludur);
        attributes.addFlashAttribute("successMessage", "Rol yaradıldı.");
        return redirect(null);
    }

    @PostMapping("/rollar/yenile")
    public String update(@RequestParam Long rolId, @RequestParam String ad,
                         @RequestParam(required=false) String aciqlama,
                         @RequestParam(defaultValue="false") boolean sistemRoludur,
                         @RequestParam(defaultValue="false") boolean aktiv, RedirectAttributes attributes) {
        rolRepository.update(rolId, ad, aciqlama, sistemRoludur, aktiv);
        attributes.addFlashAttribute("successMessage", "Rolun adı yeniləndi.");
        return redirect(rolId);
    }

    @PostMapping("/rollar/sil")
    public String delete(@RequestParam Long rolId, RedirectAttributes attributes) {
        rolRepository.deactivate(rolId);
        attributes.addFlashAttribute("successMessage", "Rol deaktiv edildi.");
        return redirect(null);
    }

    @PostMapping("/rollar/icazeler")
    public String savePermissions(@RequestParam Long rolId,
                                  @RequestParam(required = false) List<Long> modulIds,
                                  @RequestParam(required = false) List<Long> selahiyyetIds,
                                  RedirectAttributes attributes) {
        var result = rolRepository.savePermissions(rolId, modulIds == null ? List.of() : modulIds,
            selahiyyetIds == null ? List.of() : selahiyyetIds);
        attributes.addFlashAttribute(result.ugurludur() ? "successMessage" : "errorMessage",
            result.mesaj() == null || result.mesaj().isBlank()
                ? (result.ugurludur() ? "Rolun modul və səlahiyyətləri yadda saxlanıldı." : "Əməliyyat uğursuz oldu.")
                : result.mesaj());
        return redirect(rolId);
    }

    private String redirect(Long rolId) {
        return "redirect:/rollar" + (rolId == null ? "" : "?rolId=" + rolId);
    }
}
