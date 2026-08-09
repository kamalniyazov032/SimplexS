package az.simplexs.simplexs.dto.vezife;
public record Vezife(Long vezifeId, Long klinikaId, String klinikaAdi, String vezifeKodu,
 String vezifeAdi, String aciqlama, Integer siraNo, Boolean aktiv, java.time.LocalDateTime yaranmaTarixi) {}
