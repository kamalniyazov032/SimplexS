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
        model.addAttribute("aktivModulSayi",modules.stream().filter(m->Boolean.TRUE.equals(m.aktiv())).count());
        return "pages/modullar";
    }

    @PostMapping("/modullar/yenile")
    public String update(@RequestParam Long modulId,@RequestParam(required=false)Long parentId,
            @RequestParam String ad,@RequestParam(required=false)String aciqlama,
            @RequestParam(required=false)String ikon,@RequestParam(required=false)Integer siraNo,
            @RequestParam(defaultValue="false")boolean menyudaGorunsun,
            @RequestParam(defaultValue="false")boolean aktiv,RedirectAttributes attributes){
        flash(repository.update(modulId,parentId,ad,aciqlama,ikon,siraNo,menyudaGorunsun,aktiv),attributes);
        return "redirect:/modullar";
    }

    private void flash(Map<String,Object> result,RedirectAttributes attributes){
        boolean success="UGURLU".equals(result.get("status_kodu"));
        attributes.addFlashAttribute(success?"successMessage":"errorMessage",result.get("mesaj"));
    }
}
