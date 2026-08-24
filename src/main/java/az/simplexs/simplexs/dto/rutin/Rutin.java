package az.simplexs.simplexs.dto.rutin;

import java.time.LocalDateTime;
import java.math.BigDecimal;

public record Rutin(Long id, Long klinikaId, String kod, String ad, String aciqlama,
        Boolean rutinQiymetlerindenIstifadeEt, Integer xidmetSayi, BigDecimal umumiQiymet, Integer siraNo,
        Boolean aktiv, LocalDateTime yaranmaTarixi, Long yaradanPersonalId,
        LocalDateTime yenilenmeTarixi, Long yenileyenPersonalId) {}
