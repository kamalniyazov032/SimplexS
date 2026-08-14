package az.simplexs.simplexs.dto.qiymet;

import java.time.LocalDateTime;

public record QiymetQrupu(
        Long id, Long klinikaId, Long basliqId, String basliqAdi, String ad, String aciqlama,
        Boolean standartdir, Integer siraNo, Boolean aktiv, LocalDateTime yaranmaTarixi,
        Long yaradanPersonalId, LocalDateTime yenilenmeTarixi, Long yenileyenPersonalId) {
}
