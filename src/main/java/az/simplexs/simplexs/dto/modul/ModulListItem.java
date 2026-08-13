package az.simplexs.simplexs.dto.modul;

public record ModulListItem(Long id, Long parentId, String kod, String ad, String aciqlama,
        String route, String ikon, Boolean menyudaGorunsun, Boolean aktiv, Integer siraNo,
        Integer seviyye, String tamYol) {}
