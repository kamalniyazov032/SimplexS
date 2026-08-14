package az.simplexs.simplexs.dto.yataq;

public record Yataq(Long id, Long klinikaId, Long binaId, String binaAdi, Long mertebeId,
        Integer mertebeNo, String mertebeAdi, Long palataId, String otaqNomresi,
        String palataAdi, Long sobeId, String sobeAdi, String kod, String ad,
        Integer siraNo, Boolean aktiv) {
}
