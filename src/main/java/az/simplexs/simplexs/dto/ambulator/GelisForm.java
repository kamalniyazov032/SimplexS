package az.simplexs.simplexs.dto.ambulator;
import java.time.LocalDate;import java.time.LocalTime;import org.springframework.format.annotation.DateTimeFormat;
@lombok.Data public class GelisForm{public Long gelisId,xesteId,gelisNovuId,teskilatId,gonderenHekimId;@DateTimeFormat(iso=DateTimeFormat.ISO.DATE)public LocalDate gelisTarixi;@DateTimeFormat(iso=DateTimeFormat.ISO.TIME)public LocalTime gelisSaati;public boolean randevudur,aktiv=true;public String mesaj,aciqlama;}
