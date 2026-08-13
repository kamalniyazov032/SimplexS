package az.simplexs.simplexs.repository.xidmet;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import az.simplexs.simplexs.dto.xidmet.Xidmet;
import az.simplexs.simplexs.dto.xidmet.XidmetOption;
import az.simplexs.simplexs.dto.xidmet.XidmetQrupu;

@Repository
public class XidmetRepository {
    private final NamedParameterJdbcTemplate jdbc;
    public XidmetRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<XidmetQrupu> qruplar(Long klinikaId) {
        return jdbc.query("SELECT * FROM public.fn_xidmet_qrupu_siyahisi(CAST(:klinika AS bigint),CAST(:aktiv AS boolean)) ORDER BY sira_no NULLS LAST,tam_yol",
                new MapSqlParameterSource("aktiv", null).addValue("klinika",klinikaId), (r, n) -> new XidmetQrupu(l(r,"xidmet_qrupu_id"),l(r,"parent_id"),r.getString("kod"),r.getString("ad"),r.getString("aciqlama"),i(r,"seviye"),r.getString("tam_yol"),i(r,"sira_no"),r.getObject("aktiv",Boolean.class),i(r,"alt_qrup_sayi"),r.getObject("alt_qrupu_var",Boolean.class),r.getObject("kok_qrupdur",Boolean.class)));
    }
    public Map<String,Object> qrupYarat(Long klinikaId,Long parentId,String ad) { return one("SELECT * FROM public.fn_xidmet_qrupu_yarat(p_klinika_id=>:klinika,p_parent_id=>:parent,p_ad=>:ad,p_yaradan_personal_id=>NULL)",new MapSqlParameterSource().addValue("klinika",klinikaId).addValue("parent",parentId).addValue("ad",ad.trim())); }
    public Map<String,Object> qrupYenile(Long id,String ad,Long parentId,boolean aktiv) { return one("SELECT * FROM public.fn_xidmet_qrupu_yenile(p_xidmet_qrupu_id=>:id,p_ad=>:ad,p_parent_id=>:parent,p_parent_deyisdirilsin=>true,p_aktiv=>:aktiv,p_yenileyen_personal_id=>NULL)",new MapSqlParameterSource().addValue("id",id).addValue("ad",ad.trim()).addValue("parent",parentId).addValue("aktiv",aktiv)); }

