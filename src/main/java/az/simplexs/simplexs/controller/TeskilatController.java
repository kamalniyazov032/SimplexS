package az.simplexs.simplexs.controller;

import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import az.simplexs.simplexs.repository.teskilat.TeskilatRepository;
import jakarta.servlet.http.HttpSession;

@Controller
public class TeskilatController {
    private static final Locale AZ = Locale.forLanguageTag("az");
    private final TeskilatRepository repo;
    public TeskilatController(TeskilatRepository repo){this.repo=repo;}

    @GetMapping("/teskilatlar")
    public String list(@RequestParam(required=false)String q,@RequestParam(required=false)Long tipId,
            @RequestParam(required=false)String status,@RequestParam(required=false)String standart,Model m,HttpSession session){
        var all=repo.siyahi(klinikaId(session));String query=q==null?"":q.trim().toLowerCase(AZ);
        var list=all.stream().filter(x->query.isEmpty()||contains(x.ad(),query)||contains(x.qisaAd(),query)||contains(x.kod(),query)||contains(x.vergiNomresi(),query)||contains(x.selahiyyetliSexs(),query)||contains(x.mobilNomre(),query))
                .filter(x->tipId==null||tipId.equals(x.tipId()))
                .filter(x->status==null||status.isBlank()||("aktiv".equals(status)==Boolean.TRUE.equals(x.aktiv())))
                .filter(x->standart==null||standart.isBlank()||("standart".equals(standart)==Boolean.TRUE.equals(x.standartdir()))).toList();
        m.addAttribute("pageTitle","Təşkilatlar");m.addAttribute("activeMenuGroup","adminPanel");m.addAttribute("activeMenu","teskilatlar");m.addAttribute("teskilatlar",list);m.addAttribute("tipler",repo.tipler());m.addAttribute("umumiSayi",all.size());m.addAttribute("aktivSayi",all.stream().filter(x->Boolean.TRUE.equals(x.aktiv())).count());m.addAttribute("standartSayi",all.stream().filter(x->Boolean.TRUE.equals(x.standartdir())).count());m.addAttribute("filterApplied",!query.isEmpty()||tipId!=null||(status!=null&&!status.isBlank())||(standart!=null&&!standart.isBlank()));m.addAttribute("q",q);m.addAttribute("selectedTipId",tipId);m.addAttribute("selectedStatus",status);m.addAttribute("selectedStandart",standart);return "pages/teskilatlar";
    }
    @PostMapping("/teskilatlar/yeni") public String create(@RequestParam Long tipId,@RequestParam String ad,@RequestParam(required=false)String kod,@RequestParam(required=false)String qisaAd,@RequestParam(required=false)String bankHesabNomresi,@RequestParam(required=false)String seherNomresi,@RequestParam(required=false)String mobilNomre,@RequestParam(required=false)String vergiNomresi,@RequestParam(required=false)String selahiyyetliSexs,@RequestParam(defaultValue="false")boolean standartdir,HttpSession s,RedirectAttributes a){flash(repo.yarat(klinikaId(s),tipId,ad,kod,qisaAd,bankHesabNomresi,seherNomresi,mobilNomre,vergiNomresi,selahiyyetliSexs,standartdir),a,"Təşkilat yaradıldı.");return "redirect:/teskilatlar";}
    @PostMapping("/teskilatlar/yenile") public String update(@RequestParam Long teskilatId,@RequestParam Long tipId,@RequestParam String ad,@RequestParam(required=false)String kod,@RequestParam(required=false)String qisaAd,@RequestParam(required=false)String bankHesabNomresi,@RequestParam(required=false)String seherNomresi,@RequestParam(required=false)String mobilNomre,@RequestParam(required=false)String vergiNomresi,@RequestParam(required=false)String selahiyyetliSexs,@RequestParam(defaultValue="false")boolean standartdir,@RequestParam(defaultValue="false")boolean aktiv,RedirectAttributes a){flash(repo.yenile(teskilatId,tipId,ad,kod,qisaAd,bankHesabNomresi,seherNomresi,mobilNomre,vergiNomresi,selahiyyetliSexs,standartdir,aktiv),a,"Təşkilat yeniləndi.");return "redirect:/teskilatlar";}
    private void flash(Map<String,Object> r,RedirectAttributes a,String fallback){String status=String.valueOf(r.getOrDefault("status_kodu",""));String msg=String.valueOf(r.getOrDefault("mesaj",fallback));a.addFlashAttribute(status.toUpperCase().contains("UGUR")||status.equals("1")?"successMessage":"errorMessage",msg);}
    private static boolean contains(String value,String query){return value!=null&&value.toLowerCase(AZ).contains(query);}
    private Long klinikaId(HttpSession s){return (Long)s.getAttribute(KlinikaController.SELECTED_KLINIKA_ID);}
}
