package az.simplexs.simplexs.dto.rol;

public record RolPermissionSaveResult(
    String statusKodu,
    Integer modulSayi,
    Integer selahiyyetSayi,
    String mesaj
) {
    public boolean ugurludur() {
        return statusKodu != null && statusKodu.toUpperCase(java.util.Locale.ROOT).contains("UGUR");
    }
}
