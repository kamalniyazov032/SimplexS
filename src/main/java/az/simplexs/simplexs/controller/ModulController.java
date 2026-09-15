package az.simplexs.simplexs.controller;

import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import az.simplexs.simplexs.security.ModuleHierarchy;
import az.simplexs.simplexs.security.AccessService;

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
    private final MessageSource messages;
    private final AccessService access;
    public ModulController(ModulRepository repository, MessageSource messages, AccessService access){this.repository=repository;this.messages=messages;this.access=access;}

    @GetMapping("/modullar")
    public String list(Model model){
        var modules=repository.findAll();
        model.addAttribute("pageTitle",message("modules.modullarin_idare_edilmesi"));
        model.addAttribute("parentOptions", modules.stream().collect(Collectors.toMap(m -> m.id(), m -> ModuleHierarchy.parents(modules, m.id()))));
        model.addAttribute("newParentOptions", ModuleHierarchy.parents(modules, null));
        model.addAttribute("activeMenu","modullar");
        model.addAttribute("modullar",modules);
        model.addAttribute("sistemler",repository.findSystems());
        model.addAttribute("aktivModulSayi",modules.stream().filter(m->Boolean.TRUE.equals(m.aktiv())).count());
        model.addAttribute("sistemSayi",modules.stream().map(m->m.sistemId()).distinct().count());
        return "pages/idareetme/modullar";
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
            @RequestParam(required=false)Integer siraNo,@RequestParam(required=false)Long parentId,RedirectAttributes attributes){
        flash(repository.createGroup(sistemId,parentId,kod,ad,aciqlama,ikon,siraNo),attributes);return "redirect:/modullar";
    }

    private String message(String key){
        return messages.getMessage(key,null,messages.getMessage("modules.save_error",null,LocaleContextHolder.getLocale()),LocaleContextHolder.getLocale());
    }

    private void flash(Map<String,Object> result,RedirectAttributes attributes){
        boolean success="UGURLU".equals(result.get("status_kodu"));
        if (success) access.invalidateModuleCaches();
        attributes.addFlashAttribute(success?"successMessage":"errorMessage",message(success ? "modules.saved" : String.valueOf(result.get("mesaj"))));
    }
}
