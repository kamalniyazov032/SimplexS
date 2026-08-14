package az.simplexs.simplexs.dto.kassa;

import java.time.LocalDateTime;

public record Kassa(Long id, Long klinikaId, String kod, String ad, String aciqlama,
        Integer siraNo, Boolean aktiv, LocalDateTime yaranmaTarixi, Long yaradanPersonalId,
        LocalDateTime yenilenmeTarixi, Long yenileyenPersonalId) {
}
