package az.simplexs.simplexs.dto.qiymet;

import java.math.BigDecimal;

public record QiymetXidmeti(
        Long xidmetId, String xidmetKodu, String xidmetAdi, Long xidmetQrupuId,
        String xidmetQrupuKodu, String xidmetQrupuAdi, Long xidmetTipiId,
        String xidmetTipiKodu, String xidmetTipiAdi, Long xidmetQiymetiId,
        BigDecimal qiymet, BigDecimal xestePay, BigDecimal qurumPayi,
        BigDecimal xesteEndirim, BigDecimal qurumEndirim, Boolean edvAktivdir,
        Boolean qiymetAktiv, Boolean qiymetTeyinEdilib, Integer xidmetSiraNo) {
}
