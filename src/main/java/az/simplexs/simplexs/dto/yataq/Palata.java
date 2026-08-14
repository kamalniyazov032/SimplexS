package az.simplexs.simplexs.dto.yataq;

public record Palata(Long id, Long klinikaId, Long binaId, Long mertebeId, Integer mertebeNo,
        String otaqNomresi, String ad, String aciqlama, Integer siraNo, Boolean aktiv) {
}
