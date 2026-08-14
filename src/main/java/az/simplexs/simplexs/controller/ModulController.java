package az.simplexs.simplexs.controller;

import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import az.simplexs.simplexs.repository.modul.ModulRepository;

@Controller
public class ModulController {
    private final ModulRepository repository;
    public ModulController(ModulRepository repository){this.repository=repository;}

    @GetMapping("/modullar")
    public String list(Model model){
        var modules=repository.findAll();
        model.addAttribute("pageTitle","Modulların idarə edilməsi");
        model.addAttribute("activeMenu","modullar");
        model.addAttribute("modullar",modules);
        model.addAttribute("sistemler",repository.findSystems());
        model.addAttribute("aktivModulSayi",modules.stream().filter(m->Boolean.TRUE.equals(m.aktiv())).count());
        model.addAttribute("sistemSayi",modules.stream().map(m->m.sistemId()).distinct().count());
        return "pages/modullar";
    }

    @PostMapping(value="/modullar/yenile",params="action=update")
    public String update(@RequestParam Long modulId,@RequestParam Long sistemId,
            @RequestParam(required=false)Long parentId,
            @RequestParam String ad,@RequestParam(required=false)String aciqlama,
            @RequestParam(required=false)String ikon,@RequestParam(required=false)Integer siraNo,
            @RequestParam(defaultValue="false")boolean menyudaGorunsun,
            @RequestParam(defaultValue="false")boolean aktiv,RedirectAttributes attributes){
        flash(repository.update(modulId,sistemId,parentId,ad,aciqlama,ikon,siraNo,menyudaGorunsun,aktiv),attributes);
        return "redirect:/modullar";
    }

    @PostMapping(value="/modullar/yenile",params="action=createSystem")
    public String createSystem(@RequestParam String kod,@RequestParam String ad,
            @RequestParam(required=false)String ikon,@RequestParam(required=false)Integer siraNo,
            RedirectAttributes attributes){
        flash(repository.createSystem(kod,ad,ikon,siraNo),attributes);return "redirect:/modullar";
    }

    @PostMapping(value="/modullar/yenile",params="action=createGroup")
    public String createGroup(@RequestParam Long sistemId,@RequestParam String kod,@RequestParam String ad,
            @RequestParam(required=false)String aciqlama,@RequestParam(required=false)String ikon,
            @RequestParam(required=false)Integer siraNo,RedirectAttributes attributes){
        flash(repository.createGroup(sistemId,kod,ad,aciqlama,ikon,siraNo),attributes);return "redirect:/modullar";
    }

    private void flash(Map<String,Object> result,RedirectAttributes attributes){
        boolean success="UGURLU".equals(result.get("status_kodu"));
        attributes.addFlashAttribute(success?"successMessage":"errorMessage",result.get("mesaj"));
    }
}
