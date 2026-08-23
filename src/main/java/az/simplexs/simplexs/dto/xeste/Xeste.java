package az.simplexs.simplexs.dto.xeste;
import java.time.LocalDate;
import java.time.LocalDateTime;
public record Xeste(Long id,Long klinikaId,String kod,Long defaultTeskilatId,String defaultTeskilatAdi,
 String ad,String soyad,String ataAdi,Long vesiqeNovuId,String vesiqeNovuKodu,String vesiqeNovuAdi,String vesiqeNomresi,String finKodu,
 Long cinsId,String cinsKodu,String cinsAdi,LocalDate dogumTarixi,Long aileVeziyyetiId,String aileVeziyyetiKodu,String aileVeziyyetiAdi,
 Long tehsilId,String tehsilKodu,String tehsilAdi,Long dogulduguOlkeId,String dogulduguOlkeAdi,Long dogulduguSeherId,String dogulduguSeherAdi,
 Long olkeId,String olkeAdi,Long seherId,String seherAdi,Long qanQrupuId,String qanQrupuKodu,String qanQrupuAdi,
 String mobilNomre,String ikinciMobilNomre,String email,String sosialKartNomresi,String isYeri,String vezifesi,String pesesi,String unvan,String qeyd,
 Boolean aktiv,LocalDateTime yaranmaTarixi,Long yaradanPersonalId,LocalDateTime yenilenmeTarixi,Long yenileyenPersonalId){public String tamAd(){return String.join(" ",java.util.stream.Stream.of(ad,soyad,ataAdi).filter(x->x!=null&&!x.isBlank()).toList());}}
