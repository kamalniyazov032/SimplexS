package az.simplexs.simplexs.controller;


import org.springframework.stereotype.Controller;
import jakarta.servlet.http.HttpSession;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


@Controller
public class KassaOdenishController {

    @GetMapping("/kassaOdenish")
    public String list(@RequestParam(defaultValue = "aktiv") String status,
                       Model model, HttpSession session) {
        Long klinikaId = klinikaId(session);

        return "pages/maliyye/kassa";
    }


    private Long klinikaId(HttpSession session) {
        return (Long) session.getAttribute(KlinikaController.SELECTED_KLINIKA_ID);
    }
}
