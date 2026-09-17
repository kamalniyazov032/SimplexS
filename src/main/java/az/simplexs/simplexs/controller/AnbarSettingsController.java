package az.simplexs.simplexs.controller;

import java.math.BigDecimal;
import java.time.Year;
import java.util.*;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import az.simplexs.simplexs.repository.anbar.AnbarRepository;
import az.simplexs.simplexs.repository.anbar.AnbarSettingsRepository;
import az.simplexs.simplexs.security.AuthenticatedPersonal;
import jakarta.servlet.http.HttpSession;
import tools.jackson.databind.ObjectMapper;

@Controller
public class AnbarSettingsController {
    private final AnbarRepository catalog;
    private final AnbarSettingsRepository settings;
    private final MessageSource messages;
    private final ObjectMapper json;
    public AnbarSettingsController(AnbarRepository catalog,AnbarSettingsRepository settings,MessageSource messages,ObjectMapper json) {
        this.catalog=catalog;this.settings=settings;this.messages=messages;this.json=json;
    }
    private Long clinic(HttpSession s) {
        Long id=(Long)s.getAttribute(KlinikaController.SELECTED_KLINIKA_ID);
        if(id==null)throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        return id;
    }
    private void year(int year) { if(year<1900||year>9999)throw new ResponseStatusException(HttpStatus.BAD_REQUEST); }
    private String text(String key) {return messages.getMessage(key,null,LocaleContextHolder.getLocale());}
    private void flash(Map<String,Object> result,RedirectAttributes attrs) {
        String status=String.valueOf(result.get("status_kodu"));
        boolean ok="1".equals(status)||status.toUpperCase(Locale.ROOT).contains("UGUR");
        attrs.addFlashAttribute(ok?"successMessage":"errorMessage",String.valueOf(result.getOrDefault("mesaj",text(ok?"warehouse.saved":"warehouse.save_failed"))));
    }
    private void warehouse(Long k,Long id) {
        if(catalog.anbarlar(k,null,null,null).stream().noneMatch(x->id.equals(x.id())))throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
    @GetMapping("/anbar/anbarlar/{id}/settings")
    public String warehousePage(@PathVariable Long id,@RequestParam(required=false)Integer il,HttpSession session,Model model) {
        Long k=clinic(session);warehouse(k,id);int y=il==null?Year.now().getValue():il;year(y);
        model.addAttribute("pageTitle",text("warehouse.settings"));
        model.addAttribute("activeMenu","anbar.anbarlar");model.addAttribute("id",id);model.addAttribute("il",y);
        model.addAttribute("groups",settings.groups(k,id));model.addAttribute("people",settings.people(k,id));model.addAttribute("months",settings.months(k,id,y));
        return "pages/anbar/anbarSettings";
    }
    @PostMapping("/anbar/anbarlar/{id}/settings/{section}")
    public String warehouseSave(@PathVariable Long id,@PathVariable String section,@RequestParam Map<String,String> form,
            HttpSession session,@AuthenticationPrincipal AuthenticatedPersonal actor,RedirectAttributes attrs) {
        Long k=clinic(session);warehouse(k,id);int y=Integer.parseInt(form.getOrDefault("il",String.valueOf(Year.now().getValue())));year(y);
        Map<String,Object> result;
        switch(section) {
            case "groups" -> {
                List<Map<String,Object>> rows=new ArrayList<>();
                for(var row:settings.groups(k,id))if(form.containsKey("group_"+row.get("mehsul_qrupu_id")))rows.add(Map.of("mehsul_qrupu_id",row.get("mehsul_qrupu_id")));
                result=settings.saveGroups(k,id,json.writeValueAsString(rows),actor.personalId());
            }
            case "people" -> {
                List<Long> ids=new ArrayList<>();
                for(var row:settings.people(k,id))if(form.containsKey("person_"+row.get("personal_id")))ids.add(((Number)row.get("personal_id")).longValue());
                result=settings.savePeople(k,id,ids,actor.personalId());
            }
            case "months" -> {
                List<Map<String,Object>> rows=new ArrayList<>();
                for(int m=1;m<=12;m++)rows.add(Map.of("ay",m,"kilidlidir",form.containsKey("month_"+m)));
                result=settings.saveMonths(k,id,y,json.writeValueAsString(rows),actor.personalId());
            }
            default -> throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        flash(result,attrs);return "redirect:/anbar/anbarlar/"+id+"/settings?il="+y;
    }
    @PostMapping("/anbar/anbarlar/bulk-month")
    public String bulk(@RequestParam int il,@RequestParam int ay,@RequestParam(defaultValue="false")boolean kilidlidir,
            HttpSession session,@AuthenticationPrincipal AuthenticatedPersonal actor,RedirectAttributes attrs) {
        year(il);if(ay<1||ay>12)throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        flash(settings.bulkMonth(clinic(session),il,ay,kilidlidir,actor.personalId()),attrs);
        return "redirect:/anbar/anbarlar";
    }
    @GetMapping("/anbar/materiallar/{id}/vahidler")
    public String unitsPage(@PathVariable Long id,HttpSession session,Model model) {
        Long k=clinic(session);
        var material=catalog.materiallar(k,null,null,null).stream().filter(x->id.equals(x.id())).findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        var existing=settings.units(k,id);
        Map<Long,Map<String,Object>> byId=new HashMap<>();
        for(var row:existing)byId.put(((Number)row.get("vahid_id")).longValue(),row);
        model.addAttribute("pageTitle",material.ad());model.addAttribute("activeMenu","anbar.materiallar");
        model.addAttribute("id",id);model.addAttribute("units",catalog.vahidler(null));model.addAttribute("existing",byId);
        return "pages/anbar/materialUnits";
    }
    @PostMapping("/anbar/materiallar/{id}/vahidler")
    public String saveUnits(@PathVariable Long id,@RequestParam Map<String,String> form,HttpSession session,
            @AuthenticationPrincipal AuthenticatedPersonal actor,RedirectAttributes attrs) {
        Long k=clinic(session);
        if(catalog.materiallar(k,null,null,null).stream().noneMatch(x->id.equals(x.id())))throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        List<Map<String,Object>> rows=new ArrayList<>();
        Map<Long,Map<String,Object>> locked=new HashMap<>();
        for(var row:settings.units(k,id))if(Boolean.TRUE.equals(row.get("ana_vahiddir"))||!Boolean.TRUE.equals(row.get("deyisdirile_biler")))locked.put(((Number)row.get("vahid_id")).longValue(),row);
        for(var unit:catalog.vahidler(null)) {
            var current=locked.get(unit.id());
            if(current!=null) {
                if(!Boolean.TRUE.equals(current.get("ana_vahiddir"))&&Boolean.TRUE.equals(current.get("aktiv"))) {
                    Map<String,Object> preserved=new HashMap<>();preserved.put("vahid_id",unit.id());preserved.put("ana_vahide_emsal",current.get("ana_vahide_emsal"));preserved.put("sira_no",current.get("sira_no"));rows.add(preserved);
                }
            } else if(form.containsKey("unit_"+unit.id())) {
                BigDecimal factor=new BigDecimal(form.get("factor_"+unit.id()));
                if(factor.signum()<=0)throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
                Map<String,Object> row=new HashMap<>();row.put("vahid_id",unit.id());row.put("ana_vahide_emsal",factor);
                String order=form.get("order_"+unit.id());row.put("sira_no",order==null||order.isBlank()?null:Integer.valueOf(order));rows.add(row);
            }
        }
        flash(settings.saveUnits(k,id,json.writeValueAsString(rows),actor.personalId()),attrs);
        return "redirect:/anbar/materiallar/"+id+"/vahidler";
    }
}
