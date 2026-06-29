package az.simplexs.simplexs.dto.ambulator;

public class PatientDocumentFilter {
    public static final int DEFAULT_LIMIT = 100;

    private String q;
    private String idNumber;
    private String finCode;
    private String mobilePhone;

    public String getQ() {
        return q;
    }

    public void setQ(String q) {
        this.q = q;
    }

    public String getIdNumber() {
        return idNumber;
    }

    public void setIdNumber(String idNumber) {
        this.idNumber = idNumber;
    }

    public String getFinCode() {
        return finCode;
    }

    public void setFinCode(String finCode) {
        this.finCode = finCode;
    }

    public String getMobilePhone() {
        return mobilePhone;
    }

    public void setMobilePhone(String mobilePhone) {
        this.mobilePhone = mobilePhone;
    }

    public int getLimit() {
        return DEFAULT_LIMIT;
    }

    public boolean hasFilters() {
        return hasText(q) || hasText(idNumber) || hasText(finCode) || hasText(mobilePhone);
    }

    public String normalizedQ() {
        return normalize(q);
    }

    public String normalizedIdNumber() {
        return normalize(idNumber);
    }

    public String normalizedFinCode() {
        return normalize(finCode);
    }

    public String normalizedMobilePhone() {
        return normalize(mobilePhone);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String normalize(String value) {
        if (!hasText(value)) {
            return "";
        }
        return value.trim();
    }
}
