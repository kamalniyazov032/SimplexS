package az.simplexs.simplexs.dto.personal;

public record PersonalKassa(Long kassaId, String kassaKodu, String kassaAdi, Long elaqeId,
        Boolean secilib, Boolean izlesin, Boolean islesin, Boolean elaqeAktiv) {
}
