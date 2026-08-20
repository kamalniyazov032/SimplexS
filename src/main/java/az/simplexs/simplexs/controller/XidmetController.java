package az.simplexs.simplexs.controller;

import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import az.simplexs.simplexs.dto.xidmet.Xidmet;
import az.simplexs.simplexs.repository.xidmet.XidmetRepository;
import az.simplexs.simplexs.security.AuthenticatedPersonal;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@Controller
public class XidmetController {
    private final XidmetRepository repo;
    private final MessageSource messageSource;
    public XidmetController(XidmetRepository repo,MessageSource messageSource){this.repo=repo;this.messageSource=messageSource;}

    @GetMapping("/xidmetQruplari")
    public String qruplar(@RequestParam(required=false)String q,@RequestParam(required=false)String status,
            @RequestParam(required=false)String qrupTipi,Model m,HttpSession session){
        String selectedStatus=status==null?"aktiv":status;
        base(m,"Xidmət qrupları","xidmetQruplari");
        var all=repo.qruplar(klinikaId(session));
        String query=hasText(q)?q.trim().toLowerCase(Locale.forLanguageTag("az")):null;
        var filtered=all.stream()
                .filter(x->query==null||contains(x.kod(),query)||contains(x.ad(),query)||contains(x.tamYol(),query))
                .filter(x->!hasText(selectedStatus)||("aktiv".equals(selectedStatus)==Boolean.TRUE.equals(x.aktiv())))
                .filter(x->!hasText(qrupTipi)||("esas".equals(qrupTipi)==Boolean.TRUE.equals(x.kokQrupdur())))
                .toList();
        m.addAttribute("qruplar",filtered);m.addAttribute("allQruplar",all);m.addAttribute("qrupCount",filtered.size());
        m.addAttribute("filterApplied",hasText(q)||!"aktiv".equals(selectedStatus)||hasText(qrupTipi));
        m.addAttribute("q",q);m.addAttribute("selectedStatus",selectedStatus);m.addAttribute("selectedQrupTipi",qrupTipi);
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
            @RequestParam(required=false)String paketStatusu,
            @RequestParam(required=false)String q,Model m,HttpSession session){
        String selectedStatus=status==null?"aktiv":status;
        base(m,"Xidmətlər","xidmetler");
        Long kid=klinikaId(session);var qruplar=repo.qruplar(kid);var all=repo.xidmetler(kid,qrupId);
        var filtered=all.stream()
                .filter(x->xidmetTipiId==null||xidmetTipiId.equals(x.tipId()))
                .filter(x->muhasibatKoduId==null||muhasibatKoduId.equals(x.muhasibatKoduId()))
                .filter(x->selectedStatus.isBlank()||("aktiv".equals(selectedStatus)==Boolean.TRUE.equals(x.aktiv())))
                .filter(x->!hasText(paketStatusu)||("paket".equals(paketStatusu)==Boolean.TRUE.equals(x.paketXidmet())))
                .filter(x->matches(x,q)).toList();
        m.addAttribute("xidmetler",filtered);m.addAttribute("xidmetCount",filtered.size());
        m.addAttribute("filterApplied",xidmetTipiId!=null||muhasibatKoduId!=null||!"aktiv".equals(selectedStatus)||hasText(paketStatusu)||hasText(q));
        m.addAttribute("qruplar",qruplar);m.addAttribute("selectedQrupId",qrupId);
        m.addAttribute("selectedQrup",qruplar.stream().filter(x->x.id().equals(qrupId)).findFirst().orElse(null));
        m.addAttribute("selectedXidmetTipiId",xidmetTipiId);m.addAttribute("selectedMuhasibatKoduId",muhasibatKoduId);
        m.addAttribute("selectedStatus",selectedStatus);m.addAttribute("selectedPaketStatusu",paketStatusu);m.addAttribute("q",q);
        m.addAttribute("muhasibatKodlari",repo.muhasibatKodlari(kid));m.addAttribute("xidmetTipleri",repo.xidmetTipleri());
        m.addAttribute("hesabatNovleri",repo.hesabatNovleri());m.addAttribute("hesabatMecburiyyetleri",repo.hesabatMecburiyyetleri());
        return "pages/xidmet";
    }
    @PostMapping("/parXidmet/yeni")
    public String xidmetYarat(@RequestParam String kod,@RequestParam String ad,@RequestParam Long qrupId,
            @RequestParam Long muhasibatKoduId,@RequestParam Long xidmetTipiId,
            @RequestParam(required=false)String beynelxalqKod,@RequestParam(required=false)String beynelxalqAd,
            @RequestParam(required=false)Long hesabatNovuId,@RequestParam(required=false)Long hesabatMecburiyyetiId,
            @RequestParam(defaultValue="false")boolean paketXidmet,
            @RequestParam(defaultValue="true")boolean aktiv,HttpSession s,RedirectAttributes a){
        flash(repo.xidmetYarat(klinikaId(s),kod,ad,qrupId,muhasibatKoduId,xidmetTipiId,beynelxalqKod,beynelxalqAd,
                hesabatNovuId,hesabatMecburiyyetiId,paketXidmet,aktiv),a,"Xidmət yaradıldı.");return "redirect:/parXidmet";}
    @PostMapping("/parXidmet/yenile")
    public String xidmetYenile(@RequestParam Long xidmetId,@RequestParam String ad,@RequestParam Long qrupId,@RequestParam Long muhasibatKoduId,@RequestParam Long xidmetTipiId,@RequestParam(required=false)String beynelxalqKod,@RequestParam(required=false)String beynelxalqAd,@RequestParam(required=false)Long hesabatNovuId,@RequestParam(required=false)Long hesabatMecburiyyetiId,@RequestParam(defaultValue="false")boolean paketXidmet,@RequestParam(defaultValue="false")boolean aktiv,RedirectAttributes a){flash(repo.xidmetYenile(xidmetId,ad,qrupId,muhasibatKoduId,xidmetTipiId,beynelxalqKod,beynelxalqAd,hesabatNovuId,hesabatMecburiyyetiId,paketXidmet,aktiv),a,"Xidmət yeniləndi.");return "redirect:/parXidmet";}

