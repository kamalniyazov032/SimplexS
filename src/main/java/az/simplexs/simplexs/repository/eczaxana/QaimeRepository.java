package az.simplexs.simplexs.repository.eczaxana;

import java.util.*;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class QaimeRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public QaimeRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public enum Operation {
        PREPARE("SELECT * FROM public.fn_anbar_qaime_material_hazirla(p_klinika_id=>CAST(:klinika_id AS bigint), p_anbar_id=>CAST(:anbar_id AS bigint), p_qaime_tarixi=>CAST(:qaime_tarixi AS date), p_mehsul_qrupu_id=>CAST(:mehsul_qrupu_id AS bigint), p_material_id=>CAST(:material_id AS bigint), p_qutu_sayi=>CAST(:qutu_sayi AS numeric), p_qutu_ici_miqdar=>CAST(:qutu_ici_miqdar AS numeric), p_alis_qiymeti=>CAST(:alis_qiymeti AS numeric), p_satis_baza_qiymeti=>CAST(:satis_baza_qiymeti AS numeric), p_satis_faizi=>CAST(:satis_faizi AS numeric), p_son_istifade_tarixi=>CAST(:son_istifade_tarixi AS date), p_seriya_no=>CAST(:seriya_no AS character varying), p_aciqlama=>CAST(:aciqlama AS text))"),
        CREATE("SELECT * FROM public.fn_anbar_qaime_yarat(p_klinika_id=>CAST(:klinika_id AS bigint), p_anbar_id=>CAST(:anbar_id AS bigint), p_emeliyyat_novu_id=>CAST(:emeliyyat_novu_id AS bigint), p_qaime_tarixi=>CAST(:qaime_tarixi AS date), p_materiallar=>CAST(:materiallar AS jsonb), p_firma_id=>CAST(:firma_id AS bigint), p_teslim_alan_personal_id=>CAST(:teslim_alan_personal_id AS bigint), p_teslim_eden=>CAST(:teslim_eden AS character varying), p_sened_novu=>CAST(:sened_novu AS character varying), p_sened_tarixi=>CAST(:sened_tarixi AS date), p_sened_no=>CAST(:sened_no AS character varying), p_qaime_saati=>CAST(:qaime_saati AS time without time zone), p_aciqlama=>CAST(:aciqlama AS text), p_yaradan_personal_id=>CAST(:yaradan_personal_id AS bigint))"),
        UPDATE("SELECT * FROM public.fn_anbar_qaime_yenile(p_qaime_id=>CAST(:qaime_id AS bigint), p_klinika_id=>CAST(:klinika_id AS bigint), p_anbar_id=>CAST(:anbar_id AS bigint), p_firma_id=>CAST(:firma_id AS bigint), p_firma_deyisdirilsin=>true, p_teslim_alan_personal_id=>CAST(:teslim_alan_personal_id AS bigint), p_teslim_alan_deyisdirilsin=>true, p_teslim_eden=>CAST(:teslim_eden AS character varying), p_teslim_eden_deyisdirilsin=>true, p_sened_novu=>CAST(:sened_novu AS character varying), p_sened_novu_deyisdirilsin=>true, p_sened_tarixi=>CAST(:sened_tarixi AS date), p_sened_tarixi_deyisdirilsin=>true, p_sened_no=>CAST(:sened_no AS character varying), p_sened_no_deyisdirilsin=>true, p_aciqlama=>CAST(:aciqlama AS text), p_aciqlama_deyisdirilsin=>true, p_yenileyen_personal_id=>CAST(:yenileyen_personal_id AS bigint))"),
        ADD_MATERIAL("SELECT * FROM public.fn_anbar_qaime_material_elave_et(p_qaime_id=>CAST(:qaime_id AS bigint), p_klinika_id=>CAST(:klinika_id AS bigint), p_anbar_id=>CAST(:anbar_id AS bigint), p_mehsul_qrupu_id=>CAST(:mehsul_qrupu_id AS bigint), p_material_id=>CAST(:material_id AS bigint), p_qutu_sayi=>CAST(:qutu_sayi AS numeric), p_qutu_ici_miqdar=>CAST(:qutu_ici_miqdar AS numeric), p_alis_qiymeti=>CAST(:alis_qiymeti AS numeric), p_satis_baza_qiymeti=>CAST(:satis_baza_qiymeti AS numeric), p_satis_faizi=>CAST(:satis_faizi AS numeric), p_son_istifade_tarixi=>CAST(:son_istifade_tarixi AS date), p_seriya_no=>CAST(:seriya_no AS character varying), p_aciqlama=>CAST(:aciqlama AS text), p_yaradan_personal_id=>CAST(:yaradan_personal_id AS bigint))"),
        UPDATE_MATERIAL("SELECT * FROM public.fn_anbar_qaime_material_yenile(p_qaime_material_id=>CAST(:qaime_material_id AS bigint), p_klinika_id=>CAST(:klinika_id AS bigint), p_anbar_id=>CAST(:anbar_id AS bigint), p_qutu_sayi=>CAST(:qutu_sayi AS numeric), p_qutu_ici_miqdar=>CAST(:qutu_ici_miqdar AS numeric), p_alis_qiymeti=>CAST(:alis_qiymeti AS numeric), p_satis_baza_qiymeti=>CAST(:satis_baza_qiymeti AS numeric), p_satis_faizi=>CAST(:satis_faizi AS numeric), p_son_istifade_tarixi=>CAST(:son_istifade_tarixi AS date), p_son_istifade_tarixi_deyisdirilsin=>true, p_seriya_no=>CAST(:seriya_no AS character varying), p_seriya_no_deyisdirilsin=>true, p_aciqlama=>CAST(:aciqlama AS text), p_aciqlama_deyisdirilsin=>true, p_yenileyen_personal_id=>CAST(:yenileyen_personal_id AS bigint))"),
        DELETE_MATERIAL("SELECT * FROM public.fn_anbar_qaime_material_sil(p_qaime_material_id=>CAST(:qaime_material_id AS bigint), p_klinika_id=>CAST(:klinika_id AS bigint), p_anbar_id=>CAST(:anbar_id AS bigint), p_yenileyen_personal_id=>CAST(:yenileyen_personal_id AS bigint))");
        final String sql;

        Operation(String sql) {
            this.sql = sql;
        }
    }

    private MapSqlParameterSource context(Long clinic, Long warehouse) {
        return new MapSqlParameterSource("clinic", clinic).addValue("warehouse", warehouse);
    }

    private List<Map<String, Object>> rows(String sql, MapSqlParameterSource p) {
        return jdbc.queryForList(sql, p).stream().map(row -> {
            Map<String, Object> result = new LinkedHashMap<>(row);
            result.replaceAll((key, value) -> value instanceof java.sql.Date d ? d.toLocalDate().toString()
                    : value instanceof java.sql.Time t ? t.toLocalTime().toString() : value);
            return result;
        }).toList();
    }

    public Map<String, Object> list(Long clinic, Long warehouse, int year, Map<String, String> filter, int page, int size) {
        var p = context(clinic, warehouse).addValue("year", year).addValue("direction", blank(filter.get("direction")))
                .addValue("from", blank(filter.get("from"))).addValue("to", blank(filter.get("to")))
                .addValue("operation", blank(filter.get("operation"))).addValue("company", pattern(filter.get("company")))
                .addValue("document", pattern(filter.get("document"))).addValue("receiver", pattern(filter.get("receiver")))
                .addValue("sender", pattern(filter.get("sender"))).addValue("note", pattern(filter.get("note")))
                .addValue("offset", (page - 1) * size).addValue("size", size);
        String source = """
                FROM public.fn_anbar_qaime_siyahisi(:clinic,:warehouse,:year,CAST(:direction AS varchar)) q
                WHERE (CAST(:from AS date) IS NULL OR qaime_tarixi>=CAST(:from AS date))
                  AND (CAST(:to AS date) IS NULL OR qaime_tarixi<=CAST(:to AS date))
                  AND (CAST(:operation AS bigint) IS NULL OR emeliyyat_novu_id=CAST(:operation AS bigint))
                  AND (CAST(:company AS text) IS NULL OR geldiyi_yer ILIKE CAST(:company AS text))
                  AND (CAST(:document AS text) IS NULL OR sened_no ILIKE CAST(:document AS text))
                  AND (CAST(:receiver AS text) IS NULL OR teslim_alan ILIKE CAST(:receiver AS text))
                  AND (CAST(:sender AS text) IS NULL OR teslim_eden ILIKE CAST(:sender AS text))
                  AND (CAST(:note AS text) IS NULL OR aciqlama ILIKE CAST(:note AS text))
                """;
        Long total = jdbc.queryForObject("SELECT count(*) " + source, p, Long.class);
        return Map.of("rows", rows("SELECT q.* " + source + " ORDER BY qaime_tarixi DESC,qaime_saati DESC,sira_no DESC,qaime_id DESC LIMIT :size OFFSET :offset", p), "total", total == null ? 0 : total);
    }

    public Optional<Map<String, Object>> invoice(Long clinic, Long warehouse, int year, Long id) {
        return rows("SELECT * FROM public.fn_anbar_qaime_siyahisi(:clinic,:warehouse,:year,NULL) WHERE qaime_id=:id",
                context(clinic, warehouse).addValue("year", year).addValue("id", id)).stream().findFirst();
    }

    public List<Map<String, Object>> materials(Long clinic, Long warehouse, Long id) {
        return rows("SELECT * FROM public.fn_anbar_qaime_material_siyahisi(:clinic,:warehouse,:id)", context(clinic, warehouse).addValue("id", id));
    }

    public List<Map<String, Object>> catalog(Long clinic, Long warehouse, Long group) {
        return rows("SELECT * FROM public.fn_anbar_qrup_material_siyahisi(:clinic,:warehouse,:group)", context(clinic, warehouse).addValue("group", group));
    }

    public Map<String, Object> options(Long clinic, Long warehouse) {
        var p = context(clinic, warehouse);
        return Map.of("operations", rows("SELECT emeliyyat_novu_id id,ad,istiqamet FROM public.fn_anbar_emeliyyat_novu_siyahisi(:clinic,NULL,true)", p),
                "groups", rows("SELECT mehsul_qrupu_id id,qrup_adi ad FROM public.fn_anbar_mehsul_qrup_siyahisi(:clinic,:warehouse) WHERE secilib", p),
                "companies", rows("SELECT id,ad FROM public.rn_firmalar WHERE klinika_id=:clinic AND aktiv ORDER BY ad", p),
                "receivers", rows("SELECT p.id,trim(coalesce(p.ad,'')||' '||coalesce(p.soyad,'')) ad FROM public.rn_anbar_mesul_sexsler m JOIN public.rn_personallar p ON p.id=m.personal_id AND p.aktiv WHERE m.klinika_id=:clinic AND m.anbar_id=:warehouse AND m.aktiv ORDER BY ad", p));
    }

    public Map<String, Object> execute(Operation operation, Map<String, Object> values) {
        var p = new MapSqlParameterSource(values);
        // Bind optional fields to NULL; function and parameter names only come from the fixed enum.
        var matcher = java.util.regex.Pattern.compile(":([a-z_]+)").matcher(operation.sql);
        while (matcher.find()) if (!p.hasValue(matcher.group(1))) p.addValue(matcher.group(1), null);
        return jdbc.queryForMap(operation.sql, p);
    }

    private static String blank(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }

    private static String pattern(String v) {
        String s = blank(v);
        return s == null ? null : "%" + s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
    }
}
