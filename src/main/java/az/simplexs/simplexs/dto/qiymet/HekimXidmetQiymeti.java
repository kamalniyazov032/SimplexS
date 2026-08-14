package az.simplexs.simplexs.dto.qiymet;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record HekimXidmetQiymeti(
        Long id, Long qiymetCedveliId, Long xidmetId, String xidmetKodu, String xidmetAdi,
        Long hekimPersonalId, String hekimKodu, String hekimAdSoyad, BigDecimal umumiQiymet,
        BigDecimal hekimQiymeti, Boolean aktiv, LocalDateTime yaranmaTarixi,
        Long yaradanPersonalId, LocalDateTime yenilenmeTarixi, Long yenileyenPersonalId) {
}
