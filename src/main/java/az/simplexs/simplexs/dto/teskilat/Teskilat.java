package az.simplexs.simplexs.dto.teskilat;
import java.time.LocalDateTime;
public record Teskilat(Long id,Long tipId,String tipKodu,String tipAdi,String kod,String ad,String qisaAd,String bankHesabNomresi,String seherNomresi,String mobilNomre,String vergiNomresi,String selahiyyetliSexs,Integer siraNo,Boolean standartdir,Boolean aktiv,LocalDateTime yaranmaTarixi,Long yaradanPersonalId,LocalDateTime yenilenmeTarixi,Long yenileyenPersonalId) {}
