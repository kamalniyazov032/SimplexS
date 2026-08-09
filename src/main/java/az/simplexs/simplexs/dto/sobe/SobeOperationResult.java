package az.simplexs.simplexs.dto.sobe;

public record SobeOperationResult(String statusKodu, Long sobeId, String mesaj) {
    public boolean ugurludur() { return "UGURLU".equalsIgnoreCase(statusKodu); }
}
