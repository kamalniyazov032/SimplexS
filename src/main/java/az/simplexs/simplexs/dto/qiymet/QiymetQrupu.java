package az.simplexs.simplexs.dto.qiymet;
import java.math.BigDecimal;
import java.time.LocalDate;
public record QiymetQrupu(Long id,Long klinikaId,Long basliqId,String basliqAdi,String ad,String aciqlama,
        LocalDate baslamaTarixi,LocalDate bitmeTarixi,Boolean tarixdeAktivdir,Boolean standartdir,
        BigDecimal sigortaPayi,BigDecimal xestePayi,BigDecimal sigortaEndirim,BigDecimal xesteEndirim,
        Integer siraNo,Boolean aktiv) {}
