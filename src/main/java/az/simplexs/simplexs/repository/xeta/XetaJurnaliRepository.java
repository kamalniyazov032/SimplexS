package az.simplexs.simplexs.repository.xeta;

import java.time.LocalDate;
import java.util.List;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import az.simplexs.simplexs.dto.xeta.XetaJurnali;

@Repository
public class XetaJurnaliRepository {
    private final NamedParameterJdbcTemplate jdbc;
    public XetaJurnaliRepository(NamedParameterJdbcTemplate jdbc){this.jdbc=jdbc;}

    public List<XetaJurnali> find(Long klinikaId,String nov,String axtaris,LocalDate baslama,
            LocalDate bitme,int limit,int offset){
        return jdbc.query("""
            SELECT * FROM public.kn_xeta_jurnali_siyahisi(
              CAST(:klinika AS bigint),CAST(:nov AS varchar),CAST(:axtaris AS varchar),
              CAST(:baslama AS date),CAST(:bitme AS date),CAST(:limit AS integer),CAST(:offset AS integer))
            """,new MapSqlParameterSource("klinika",klinikaId).addValue("nov",blank(nov))
                .addValue("axtaris",blank(axtaris)).addValue("baslama",baslama).addValue("bitme",bitme)
                .addValue("limit",limit).addValue("offset",offset),(r,n)->new XetaJurnali(
                    r.getObject("id",Long.class),r.getString("xeta_kodu"),r.getString("xeta_novu"),
                    r.getObject("personal_id",Long.class),r.getString("istifadeci_adi"),
                    r.getObject("klinika_id",Long.class),r.getString("route"),r.getString("http_metod"),
                    r.getString("ip_unvan"),r.getString("exception_sinfi"),r.getString("qisa_aciqlama"),
                    r.getObject("yaranma_tarixi",java.time.LocalDateTime.class),r.getObject("total_sayi",Long.class)));
    }

    public Long write(String kod,String nov,Long personalId,String username,Long klinikaId,
            String route,String method,String ip,String exceptionClass,String summary){
        return jdbc.queryForObject("""
            SELECT public.kn_xeta_jurnali_yaz(CAST(:kod AS varchar),CAST(:nov AS varchar),
              CAST(:pid AS bigint),CAST(:username AS varchar),CAST(:klinika AS bigint),
              CAST(:route AS varchar),CAST(:method AS varchar),CAST(:ip AS varchar),
              CAST(:exceptionClass AS varchar),CAST(:summary AS varchar))
            """,new MapSqlParameterSource("kod",kod).addValue("nov",nov).addValue("pid",personalId)
                .addValue("username",username).addValue("klinika",klinikaId).addValue("route",route)
                .addValue("method",method).addValue("ip",ip).addValue("exceptionClass",exceptionClass)
                .addValue("summary",summary),Long.class);
    }
    private String blank(String value){return value==null||value.isBlank()?null:value.trim();}
}
