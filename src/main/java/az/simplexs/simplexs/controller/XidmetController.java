package az.simplexs.simplexs.controller;

import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import az.simplexs.simplexs.repository.xidmet.XidmetRepository;

@Controller
public class XidmetController {
    private final XidmetRepository repo;
    public XidmetController(XidmetRepository repo){this.repo=repo;}

    @GetMapping("/xidmetQruplari")
    public String qruplar(Model m){base(m,"Xidmət qrupları","xidmetQruplari");m.addAttribute("qruplar",repo.qruplar());return "pages/xidmetQruplari";}
    @PostMapping("/xidmetQruplari/yeni")
    public String qrupYarat(@RequestParam(required=false)Long parentId,@RequestParam String ad,RedirectAttributes a){flash(repo.qrupYarat(parentId,ad),a,"Xidmət qrupu yaradıldı.");return "redirect:/xidmetQruplari";}
    @PostMapping("/xidmetQruplari/yenile")
    public String qrupYenile(@RequestParam Long xidmetQrupuId,@RequestParam String ad,@RequestParam(required=false)Long parentId,@RequestParam(defaultValue="false")boolean aktiv,RedirectAttributes a){flash(repo.qrupYenile(xidmetQrupuId,ad,parentId,aktiv),a,"Xidmət qrupu yeniləndi.");return "redirect:/xidmetQruplari";}

    @GetMapping("/parXidmet")
    public String xidmetler(@RequestParam(required=false)Long qrupId,Model m){base(m,"Xidmətlər","xidmetler");m.addAttribute("xidmetler",repo.xidmetler(qrupId));m.addAttribute("qruplar",repo.qruplar());m.addAttribute("selectedQrupId",qrupId);m.addAttribute("muhasibatKodlari",repo.muhasibatKodlari());m.addAttribute("xidmetTipleri",repo.xidmetTipleri());m.addAttribute("hesabatNovleri",repo.hesabatNovleri());m.addAttribute("hesabatMecburiyyetleri",repo.hesabatMecburiyyetleri());return "pages/xidmet";}
    @PostMapping("/parXidmet/yeni")
    public String xidmetYarat(@RequestParam String kod,@RequestParam String ad,@RequestParam Long qrupId,@RequestParam Long muhasibatKoduId,@RequestParam Long xidmetTipiId,RedirectAttributes a){flash(repo.xidmetYarat(kod,ad,qrupId,muhasibatKoduId,xidmetTipiId),a,"Xidmət yaradıldı.");return "redirect:/parXidmet";}
    @PostMapping("/parXidmet/yenile")
    public String xidmetYenile(@RequestParam Long xidmetId,@RequestParam String ad,@RequestParam Long qrupId,@RequestParam Long muhasibatKoduId,@RequestParam Long xidmetTipiId,@RequestParam(required=false)String beynelxalqKod,@RequestParam(required=false)String beynelxalqAd,@RequestParam(required=false)Long hesabatNovuId,@RequestParam(required=false)Long hesabatMecburiyyetiId,@RequestParam(defaultValue="false")boolean aktiv,RedirectAttributes a){flash(repo.xidmetYenile(xidmetId,ad,qrupId,muhasibatKoduId,xidmetTipiId,beynelxalqKod,beynelxalqAd,hesabatNovuId,hesabatMecburiyyetiId,aktiv),a,"Xidmət yeniləndi.");return "redirect:/parXidmet";}

    private void base(Model m,String title,String active){m.addAttribute("pageTitle",title);m.addAttribute("activeMenuGroup","adminPanel");m.addAttribute("activeMenu",active);}
    private void flash(Map<String,Object> result,RedirectAttributes a,String fallback){String status=String.valueOf(result.getOrDefault("status_kodu",""));String message=String.valueOf(result.getOrDefault("mesaj",fallback));a.addFlashAttribute(status.toUpperCase().contains("UGUR")||status.equals("1")?"successMessage":"errorMessage",message);}
}
