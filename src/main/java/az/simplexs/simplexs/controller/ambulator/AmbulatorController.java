package az.simplexs.simplexs.controller.ambulator;

import java.security.Principal;
import java.util.List;
import java.util.Map;

import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import az.simplexs.simplexs.dto.ambulator.AmbulatorLookups;
import az.simplexs.simplexs.dto.ambulator.PatientDocumentForm;
import az.simplexs.simplexs.dto.ambulator.PatientDocumentListItem;
import az.simplexs.simplexs.service.ambulator.AmbulatorService;

@Controller
public class AmbulatorController {
    private final AmbulatorService ambulatorService;

    public AmbulatorController(AmbulatorService ambulatorService) {
        this.ambulatorService = ambulatorService;
    }

    @GetMapping("/ambulatorQebul")
    public String ambulatorQebul(Model model) {
        addPageAttributes(model);
        addLookups(model);
        addPatientDocuments(model);
        if (!model.containsAttribute("patientDocumentForm")) {
            model.addAttribute("patientDocumentForm", new PatientDocumentForm());
        }
        return "pages/pasienQebulu/ambulator/ambulatorQebul";
    }

    @PostMapping("/ambulatorQebul/patient")
    public String createPatient(
        @ModelAttribute PatientDocumentForm patientDocumentForm,
        Principal principal,
        RedirectAttributes redirectAttributes
    ) {
        String createdBy = principal == null ? "admin" : principal.getName();
        try {
            Map<String, Object> result = ambulatorService.createPatientDocument(patientDocumentForm, createdBy);
            String patientCode = ambulatorService.toText(result.get("patient_code"));
            String message = patientCode.isBlank()
                ? "Yeni ambulator pasient yaradıldı."
                : "Yeni ambulator pasient yaradıldı. Pasient kodu: " + patientCode;
            redirectAttributes.addFlashAttribute("successMessage", message);
        } catch (DataAccessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMostSpecificCause().getMessage());
            redirectAttributes.addFlashAttribute("patientDocumentForm", patientDocumentForm);
        } catch (IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            redirectAttributes.addFlashAttribute("patientDocumentForm", patientDocumentForm);
        }
        return "redirect:/ambulatorQebul";
    }

    private void addPageAttributes(Model model) {
        model.addAttribute("pageTitle", "Ambulator qəbul");
        model.addAttribute("activeMenuGroup", "pasientQebulu");
        model.addAttribute("activeMenu", "ambulatorQebul");
    }

    private void addLookups(Model model) {
        try {
            AmbulatorLookups lookups = ambulatorService.getLookups();
            model.addAttribute("lookups", lookups);
        } catch (DataAccessException ex) {
            model.addAttribute("lookups", emptyLookups());
            model.addAttribute("errorMessage", ex.getMostSpecificCause().getMessage());
        }
    }

    private AmbulatorLookups emptyLookups() {
        return new AmbulatorLookups(
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of()
        );
    }

    private void addPatientDocuments(Model model) {
        try {
            List<PatientDocumentListItem> patientDocuments = ambulatorService.getPatientDocuments();
            model.addAttribute("patientDocuments", patientDocuments);
            model.addAttribute("activePatientCount", ambulatorService.countActivePatientDocuments());
            model.addAttribute("todayPatientCount", ambulatorService.countTodayPatientDocuments());
        } catch (DataAccessException ex) {
            model.addAttribute("patientDocuments", List.of());
            model.addAttribute("activePatientCount", 0);
            model.addAttribute("todayPatientCount", 0);
            if (!model.containsAttribute("errorMessage")) {
                model.addAttribute("errorMessage", ex.getMostSpecificCause().getMessage());
            }
        }
    }
}
