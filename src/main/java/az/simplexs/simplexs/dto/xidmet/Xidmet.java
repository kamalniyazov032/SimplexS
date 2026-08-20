package az.simplexs.simplexs.dto.xidmet;

import java.time.LocalDateTime;

public record Xidmet(Long id, String kod, String ad, Long qrupId, String qrupKodu, String qrupAdi,
        Long muhasibatKoduId, String muhasibatKoduAdi, Long tipId, String tipKodu, String tipAdi,
        String beynelxalqKod, String beynelxalqAd, Long hesabatNovuId, String hesabatNovuKodu,
        String hesabatNovuAdi, Long hesabatMecburiyyetiId, String hesabatMecburiyyetiKodu,
        String hesabatMecburiyyetiAdi, Boolean paketXidmet, Boolean aktiv, LocalDateTime yaranmaTarixi) {}
