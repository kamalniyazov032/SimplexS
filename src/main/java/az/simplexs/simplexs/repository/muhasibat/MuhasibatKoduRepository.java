package az.simplexs.simplexs.repository.muhasibat;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import az.simplexs.simplexs.dto.muhasibat.MuhasibatKodu;
import az.simplexs.simplexs.dto.muhasibat.MuhasibatTipi;

@Repository
public class MuhasibatKoduRepository {
    private final NamedParameterJdbcTemplate jdbc;
    public MuhasibatKoduRepository(NamedParameterJdbcTemplate jdbc){this.jdbc=jdbc;}
    public List<MuhasibatTipi> tipler(){return jdbc.query("SELECT * FROM public.fn_muhasibat_kodu_tipi_siyahisi() ORDER BY tip_adi",(r,n)->new MuhasibatTipi(l(r,"tip_id"),r.getString("tip_kodu"),r.getString("tip_adi")));}
    public List<MuhasibatKodu> siyahi(Long klinikaId){return jdbc.query("SELECT * FROM public.fn_muhasibat_kodu_siyahisi(CAST(:klinika AS bigint),CAST(:aktiv AS boolean)) ORDER BY tip_adi,ad",new MapSqlParameterSource("aktiv",null).addValue("klinika",klinikaId),(r,n)->new MuhasibatKodu(l(r,"muhasibat_kodu_id"),r.getString("tip_kodu"),r.getString("tip_adi"),r.getString("ad"),r.getString("aciqlama"),r.getObject("aktiv",Boolean.class)));}
    public Map<String,Object> yarat(Long klinikaId,Long tipId,String ad){return one("SELECT * FROM public.fn_muhasibat_kodu_yarat(p_klinika_id=>:klinika,p_tip_id=>:tip,p_ad=>:ad)",new MapSqlParameterSource().addValue("klinika",klinikaId).addValue("tip",tipId).addValue("ad",ad.trim()));}
    public Map<String,Object> yenile(Long id,String ad,String aciqlama,Integer siraNo,boolean aktiv){return one("SELECT * FROM public.fn_muhasibat_kodu_yenile(p_muhasibat_kodu_id=>:id,p_ad=>:ad,p_aciqlama=>:aciqlama,p_aciqlama_deyisdirilsin=>true,p_sira_no=>:sira,p_sira_no_deyisdirilsin=>:siraDeyis,p_aktiv=>:aktiv)",new MapSqlParameterSource().addValue("id",id).addValue("ad",ad.trim()).addValue("aciqlama",blank(aciqlama)).addValue("sira",siraNo).addValue("siraDeyis",siraNo!=null).addValue("aktiv",aktiv));}
    private Map<String,Object> one(String sql,MapSqlParameterSource p){List<Map<String,Object>> rows=jdbc.queryForList(sql,p);return rows.isEmpty()?Map.of():rows.getFirst();}
    private static String blank(String s){return s==null||s.isBlank()?null:s.trim();}
    private static Long l(ResultSet r,String c)throws SQLException{Object x=r.getObject(c);return x instanceof Number n?n.longValue():null;}
}
