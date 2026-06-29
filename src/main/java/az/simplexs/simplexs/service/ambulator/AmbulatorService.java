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

    public List<Map<String, Object>> createPatientDocument(PatientDocumentForm form, String createdBy) {
        return ambulatorRepository.createPatientDocument(form, createdBy);
    }
}
