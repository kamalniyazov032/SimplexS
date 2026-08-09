package az.simplexs.simplexs.dto.sobe;

import java.time.LocalDateTime;

public record SobeListItem(
    Long sobeId, Long klinikaId, String klinikaAdi, String sobeAdi, Integer siraNo,
    Long sobeTipiId, String sobeTipiKodu, String sobeTipiAdi,
    Long hekimSecimQaydasiId, String hekimSecimQaydasiKodu, String hekimSecimQaydasiAdi,
    Long sobeMudiriPersonalId, String sobeMudiriKodu, String sobeMudiriAdi,
    Long boyukTibbBacisiPersonalId, String boyukTibbBacisiKodu, String boyukTibbBacisiAdi,
    Long cinsId, String cinsKodu, String cinsAdi,
    Boolean aktiv, LocalDateTime yaranmaTarixi,
    Long yaradanPersonalId, String yaradanPersonalAdi,
    LocalDateTime yenilenmeTarixi, Long yenileyenPersonalId, String yenileyenPersonalAdi
) {}
