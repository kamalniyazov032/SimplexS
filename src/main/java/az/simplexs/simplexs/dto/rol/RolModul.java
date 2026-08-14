package az.simplexs.simplexs.dto.rol;

public record RolModul(
    Long sistemId, String sistemKodu, String sistemAdi, Long modulId, Long parentId, String modulKodu,
    String modulAdi, String modulAciqlamasi, String route, String ikon,
    Boolean menyudaGorunsun, Integer siraNomresi, Integer seviyye, Boolean secilib
) {
}
