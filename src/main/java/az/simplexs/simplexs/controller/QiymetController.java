package az.simplexs.simplexs.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import az.simplexs.simplexs.repository.qiymet.QiymetRepository;
import az.simplexs.simplexs.repository.teskilat.TeskilatRepository;
import az.simplexs.simplexs.repository.xidmet.XidmetRepository;
import jakarta.servlet.http.HttpSession;

@Controller
public class QiymetController {
    private final QiymetRepository repo;
    private final XidmetRepository xidmetRepo;
    private final TeskilatRepository teskilatRepo;
    public QiymetController(QiymetRepository r,XidmetRepository x,TeskilatRepository t){repo=r;xidmetRepo=x;teskilatRepo=t;}

    @GetMapping("/xidmetQiymetleri")
    public String list(@RequestParam(required=false)Long basliqId,Model m,HttpSession session){Long kid=klinikaId(session);var basliqlar=repo.basliqlar(kid);if(basliqId==null&&!basliqlar.isEmpty())basliqId=basliqlar.getFirst().id();var qruplar=repo.qruplar(kid,basliqId);m.addAttribute("pageTitle","Xidmət qiymətləri");m.addAttribute("activeMenuGroup","adminPanel");m.addAttribute("activeMenu","xidmetQiymetleri");m.addAttribute("basliqlar",basliqlar);m.addAttribute("qruplar",qruplar);m.addAttribute("qrupTeskilatIds",repo.qruplarinTeskilatIdleri(qruplar.stream().map(x->x.id()).toList()));m.addAttribute("selectedBasliqId",basliqId);m.addAttribute("teskilatlar",teskilatRepo.siyahi(kid).stream().filter(t->Boolean.TRUE.equals(t.aktiv())).toList());return "pages/xidmetQiymetleri";}

    @GetMapping("/xidmetQiymetleri/qrup/{qrupId}")
    public String tarifler(@PathVariable Long qrupId,@RequestParam(required=false)Long xidmetQrupuId,@RequestParam(required=false)String q,@RequestParam(required=false)String qiymetStatus,@RequestParam(defaultValue="1")int page,@RequestParam(defaultValue="100")int size,Model m,HttpSession session){Long kid=klinikaId(session);var qrup=repo.qruplar(kid,null).stream().filter(x->x.id().equals(qrupId)).findFirst().orElseThrow();size=Math.max(20,Math.min(size,100));long total=repo.xidmetSayi(qrupId,xidmetQrupuId,q,qiymetStatus);int totalPages=Math.max(1,(int)Math.ceil((double)total/size));page=Math.max(1,Math.min(page,totalPages));m.addAttribute("pageTitle",qrup.ad());m.addAttribute("activeMenuGroup","adminPanel");m.addAttribute("activeMenu","xidmetQiymetleri");m.addAttribute("qrup",qrup);m.addAttribute("xidmetQruplari",xidmetRepo.qruplar(kid));m.addAttribute("xidmetler",repo.xidmetler(qrupId,xidmetQrupuId,q,qiymetStatus,size,(page-1)*size));m.addAttribute("selectedXidmetQrupuId",xidmetQrupuId);m.addAttribute("q",q);m.addAttribute("qiymetStatus",qiymetStatus);m.addAttribute("totalCount",total);m.addAttribute("totalPages",totalPages);m.addAttribute("currentPage",page);m.addAttribute("pageSize",size);return "pages/xidmetQiymetTarifleri";}

