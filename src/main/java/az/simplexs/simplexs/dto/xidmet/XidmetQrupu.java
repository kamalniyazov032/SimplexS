package az.simplexs.simplexs.dto.xidmet;

public record XidmetQrupu(Long id, Long parentId, String kod, String ad, String aciqlama,
        Integer seviye, String tamYol, Integer siraNo, Boolean aktiv,
        Integer altQrupSayi, Boolean altQrupuVar, Boolean kokQrupdur) {}
