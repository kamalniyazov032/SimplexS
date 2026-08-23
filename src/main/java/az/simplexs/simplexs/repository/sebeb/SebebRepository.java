package az.simplexs.simplexs.repository.sebeb;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import az.simplexs.simplexs.dto.sebeb.Sebeb;
import az.simplexs.simplexs.dto.sebeb.SebebNovu;

@Repository
public class SebebRepository {
    private final NamedParameterJdbcTemplate jdbc;
    public SebebRepository(NamedParameterJdbcTemplate jdbc){this.jdbc=jdbc;}
    public List<SebebNovu> novler(Boolean aktiv){return jdbc.query("SELECT * FROM public.fn_sebeb_novu_siyahisi(p_aktiv=>CAST(:aktiv AS boolean)) ORDER BY sira_no NULLS LAST,ad",new MapSqlParameterSource("aktiv",aktiv),this::mapNov);}
    public List<Sebeb> sebebler(Long novId,String novKodu,Boolean aktiv){return jdbc.query("SELECT * FROM public.fn_sebeb_siyahisi(p_sebeb_novu_id=>CAST(:nov AS bigint),p_sebeb_novu_kodu=>CAST(:kod AS text),p_aktiv=>CAST(:aktiv AS boolean)) ORDER BY sira_no NULLS LAST,sebeb_adi",new MapSqlParameterSource("nov",novId).addValue("kod",blank(novKodu)).addValue("aktiv",aktiv),this::mapSebeb);}
    public Map<String,Object> yarat(Long novId,String kod,String ad,String aciqlama,Long personalId){return one("SELECT * FROM public.fn_sebeb_yarat(p_sebeb_novu_id=>:nov,p_kod=>:kod,p_ad=>:ad,p_aciqlama=>:aciqlama,p_yaradan_personal_id=>:personal)",new MapSqlParameterSource("nov",novId).addValue("kod",kod.trim().toUpperCase()).addValue("ad",ad.trim()).addValue("aciqlama",blank(aciqlama)).addValue("personal",personalId));}
    public Map<String,Object> yenile(Long id,String kod,String ad,String aciqlama,boolean aktiv,Long personalId){return one("SELECT * FROM public.fn_sebeb_yenile(p_sebeb_id=>:id,p_kod=>:kod,p_ad=>:ad,p_aciqlama=>:aciqlama,p_aciqlama_deyisdirilsin=>true,p_aktiv=>:aktiv,p_yenileyen_personal_id=>:personal)",new MapSqlParameterSource("id",id).addValue("kod",blank(kod)).addValue("ad",ad.trim()).addValue("aciqlama",blank(aciqlama)).addValue("aktiv",aktiv).addValue("personal",personalId));}
    private SebebNovu mapNov(ResultSet r,int n)throws SQLException{return new SebebNovu(l(r,"sebeb_novu_id"),r.getString("kod"),r.getString("ad"),r.getString("aciqlama"),i(r,"sira_no"),r.getObject("aktiv",Boolean.class),r.getObject("yaranma_tarixi",java.time.LocalDateTime.class),l(r,"yaradan_personal_id"),r.getObject("yenilenme_tarixi",java.time.LocalDateTime.class),l(r,"yenileyen_personal_id"));}
    private Sebeb mapSebeb(ResultSet r,int n)throws SQLException{return new Sebeb(l(r,"sebeb_id"),l(r,"sebeb_novu_id"),r.getString("sebeb_novu_kodu"),r.getString("sebeb_novu_adi"),r.getString("sebeb_kodu"),r.getString("sebeb_adi"),r.getString("aciqlama"),i(r,"sira_no"),r.getObject("aktiv",Boolean.class));}
    private Map<String,Object> one(String sql,MapSqlParameterSource p){var rows=jdbc.queryForList(sql,p);return rows.isEmpty()?Map.of():rows.getFirst();}
    private static String blank(String s){return s==null||s.isBlank()?null:s.trim();}
    private static Long l(ResultSet r,String c)throws SQLException{Object x=r.getObject(c);return x instanceof Number n?n.longValue():null;}
    private static Integer i(ResultSet r,String c)throws SQLException{Object x=r.getObject(c);return x instanceof Number n?n.intValue():null;}
}
