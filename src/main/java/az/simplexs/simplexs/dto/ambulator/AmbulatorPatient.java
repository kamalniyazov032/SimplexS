package az.simplexs.simplexs.dto.ambulator;

import java.time.LocalDate;

public record AmbulatorPatient(
    Long id, String kod, String ad, String meta, Long teskilatId,
    String teskilatAdi, String vesiqeNomresi, LocalDate dogumTarixi,
    String cinsAdi, String qanQrupuAdi, String mobilNomre,
    String sosialKartNomresi, String unvan, Boolean aktiv
) {}
