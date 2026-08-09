package az.simplexs.simplexs.dto.parametr;

import java.time.LocalDateTime;
import java.util.List;

public record KlinikaParametr(
    Long parametrId,
    String parametrKodu,
    String parametrAdi,
    String aciqlama,
    String parametrTipi,
    Integer siraNo,
    Long parametrDeyerId,
    Boolean booleanDeyer,
    String textDeyer,
    Long secimId,
    String secimKodu,
    String secimAdi,
    Boolean deyerTeyinEdilib,
    Long yenileyenPersonalId,
    LocalDateTime yenilenmeTarixi,
    List<ParametrSecim> secimler
) {
    public String controlType() {
        String normalized = parametrTipi == null ? "" : parametrTipi.toLowerCase();
        if (normalized.contains("bool")) {
            return "boolean";
        }
        if (normalized.contains("sec") || normalized.contains("seç") || normalized.contains("select")) {
            return "select";
        }
        return "text";
    }
}
