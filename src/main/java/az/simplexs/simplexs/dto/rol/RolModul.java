package az.simplexs.simplexs.dto.rol;

public record RolModul(
    Long sistemId, String sistemKodu, Long modulId, Long parentId, String modulKodu,
    String modulAdi, String modulAciqlamasi, String route, String ikon,
    Integer siraNomresi, Integer seviyye, Boolean secilib
) {
}
