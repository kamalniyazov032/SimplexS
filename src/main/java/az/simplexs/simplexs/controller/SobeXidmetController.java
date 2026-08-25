package az.simplexs.simplexs.controller;

import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import az.simplexs.simplexs.repository.sobe.SobeRepository;
import az.simplexs.simplexs.repository.xidmet.SobeXidmetRepository;
import az.simplexs.simplexs.repository.xidmet.XidmetRepository;
import jakarta.servlet.http.HttpSession;

@Controller
public class SobeXidmetController {
    private final SobeRepository sobeRepo;private final XidmetRepository xidmetRepo;private final SobeXidmetRepository relationRepo;
    public SobeXidmetController(SobeRepository s,XidmetRepository x,SobeXidmetRepository r){sobeRepo=s;xidmetRepo=x;relationRepo=r;}
    @GetMapping("/parShobeXidmet") public String list(@RequestParam(required=false)Long sobeId,@RequestParam(required=false)Long qrupId,@RequestParam(required=false)String q,@RequestParam(defaultValue="0")int page,@RequestParam(required=false)String bagliQ,@RequestParam(defaultValue="0")int bagliPage,Model m,HttpSession session){final int size=100;Long klinikaId=(Long)session.getAttribute(KlinikaController.SELECTED_KLINIKA_ID);var sobeler=sobeRepo.findByKlinikaId(klinikaId).stream().filter(s->Boolean.TRUE.equals(s.aktiv())).toList();if(sobeId==null&&!sobeler.isEmpty())sobeId=sobeler.getFirst().sobeId();Long selected=sobeId;if(sobeler.stream().noneMatch(s->s.sobeId().equals(selected)))sobeId=null;page=Math.max(0,page);bagliPage=Math.max(0,bagliPage);long availableCount=xidmetRepo.countAvailableForDepartment(klinikaId,sobeId,qrupId,q);long assignedCount=relationRepo.count(sobeId,bagliQ);int availablePages=(int)Math.ceil(availableCount/(double)size),assignedPages=(int)Math.ceil(assignedCount/(double)size);if(page>0&&page>=availablePages)page=Math.max(0,availablePages-1);if(bagliPage>0&&bagliPage>=assignedPages)bagliPage=Math.max(0,assignedPages-1);var available=xidmetRepo.availableForDepartment(klinikaId,sobeId,qrupId,q,size,page*size);var assigned=relationRepo.findPage(sobeId,bagliQ,size,bagliPage*size);m.addAttribute("pageTitle","Şöbə xidmətləri");m.addAttribute("activeMenuGroup","adminPanel");m.addAttribute("activeMenu","sobeXidmetleri");m.addAttribute("sobeler",sobeler);m.addAttribute("selectedSobeId",sobeId);m.addAttribute("bagliXidmetler",assigned);m.addAttribute("movcudXidmetler",available);m.addAttribute("xidmetQruplari",xidmetRepo.qruplar(klinikaId).stream().filter(group->Boolean.TRUE.equals(group.aktiv())).toList());m.addAttribute("selectedQrupId",qrupId);m.addAttribute("q",q);m.addAttribute("bagliQ",bagliQ);m.addAttribute("availableCount",availableCount);m.addAttribute("assignedCount",assignedCount);m.addAttribute("page",page);m.addAttribute("bagliPage",bagliPage);m.addAttribute("availablePages",availablePages);m.addAttribute("assignedPages",assignedPages);return "pages/shobeXidmet";}
    @PostMapping("/parShobeXidmet/elave") public String add(@RequestParam Long sobeId,@RequestParam List<Long> xidmetIdleri,HttpSession s,RedirectAttributes a){if(!belongs(sobeId,s))return error(a,"Şöbə seçilmiş klinikaya aid deyil.",sobeId);flash(relationRepo.add(sobeId,xidmetIdleri),a,"Xidmətlər şöbəyə əlavə edildi.");return redirect(sobeId);}
    @PostMapping("/parShobeXidmet/cixar") public String remove(@RequestParam Long sobeId,@RequestParam List<Long> xidmetIdleri,HttpSession s,RedirectAttributes a){if(!belongs(sobeId,s))return error(a,"Şöbə seçilmiş klinikaya aid deyil.",sobeId);flash(relationRepo.remove(sobeId,xidmetIdleri),a,"Xidmətlər şöbədən çıxarıldı.");return redirect(sobeId);}
    private boolean belongs(Long id,HttpSession s){Long kid=(Long)s.getAttribute(KlinikaController.SELECTED_KLINIKA_ID);return sobeRepo.findByKlinikaId(kid).stream().anyMatch(x->x.sobeId().equals(id));}
    private void flash(Map<String,Object> r,RedirectAttributes a,String fallback){String status=String.valueOf(r.getOrDefault("status_kodu",""));String msg=String.valueOf(r.getOrDefault("mesaj",fallback));a.addFlashAttribute(status.toUpperCase().contains("UGUR")||status.equals("1")?"successMessage":"errorMessage",msg);}
    private String error(RedirectAttributes a,String msg,Long id){a.addFlashAttribute("errorMessage",msg);return redirect(id);}private String redirect(Long id){return "redirect:/parShobeXidmet?sobeId="+id;}
}
