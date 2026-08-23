package az.simplexs.simplexs.controller;

import java.util.Locale;
import java.util.Map;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import az.simplexs.simplexs.repository.sebeb.SebebRepository;
import az.simplexs.simplexs.security.AuthenticatedPersonal;

@Controller @RequestMapping("/parametrler/sebebler")
public class SebebController {
    private final SebebRepository repo;private final MessageSource messages;
    public SebebController(SebebRepository repo,MessageSource messages){this.repo=repo;this.messages=messages;}
    @GetMapping public String siyahi(@RequestParam(required=false)Long novId,@RequestParam(required=false)String q,
            @RequestParam(defaultValue="aktiv")String status,Model model){
        var novler=repo.novler(true);if(novId==null&&!novler.isEmpty())novId=novler.getFirst().id();Long selectedId=novId;
        Boolean aktiv=status.isBlank()?null:!"passiv".equals(status);String query=q==null?"":q.trim().toLowerCase(Locale.forLanguageTag("az"));
        var sebebler=novId==null?java.util.List.<az.simplexs.simplexs.dto.sebeb.Sebeb>of():repo.sebebler(novId,null,aktiv).stream().filter(x->query.isEmpty()||contains(x.kod(),query)||contains(x.ad(),query)||contains(x.aciqlama(),query)).toList();
        model.addAttribute("pageTitle",msg("reasons.title"));model.addAttribute("activeMenuGroup","adminPanel");model.addAttribute("activeMenu","sebebler");model.addAttribute("novler",novler);model.addAttribute("selectedNov",novler.stream().filter(x->x.id().equals(selectedId)).findFirst().orElse(null));model.addAttribute("sebebler",sebebler);model.addAttribute("q",q);model.addAttribute("selectedStatus",status);return "pages/sebebler";
    }
    @PostMapping("/yeni") public String yarat(@RequestParam Long novId,@RequestParam String kod,@RequestParam String ad,@RequestParam(required=false)String aciqlama,@AuthenticationPrincipal AuthenticatedPersonal personal,RedirectAttributes flash){result(repo.yarat(novId,kod,ad,aciqlama,personal.personalId()),flash,msg("reasons.created"));return redirect(novId);}
    @PostMapping("/yenile") public String yenile(@RequestParam Long sebebId,@RequestParam Long novId,@RequestParam String kod,@RequestParam String ad,@RequestParam(required=false)String aciqlama,@RequestParam(defaultValue="false")boolean aktiv,@AuthenticationPrincipal AuthenticatedPersonal personal,RedirectAttributes flash){result(repo.yenile(sebebId,kod,ad,aciqlama,aktiv,personal.personalId()),flash,msg("reasons.updated"));return redirect(novId);}
    private void result(Map<String,Object> r,RedirectAttributes f,String fallback){String status=String.valueOf(r.getOrDefault("status_kodu",""));String message=String.valueOf(r.getOrDefault("mesaj",fallback));f.addFlashAttribute(status.toUpperCase().contains("UGUR")||"1".equals(status)?"successMessage":"errorMessage",message);}
    private String redirect(Long id){return "redirect:/parametrler/sebebler?novId="+id;}private boolean contains(String v,String q){return v!=null&&v.toLowerCase(Locale.forLanguageTag("az")).contains(q);}private String msg(String key){return messages.getMessage(key,null,LocaleContextHolder.getLocale());}
}
