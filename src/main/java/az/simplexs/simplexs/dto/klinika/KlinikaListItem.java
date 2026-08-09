package az.simplexs.simplexs.dto.klinika;

import java.time.LocalDateTime;

public record KlinikaListItem(
    Long klinikaId,
    Integer siraNo,
    String klinikaAdi,
    String email,
    String vergiNomresi,
    Long direktorId,
    String direktorAdi,
    String direktorSoyadi,
    String direktorAtaAdi,
    String direktorTamAdi,
    Long basHekimId,
    String basHekimAdi,
    String basHekimSoyadi,
    String basHekimAtaAdi,
    String basHekimTamAdi,
    Boolean aktiv,
    LocalDateTime yaranmaTarixi
) {
}
