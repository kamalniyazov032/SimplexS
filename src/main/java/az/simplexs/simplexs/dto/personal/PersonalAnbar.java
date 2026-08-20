package az.simplexs.simplexs.dto.personal;

public record PersonalAnbar(Long anbarId, String anbarKodu, String anbarAdi,
        Long anbarNovuId, String anbarNovuKodu, String anbarNovuAdi,
        Boolean secilib, Boolean izlesin, Boolean islesin, Boolean elaqeAktiv) {
}
