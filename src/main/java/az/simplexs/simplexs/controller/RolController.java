package az.simplexs.simplexs.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.servlet.http.HttpSession;

import az.simplexs.simplexs.dto.rol.Rol;
import az.simplexs.simplexs.repository.rol.RolRepository;

@Controller
public class RolController {
    private final RolRepository rolRepository;

    public RolController(RolRepository rolRepository) {
        this.rolRepository = rolRepository;
    }

    @GetMapping("/rollar")
    public String rollar(@RequestParam(required = false) Long rolId, Model model, HttpSession session) {
        model.addAttribute("pageTitle", "Rollar və səlahiyyətlər");
        model.addAttribute("activeMenuGroup", "adminPanel");
        model.addAttribute("activeMenu", "rollar");
        model.addAttribute("selectedRol", new Rol(null, "", null, false, 0, false, null));
            var rollar = rolRepository.findAll((Long) session.getAttribute(KlinikaController.SELECTED_KLINIKA_ID));
            model.addAttribute("rollar", rollar);
            Long selectedRolId = rolId != null ? rolId : (rollar.isEmpty() ? null : rollar.getFirst().rolId());
            model.addAttribute("selectedRolId", selectedRolId);
            if (selectedRolId != null) {
                model.addAttribute("selectedRol", rollar.stream()
                    .filter(rol -> rol.rolId().equals(selectedRolId))
                    .findFirst()
                    .orElse(new Rol(null, "", null, false, 0, false, null)));
                model.addAttribute("modullar", rolRepository.findModullar(selectedRolId));
                model.addAttribute("selahiyyetler", rolRepository.findSelahiyyetler(selectedRolId));
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
        rolRepository.savePermissions(rolId, modulIds == null ? List.of() : modulIds, selahiyyetIds == null ? List.of() : selahiyyetIds);
        attributes.addFlashAttribute("successMessage", "Rolun modul və səlahiyyətləri yadda saxlanıldı.");
        return redirect(rolId);
    }

    private String redirect(Long rolId) {
        return "redirect:/rollar" + (rolId == null ? "" : "?rolId=" + rolId);
    }
}
