package az.simplexs.simplexs.controller;
import java.util.Map;
import java.util.Locale;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import az.simplexs.simplexs.repository.muhasibat.MuhasibatKoduRepository;
import jakarta.servlet.http.HttpSession;
@Controller public class MuhasibatKoduController{
 private final MuhasibatKoduRepository repo;public MuhasibatKoduController(MuhasibatKoduRepository r){repo=r;}
 @GetMapping("/muhasibatKodu") public String list(@RequestParam(required=false)String ad,@RequestParam(required=false)Long tipId,@RequestParam(required=false)String aciqlama,@RequestParam(required=false)String status,Model m,HttpSession s){String selectedStatus=status==null?"aktiv":status.isBlank()||"hamisi".equals(status)?"hamisi":"passiv".equals(status)?"passiv":"aktiv";Boolean aktiv="hamisi".equals(selectedStatus)?null:"aktiv".equals(selectedStatus);String adFilter=norm(ad),aciqlamaFilter=norm(aciqlama);var kodlar=repo.siyahi(kid(s),aktiv).stream().filter(k->adFilter.isEmpty()||norm(k.ad()).contains(adFilter)).filter(k->tipId==null||tipId.equals(k.tipId())).filter(k->aciqlamaFilter.isEmpty()||norm(k.aciqlama()).contains(aciqlamaFilter)).toList();m.addAttribute("pageTitle","Mühasibat kodları");m.addAttribute("activeMenuGroup","adminPanel");m.addAttribute("activeMenu","muhasibatKodlari");m.addAttribute("kodlar",kodlar);m.addAttribute("tipler",repo.tipler());m.addAttribute("ad",ad);m.addAttribute("selectedTipId",tipId);m.addAttribute("aciqlama",aciqlama);m.addAttribute("selectedStatus",selectedStatus);m.addAttribute("filterApplied",!adFilter.isEmpty()||tipId!=null||!aciqlamaFilter.isEmpty()||!"aktiv".equals(selectedStatus));return "pages/muhasibatKodu";}
 @PostMapping("/muhasibatKodu/yeni") public String create(@RequestParam Long tipId,@RequestParam String ad,HttpSession s,RedirectAttributes a){flash(repo.yarat(kid(s),tipId,ad),a,"Mühasibat kodu yaradıldı.");return "redirect:/muhasibatKodu";}
 @PostMapping("/muhasibatKodu/yenile") public String update(@RequestParam Long muhasibatKoduId,@RequestParam String ad,@RequestParam(required=false)String aciqlama,@RequestParam(defaultValue="false")boolean aktiv,RedirectAttributes a){flash(repo.yenile(muhasibatKoduId,ad,aciqlama,aktiv),a,"Mühasibat kodu yeniləndi.");return "redirect:/muhasibatKodu";}
 private Long kid(HttpSession s){return(Long)s.getAttribute(KlinikaController.SELECTED_KLINIKA_ID);}private static String norm(String value){return value==null?"":value.trim().toLowerCase(Locale.forLanguageTag("az"));}private void flash(Map<String,Object>r,RedirectAttributes a,String f){String s=String.valueOf(r.getOrDefault("status_kodu",""));a.addFlashAttribute(s.toUpperCase().contains("UGUR")||s.equals("1")?"successMessage":"errorMessage",String.valueOf(r.getOrDefault("mesaj",f)));}
}
