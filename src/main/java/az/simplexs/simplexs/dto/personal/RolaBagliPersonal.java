package az.simplexs.simplexs.dto.personal;

public record RolaBagliPersonal(
        Long personalId, String personalKodu, String ad, String soyad, String ataAdi, String tamAd,
        Long personalKlinikaId, Long klinikaId, String klinikaAdi, Long rolId, String rolAdi,
        Boolean elaqeAktiv, Boolean personalAktiv, Boolean isdenAyrilib) {
}
