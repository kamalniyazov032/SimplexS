package az.simplexs.simplexs.controller;

import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import az.simplexs.simplexs.dto.xidmet.Xidmet;
import az.simplexs.simplexs.repository.xidmet.XidmetRepository;
import jakarta.servlet.http.HttpSession;

@Controller
public class XidmetController {
    private final XidmetRepository repo;
    public XidmetController(XidmetRepository repo){this.repo=repo;}

    @GetMapping("/xidmetQruplari")
    public String qruplar(@RequestParam(required=false)String q,@RequestParam(required=false)String status,
            @RequestParam(required=false)String qrupTipi,Model m,HttpSession session){
        base(m,"Xidmət qrupları","xidmetQruplari");
        var all=repo.qruplar(klinikaId(session));
        String query=hasText(q)?q.trim().toLowerCase(Locale.forLanguageTag("az")):null;
        var filtered=all.stream()
                .filter(x->query==null||contains(x.kod(),query)||contains(x.ad(),query)||contains(x.tamYol(),query))
                .filter(x->!hasText(status)||("aktiv".equals(status)==Boolean.TRUE.equals(x.aktiv())))
                .filter(x->!hasText(qrupTipi)||("esas".equals(qrupTipi)==Boolean.TRUE.equals(x.kokQrupdur())))
                .toList();
        m.addAttribute("qruplar",filtered);m.addAttribute("allQruplar",all);m.addAttribute("qrupCount",filtered.size());
        m.addAttribute("filterApplied",hasText(q)||hasText(status)||hasText(qrupTipi));
        m.addAttribute("q",q);m.addAttribute("selectedStatus",status);m.addAttribute("selectedQrupTipi",qrupTipi);
        return "pages/xidmetQruplari";
    }
    @PostMapping("/xidmetQruplari/yeni")
    public String qrupYarat(@RequestParam(required=false)Long parentId,@RequestParam String ad,HttpSession s,RedirectAttributes a){flash(repo.qrupYarat(klinikaId(s),parentId,ad),a,"Xidmət qrupu yaradıldı.");return "redirect:/xidmetQruplari";}
    @PostMapping("/xidmetQruplari/yenile")
    public String qrupYenile(@RequestParam Long xidmetQrupuId,@RequestParam String ad,@RequestParam(required=false)Long parentId,@RequestParam(defaultValue="false")boolean aktiv,RedirectAttributes a){flash(repo.qrupYenile(xidmetQrupuId,ad,parentId,aktiv),a,"Xidmət qrupu yeniləndi.");return "redirect:/xidmetQruplari";}

    @GetMapping("/parXidmet")
    public String xidmetler(@RequestParam(required=false)Long qrupId,
            @RequestParam(required=false)Long xidmetTipiId,
            @RequestParam(required=false)Long muhasibatKoduId,
            @RequestParam(required=false)String status,
            @RequestParam(required=false)String q,Model m,HttpSession session){
        base(m,"Xidmətlər","xidmetler");
        Long kid=klinikaId(session);var all=repo.xidmetler(kid,qrupId);
        var filtered=all.stream()
                .filter(x->xidmetTipiId==null||xidmetTipiId.equals(x.tipId()))
                .filter(x->muhasibatKoduId==null||muhasibatKoduId.equals(x.muhasibatKoduId()))
                .filter(x->status==null||status.isBlank()||("aktiv".equals(status)==Boolean.TRUE.equals(x.aktiv())))
                .filter(x->matches(x,q)).toList();
        m.addAttribute("xidmetler",filtered);m.addAttribute("xidmetCount",filtered.size());
        m.addAttribute("filterApplied",qrupId!=null||xidmetTipiId!=null||muhasibatKoduId!=null||hasText(status)||hasText(q));
        m.addAttribute("qruplar",repo.qruplar(kid));m.addAttribute("selectedQrupId",qrupId);
        m.addAttribute("selectedXidmetTipiId",xidmetTipiId);m.addAttribute("selectedMuhasibatKoduId",muhasibatKoduId);
        m.addAttribute("selectedStatus",status);m.addAttribute("q",q);
        m.addAttribute("muhasibatKodlari",repo.muhasibatKodlari(kid));m.addAttribute("xidmetTipleri",repo.xidmetTipleri());
        m.addAttribute("hesabatNovleri",repo.hesabatNovleri());m.addAttribute("hesabatMecburiyyetleri",repo.hesabatMecburiyyetleri());
        return "pages/xidmet";
    }
    @PostMapping("/parXidmet/yeni")
    public String xidmetYarat(@RequestParam String kod,@RequestParam String ad,@RequestParam Long qrupId,
            @RequestParam Long muhasibatKoduId,@RequestParam Long xidmetTipiId,
            @RequestParam(required=false)String beynelxalqKod,@RequestParam(required=false)String beynelxalqAd,
            @RequestParam(required=false)Long hesabatNovuId,@RequestParam(required=false)Long hesabatMecburiyyetiId,
            @RequestParam(defaultValue="true")boolean aktiv,HttpSession s,RedirectAttributes a){
        flash(repo.xidmetYarat(klinikaId(s),kod,ad,qrupId,muhasibatKoduId,xidmetTipiId,beynelxalqKod,beynelxalqAd,
                hesabatNovuId,hesabatMecburiyyetiId,aktiv),a,"Xidmət yaradıldı.");return "redirect:/parXidmet";}
    @PostMapping("/parXidmet/yenile")
    public String xidmetYenile(@RequestParam Long xidmetId,@RequestParam String ad,@RequestParam Long qrupId,@RequestParam Long muhasibatKoduId,@RequestParam Long xidmetTipiId,@RequestParam(required=false)String beynelxalqKod,@RequestParam(required=false)String beynelxalqAd,@RequestParam(required=false)Long hesabatNovuId,@RequestParam(required=false)Long hesabatMecburiyyetiId,@RequestParam(defaultValue="false")boolean aktiv,RedirectAttributes a){flash(repo.xidmetYenile(xidmetId,ad,qrupId,muhasibatKoduId,xidmetTipiId,beynelxalqKod,beynelxalqAd,hesabatNovuId,hesabatMecburiyyetiId,aktiv),a,"Xidmət yeniləndi.");return "redirect:/parXidmet";}

    private void base(Model m,String title,String active){m.addAttribute("pageTitle",title);m.addAttribute("activeMenuGroup","adminPanel");m.addAttribute("activeMenu",active);}
    private Long klinikaId(HttpSession s){return (Long)s.getAttribute(KlinikaController.SELECTED_KLINIKA_ID);}
    private void flash(Map<String,Object> result,RedirectAttributes a,String fallback){String status=String.valueOf(result.getOrDefault("status_kodu",""));String message=String.valueOf(result.getOrDefault("mesaj",fallback));a.addFlashAttribute(status.toUpperCase().contains("UGUR")||status.equals("1")?"successMessage":"errorMessage",message);}
    private boolean matches(Xidmet x,String query){if(!hasText(query))return true;String n=query.trim().toLowerCase(Locale.forLanguageTag("az"));return contains(x.kod(),n)||contains(x.ad(),n)||contains(x.beynelxalqKod(),n)||contains(x.beynelxalqAd(),n);}
    private boolean contains(String value,String query){return value!=null&&value.toLowerCase(Locale.forLanguageTag("az")).contains(query);}
    private boolean hasText(String value){return value!=null&&!value.isBlank();}
}
