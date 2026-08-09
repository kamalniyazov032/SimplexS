package az.simplexs.simplexs.controller;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
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
    @GetMapping("/parShobeXidmet") public String list(@RequestParam(required=false)Long sobeId,Model m,HttpSession session){Long klinikaId=(Long)session.getAttribute(KlinikaController.SELECTED_KLINIKA_ID);var sobeler=sobeRepo.findByKlinikaId(klinikaId).stream().filter(s->Boolean.TRUE.equals(s.aktiv())).toList();if(sobeId==null&&!sobeler.isEmpty())sobeId=sobeler.getFirst().sobeId();Long selected=sobeId;boolean belongs=sobeler.stream().anyMatch(s->s.sobeId().equals(selected));if(!belongs)sobeId=null;var assigned=relationRepo.find(sobeId).stream().filter(x->Boolean.TRUE.equals(x.elaqeAktiv())).toList();Set<Long> assignedIds=assigned.stream().map(x->x.xidmetId()).collect(Collectors.toSet());var available=xidmetRepo.xidmetler(null).stream().filter(x->Boolean.TRUE.equals(x.aktiv())&&!assignedIds.contains(x.id())).toList();m.addAttribute("pageTitle","Şöbə xidmətləri");m.addAttribute("activeMenuGroup","adminPanel");m.addAttribute("activeMenu","sobeXidmetleri");m.addAttribute("sobeler",sobeler);m.addAttribute("selectedSobeId",sobeId);m.addAttribute("bagliXidmetler",assigned);m.addAttribute("movcudXidmetler",available);return "pages/shobeXidmet";}
    @PostMapping("/parShobeXidmet/elave") public String add(@RequestParam Long sobeId,@RequestParam List<Long> xidmetIdleri,HttpSession s,RedirectAttributes a){if(!belongs(sobeId,s))return error(a,"Şöbə seçilmiş klinikaya aid deyil.",sobeId);flash(relationRepo.add(sobeId,xidmetIdleri),a,"Xidmətlər şöbəyə əlavə edildi.");return redirect(sobeId);}
    @PostMapping("/parShobeXidmet/cixar") public String remove(@RequestParam Long sobeId,@RequestParam List<Long> xidmetIdleri,HttpSession s,RedirectAttributes a){if(!belongs(sobeId,s))return error(a,"Şöbə seçilmiş klinikaya aid deyil.",sobeId);flash(relationRepo.remove(sobeId,xidmetIdleri),a,"Xidmətlər şöbədən çıxarıldı.");return redirect(sobeId);}
    private boolean belongs(Long id,HttpSession s){Long kid=(Long)s.getAttribute(KlinikaController.SELECTED_KLINIKA_ID);return sobeRepo.findByKlinikaId(kid).stream().anyMatch(x->x.sobeId().equals(id));}
    private void flash(Map<String,Object> r,RedirectAttributes a,String fallback){String status=String.valueOf(r.getOrDefault("status_kodu",""));String msg=String.valueOf(r.getOrDefault("mesaj",fallback));a.addFlashAttribute(status.toUpperCase().contains("UGUR")||status.equals("1")?"successMessage":"errorMessage",msg);}
    private String error(RedirectAttributes a,String msg,Long id){a.addFlashAttribute("errorMessage",msg);return redirect(id);}private String redirect(Long id){return "redirect:/parShobeXidmet?sobeId="+id;}
}
