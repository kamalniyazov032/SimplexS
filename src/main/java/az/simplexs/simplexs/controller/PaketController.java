package az.simplexs.simplexs.controller;

import java.util.Map;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import az.simplexs.simplexs.repository.xidmet.XidmetRepository;
import az.simplexs.simplexs.security.AuthenticatedPersonal;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/parPaket")
public class PaketController {
    private static final int PAGE_SIZE=30;
    private final XidmetRepository repo; private final MessageSource messages;
    public PaketController(XidmetRepository repo,MessageSource messages){this.repo=repo;this.messages=messages;}

    @GetMapping
    public String siyahi(@RequestParam(required=false)String q,@RequestParam(defaultValue="aktiv")String status,
            @RequestParam(required=false)Long paketId,Model model,HttpSession session){
        Boolean aktiv=status.isBlank()?null:!"passiv".equals(status);
        var paketler=repo.paketler(klinikaId(session),aktiv,q);
        if(paketId==null&&!paketler.isEmpty())paketId=paketler.getFirst().id();
        Long selectedId=paketId;var selected=paketler.stream().filter(x->x.id().equals(selectedId)).findFirst().orElse(null);
        model.addAttribute("pageTitle",msg("packages.title"));model.addAttribute("activeMenuGroup","adminPanel");model.addAttribute("activeMenu","parPaket");
        model.addAttribute("paketler",paketler);model.addAttribute("selectedPaket",selected);
        model.addAttribute("terkib",selected==null?java.util.List.of():repo.paketXidmetleri(selected.id()));
        model.addAttribute("qruplar",repo.qruplar(klinikaId(session)).stream().filter(x->Boolean.TRUE.equals(x.aktiv())).toList());
        model.addAttribute("q",q);model.addAttribute("selectedStatus",status);return "pages/paket";
    }

    @GetMapping("/terkib") @ResponseBody
    public Map<String,Object> terkib(@RequestParam Long paketId,@RequestParam(required=false)Long qrupId,
            @RequestParam(required=false)String q,@RequestParam(defaultValue="0")int page,HttpSession session){
        int safePage=Math.max(0,page);var rows=repo.paketUcunAxtar(klinikaId(session),paketId,qrupId,q,PAGE_SIZE+1,safePage*PAGE_SIZE);
        boolean more=rows.size()>PAGE_SIZE;return Map.of("terkib",repo.paketXidmetleri(paketId),
                "xidmetler",more?rows.subList(0,PAGE_SIZE):rows,"hasMore",more,"page",safePage);
    }

    @PostMapping("/terkib")
    public String saxla(@RequestParam Long paketId,@RequestParam String xidmetlerJson,
            @AuthenticationPrincipal AuthenticatedPersonal personal,RedirectAttributes flash){
        String value=xidmetlerJson.trim();
        if(value.length()>1_000_000||!value.startsWith("[")||!value.endsWith("]"))flash.addFlashAttribute("errorMessage",msg("services.paket_terkibi_duzgun_deyil"));
        else result(repo.paketXidmetleriniSaxla(paketId,value,personal.personalId()),flash,msg("services.paket_terkibi_yenilendi"));
        return "redirect:/parPaket?paketId="+paketId;
    }
    private Long klinikaId(HttpSession s){return (Long)s.getAttribute(KlinikaController.SELECTED_KLINIKA_ID);}
    private void result(Map<String,Object> r,RedirectAttributes f,String fallback){String status=String.valueOf(r.getOrDefault("status_kodu",""));String message=String.valueOf(r.getOrDefault("mesaj",fallback));f.addFlashAttribute(status.toUpperCase().contains("UGUR")||"1".equals(status)?"successMessage":"errorMessage",message);}
    private String msg(String key){return messages.getMessage(key,null,LocaleContextHolder.getLocale());}
}
