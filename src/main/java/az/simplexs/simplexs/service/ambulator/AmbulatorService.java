package az.simplexs.simplexs.service.ambulator;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import az.simplexs.simplexs.dto.ambulator.AmbulatorLookups;
import az.simplexs.simplexs.dto.ambulator.PatientDocumentForm;
import az.simplexs.simplexs.dto.ambulator.PatientDocumentListItem;
import az.simplexs.simplexs.repository.ambulator.AmbulatorRepository;

@Service
public class AmbulatorService {
    private final AmbulatorRepository ambulatorRepository;

    public AmbulatorService(AmbulatorRepository ambulatorRepository) {
        this.ambulatorRepository = ambulatorRepository;
    }

    public AmbulatorLookups getLookups() {
        return ambulatorRepository.getLookups();
    }

    public List<PatientDocumentListItem> getPatientDocuments() {
        return ambulatorRepository.getPatientDocuments();
    }

    public int countActivePatientDocuments() {
        return ambulatorRepository.countActivePatientDocuments();
    }

    public int countTodayPatientDocuments() {
        return ambulatorRepository.countTodayPatientDocuments();
    }

    public Map<String, Object> createPatientDocument(PatientDocumentForm form, String createdBy) {
        List<Map<String, Object>> result = ambulatorRepository.createPatientDocument(form, createdBy);
        if (result.isEmpty()) {
            throw new IllegalStateException("Pasient yaradılmadı: DB funksiya nəticə qaytarmadı.");
        }

        Map<String, Object> row = result.getFirst();
        String statusCode = toText(row.get("status_code"));
        Long patientDocumentId = toLong(row.get("patient_document_id"));
        if (!"SUCCESS".equals(statusCode) || patientDocumentId == null || patientDocumentId <= 0) {
            throw new IllegalStateException(statusMessage(statusCode));
        }
        if (!ambulatorRepository.patientDocumentExists(patientDocumentId)) {
            throw new IllegalStateException(
                "Pasient yaradılmadı: DB funksiya SUCCESS qaytardı, amma pasient cədvəldə tapılmadı. ID: "
                    + patientDocumentId
            );
        }
        return row;
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(value.toString().trim());
    }

    public String toText(Object value) {
        if (value == null) {
            return "";
        }
        return value.toString().trim();
    }

    private String statusMessage(String statusCode) {
        return switch (statusCode) {
            case "MISSING_REQUIRED_FIELD" -> "Pasient yaradılmadı: məcburi xanalar tam doldurulmayıb.";
            case "BUILDING_NOT_FOUND" -> "Pasient yaradılmadı: seçilən bina aktiv deyil və ya bazada tapılmadı.";
            case "ORGANIZATION_NOT_FOUND" -> "Pasient yaradılmadı: seçilən təşkilat aktiv deyil və ya bazada tapılmadı.";
            case "DUPLICATE_FIN_CODE" -> "Pasient yaradılmadı: bu FİN kod seçilən binada artıq mövcuddur.";
            case "DUPLICATE_ID_NUMBER" -> "Pasient yaradılmadı: bu Ş/V nömrəsi seçilən binada artıq mövcuddur.";
            case "DUPLICATE_PATIENT_CODE" -> "Pasient yaradılmadı: pasient kodu təkrarlanır, yenidən cəhd edin.";
            case "INVALID_REFERENCE_ID" -> "Pasient yaradılmadı: seçilən məlumatlardan biri bazada düzgün deyil.";
            case "UNKNOWN_ERROR" -> "Pasient yaradılmadı: DB funksiyasında naməlum xəta baş verdi.";
            default -> "Pasient yaradılmadı. DB status: " + (statusCode.isBlank() ? "boş cavab" : statusCode);
        };
    }
}
