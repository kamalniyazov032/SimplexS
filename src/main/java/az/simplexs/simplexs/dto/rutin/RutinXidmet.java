package az.simplexs.simplexs.dto.rutin;

import java.math.BigDecimal;

public record RutinXidmet(Long xidmetId, String xidmetKodu, String xidmetAdi,
        Long xidmetQrupuId, String xidmetQrupuKodu, String xidmetQrupuAdi,
        Long xidmetTipiId, String xidmetTipiKodu, String xidmetTipiAdi,
        BigDecimal qiymet, Integer siraNo, Boolean xidmetAktiv, Boolean elaqeAktiv) {}
