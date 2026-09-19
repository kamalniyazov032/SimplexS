package az.simplexs.simplexs.repository.eczaxana;

import java.util.*;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class TelebRepository {
    private final NamedParameterJdbcTemplate jdbc;
    public TelebRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<String> modules(Long clinic, Long personal) {
        return jdbc.queryForList("SELECT modul_kodu FROM public.fn_personal_modul_siyahisi(:personal,:clinic) WHERE modul_kodu IN ('HIS_PHARMACY_REQUESTS_SENT','HIS_PHARMACY_REQUESTS_INCOMING')",
                new MapSqlParameterSource("clinic", clinic).addValue("personal", personal), String.class);
    }

    public List<Map<String,Object>> list(Long clinic, Long warehouse, String side, String status, String from, String to) {
        return rows("SELECT * FROM public.fn_anbar_teleb_siyahisi(:clinic,:warehouse,CAST(:side AS varchar),CAST(:status AS varchar),CAST(:from AS date),CAST(:to AS date))",
                new MapSqlParameterSource("clinic", clinic).addValue("warehouse", warehouse).addValue("side", side)
                        .addValue("status", status).addValue("from", from).addValue("to", to));
    }

    public List<Map<String,Object>> counterpart(Long warehouse) {
        return rows("SELECT * FROM public.fn_anbar_teleb_ucun_qarsi_anbar_siyahisi(:warehouse)", new MapSqlParameterSource("warehouse", warehouse));
    }

    public List<Map<String,Object>> groups(Long warehouse) {
        return rows("SELECT mehsul_qrupu_id id,qrup_adi ad FROM public.fn_anbar_mehsul_qrup_siyahisi(:warehouse) WHERE secilib", new MapSqlParameterSource("warehouse", warehouse));
    }

    public List<Map<String,Object>> catalog(Long warehouse, Long group) {
        return rows("""
                SELECT m.* FROM public.fn_anbar_teleb_material_secim_siyahisi(:warehouse) m
                WHERE CAST(:group AS bigint) IS NULL OR EXISTS (
                    SELECT 1 FROM public.fn_anbar_qrup_material_siyahisi(:warehouse,CAST(:group AS bigint)) g WHERE g.material_id=m.material_id)
                """, new MapSqlParameterSource("warehouse", warehouse).addValue("group", group));
    }

    public List<Map<String,Object>> units(Long material) {
        return rows("SELECT * FROM public.fn_material_vahid_siyahisi(:material) WHERE aktiv", new MapSqlParameterSource("material", material));
    }

    public Map<String,Object> editStatus(Long id, Long personal) {
        return rows("SELECT * FROM public.fn_anbar_teleb_redakte_statusu(:id,:personal)",
                new MapSqlParameterSource("id", id).addValue("personal", personal)).stream().findFirst().orElse(Map.of());
    }

    public List<Map<String,Object>> materials(Long id) {
        return rows("SELECT * FROM public.fn_anbar_teleb_material_siyahisi(:id,NULL)", new MapSqlParameterSource("id", id));
    }

    public List<Map<String,Object>> fulfillmentMaterials(Long id, String date) {
        return rows("SELECT * FROM public.fn_anbar_teleb_qarsilama_material_siyahisi(:id,CAST(:date AS date),NULL)", new MapSqlParameterSource("id", id).addValue("date", date));
    }

    public Map<String,Object> viewed(Long id, Long warehouse, Long personal) {
        return jdbc.queryForMap("SELECT * FROM public.fn_anbar_telebe_baxilir(:id,:warehouse,:personal)",
                new MapSqlParameterSource("id", id).addValue("warehouse", warehouse).addValue("personal", personal));
    }

    public Map<String,Object> save(Long id, Long warehouse, Long target, String date, String materials, String note, Long personal) {
        var p = new MapSqlParameterSource("id", id).addValue("warehouse", warehouse).addValue("target", target)
                .addValue("date", date).addValue("materials", materials).addValue("note", note).addValue("personal", personal);
        return jdbc.queryForMap(id == null ? """
                SELECT * FROM public.fn_anbar_teleb_yarat(p_isteyen_anbar_id=>:warehouse,p_qarsilayan_anbar_id=>:target,
                    p_teleb_tarixi=>CAST(:date AS date),p_materiallar=>CAST(:materials AS jsonb),p_aciqlama=>CAST(:note AS text),p_yaradan_personal_id=>:personal)
                """ : """
                SELECT * FROM public.fn_anbar_teleb_yenile(p_teleb_id=>:id,p_isteyen_anbar_id=>:warehouse,p_qarsilayan_anbar_id=>:target,
                    p_teleb_tarixi=>CAST(:date AS date),p_materiallar=>CAST(:materials AS jsonb),p_aciqlama=>CAST(:note AS text),p_yenileyen_personal_id=>:personal)
                """, p);
    }

    public Map<String,Object> fulfill(Long id, Long warehouse, String date, String time, String decisions, String note, Long personal) {
        return jdbc.queryForMap("""
                SELECT * FROM public.fn_anbar_teleb_qarsila(p_teleb_id=>:id,p_qarsilayan_anbar_id=>:warehouse,
                    p_qarsilama_tarixi=>CAST(:date AS date),p_qerarlar=>CAST(:decisions AS jsonb),p_qarsilama_saati=>CAST(:time AS time),
                    p_aciqlama=>CAST(:note AS text),p_tesdiq_eden_personal_id=>:personal)
                """, new MapSqlParameterSource("id", id).addValue("warehouse", warehouse).addValue("date", date)
                .addValue("time", time).addValue("decisions", decisions).addValue("note", note).addValue("personal", personal));
    }

    public List<Map<String,Object>> history(Long id) {
        return rows("SELECT * FROM public.fn_anbar_teleb_qarsilama_tarixcesi(:id)", new MapSqlParameterSource("id", id));
    }

    public List<Map<String,Object>> historyDetail(Long clinic, Long id) {
        return rows("SELECT * FROM public.fn_anbar_teleb_qarsilama_detali(:clinic,:id)", new MapSqlParameterSource("clinic", clinic).addValue("id", id));
    }

    private List<Map<String,Object>> rows(String sql, MapSqlParameterSource p) {
        return jdbc.queryForList(sql, p).stream().map(row -> {
            Map<String,Object> result = new LinkedHashMap<>(row);
            result.replaceAll((key,value) -> value instanceof java.sql.Date d ? d.toLocalDate().toString()
                    : value instanceof java.sql.Time t ? t.toLocalTime().toString()
                    : value instanceof java.sql.Timestamp t ? t.toLocalDateTime().toString() : value);
            return result;
        }).toList();
    }
}
