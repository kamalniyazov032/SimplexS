package az.simplexs.simplexs.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import az.simplexs.simplexs.repository.bina.BinaRepository;
import az.simplexs.simplexs.repository.parametr.ParametrRepository;
import jakarta.servlet.http.HttpSession;

@Controller
public class HomeController {
    private final BinaRepository binaRepository;
    private final ParametrRepository parametrRepository;

    public HomeController(BinaRepository binaRepository, ParametrRepository parametrRepository) {
        this.binaRepository = binaRepository;
        this.parametrRepository = parametrRepository;
    }

    @GetMapping("/login")
    public String login() {
        return "pages/login";
    }

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

    @GetMapping("/xesteKarti")
    public String xesteKarti(Model model) {
        model.addAttribute("pageTitle", "Xəstə Kartı");
        model.addAttribute("activeMenuGroup", "pasientQebulu");
        model.addAttribute("activeMenu", "xesteKarti");
        return "pages/pasienQebulu/xesteKarti";
    }

    @GetMapping("/kassa")
    public String kassa(Model model) {
        model.addAttribute("pageTitle", "Home");
        model.addAttribute("activeMenu", "dashboard");
        return "pages/kassa";
    }

    @GetMapping("/patientService")
    public String patientService(Model model) {
        model.addAttribute("pageTitle", "Xəstə Xidmət Ödənişi");
        model.addAttribute("activeMenu", "dashboard");
        return "pages/patientService";
    }

    @GetMapping("/qebz")
    public String qebz(Model model) {
        model.addAttribute("pageTitle", "Home");
        model.addAttribute("activeMenu", "dashboard");
        return "pages/kassaQebz";
    }

    @GetMapping("/binalar")
    public String binalar(Model model, HttpSession session) {
        Long klinikaId = (Long) session.getAttribute(KlinikaController.SELECTED_KLINIKA_ID);
        model.addAttribute("pageTitle", "Binalar");
        model.addAttribute("activeMenuGroup", "adminPanel");
        model.addAttribute("activeMenu", "binalar");
        model.addAttribute("binalar", binaRepository.findByKlinikaId(klinikaId));
        model.addAttribute("binaNovleri", binaRepository.findBinaNovleri());
        return "pages/binalar";
    }

    @PostMapping("/binalar/yenile")
    public String binaYenile(
        @RequestParam Long binaId,
        @RequestParam(required = false) String unvan,
        @RequestParam(required = false) String telefon,
        @RequestParam(required = false) String mobilNomre,
        @RequestParam(required = false) Integer mertebeSayi,
        @RequestParam Long binaNovuId,
        HttpSession session,
        RedirectAttributes redirectAttributes
    ) {
        Long klinikaId = (Long) session.getAttribute(KlinikaController.SELECTED_KLINIKA_ID);
        boolean belongsToSelectedClinic = binaRepository.findByKlinikaId(klinikaId).stream()
            .anyMatch(bina -> bina.binaId().equals(binaId));

        if (!belongsToSelectedClinic) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bina seçilmiş klinikaya aid deyil.");
            return "redirect:/binalar";
        }

        var result = binaRepository.update(binaId, unvan, telefon, mobilNomre, mertebeSayi, binaNovuId);
        if (result.statusKodu() != null && result.statusKodu() > 0) {
            redirectAttributes.addFlashAttribute("successMessage", result.mesaj());
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", result.mesaj());
        }
        return "redirect:/binalar";
    }

    @GetMapping("/bina-parametrleri")
    public String binaParametrleri(Model model, HttpSession session) {
        Long klinikaId = (Long) session.getAttribute(KlinikaController.SELECTED_KLINIKA_ID);
        model.addAttribute("pageTitle", "Ümumi parametrlər");
        model.addAttribute("activeMenuGroup", "adminPanel");
        model.addAttribute("activeMenu", "binaParametrleri");
        model.addAttribute("parametrler", parametrRepository.findByKlinikaId(klinikaId));
        return "pages/binaParametrleri";
    }

    @PostMapping("/bina-parametrleri/yadda-saxla")
    public String klinikaParametriniYaddaSaxla(
        @RequestParam Long parametrId,
        @RequestParam(required = false) Boolean booleanDeyer,
        @RequestParam(required = false) String textDeyer,
        @RequestParam(required = false) Long secimId,
        HttpSession session,
        RedirectAttributes redirectAttributes
    ) {
        Long klinikaId = (Long) session.getAttribute(KlinikaController.SELECTED_KLINIKA_ID);
        boolean parametrIsAvailable = parametrRepository.findByKlinikaId(klinikaId).stream()
            .anyMatch(parametr -> parametr.parametrId().equals(parametrId));

        if (!parametrIsAvailable) {
            redirectAttributes.addFlashAttribute("errorMessage", "Parametr seçilmiş klinika üçün mövcud deyil.");
            return "redirect:/bina-parametrleri";
        }

        var result = parametrRepository.save(
            klinikaId, parametrId, booleanDeyer, textDeyer, secimId, 17L);
        if (result.statusKodu() != null && result.statusKodu() > 0) {
            redirectAttributes.addFlashAttribute("successMessage", result.mesaj());
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", result.mesaj());
        }
        return "redirect:/bina-parametrleri";
    }

    @GetMapping("/stasionar")
    public String stasionar(Model model) {
        model.addAttribute("pageTitle", "Stasionar");
        model.addAttribute("activeMenu", "dashboard");
        return "pages/stasionar";
    }

    @GetMapping("/stasionar/teyinat")
    public String stasionarTeyinat(Model model) {
        model.addAttribute("pageTitle", "Həkim təyinatı");
        return "pages/stasionarTeyinat";
    }

    @GetMapping("/stasionar/teyinat-yaz")
    public String stasionarTeyinatYaz(Model model) {
        model.addAttribute("pageTitle", "Həkim təyinat yaz");
        return "pages/stasionarTeyinatYaz";
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

    @GetMapping("/xesteTarixcesi")
    public String xesteTarixcesi(Model model) {
        model.addAttribute("pageTitle", "Home");
        model.addAttribute("activeMenu", "dashboard");
        return "pages/xesteTarixcesi";
    }

    @GetMapping("/anamnez")
    public String anamnez(Model model) {
        model.addAttribute("pageTitle", "Home");
        model.addAttribute("activeMenu", "dashboard");
        return "pages/anamnez";
    }
      @GetMapping("/esasDiaqnoz")
    public String esasDiaqnoz(Model model) {
        model.addAttribute("pageTitle", "Home");
        model.addAttribute("activeMenu", "dashboard");
        return "pages/esasDiaqnoz";
    }

          @GetMapping("/randevu")
    public String randevu(Model model) {
        model.addAttribute("pageTitle", "Home");
        model.addAttribute("activeMenu", "dashboard");
        return "pages/randevu";
    }

    @GetMapping("/radiologiya")
    public String radiologiya(Model model) {
        model.addAttribute("pageTitle", "Radiologiya");
        model.addAttribute("activeMenu", "dashboard");
        return "pages/radiologiya";
    }

}
