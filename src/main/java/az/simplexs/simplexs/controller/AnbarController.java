package az.simplexs.simplexs.controller;

import java.math.BigDecimal;
import java.util.Map;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import az.simplexs.simplexs.repository.anbar.AnbarRepository;
import az.simplexs.simplexs.security.AuthenticatedPersonal;
import jakarta.servlet.http.HttpSession;

@Controller
public class AnbarController {
    private final AnbarRepository repository;
    private final MessageSource messages;
    public AnbarController(AnbarRepository repository,MessageSource messages){this.repository=repository;this.messages=messages;}

    @GetMapping("/parametrler/anbar")
    public String legacy(){return "redirect:/anbar/anbarlar";}

    @GetMapping("/anbar/{tab}")
    public String page(@PathVariable String tab,@RequestParam(defaultValue="aktiv")String status,
            @RequestParam(required=false)String axtaris,@RequestParam(required=false)String istiqamet,Model model,HttpSession session){
        if(!java.util.Set.of("anbarlar","firmalar","vahidler","qruplar","materiallar","emeliyyatlar").contains(tab))
            return "redirect:/anbar/anbarlar";
        Long k=clinic(session);Boolean aktiv=status(status);
        model.addAttribute("pageTitle",msg("warehouse.tab."+tab));model.addAttribute("activeMenu","anbar."+tab);
        model.addAttribute("tab",tab);model.addAttribute("status",status);model.addAttribute("axtaris",axtaris);
        model.addAttribute("filterApplied",(axtaris!=null&&!axtaris.isBlank())||!"aktiv".equals(status));
        model.addAttribute("firmalar",java.util.List.of());model.addAttribute("vahidler",java.util.List.of());
        model.addAttribute("qrupNovleri",java.util.List.of());model.addAttribute("qruplar",java.util.List.of());
        model.addAttribute("materiallar",java.util.List.of());model.addAttribute("kateqoriyalar",java.util.List.of());
        model.addAttribute("emeliyyatlar",java.util.List.of());model.addAttribute("anbarNovleri",java.util.List.of());
        model.addAttribute("anbarlar",java.util.List.of());model.addAttribute("istiqamet",istiqamet);
        switch(tab){
            case "firmalar" -> model.addAttribute("firmalar",repository.firmalar(aktiv).stream().filter(x->matches(axtaris,x.musteriNomresi(),x.ad(),x.unvan(),x.email())).toList());
            case "vahidler" -> model.addAttribute("vahidler",repository.vahidler(aktiv).stream().filter(x->matches(axtaris,x.ad(),x.altVahidAdi())).toList());
            case "qruplar" -> {model.addAttribute("qrupNovleri",repository.qrupNovleri());model.addAttribute("qruplar",repository.qruplar(k,aktiv).stream().filter(x->matches(axtaris,x.kod(),x.ad(),x.aciqlama())).toList());}
            case "materiallar" -> {model.addAttribute("qruplar",repository.qruplar(k,true));model.addAttribute("vahidler",repository.vahidler(true));model.addAttribute("materiallar",repository.materiallar(k,null,aktiv,axtaris));}
            case "emeliyyatlar" -> {model.addAttribute("kateqoriyalar",repository.kateqoriyalar(istiqamet));model.addAttribute("emeliyyatlar",repository.emeliyyatlar(k,istiqamet,aktiv).stream().filter(x->matches(axtaris,x.kod(),x.ad(),x.kateqoriyaAdi(),x.aciqlama())).toList());}
            default -> {model.addAttribute("anbarNovleri",repository.anbarNovleri());model.addAttribute("anbarlar",repository.anbarlar(k,null,aktiv,axtaris));}
        }
        return "pages/anbarParametrleri";
    }

