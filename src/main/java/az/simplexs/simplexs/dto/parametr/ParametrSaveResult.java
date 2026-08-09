package az.simplexs.simplexs.dto.parametr;

public record ParametrSaveResult(
    Integer statusKodu,
    Long parametrDeyerId,
    String parametrTipi,
    String mesaj
) {
}
