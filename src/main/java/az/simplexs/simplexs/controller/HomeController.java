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
}

