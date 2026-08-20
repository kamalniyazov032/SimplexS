package az.simplexs.simplexs.dto.xidmet;

public record PaketXidmetItem(Long xidmetId, String xidmetKodu, String xidmetAdi,
        Long xidmetQrupuId, String xidmetQrupuKodu, String xidmetQrupuAdi,
        Long xidmetTipiId, String xidmetTipiKodu, String xidmetTipiAdi,
        Integer miqdar, Integer siraNo, Boolean xidmetAktiv, Boolean elaqeAktiv) {
}