    @PostMapping("/xidmetQiymetleri/basliq/yeni") public String basliqYarat(@RequestParam String ad,@RequestParam(required=false)String aciqlama,HttpSession s,RedirectAttributes a){flash(repo.basliqYarat(klinikaId(s),ad,aciqlama),a,"Qiymət başlığı yaradıldı.");return redirect(null);}
    @PostMapping("/xidmetQiymetleri/basliq/yenile") public String basliqYenile(@RequestParam Long id,@RequestParam String ad,@RequestParam(required=false)String aciqlama,@RequestParam(defaultValue="false")boolean aktiv,RedirectAttributes a){flash(repo.basliqYenile(id,ad,aciqlama,aktiv),a,"Qiymət başlığı yeniləndi.");return redirect(id);}
    @PostMapping("/xidmetQiymetleri/qrup/yeni") public String qrupYarat(@RequestParam Long basliqId,@RequestParam String ad,@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE)LocalDate baslamaTarixi,@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE)LocalDate bitmeTarixi,@RequestParam(required=false)String aciqlama,@RequestParam(required=false)Long klonQrupId,@RequestParam(required=false)List<Long> teskilatIds,@RequestParam(defaultValue="false")boolean standartdir,@RequestParam(required=false)BigDecimal xestePayi,@RequestParam(required=false)BigDecimal sigortaPayi,@RequestParam(required=false)BigDecimal xesteEndirimi,@RequestParam(required=false)BigDecimal sigortaEndirimi,HttpSession s,RedirectAttributes a){Long kid=klinikaId(s);if(kid==null)return clinicRequired(a,basliqId);flash(repo.qrupYarat(basliqId,ad,baslamaTarixi,bitmeTarixi,aciqlama,klonQrupId,teskilatIds,kid,standartdir,xestePayi,sigortaPayi,xesteEndirimi,sigortaEndirimi),a,"Qiymət qrupu yaradıldı.");return redirect(basliqId);}
    @PostMapping("/xidmetQiymetleri/qrup/yenile") public String qrupYenile(@RequestParam Long id,@RequestParam Long basliqId,@RequestParam String ad,@RequestParam(required=false)String aciqlama,@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE)LocalDate baslamaTarixi,@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE)LocalDate bitmeTarixi,@RequestParam(required=false)List<Long> teskilatIds,@RequestParam(defaultValue="false")boolean standartdir,@RequestParam(required=false)BigDecimal xestePayi,@RequestParam(required=false)BigDecimal sigortaPayi,@RequestParam(required=false)BigDecimal xesteEndirimi,@RequestParam(required=false)BigDecimal sigortaEndirimi,@RequestParam(defaultValue="false")boolean aktiv,RedirectAttributes a){flash(repo.qrupYenile(id,ad,aciqlama,baslamaTarixi,bitmeTarixi,standartdir,aktiv,teskilatIds,xestePayi,sigortaPayi,xesteEndirimi,sigortaEndirimi),a,"Qiymət qrupu yeniləndi.");return redirect(basliqId);}
    @PostMapping("/xidmetQiymetleri/qiymet") public String qiymet(@RequestParam Long qrupId,@RequestParam Long xidmetId,@RequestParam BigDecimal qiymet,@RequestParam(required=false)BigDecimal xestePayFaizi,@RequestParam(required=false)BigDecimal xesteEndirimFaizi,@RequestParam(required=false)BigDecimal qurumEndirimFaizi,@RequestParam(defaultValue="false")boolean edvAktivdir,RedirectAttributes a){flash(repo.qiymetSaxla(qrupId,xidmetId,qiymet,xestePayFaizi,xesteEndirimFaizi,qurumEndirimFaizi,edvAktivdir),a,"Xidmət qiyməti yadda saxlanıldı.");return "redirect:/xidmetQiymetleri/qrup/"+qrupId;}
    @PostMapping("/xidmetQiymetleri/qiymetler") public String qiymetler(@RequestParam Long qrupId,@RequestParam String qiymetlerJson,RedirectAttributes a){if(qiymetlerJson.length()>1_000_000||!qiymetlerJson.trim().startsWith("["))a.addFlashAttribute("errorMessage","Göndərilən qiymət məlumatları düzgün deyil.");else flash(repo.qiymetleriSaxla(qrupId,qiymetlerJson),a,"Dəyişdirilmiş xidmət qiymətləri yadda saxlanıldı.");return "redirect:/xidmetQiymetleri/qrup/"+qrupId;}

    private Long klinikaId(HttpSession s){return (Long)s.getAttribute(KlinikaController.SELECTED_KLINIKA_ID);}
    private String clinicRequired(RedirectAttributes a,Long b){a.addFlashAttribute("errorMessage","Əvvəlcə klinika seçin.");return redirect(b);}
    private void flash(Map<String,Object>r,RedirectAttributes a,String f){String status=String.valueOf(r.getOrDefault("status_kodu",""));a.addFlashAttribute(status.toUpperCase().contains("UGUR")||status.equals("1")?"successMessage":"errorMessage",String.valueOf(r.getOrDefault("mesaj",f)));}
    private String redirect(Long b){return "redirect:/xidmetQiymetleri"+(b==null?"":"?basliqId="+b);}
}