    public List<Xidmet> xidmetler(Long klinikaId,Long qrupId) {
        return jdbc.query("SELECT * FROM public.fn_xidmet_siyahisi(CAST(:klinika AS bigint),CAST(:qrup AS bigint),CAST(:aktiv AS boolean),CAST(:alt AS boolean)) ORDER BY xidmet_adi",
                new MapSqlParameterSource().addValue("klinika",klinikaId).addValue("qrup",qrupId).addValue("aktiv",null).addValue("alt",true),(r,n)->new Xidmet(l(r,"xidmet_id"),r.getString("xidmet_kodu"),r.getString("xidmet_adi"),l(r,"xidmet_qrupu_id"),r.getString("xidmet_qrupu_kodu"),r.getString("xidmet_qrupu_adi"),l(r,"muhasibat_kodu_id"),r.getString("muhasibat_kodu_adi"),l(r,"xidmet_tipi_id"),r.getString("xidmet_tipi_kodu"),r.getString("xidmet_tipi_adi"),r.getString("beynelxalq_kod"),r.getString("beynelxalq_ad"),l(r,"hesabat_novu_id"),r.getString("hesabat_novu_kodu"),r.getString("hesabat_novu_adi"),l(r,"hesabat_mecburiyyeti_id"),r.getString("hesabat_mecburiyyeti_kodu"),r.getString("hesabat_mecburiyyeti_adi"),r.getObject("aktiv",Boolean.class),r.getObject("yaranma_tarixi",java.time.LocalDateTime.class)));
    }
    public List<Xidmet> availableForDepartment(Long klinikaId,Long sobeId,Long qrupId,String query,int limit,int offset){
        String sql="""
                SELECT f.* FROM public.fn_xidmet_siyahisi(CAST(:klinika AS bigint),CAST(:qrup AS bigint),true,true) f
                WHERE (CAST(:q AS text) IS NULL OR f.xidmet_adi ILIKE '%'||:q||'%' OR f.xidmet_kodu ILIKE '%'||:q||'%')
                AND NOT EXISTS (SELECT 1 FROM public.fn_xidmet_sobe_siyahisi(CAST(:sobe AS bigint),true) s WHERE s.xidmet_id=f.xidmet_id)
                ORDER BY f.xidmet_adi LIMIT :limit OFFSET :offset""";
        return jdbc.query(sql,new MapSqlParameterSource().addValue("klinika",klinikaId).addValue("sobe",sobeId).addValue("qrup",qrupId).addValue("q",blank(query)).addValue("limit",limit).addValue("offset",offset),this::mapXidmet);
    }
    public long countAvailableForDepartment(Long klinikaId,Long sobeId,Long qrupId,String query){
        String sql="""
                SELECT count(*) FROM public.fn_xidmet_siyahisi(CAST(:klinika AS bigint),CAST(:qrup AS bigint),true,true) f
                WHERE (CAST(:q AS text) IS NULL OR f.xidmet_adi ILIKE '%'||:q||'%' OR f.xidmet_kodu ILIKE '%'||:q||'%')
                AND NOT EXISTS (SELECT 1 FROM public.fn_xidmet_sobe_siyahisi(CAST(:sobe AS bigint),true) s WHERE s.xidmet_id=f.xidmet_id)""";
        Long count=jdbc.queryForObject(sql,new MapSqlParameterSource().addValue("klinika",klinikaId).addValue("sobe",sobeId).addValue("qrup",qrupId).addValue("q",blank(query)),Long.class);return count==null?0:count;
    }
    public List<XidmetOption> muhasibatKodlari(Long klinikaId){return options("SELECT muhasibat_kodu_id id,tip_kodu kod,ad FROM public.fn_muhasibat_kodu_siyahisi(CAST("+klinikaId+" AS bigint),true)");}
    public List<XidmetOption> xidmetTipleri(){return options("SELECT xidmet_tipi_id id,xidmet_tipi_kodu kod,xidmet_tipi_adi ad FROM public.fn_xidmet_tipi_siyahisi()");}
    public List<XidmetOption> hesabatNovleri(){return options("SELECT hesabat_novu_id id,hesabat_novu_kodu kod,hesabat_novu_adi ad FROM public.fn_hesabat_novu_siyahisi()");}
    public List<XidmetOption> hesabatMecburiyyetleri(){return options("SELECT hesabat_mecburiyyeti_id id,hesabat_mecburiyyeti_kodu kod,hesabat_mecburiyyeti_adi ad FROM public.fn_hesabat_mecburiyyeti_siyahisi()");}
    @Transactional
    public Map<String,Object> xidmetYarat(Long klinikaId,String kod,String ad,Long qrupId,Long muhasibatId,Long tipId,
            String beynelxalqKod,String beynelxalqAd,Long hesabatNovuId,Long mecburiyyetId,boolean aktiv){
        Map<String,Object> created=one("SELECT * FROM public.fn_xidmet_yarat(p_klinika_id=>:klinika,p_kod=>:kod,p_ad=>:ad,p_xidmet_qrupu_id=>:qrup,p_muhasibat_kodu_id=>:muh,p_xidmet_tipi_id=>:tip,p_yaradan_personal_id=>NULL)",new MapSqlParameterSource().addValue("klinika",klinikaId).addValue("kod",kod.trim().toUpperCase()).addValue("ad",ad.trim()).addValue("qrup",qrupId).addValue("muh",muhasibatId).addValue("tip",tipId));
        if(!successful(created))return created;
        Object idValue=created.get("xidmet_id");
        if(!(idValue instanceof Number number)){TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();return Map.of("status_kodu","SISTEM_XETASI","mesaj","Yaradılmış xidmətin ID-si alınmadı");}
        Map<String,Object> updated=xidmetYenile(number.longValue(),ad,qrupId,muhasibatId,tipId,beynelxalqKod,beynelxalqAd,hesabatNovuId,mecburiyyetId,aktiv);
        if(!successful(updated))TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        return updated;
    }
    public Map<String,Object> xidmetYenile(Long id,String ad,Long qrupId,Long muhasibatId,Long tipId,String beynelxalqKod,String beynelxalqAd,Long hesabatNovuId,Long mecburiyyetId,boolean aktiv){return one("SELECT * FROM public.fn_xidmet_yenile(p_xidmet_id=>:id,p_ad=>:ad,p_xidmet_qrupu_id=>:qrup,p_muhasibat_kodu_id=>:muh,p_xidmet_tipi_id=>:tip,p_beynelxalq_kod=>:bk,p_beynelxalq_kod_deyisdirilsin=>true,p_beynelxalq_ad=>:ba,p_beynelxalq_ad_deyisdirilsin=>true,p_hesabat_novu_id=>:hn,p_hesabat_novu_deyisdirilsin=>true,p_hesabat_mecburiyyeti_id=>:hm,p_hesabat_mecburiyyeti_deyisdirilsin=>true,p_aktiv=>:aktiv,p_yenileyen_personal_id=>NULL)",new MapSqlParameterSource().addValue("id",id).addValue("ad",ad.trim()).addValue("qrup",qrupId).addValue("muh",muhasibatId).addValue("tip",tipId).addValue("bk",blank(beynelxalqKod)).addValue("ba",blank(beynelxalqAd)).addValue("hn",hesabatNovuId).addValue("hm",mecburiyyetId).addValue("aktiv",aktiv));}

