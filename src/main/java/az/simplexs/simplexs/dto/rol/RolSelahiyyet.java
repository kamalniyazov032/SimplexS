package az.simplexs.simplexs.dto.rol;

public record RolSelahiyyet(
    Long selahiyyetId, Long modulId, String modulAdi, String selahiyyetKodu,
    String selahiyyetAdi, String aciqlama, Boolean secilib
) {
}