    @PostMapping("/parametrler/anbar/firma")
    public String firma(@RequestParam Map<String,String> f,HttpSession s,@AuthenticationPrincipal AuthenticatedPersonal p,RedirectAttributes a){Long id=L(f,"id");flash(id==null?repository.firmaYarat(clinic(s),f.get("ad"),f.get("musteriNomresi"),f.get("unvan"),f.get("telefon"),f.get("faks"),f.get("email"),f.get("qeyd"),f.get("bankAdi"),f.get("bankHesabi"),f.get("vergiNomresi"),f.get("vergiIdaresi"),p.personalId()):repository.firmaYenile(clinic(s),id,f.get("ad"),f.get("musteriNomresi"),f.get("unvan"),f.get("telefon"),f.get("faks"),f.get("email"),f.get("qeyd"),f.get("bankAdi"),f.get("bankHesabi"),f.get("vergiNomresi"),f.get("vergiIdaresi"),B(f,"aktiv"),p.personalId()),a);return back("firmalar");}
    @PostMapping("/parametrler/anbar/vahid")
    public String vahid(@RequestParam Map<String,String> f,HttpSession s,@AuthenticationPrincipal AuthenticatedPersonal p,RedirectAttributes a){Long id=L(f,"id");flash(id==null?repository.vahidYarat(clinic(s),f.get("ad"),L(f,"altVahidId"),D(f,"vurmaEmsali"),B(f,"sifarisdeGorunsun"),p.personalId()):repository.vahidYenile(clinic(s),id,f.get("ad"),L(f,"altVahidId"),D(f,"vurmaEmsali"),B(f,"sifarisdeGorunsun"),B(f,"aktiv"),p.personalId()),a);return back("vahidler");}
    @PostMapping("/parametrler/anbar/qrup")
    public String qrup(@RequestParam Map<String,String> f,HttpSession s,@AuthenticationPrincipal AuthenticatedPersonal p,RedirectAttributes a){Long id=L(f,"id");flash(repository.qrupYaddaSaxla(id==null,clinic(s),id,L(f,"qrupNovuId"),f.get("kod"),f.get("ad"),f.get("aciqlama"),B(f,"xesteyeCixis"),B(f,"sifarisdeGorunsun"),B(f,"xesteMaterial"),B(f,"cihazaCixis"),B(f,"aktiv"),p.personalId()),a);return back("qruplar");}
    @PostMapping("/parametrler/anbar/material")
    public String material(@RequestParam Map<String,String> f,HttpSession s,@AuthenticationPrincipal AuthenticatedPersonal p,RedirectAttributes a){Long id=L(f,"id");flash(repository.materialYaddaSaxla(id==null,id,clinic(s),L(f,"qrupId"),L(f,"vahidId"),f.get("ad"),f.get("qisaAd"),f.get("barkod"),D(f,"minimumMiqdar"),D(f,"maksimumMiqdar"),B(f,"mehvEdileBiler"),B(f,"paketdenKenar"),B(f,"aktiv"),f.get("farmasevtikMelumat"),f.get("istifadeQaydasi"),p.personalId()),a);return back("materiallar");}
    @PostMapping("/parametrler/anbar/emeliyyat")
    public String emeliyyat(@RequestParam Map<String,String> f,HttpSession s,@AuthenticationPrincipal AuthenticatedPersonal p,RedirectAttributes a){Long id=L(f,"id");flash(repository.emeliyyatYaddaSaxla(id==null,id,clinic(s),L(f,"kateqoriyaId"),f.get("kod"),f.get("ad"),B(f,"standartdir"),f.get("aciqlama"),B(f,"aktiv"),p.personalId()),a);return back("emeliyyatlar");}
    @PostMapping("/parametrler/anbar/anbar")
    public String anbar(@RequestParam Map<String,String> f,HttpSession s,@AuthenticationPrincipal AuthenticatedPersonal p,RedirectAttributes a){Long id=L(f,"id");flash(repository.anbarYaddaSaxla(id==null,id,clinic(s),L(f,"anbarNovuId"),f.get("kod"),f.get("ad"),f.get("aciqlama"),B(f,"telebAnbaridir"),B(f,"telebdeStokGorunsun"),B(f,"dermanPaketi"),B(f,"istehsalCixisi"),B(f,"mehvCixisi"),B(f,"aktiv"),p.personalId()),a);return back("anbarlar");}

    private Long clinic(HttpSession s){return (Long)s.getAttribute(KlinikaController.SELECTED_KLINIKA_ID);} private Boolean status(String s){return "hamisi".equals(s)?null:!"passiv".equals(s);} private String back(String tab){return "redirect:/anbar/"+tab;}
    private void flash(Map<String,Object> r,RedirectAttributes a){String code=String.valueOf(r.getOrDefault("status_kodu",""));boolean ok=code.equals("1")||code.toUpperCase().contains("UGUR");a.addFlashAttribute(ok?"successMessage":"errorMessage",String.valueOf(r.getOrDefault("mesaj",msg(ok?"warehouse.saved":"warehouse.save_failed"))));}
    private String msg(String k){return messages.getMessage(k,null,LocaleContextHolder.getLocale());}
    private static boolean matches(String query,String... values){if(query==null||query.isBlank())return true;String q=query.trim().toLowerCase(java.util.Locale.ROOT);return java.util.Arrays.stream(values).filter(java.util.Objects::nonNull).anyMatch(v->v.toLowerCase(java.util.Locale.ROOT).contains(q));}
    private static boolean B(Map<String,String>f,String k){return "true".equals(f.get(k));} private static Long L(Map<String,String>f,String k){try{return f.get(k)==null||f.get(k).isBlank()?null:Long.valueOf(f.get(k));}catch(Exception e){return null;}} private static BigDecimal D(Map<String,String>f,String k){try{return f.get(k)==null||f.get(k).isBlank()?null:new BigDecimal(f.get(k));}catch(Exception e){return null;}}
}
