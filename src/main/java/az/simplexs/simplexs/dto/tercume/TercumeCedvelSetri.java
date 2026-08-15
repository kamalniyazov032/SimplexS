package az.simplexs.simplexs.dto.tercume;

import java.util.Map;

public record TercumeCedvelSetri(String acar, String azerbaycanca, Map<String,String> deyerler) {
    public String deyer(String dilKodu) {
        return deyerler.getOrDefault(dilKodu, "");
    }
}
