package az.simplexs.simplexs.repository.teskilat;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.context.i18n.LocaleContextHolder;
import az.simplexs.simplexs.dto.teskilat.Teskilat;
import az.simplexs.simplexs.dto.teskilat.TeskilatTipi;

@Repository
public class TeskilatRepository {
    private final NamedParameterJdbcTemplate jdbc;
    public TeskilatRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<TeskilatTipi> tipler() {
        return jdbc.query("SELECT * FROM public.fn_teskilat_tipi_siyahisi() ORDER BY teskilat_tipi_adi",
            (r,n) -> new TeskilatTipi(l(r,"teskilat_tipi_id"),r.getString("teskilat_tipi_kodu"),r.getString("teskilat_tipi_adi")));
    }

    public List<Teskilat> siyahi(Long klinikaId) {
        return jdbc.query("""
            SELECT f.*,COALESCE(NULLIF(t.deyer,''),f.ad) lokallasdirilmis_ad
            FROM public.fn_teskilat_siyahisi(p_klinika_id=>CAST(:klinika AS bigint),p_aktiv=>CAST(:aktiv AS boolean)) f
            LEFT JOIN public.kn_diller d ON d.kod=:dil AND d.aktiv
            LEFT JOIN public.kn_melumat_tercumeleri t ON t.melumat_novu='TESKILAT' AND t.menbe_id=f.teskilat_id AND t.saha='ad' AND t.dil_id=d.id
            ORDER BY f.sira_no NULLS LAST,lokallasdirilmis_ad
            """,new MapSqlParameterSource("aktiv",null).addValue("klinika",klinikaId).addValue("dil",LocaleContextHolder.getLocale().getLanguage()),
            (r,n) -> new Teskilat(l(r,"teskilat_id"),l(r,"klinika_id"),l(r,"teskilat_tipi_id"),r.getString("teskilat_tipi_kodu"),r.getString("teskilat_tipi_adi"),r.getString("lokallasdirilmis_ad"),r.getString("qisa_ad"),r.getString("bank_hesab_nomresi"),r.getString("seher_nomresi"),r.getString("mobil_nomre"),r.getString("vergi_nomresi"),r.getString("selahiyyetli_sexs"),i(r,"sira_no"),r.getObject("standartdir",Boolean.class),r.getObject("aktiv",Boolean.class),r.getObject("yaranma_tarixi",java.time.LocalDateTime.class),l(r,"yaradan_personal_id"),r.getObject("yenilenme_tarixi",java.time.LocalDateTime.class),l(r,"yenileyen_personal_id")));
    }

    public Map<String,Object> yarat(Long klinikaId,Long tipId,String ad,String qisaAd,String bank,String seher,String mobil,String vergi,String sexs,boolean standart,Long personalId) {
        String sql="SELECT * FROM public.fn_teskilat_yarat(p_klinika_id=>CAST(:klinika AS bigint),p_teskilat_tipi_id=>CAST(:tip AS bigint),p_ad=>CAST(:ad AS varchar),p_qisa_ad=>CAST(:qisa AS varchar),p_bank_hesab_nomresi=>CAST(:bank AS varchar),p_seher_nomresi=>CAST(:seher AS varchar),p_mobil_nomre=>CAST(:mobil AS varchar),p_vergi_nomresi=>CAST(:vergi AS varchar),p_selahiyyetli_sexs=>CAST(:sexs AS varchar),p_standartdir=>CAST(:standart AS boolean),p_yaradan_personal_id=>CAST(:personal AS bigint))";
        return one(sql,params(ad,qisaAd,bank,seher,mobil,vergi,sexs,standart).addValue("klinika",klinikaId).addValue("tip",tipId).addValue("personal",personalId));
    }

    public Map<String,Object> yenile(Long id,String ad,String qisaAd,String bank,String seher,String mobil,String vergi,String sexs,boolean standart,boolean aktiv,Long personalId) {
        String sql="SELECT * FROM public.fn_teskilat_yenile(p_teskilat_id=>CAST(:id AS bigint),p_ad=>CAST(:ad AS varchar),p_qisa_ad=>CAST(:qisa AS varchar),p_qisa_ad_deyisdirilsin=>true,p_bank_hesab_nomresi=>CAST(:bank AS varchar),p_bank_hesab_nomresi_deyisdirilsin=>true,p_seher_nomresi=>CAST(:seher AS varchar),p_seher_nomresi_deyisdirilsin=>true,p_mobil_nomre=>CAST(:mobil AS varchar),p_mobil_nomre_deyisdirilsin=>true,p_vergi_nomresi=>CAST(:vergi AS varchar),p_vergi_nomresi_deyisdirilsin=>true,p_selahiyyetli_sexs=>CAST(:sexs AS varchar),p_selahiyyetli_sexs_deyisdirilsin=>true,p_standartdir=>CAST(:standart AS boolean),p_aktiv=>CAST(:aktiv AS boolean),p_yenileyen_personal_id=>CAST(:personal AS bigint))";
        return one(sql,params(ad,qisaAd,bank,seher,mobil,vergi,sexs,standart).addValue("id",id).addValue("aktiv",aktiv).addValue("personal",personalId));
    }

    private MapSqlParameterSource params(String ad,String qisa,String bank,String seher,String mobil,String vergi,String sexs,boolean standart) {
        return new MapSqlParameterSource().addValue("ad",ad.trim()).addValue("qisa",blank(qisa)).addValue("bank",blank(bank)).addValue("seher",blank(seher)).addValue("mobil",blank(mobil)).addValue("vergi",blank(vergi)).addValue("sexs",blank(sexs)).addValue("standart",standart);
    }
    private Map<String,Object> one(String sql,MapSqlParameterSource p){List<Map<String,Object>> rows=jdbc.queryForList(sql,p);return rows.isEmpty()?Map.of():rows.getFirst();}
    private static String blank(String s){return s==null||s.isBlank()?null:s.trim();}
    private static Long l(ResultSet r,String c)throws SQLException{Object x=r.getObject(c);return x instanceof Number n?n.longValue():null;}
    private static Integer i(ResultSet r,String c)throws SQLException{Object x=r.getObject(c);return x instanceof Number n?n.intValue():null;}
}
