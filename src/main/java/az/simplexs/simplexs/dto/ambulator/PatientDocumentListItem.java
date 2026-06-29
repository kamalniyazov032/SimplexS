package az.simplexs.simplexs.dto.ambulator;

import java.time.LocalDate;

public record PatientDocumentListItem(
    Long id,
    String patientCode,
    String idNumber,
    String finCode,
    String firstName,
    String lastName,
    String fatherName,
    LocalDate birthDate,
    String mobilePhone,
    String workplace
) {
    public String fullName() {
        StringBuilder fullName = new StringBuilder();
        appendPart(fullName, firstName);
        appendPart(fullName, lastName);
        appendPart(fullName, fatherName);
        return fullName.toString();
    }

    private void appendPart(StringBuilder builder, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        if (!builder.isEmpty()) {
            builder.append(' ');
        }
        builder.append(value.trim());
    }
}
