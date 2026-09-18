package az.simplexs.simplexs.dto.anbar;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public final class AnbarModels {
    private AnbarModels() {
    }

    public record Firma(Long id,String ad, String unvan, String isTelefonu, String faksNomresi,
                        String email, String qeyd, String bankAdi, String bankHesabNomresi, String vergiNomresi,
                        String vergiIdaresi,
                        Boolean aktiv, LocalDateTime yaranmaTarixi, LocalDateTime yenilenmeTarixi) {
    }

    public record Vahid(Long id, String ad, Long altVahidId, String altVahidAdi, BigDecimal vurmaEmsali, Integer siraNo,
                        Boolean sifarisdeGorunsun, Boolean aktiv, LocalDateTime yaranmaTarixi,
                        LocalDateTime yenilenmeTarixi) {
    }

    public record QrupNovu(Long id, String kod, String ad, String aciqlama, Integer siraNo, Boolean aktiv) {
    }

    public record MehsulQrupu(Long id, Long qrupNovuId, String qrupNovuKodu, String qrupNovuAdi, String ad, String aciqlama, Boolean xesteyeCixis,
                              Boolean sifarisdeGorunsun, Boolean xesteMaterialCixisindaGorunsun, Boolean cihazaCixis,
                              Boolean aktiv) {
    }

    public record Material(Long id, Long qrupId, String qrupAdi, Long anaVahidId, String anaVahidAdi,
                           String ad,
                           String qisaAd, String barkod, BigDecimal minimumMiqdar, BigDecimal maksimumMiqdar,
                           Boolean mehvEdileBiler,
                           Boolean paketdenKenar,
                           Boolean aktiv, String farmasevtikMelumat, String istifadeQaydasi, BigDecimal minimumDoza,
                           BigDecimal maksimumDoza,
                           Integer maksimumTelebMuddeti, BigDecimal maksimumTelebMiqdari, Integer yasAsagi,
                           Integer yasYuxari,
                           BigDecimal bedenCekisi, Boolean avtomatikHekimTesdiqiOlmasin, Boolean vahidDeyisdirileBiler) {
    }

    public record EmeliyyatKateqoriyasi(Long id, String ad, String istiqamet, String aciqlama,
                                        Integer siraNo, Boolean aktiv) {
    }

    public record EmeliyyatNovu(Long id, Long kateqoriyaId, String kateqoriyaAdi,
                                String istiqamet,
                                String ad, Boolean standartdir, String aciqlama, Integer siraNo,
                                Boolean aktiv) {
    }

    public record AnbarNovu(Long id, String ad, String aciqlama, Integer siraNo, Boolean aktiv) {
    }

    public record Anbar(Long id, Long novId, String novAdi, String ad, String aciqlama,
                        Boolean telebAnbaridir, Boolean telebdeStokGorunsun, Boolean dermanPaketi,
                        Boolean istehsalCixisi,
                        Boolean mehvCixisi, Integer siraNo, Boolean aktiv, LocalDate baslamaTarixi, String cixisUsulu, String effektivCixisUsulu, LocalDateTime yaranmaTarixi) {
    }
}
