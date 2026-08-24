package az.simplexs.simplexs.dto.ambulator;
import java.time.*;
public record Gelis(Long id,Long xesteId,String xesteKodu,String xesteAd,String xesteSoyad,String xesteAtaAdi,String finKodu,String vesiqeNomresi,String mobilNomre,Long gelisNovuId,String gelisNovuKodu,String gelisNovuAdi,Long teskilatId,String teskilatAdi,String protokolKodu,LocalDate gelisTarixi,LocalTime gelisSaati,Boolean randevudur,Long gonderenHekimId,String gonderenHekimAd,String mesaj,String aciqlama,Boolean aktiv){}
