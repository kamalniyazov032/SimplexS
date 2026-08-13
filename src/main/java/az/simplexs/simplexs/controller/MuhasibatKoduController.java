package az.simplexs.simplexs.controller;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import az.simplexs.simplexs.repository.muhasibat.MuhasibatKoduRepository;
import jakarta.servlet.http.HttpSession;
@Controller public class MuhasibatKoduController{
 private final MuhasibatKoduRepository repo;public MuhasibatKoduController(MuhasibatKoduRepository r){repo=r;}
 @GetMapping("/muhasibatKodu") public String list(Model m,HttpSession s){m.addAttribute("pageTitle","Mühasibat kodları");m.addAttribute("activeMenuGroup","adminPanel");m.addAttribute("activeMenu","muhasibatKodlari");m.addAttribute("kodlar",repo.siyahi(kid(s)));m.addAttribute("tipler",repo.tipler());return "pages/muhasibatKodu";}
 @PostMapping("/muhasibatKodu/yeni") public String create(@RequestParam Long tipId,@RequestParam String ad,HttpSession s,RedirectAttributes a){flash(repo.yarat(kid(s),tipId,ad),a,"Mühasibat kodu yaradıldı.");return "redirect:/muhasibatKodu";}
 @PostMapping("/muhasibatKodu/yenile") public String update(@RequestParam Long muhasibatKoduId,@RequestParam String ad,@RequestParam(required=false)String aciqlama,@RequestParam(required=false)Integer siraNo,@RequestParam(defaultValue="false")boolean aktiv,RedirectAttributes a){flash(repo.yenile(muhasibatKoduId,ad,aciqlama,siraNo,aktiv),a,"Mühasibat kodu yeniləndi.");return "redirect:/muhasibatKodu";}
 private Long kid(HttpSession s){return(Long)s.getAttribute(KlinikaController.SELECTED_KLINIKA_ID);}private void flash(Map<String,Object>r,RedirectAttributes a,String f){String s=String.valueOf(r.getOrDefault("status_kodu",""));a.addFlashAttribute(s.toUpperCase().contains("UGUR")||s.equals("1")?"successMessage":"errorMessage",String.valueOf(r.getOrDefault("mesaj",f)));}
}