    @GetMapping("/parXidmet/paket-terkibi")
    @ResponseBody
    public Map<String,Object> paketTerkibi(@RequestParam Long paketXidmetId,
            @RequestParam(required=false)Long qrupId,@RequestParam(required=false)String q,
            @RequestParam(defaultValue="0")int page,HttpSession session){
        int safePage=Math.max(0,page);int size=30;
        var candidates=repo.paketUcunAxtar(klinikaId(session),paketXidmetId,qrupId,q,size+1,safePage*size);
        boolean hasMore=candidates.size()>size;
        return Map.of("terkib",repo.paketXidmetleri(paketXidmetId),
                "xidmetler",hasMore?candidates.subList(0,size):candidates,"hasMore",hasMore,"page",safePage);
    }

    @PostMapping("/parXidmet/paket-terkibi")
    public String paketTerkibiniSaxla(@RequestParam Long paketXidmetId,@RequestParam String xidmetlerJson,
            @AuthenticationPrincipal AuthenticatedPersonal personal,RedirectAttributes a){
        if(xidmetlerJson.length()>1_000_000||!xidmetlerJson.trim().startsWith("[")||!xidmetlerJson.trim().endsWith("]")){
            a.addFlashAttribute("errorMessage",msg("services.paket_terkibi_duzgun_deyil"));
        }else{
            flash(repo.paketXidmetleriniSaxla(paketXidmetId,xidmetlerJson,personal.personalId()),a,
                    msg("services.paket_terkibi_yenilendi"));
        }
        return "redirect:/parXidmet";
    }

    private void base(Model m,String title,String active){m.addAttribute("pageTitle",title);m.addAttribute("activeMenuGroup","adminPanel");m.addAttribute("activeMenu",active);}
    private Long klinikaId(HttpSession s){return (Long)s.getAttribute(KlinikaController.SELECTED_KLINIKA_ID);}
    private void flash(Map<String,Object> result,RedirectAttributes a,String fallback){String status=String.valueOf(result.getOrDefault("status_kodu",""));String message=String.valueOf(result.getOrDefault("mesaj",fallback));a.addFlashAttribute(status.toUpperCase().contains("UGUR")||status.equals("1")?"successMessage":"errorMessage",message);}
    private boolean matches(Xidmet x,String query){if(!hasText(query))return true;String n=query.trim().toLowerCase(Locale.forLanguageTag("az"));return contains(x.kod(),n)||contains(x.ad(),n)||contains(x.beynelxalqKod(),n)||contains(x.beynelxalqAd(),n);}
    private boolean contains(String value,String query){return value!=null&&value.toLowerCase(Locale.forLanguageTag("az")).contains(query);}
    private boolean hasText(String value){return value!=null&&!value.isBlank();}
    private String msg(String key){return messageSource.getMessage(key,null,LocaleContextHolder.getLocale());}
}
