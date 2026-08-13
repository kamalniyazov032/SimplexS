package az.simplexs.simplexs.dto.personal;

public record PersonalKlinika(
        Long personalId,
        Long personalKlinikaId,
        Long klinikaId,
        String klinikaAdi,
        Boolean aktiv) {
}
