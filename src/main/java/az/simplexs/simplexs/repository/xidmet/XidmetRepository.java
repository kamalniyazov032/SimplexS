package az.simplexs.simplexs.repository.xidmet;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import az.simplexs.simplexs.dto.xidmet.Xidmet;
import az.simplexs.simplexs.dto.xidmet.XidmetOption;
import az.simplexs.simplexs.dto.xidmet.XidmetQrupu;

@Repository
public class XidmetRepository {
    private final NamedParameterJdbcTemplate jdbc;
    public XidmetRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<XidmetQrupu> qruplar() {
        return jdbc.query("SELECT * FROM public.fn_xidmet_qrupu_siyahisi(CAST(:aktiv AS boolean)) ORDER BY sira_no NULLS LAST,tam_yol",
                new MapSqlParameterSource("aktiv", null), (r, n) -> new XidmetQrupu(l(r,"xidmet_qrupu_id"),l(r,"parent_id"),r.getString("kod"),r.getString("ad"),r.getString("aciqlama"),i(r,"seviye"),r.getString("tam_yol"),i(r,"sira_no"),r.getObject("aktiv",Boolean.class),i(r,"alt_qrup_sayi"),r.getObject("alt_qrupu_var",Boolean.class),r.getObject("kok_qrupdur",Boolean.class)));
    }
    public Map<String,Object> qrupYarat(Long parentId,String ad) { return one("SELECT * FROM public.fn_xidmet_qrupu_yarat(p_parent_id=>:parent,p_ad=>:ad,p_yaradan_personal_id=>NULL)",new MapSqlParameterSource().addValue("parent",parentId).addValue("ad",ad.trim())); }
    public Map<String,Object> qrupYenile(Long id,String ad,Long parentId,boolean aktiv) { return one("SELECT * FROM public.fn_xidmet_qrupu_yenile(p_xidmet_qrupu_id=>:id,p_ad=>:ad,p_parent_id=>:parent,p_parent_deyisdirilsin=>true,p_aktiv=>:aktiv,p_yenileyen_personal_id=>NULL)",new MapSqlParameterSource().addValue("id",id).addValue("ad",ad.trim()).addValue("parent",parentId).addValue("aktiv",aktiv)); }

    public List<Xidmet> xidmetler(Long qrupId) {
        return jdbc.query("SELECT * FROM public.fn_xidmet_siyahisi(CAST(:qrup AS bigint),CAST(:aktiv AS boolean),CAST(:alt AS boolean)) ORDER BY xidmet_adi",
                new MapSqlParameterSource().addValue("qrup",qrupId).addValue("aktiv",null).addValue("alt",true),(r,n)->new Xidmet(l(r,"xidmet_id"),r.getString("xidmet_kodu"),r.getString("xidmet_adi"),l(r,"xidmet_qrupu_id"),r.getString("xidmet_qrupu_kodu"),r.getString("xidmet_qrupu_adi"),l(r,"muhasibat_kodu_id"),r.getString("muhasibat_kodu_adi"),l(r,"xidmet_tipi_id"),r.getString("xidmet_tipi_kodu"),r.getString("xidmet_tipi_adi"),r.getString("beynelxalq_kod"),r.getString("beynelxalq_ad"),l(r,"hesabat_novu_id"),r.getString("hesabat_novu_kodu"),r.getString("hesabat_novu_adi"),l(r,"hesabat_mecburiyyeti_id"),r.getString("hesabat_mecburiyyeti_kodu"),r.getString("hesabat_mecburiyyeti_adi"),r.getObject("aktiv",Boolean.class),r.getObject("yaranma_tarixi",java.time.LocalDateTime.class)));
    }
    public List<XidmetOption> muhasibatKodlari(){return options("SELECT muhasibat_kodu_id id,tip_kodu kod,ad FROM public.fn_muhasibat_kodu_siyahisi(true)");}
    public List<XidmetOption> xidmetTipleri(){return options("SELECT xidmet_tipi_id id,xidmet_tipi_kodu kod,xidmet_tipi_adi ad FROM public.fn_xidmet_tipi_siyahisi()");}
    public List<XidmetOption> hesabatNovleri(){return options("SELECT hesabat_novu_id id,hesabat_novu_kodu kod,hesabat_novu_adi ad FROM public.fn_hesabat_novu_siyahisi()");}
    public List<XidmetOption> hesabatMecburiyyetleri(){return options("SELECT hesabat_mecburiyyeti_id id,hesabat_mecburiyyeti_kodu kod,hesabat_mecburiyyeti_adi ad FROM public.fn_hesabat_mecburiyyeti_siyahisi()");}
    public Map<String,Object> xidmetYarat(String kod,String ad,Long qrupId,Long muhasibatId,Long tipId){return one("SELECT * FROM public.fn_xidmet_yarat(p_kod=>:kod,p_ad=>:ad,p_xidmet_qrupu_id=>:qrup,p_muhasibat_kodu_id=>:muh,p_xidmet_tipi_id=>:tip,p_yaradan_personal_id=>NULL)",new MapSqlParameterSource().addValue("kod",kod.trim().toUpperCase()).addValue("ad",ad.trim()).addValue("qrup",qrupId).addValue("muh",muhasibatId).addValue("tip",tipId));}
    public Map<String,Object> xidmetYenile(Long id,String ad,Long qrupId,Long muhasibatId,Long tipId,String beynelxalqKod,String beynelxalqAd,Long hesabatNovuId,Long mecburiyyetId,boolean aktiv){return one("SELECT * FROM public.fn_xidmet_yenile(p_xidmet_id=>:id,p_ad=>:ad,p_xidmet_qrupu_id=>:qrup,p_muhasibat_kodu_id=>:muh,p_xidmet_tipi_id=>:tip,p_beynelxalq_kod=>:bk,p_beynelxalq_kod_deyisdirilsin=>true,p_beynelxalq_ad=>:ba,p_beynelxalq_ad_deyisdirilsin=>true,p_hesabat_novu_id=>:hn,p_hesabat_novu_deyisdirilsin=>true,p_hesabat_mecburiyyeti_id=>:hm,p_hesabat_mecburiyyeti_deyisdirilsin=>true,p_sira_no_deyisdirilsin=>false,p_aktiv=>:aktiv,p_yenileyen_personal_id=>NULL)",new MapSqlParameterSource().addValue("id",id).addValue("ad",ad.trim()).addValue("qrup",qrupId).addValue("muh",muhasibatId).addValue("tip",tipId).addValue("bk",blank(beynelxalqKod)).addValue("ba",blank(beynelxalqAd)).addValue("hn",hesabatNovuId).addValue("hm",mecburiyyetId).addValue("aktiv",aktiv));}

    private List<XidmetOption> options(String sql){return jdbc.query(sql,(r,n)->new XidmetOption(l(r,"id"),r.getString("kod"),r.getString("ad")));}
    private Map<String,Object> one(String sql,MapSqlParameterSource p){List<Map<String,Object>> rows=jdbc.queryForList(sql,p);return rows.isEmpty()?Map.of():rows.getFirst();}
    private static String blank(String s){return s==null||s.isBlank()?null:s.trim();}
    private static Long l(ResultSet r,String c)throws SQLException{Object x=r.getObject(c);return x instanceof Number n?n.longValue():null;}
    private static Integer i(ResultSet r,String c)throws SQLException{Object x=r.getObject(c);return x instanceof Number n?n.intValue():null;}
}
