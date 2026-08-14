package az.simplexs.simplexs.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import az.simplexs.simplexs.repository.xeta.XetaJurnaliRepository;
import jakarta.servlet.http.HttpSession;

@Controller
public class XetaJurnaliController {
    private final XetaJurnaliRepository repository;
    public XetaJurnaliController(XetaJurnaliRepository repository){this.repository=repository;}

    @GetMapping("/xetaJurnali")
    public String list(@RequestParam(required=false)String q,@RequestParam(required=false)String nov,
            @RequestParam(required=false)@DateTimeFormat(iso=DateTimeFormat.ISO.DATE)LocalDate baslama,
            @RequestParam(required=false)@DateTimeFormat(iso=DateTimeFormat.ISO.DATE)LocalDate bitme,
            @RequestParam(defaultValue="1")int page,HttpSession session,Model model){
        int size=50; page=Math.max(page,1);
        Long klinikaId=(Long)session.getAttribute(KlinikaController.SELECTED_KLINIKA_ID);
        var items=repository.find(klinikaId,nov,q,baslama,bitme,size,(page-1)*size);
        long total=items.isEmpty()?0:items.getFirst().totalSayi();
        int pages=Math.max(1,(int)Math.ceil((double)total/size));
        model.addAttribute("pageTitle","Xəta jurnalı");model.addAttribute("activeMenu","xetaJurnali");
        model.addAttribute("xetalar",items);model.addAttribute("q",q);model.addAttribute("nov",nov);
        model.addAttribute("baslama",baslama);model.addAttribute("bitme",bitme);
        model.addAttribute("currentPage",page);model.addAttribute("totalPages",pages);model.addAttribute("totalCount",total);
        return "pages/xetaJurnali";
    }
}
