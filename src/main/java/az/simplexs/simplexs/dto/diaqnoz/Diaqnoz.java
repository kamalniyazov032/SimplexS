package az.simplexs.simplexs.dto.diaqnoz;

import java.time.LocalDateTime;

public record Diaqnoz(Long id, Long sistemId, String sistemKodu, String sistemAdi,
        Long parentId, String parentKodu, String parentAdi, String kod, String ad,
        String aciqlama, Boolean kateqoriyadir, Boolean secileBiler,
        Boolean qadinaVerileBiler, Boolean kisiyeVerileBiler, Integer siraNo,
        Boolean aktiv, LocalDateTime yaranmaTarixi, LocalDateTime yenilenmeTarixi) {
}
