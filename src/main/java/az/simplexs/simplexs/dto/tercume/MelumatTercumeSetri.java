package az.simplexs.simplexs.dto.tercume;

import java.util.Map;

public record MelumatTercumeSetri(Long id, String kod, String azerbaycanca, String saha,
        Map<String,String> deyerler) {
    public String deyer(String dilKodu){return deyerler.getOrDefault(dilKodu,"");}
}
