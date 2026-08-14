package az.simplexs.simplexs.dto.qiymet;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record QiymetCedveli(
        Long id, Long klinikaId, Long basliqId, String basliqAdi, Long qrupId, String qrupAdi,
        LocalDate baslamaTarixi, LocalDate bitmeTarixi, Boolean tarixdeAktivdir,
        BigDecimal xestePayi, BigDecimal sigortaPayi, BigDecimal xesteEndirim,
        BigDecimal sigortaEndirim, Boolean aktiv, LocalDateTime yaranmaTarixi,
        Long yaradanPersonalId, LocalDateTime yenilenmeTarixi, Long yenileyenPersonalId) {
}
