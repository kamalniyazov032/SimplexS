package az.simplexs.simplexs.dto.xeste;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
@lombok.Data
public class XesteForm {
 public Long xesteId,defaultTeskilatId,vesiqeNovuId,cinsId,aileVeziyyetiId,tehsilId,dogulduguOlkeId,dogulduguSeherId,olkeId,seherId,qanQrupuId;
 public String ad,soyad,ataAdi,vesiqeNomresi,finKodu,mobilNomre,ikinciMobilNomre,email,sosialKartNomresi,isYeri,vezifesi,pesesi,unvan,qeyd;
 @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) public LocalDate dogumTarixi;
 public boolean aktiv=true;
}