    private List<XidmetOption> options(String sql){return jdbc.query(sql,(r,n)->new XidmetOption(l(r,"id"),r.getString("kod"),r.getString("ad")));}
    private Xidmet mapXidmet(ResultSet r,int n)throws SQLException{return new Xidmet(l(r,"xidmet_id"),r.getString("xidmet_kodu"),r.getString("xidmet_adi"),l(r,"xidmet_qrupu_id"),r.getString("xidmet_qrupu_kodu"),r.getString("xidmet_qrupu_adi"),l(r,"muhasibat_kodu_id"),r.getString("muhasibat_kodu_adi"),l(r,"xidmet_tipi_id"),r.getString("xidmet_tipi_kodu"),r.getString("xidmet_tipi_adi"),r.getString("beynelxalq_kod"),r.getString("beynelxalq_ad"),l(r,"hesabat_novu_id"),r.getString("hesabat_novu_kodu"),r.getString("hesabat_novu_adi"),l(r,"hesabat_mecburiyyeti_id"),r.getString("hesabat_mecburiyyeti_kodu"),r.getString("hesabat_mecburiyyeti_adi"),r.getObject("aktiv",Boolean.class),r.getObject("yaranma_tarixi",java.time.LocalDateTime.class));}
    private Map<String,Object> one(String sql,MapSqlParameterSource p){List<Map<String,Object>> rows=jdbc.queryForList(sql,p);return rows.isEmpty()?Map.of():rows.getFirst();}
    private static String blank(String s){return s==null||s.isBlank()?null:s.trim();}
    private static boolean successful(Map<String,Object> result){String status=String.valueOf(result.getOrDefault("status_kodu",""));return status.toUpperCase().contains("UGUR")||"1".equals(status);}
    private static Long l(ResultSet r,String c)throws SQLException{Object x=r.getObject(c);return x instanceof Number n?n.longValue():null;}
    private static Integer i(ResultSet r,String c)throws SQLException{Object x=r.getObject(c);return x instanceof Number n?n.intValue():null;}
}
