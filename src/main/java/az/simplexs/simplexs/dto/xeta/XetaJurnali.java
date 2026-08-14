package az.simplexs.simplexs.dto.xeta;

import java.time.LocalDateTime;

public record XetaJurnali(Long id, String xetaKodu, String xetaNovu, Long personalId,
        String istifadeciAdi, Long klinikaId, String route, String httpMetod,
        String ipUnvan, String exceptionSinfi, String qisaAciqlama,
        LocalDateTime yaranmaTarixi, Long totalSayi) {}
