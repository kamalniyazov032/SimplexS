package az.simplexs.simplexs.repository.rutin;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import az.simplexs.simplexs.dto.rutin.Rutin;
import az.simplexs.simplexs.dto.rutin.RutinXidmet;
import az.simplexs.simplexs.dto.xidmet.Xidmet;

@Repository
public class RutinRepository {
    private final NamedParameterJdbcTemplate jdbc;
    public RutinRepository(NamedParameterJdbcTemplate jdbc){this.jdbc=jdbc;}

    public List<Rutin> siyahi(Long klinikaId,Boolean aktiv,String query){
        return jdbc.query("""
                SELECT r.*, q.umumi_qiymet
                FROM public.fn_rutin_siyahisi(p_klinika_id=>:klinika,p_aktiv=>:aktiv,p_axtaris=>:q) r
                LEFT JOIN LATERAL (
                    SELECT SUM(x.qiymet) umumi_qiymet
                    FROM public.fn_rutin_xidmet_siyahisi(p_rutin_id=>r.rutin_id,p_aktiv=>true) x
                    WHERE x.qiymet IS NOT NULL
                ) q ON true
                """,
                new MapSqlParameterSource("klinika",klinikaId).addValue("aktiv",aktiv).addValue("q",blank(query)),this::mapRutin);
    }
    public Map<String,Object> yarat(Long klinikaId,String kod,String ad,String aciqlama,boolean rutinQiymeti,Long personalId){
        return one("SELECT * FROM public.fn_rutin_yarat(p_klinika_id=>:klinika,p_kod=>:kod,p_ad=>:ad,p_aciqlama=>:aciqlama,p_rutin_qiymetlerinden_istifade_et=>:qiymet,p_yaradan_personal_id=>:personal)",
                new MapSqlParameterSource("klinika",klinikaId).addValue("kod",kod.trim().toUpperCase()).addValue("ad",ad.trim())
                        .addValue("aciqlama",blank(aciqlama)).addValue("qiymet",rutinQiymeti).addValue("personal",personalId));
    }
    public Map<String,Object> yenile(Long id,Long klinikaId,String kod,String ad,String aciqlama,
            boolean rutinQiymeti,boolean aktiv,Long personalId){
        return one("SELECT * FROM public.fn_rutin_yenile(p_rutin_id=>:id,p_klinika_id=>:klinika,p_kod=>:kod,p_ad=>:ad,p_aciqlama=>:aciqlama,p_aciqlama_deyisdirilsin=>true,p_rutin_qiymetlerinden_istifade_et=>:qiymet,p_aktiv=>:aktiv,p_yenileyen_personal_id=>:personal)",
                new MapSqlParameterSource("id",id).addValue("klinika",klinikaId).addValue("kod",blank(kod))
                        .addValue("ad",blank(ad)).addValue("aciqlama",blank(aciqlama)).addValue("qiymet",rutinQiymeti)
                        .addValue("aktiv",aktiv).addValue("personal",personalId));
    }
    public List<RutinXidmet> terkib(Long rutinId){
        return jdbc.query("SELECT * FROM public.fn_rutin_xidmet_siyahisi(p_rutin_id=>:rutin,p_aktiv=>CAST(:aktiv AS boolean)) ORDER BY sira_no NULLS LAST,xidmet_adi",
                new MapSqlParameterSource("rutin",rutinId).addValue("aktiv",true),this::mapRutinXidmet);
    }
    public List<Xidmet> xidmetAxtar(Long klinikaId,Long qrupId,String query,int limit,int offset){
        return jdbc.query("""
                SELECT f.* FROM public.fn_xidmet_siyahisi(CAST(:klinika AS bigint),CAST(:qrup AS bigint),true,true) f
                WHERE (CAST(:q AS text) IS NULL OR f.xidmet_adi ILIKE '%'||:q||'%' OR f.xidmet_kodu ILIKE '%'||:q||'%')
                ORDER BY f.xidmet_adi LIMIT :limit OFFSET :offset
                """,new MapSqlParameterSource("klinika",klinikaId).addValue("qrup",qrupId).addValue("q",blank(query))
                        .addValue("limit",limit).addValue("offset",offset),this::mapXidmet);
    }
    public Map<String,Object> terkibiSaxla(Long rutinId,String json,Long personalId){
        return one("SELECT * FROM public.fn_rutin_xidmetlerini_yadda_saxla(p_rutin_id=>:rutin,p_xidmetler=>CAST(:json AS jsonb),p_emel_eden_personal_id=>:personal)",
                new MapSqlParameterSource("rutin",rutinId).addValue("json",json).addValue("personal",personalId));
    }
    public boolean rutinQiymetlerindenIstifadeEdir(Long klinikaId,Long rutinId){
        Boolean value=jdbc.queryForObject("""
                SELECT rutin_qiymetlerinden_istifade_et
                FROM public.fn_rutin_siyahisi(p_klinika_id=>:klinika,p_aktiv=>NULL,p_axtaris=>NULL)
                WHERE rutin_id=:id
                """,new MapSqlParameterSource("klinika",klinikaId).addValue("id",rutinId),Boolean.class);
        return Boolean.TRUE.equals(value);
    }
    private Rutin mapRutin(ResultSet r,int n)throws SQLException{return new Rutin(l(r,"rutin_id"),l(r,"klinika_id"),r.getString("kod"),r.getString("ad"),r.getString("aciqlama"),r.getObject("rutin_qiymetlerinden_istifade_et",Boolean.class),i(r,"xidmet_sayi"),r.getBigDecimal("umumi_qiymet"),i(r,"sira_no"),r.getObject("aktiv",Boolean.class),r.getObject("yaranma_tarixi",java.time.LocalDateTime.class),l(r,"yaradan_personal_id"),r.getObject("yenilenme_tarixi",java.time.LocalDateTime.class),l(r,"yenileyen_personal_id"));}
    private RutinXidmet mapRutinXidmet(ResultSet r,int n)throws SQLException{return new RutinXidmet(l(r,"xidmet_id"),r.getString("xidmet_kodu"),r.getString("xidmet_adi"),l(r,"xidmet_qrupu_id"),r.getString("xidmet_qrupu_kodu"),r.getString("xidmet_qrupu_adi"),l(r,"xidmet_tipi_id"),r.getString("xidmet_tipi_kodu"),r.getString("xidmet_tipi_adi"),r.getBigDecimal("qiymet"),i(r,"sira_no"),r.getObject("xidmet_aktiv",Boolean.class),r.getObject("elaqe_aktiv",Boolean.class));}
    private Xidmet mapXidmet(ResultSet r,int n)throws SQLException{return new Xidmet(l(r,"xidmet_id"),r.getString("xidmet_kodu"),r.getString("xidmet_adi"),l(r,"xidmet_qrupu_id"),r.getString("xidmet_qrupu_kodu"),r.getString("xidmet_qrupu_adi"),l(r,"muhasibat_kodu_id"),r.getString("muhasibat_kodu_adi"),l(r,"xidmet_tipi_id"),r.getString("xidmet_tipi_kodu"),r.getString("xidmet_tipi_adi"),r.getString("beynelxalq_kod"),r.getString("beynelxalq_ad"),l(r,"hesabat_novu_id"),r.getString("hesabat_novu_kodu"),r.getString("hesabat_novu_adi"),l(r,"hesabat_mecburiyyeti_id"),r.getString("hesabat_mecburiyyeti_kodu"),r.getString("hesabat_mecburiyyeti_adi"),r.getObject("paket_xidmet",Boolean.class),r.getObject("aktiv",Boolean.class),r.getObject("yaranma_tarixi",java.time.LocalDateTime.class));}
    private Map<String,Object> one(String sql,MapSqlParameterSource p){var rows=jdbc.queryForList(sql,p);return rows.isEmpty()?Map.of():rows.getFirst();}
    private static String blank(String s){return s==null||s.isBlank()?null:s.trim();}
    private static Long l(ResultSet r,String c)throws SQLException{Object x=r.getObject(c);return x instanceof Number n?n.longValue():null;}
    private static Integer i(ResultSet r,String c)throws SQLException{Object x=r.getObject(c);return x instanceof Number n?n.intValue():null;}
}
