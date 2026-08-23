package az.simplexs.simplexs.dto.sebeb;

import java.time.LocalDateTime;

public record SebebNovu(Long id,String kod,String ad,String aciqlama,Integer siraNo,Boolean aktiv,
        LocalDateTime yaranmaTarixi,Long yaradanPersonalId,LocalDateTime yenilenmeTarixi,Long yenileyenPersonalId) {}
