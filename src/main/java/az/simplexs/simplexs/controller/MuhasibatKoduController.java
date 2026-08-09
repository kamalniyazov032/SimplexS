package az.simplexs.simplexs.controller;

import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import az.simplexs.simplexs.repository.muhasibat.MuhasibatKoduRepository;

@Controller
public class MuhasibatKoduController {
    private final MuhasibatKoduRepository repo;
    public MuhasibatKoduController(MuhasibatKoduRepository repo){this.repo=repo;}
    @GetMapping("/muhasibatKodu") public String list(Model m){m.addAttribute("pageTitle","Mühasibat kodları");m.addAttribute("activeMenuGroup","adminPanel");m.addAttribute("activeMenu","muhasibatKodlari");m.addAttribute("kodlar",repo.siyahi());m.addAttribute("tipler",repo.tipler());return "pages/muhasibatKodu";}
    @PostMapping("/muhasibatKodu/yeni") public String create(@RequestParam Long tipId,@RequestParam String ad,RedirectAttributes a){flash(repo.yarat(tipId,ad),a,"Mühasibat kodu yaradıldı.");return "redirect:/muhasibatKodu";}
    @PostMapping("/muhasibatKodu/yenile") public String update(@RequestParam Long muhasibatKoduId,@RequestParam String ad,@RequestParam(required=false)String aciqlama,@RequestParam(required=false)Integer siraNo,@RequestParam(defaultValue="false")boolean aktiv,RedirectAttributes a){flash(repo.yenile(muhasibatKoduId,ad,aciqlama,siraNo,aktiv),a,"Mühasibat kodu yeniləndi.");return "redirect:/muhasibatKodu";}
    private void flash(Map<String,Object> r,RedirectAttributes a,String fallback){String status=String.valueOf(r.getOrDefault("status_kodu",""));String message=String.valueOf(r.getOrDefault("mesaj",fallback));a.addFlashAttribute(status.toUpperCase().contains("UGUR")||status.equals("1")?"successMessage":"errorMessage",message);}
}
