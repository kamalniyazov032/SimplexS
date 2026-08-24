package az.simplexs.simplexs.controller;

import java.util.Map;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import az.simplexs.simplexs.repository.rutin.RutinRepository;
import az.simplexs.simplexs.repository.xidmet.XidmetRepository;
import az.simplexs.simplexs.security.AuthenticatedPersonal;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/parametrler/rutinler")
public class RutinController {
    private static final int PAGE_SIZE=30;
    private final RutinRepository repo; private final XidmetRepository xidmetRepo; private final MessageSource messages; private final JsonMapper jsonMapper;
    public RutinController(RutinRepository repo,XidmetRepository xidmetRepo,MessageSource messages,JsonMapper jsonMapper){this.repo=repo;this.xidmetRepo=xidmetRepo;this.messages=messages;this.jsonMapper=jsonMapper;}

    @GetMapping
    public String siyahi(@RequestParam(required=false)String q,@RequestParam(defaultValue="aktiv")String status,
            @RequestParam(required=false)Long rutinId,Model model,HttpSession session){
        Boolean aktiv=status.isBlank()?null:!"passiv".equals(status);
        var rutinler=repo.siyahi(klinikaId(session),aktiv,q);
        if(rutinId==null&&!rutinler.isEmpty())rutinId=rutinler.getFirst().id();
        Long selectedId=rutinId;
        var selected=rutinler.stream().filter(r->r.id().equals(selectedId)).findFirst().orElse(null);
        model.addAttribute("pageTitle",msg("routines.title"));model.addAttribute("activeMenuGroup","adminPanel");model.addAttribute("activeMenu","rutinler");
        model.addAttribute("rutinler",rutinler);model.addAttribute("selectedRutin",selected);model.addAttribute("terkib",selected==null?java.util.List.of():repo.terkib(selected.id()));
        model.addAttribute("qruplar",xidmetRepo.qruplar(klinikaId(session)).stream().filter(x->Boolean.TRUE.equals(x.aktiv())).toList());
        model.addAttribute("q",q);model.addAttribute("selectedStatus",status);return "pages/rutinler";
    }
    @PostMapping("/yeni")
    public String yarat(@RequestParam String kod,@RequestParam String ad,@RequestParam(required=false)String aciqlama,
            @RequestParam(defaultValue="false")boolean rutinQiymetlerindenIstifadeEt,HttpSession session,
            @AuthenticationPrincipal AuthenticatedPersonal personal,RedirectAttributes flash){
        result(repo.yarat(klinikaId(session),kod,ad,aciqlama,rutinQiymetlerindenIstifadeEt,personal.personalId()),flash,msg("routines.created"));return redirect();
    }
    @PostMapping("/yenile")
    public String yenile(@RequestParam Long rutinId,@RequestParam String kod,@RequestParam String ad,
            @RequestParam(required=false)String aciqlama,@RequestParam(defaultValue="false")boolean rutinQiymetlerindenIstifadeEt,
            @RequestParam(defaultValue="false")boolean aktiv,HttpSession session,@AuthenticationPrincipal AuthenticatedPersonal personal,
            RedirectAttributes flash){
        result(repo.yenile(rutinId,klinikaId(session),kod,ad,aciqlama,rutinQiymetlerindenIstifadeEt,aktiv,personal.personalId()),flash,msg("routines.updated"));return redirect(rutinId);
    }
    @GetMapping("/xidmetler") @ResponseBody
    public Map<String,Object> xidmetler(@RequestParam Long rutinId,@RequestParam(required=false)Long qrupId,
            @RequestParam(required=false)String q,@RequestParam(defaultValue="0")int page,HttpSession session){
        int safePage=Math.max(0,page);var rows=repo.xidmetAxtar(klinikaId(session),qrupId,q,PAGE_SIZE+1,safePage*PAGE_SIZE);boolean more=rows.size()>PAGE_SIZE;
        return Map.of("terkib",repo.terkib(rutinId),"xidmetler",more?rows.subList(0,PAGE_SIZE):rows,"hasMore",more,"page",safePage);
    }
    @PostMapping("/xidmetler")
    public String terkibiSaxla(@RequestParam Long rutinId,@RequestParam String xidmetlerJson,
            @AuthenticationPrincipal AuthenticatedPersonal personal,HttpSession session,RedirectAttributes flash){
        String value=xidmetlerJson.trim();
        if(value.length()>1_000_000||!validContents(klinikaId(session),rutinId,value))flash.addFlashAttribute("errorMessage",msg("routines.invalid_contents"));
        else result(repo.terkibiSaxla(rutinId,value,personal.personalId()),flash,msg("routines.contents_saved"));
        return redirect(rutinId);
    }
    private boolean validContents(Long klinikaId,Long rutinId,String value){
        try{
            JsonNode rows=jsonMapper.readTree(value);
            if(!rows.isArray())return false;
            boolean priceRequired=repo.rutinQiymetlerindenIstifadeEdir(klinikaId,rutinId);
            for(JsonNode row:rows){
                JsonNode serviceId=row.get("xidmet_id"),price=row.get("qiymet");
                if(serviceId==null||!serviceId.canConvertToLong()||serviceId.longValue()<=0)return false;
                if(priceRequired&&(price==null||!price.isNumber()||price.decimalValue().signum()<0))return false;
                if(!priceRequired&&price!=null&&!price.isNull()&&(!price.isNumber()||price.decimalValue().signum()<0))return false;
            }
            return true;
        }catch(Exception e){return false;}
    }
    private Long klinikaId(HttpSession s){return (Long)s.getAttribute(KlinikaController.SELECTED_KLINIKA_ID);}
    private void result(Map<String,Object> r,RedirectAttributes f,String fallback){String status=String.valueOf(r.getOrDefault("status_kodu",""));String message=String.valueOf(r.getOrDefault("mesaj",fallback));f.addFlashAttribute(status.toUpperCase().contains("UGUR")||"1".equals(status)?"successMessage":"errorMessage",message);}
    private String msg(String key){return messages.getMessage(key,null,LocaleContextHolder.getLocale());}
    private String redirect(){return "redirect:/parametrler/rutinler";} private String redirect(Long id){return redirect()+"?rutinId="+id;}
}
