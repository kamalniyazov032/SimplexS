package az.simplexs.simplexs.repository.vezife;
import java.util.*;import org.springframework.jdbc.core.namedparam.*;import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import az.simplexs.simplexs.dto.vezife.Vezife;
@Repository public class VezifeRepository{
 private final NamedParameterJdbcTemplate jdbc; public VezifeRepository(NamedParameterJdbcTemplate j){jdbc=j;}
 public List<Vezife> findAll(){return jdbc.query("SELECT * FROM public.fn_vezife_siyahisi(CAST(:aktiv AS boolean)) ORDER BY sira_no NULLS LAST,vezife_adi",new MapSqlParameterSource("aktiv",null),(r,n)->new Vezife(num(r.getObject("vezife_id")),null,null,r.getString("vezife_kodu"),r.getString("vezife_adi"),r.getString("aciqlama"),integer(r.getObject("sira_no")),r.getObject("aktiv",Boolean.class),r.getObject("yaranma_tarixi",java.time.LocalDateTime.class)));}
 public void create(String kod,String ad,String aciqlama){jdbc.queryForList("SELECT * FROM public.fn_vezife_yarat(p_kod=>:kod,p_ad=>:ad,p_aciqlama=>:aciqlama,p_aktiv=>true)",new MapSqlParameterSource().addValue("kod",kod.trim().toUpperCase()).addValue("ad",ad.trim()).addValue("aciqlama",blank(aciqlama)));}
 @Transactional public void update(Long id,String ad,String aciqlama,boolean aktiv){MapSqlParameterSource params=new MapSqlParameterSource().addValue("id",id).addValue("ad",ad.trim()).addValue("aciqlama",blank(aciqlama)).addValue("aktiv",aktiv);jdbc.update("UPDATE public.rn_vezifeler SET ad=:ad,aciqlama=:aciqlama WHERE id=:id",params);jdbc.queryForList("SELECT * FROM public.fn_vezife_yenile(p_vezife_id=>:id,p_aktiv=>:aktiv,p_sira_no_deyisdirilsin=>false)",params);}
 private static String blank(String s){return s==null||s.isBlank()?null:s.trim();}private static Long num(Object x){return x instanceof Number n?n.longValue():null;}private static Integer integer(Object x){return x instanceof Number n?n.intValue():null;}
}
