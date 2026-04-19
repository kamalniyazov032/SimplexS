package az.simplexs.simplexs.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "redirect:/dashboard";
    }



    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("pageTitle", "Home");
        model.addAttribute("activeMenu", "dashboard");
        return "pages/dashboard";
    }

    @GetMapping("/ambulatorQebul")
    public String ambulatorQebul(Model model) {
        model.addAttribute("pageTitle", "Home");
        model.addAttribute("activeMenu", "dashboard");
        return "pages/pasienQebulu/ambulator/ambulatorQebul";
    }

    @GetMapping("/parXidmet")
    public String parXidemt(Model model) {
        model.addAttribute("pageTitle", "Home");
        model.addAttribute("activeMenu", "dashboard");
        return "pages/xidmet";
    }

    @GetMapping("/parPaket")
    public String parPaket(Model model) {
        model.addAttribute("pageTitle", "Home");
        model.addAttribute("activeMenu", "dashboard");
        return "pages/paket";
    }
    @GetMapping("/parShobeXidmet")
    public String parShobeXidmet(Model model) {
        model.addAttribute("pageTitle", "Home");
        model.addAttribute("activeMenu", "dashboard");
        return "pages/shobeXidmet";
    }
    @GetMapping("/kassa")
    public String kassa(Model model) {
        model.addAttribute("pageTitle", "Home");
        model.addAttribute("activeMenu", "dashboard");
        return "pages/kassa";
    }
    @GetMapping("/qebz")
    public String qebz(Model model) {
        model.addAttribute("pageTitle", "Home");
        model.addAttribute("activeMenu", "dashboard");
        return "pages/kassaQebz";
    }

    @GetMapping("/muhasibatKodu")
    public String muhasibatKodu(Model model) {
        model.addAttribute("pageTitle", "Home");
        model.addAttribute("activeMenu", "dashboard");
        return "pages/muhasibatKodu";
    }
    @GetMapping("/xidmetQruplari")
    public String xidmetQruplari(Model model) {
        model.addAttribute("pageTitle", "Home");
        model.addAttribute("activeMenu", "dashboard");
        return "pages/xidmetQruplari";
    }

    @GetMapping("/xidmetQiymetleri")
    public String xidmetQiymetleri(Model model) {
        model.addAttribute("pageTitle", "Home");
        model.addAttribute("activeMenu", "dashboard");
        return "pages/xidmetQiymetleri";
    }

    @GetMapping("/muessise")
    public String muessise(Model model) {
        model.addAttribute("pageTitle", "Home");
        model.addAttribute("activeMenu", "dashboard");
        return "pages/muessise";
    }
    @GetMapping("/emekdash")
    public String emekdash(Model model) {
        model.addAttribute("pageTitle", "Home");
        model.addAttribute("activeMenu", "dashboard");
        return "pages/emekdash";
    }
    @GetMapping("/shobe")
    public String shobe(Model model) {
        model.addAttribute("pageTitle", "Home");
        model.addAttribute("activeMenu", "dashboard");
        return "pages/shobe";
    }
    @GetMapping("/pasientAvans")
    public String pasientAvans(Model model) {
        model.addAttribute("pageTitle", "Home");
        model.addAttribute("activeMenu", "dashboard");
        return "pages/pasientAvans";
    }
    @GetMapping("/poliklinikHekim")
    public String poliklinikHekim(Model model) {
        model.addAttribute("pageTitle", "Home");
        model.addAttribute("activeMenu", "dashboard");
        return "pages/poliklinikHekim";
    }
    @GetMapping("/qaimeOdenishiFirma")
    public String qaimeOdenishiFirma(Model model) {
        model.addAttribute("pageTitle", "Home");
        model.addAttribute("activeMenu", "dashboard");
        return "pages/qaimeOdenishiFirma";
    }
    @GetMapping("/qaimeHereketiFirma")
    public String qaimeHereketiFirma(Model model) {
        model.addAttribute("pageTitle", "Home");
        model.addAttribute("activeMenu", "dashboard");
        return "pages/qaimeHereketiFirma";
    }


}

