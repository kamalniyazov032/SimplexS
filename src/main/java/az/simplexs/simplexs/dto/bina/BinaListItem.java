package az.simplexs.simplexs.dto.bina;

import java.time.LocalDateTime;

public record BinaListItem(
    Long binaId,
    Long klinikaId,
    String klinikaAdi,
    Integer siraNo,
    String binaAdi,
    String unvan,
    String telefon,
    String mobilNomre,
    Integer mertebeSayi,
    Long binaNovuId,
    String binaNovuKodu,
    String binaNovuAdi,
    Boolean aktiv,
    LocalDateTime yaranmaTarixi
) {
}
