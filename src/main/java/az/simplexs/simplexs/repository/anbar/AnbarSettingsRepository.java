package az.simplexs.simplexs.repository.anbar;

import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AnbarSettingsRepository {
    private final NamedParameterJdbcTemplate jdbc;
    public AnbarSettingsRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc=jdbc; }
    private MapSqlParameterSource params(Long clinic,Long id) { return new MapSqlParameterSource("k",clinic).addValue("id",id); }
    public List<Map<String,Object>> groups(Long k,Long id) {
        return jdbc.queryForList("SELECT * FROM public.fn_anbar_mehsul_qrup_siyahisi(p_klinika_id=>CAST(:k AS bigint),p_anbar_id=>CAST(:id AS bigint))",params(k,id));
    }
    public List<Map<String,Object>> people(Long k,Long id) {
        return jdbc.queryForList("SELECT * FROM public.fn_anbar_mesul_sexs_siyahisi(p_klinika_id=>CAST(:k AS bigint),p_anbar_id=>CAST(:id AS bigint))",params(k,id));
    }
    public List<Map<String,Object>> months(Long k,Long id,int year) {
        return jdbc.queryForList("SELECT * FROM public.fn_anbar_kilid_ay_siyahisi(p_klinika_id=>CAST(:k AS bigint),p_anbar_id=>CAST(:id AS bigint),p_il=>CAST(:year AS integer))",params(k,id).addValue("year",year));
    }
    public List<Map<String,Object>> units(Long k,Long id) {
        return jdbc.queryForList("SELECT * FROM public.fn_material_vahid_siyahisi(p_klinika_id=>CAST(:k AS bigint),p_material_id=>CAST(:id AS bigint))",params(k,id));
    }
    private Map<String,Object> save(String sql,MapSqlParameterSource params) {
        return jdbc.queryForMap(sql,params);
    }
    public Map<String,Object> saveGroups(Long k,Long id,String json,Long actor) {
        return save("SELECT * FROM public.fn_anbar_mehsul_qruplari_yadda_saxla(p_klinika_id=>CAST(:k AS bigint),p_anbar_id=>CAST(:id AS bigint),p_qruplar=>CAST(:json AS jsonb),p_emel_eden_personal_id=>CAST(:actor AS bigint))",params(k,id).addValue("json",json).addValue("actor",actor));
    }
    public Map<String,Object> savePeople(Long k,Long id,List<Long> people,Long actor) {
        String array=people.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(",","{","}"));
        return save("SELECT * FROM public.fn_anbar_mesul_sexsleri_yadda_saxla(p_klinika_id=>CAST(:k AS bigint),p_anbar_id=>CAST(:id AS bigint),p_personal_ids=>CAST(:people AS bigint[]),p_emel_eden_personal_id=>CAST(:actor AS bigint))",params(k,id).addValue("people",array).addValue("actor",actor));
    }
    public Map<String,Object> saveMonths(Long k,Long id,int year,String json,Long actor) {
        return save("SELECT * FROM public.fn_anbar_kilidlerini_yadda_saxla(p_klinika_id=>CAST(:k AS bigint),p_anbar_id=>CAST(:id AS bigint),p_il=>CAST(:year AS integer),p_aylar=>CAST(:json AS jsonb),p_emel_eden_personal_id=>CAST(:actor AS bigint))",params(k,id).addValue("year",year).addValue("json",json).addValue("actor",actor));
    }
    public Map<String,Object> bulkMonth(Long k,int year,int month,boolean locked,Long actor) {
        return save("SELECT * FROM public.fn_anbar_kilid_ayi_toplu_yenile(p_klinika_id=>CAST(:k AS bigint),p_il=>CAST(:year AS integer),p_ay=>CAST(:month AS integer),p_kilidlidir=>CAST(:locked AS boolean),p_emel_eden_personal_id=>CAST(:actor AS bigint))",params(k,null).addValue("year",year).addValue("month",month).addValue("locked",locked).addValue("actor",actor));
    }
    public Map<String,Object> saveUnits(Long k,Long id,String json,Long actor) {
        return save("SELECT * FROM public.fn_material_vahidlerini_yadda_saxla(p_klinika_id=>CAST(:k AS bigint),p_material_id=>CAST(:id AS bigint),p_vahidler=>CAST(:json AS jsonb),p_emel_eden_personal_id=>CAST(:actor AS bigint))",params(k,id).addValue("json",json).addValue("actor",actor));
    }
}
