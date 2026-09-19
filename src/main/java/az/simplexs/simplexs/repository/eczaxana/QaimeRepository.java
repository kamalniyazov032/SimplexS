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

    public Map<String, Object> list(Long clinic, Long warehouse, int year,
                                    Map<String, String> filter, int page, int size) {

        var p = new MapSqlParameterSource()
                .addValue("clinic", clinic)
                .addValue("warehouse", warehouse)
                .addValue("year", year)
                .addValue("direction", blank(filter.get("direction")))
                .addValue("from", blank(filter.get("from")))
                .addValue("to", blank(filter.get("to")))
                .addValue("operation", blank(filter.get("operation")))
                .addValue("company", pattern(filter.get("company")))
                .addValue("document", pattern(filter.get("document")))
                .addValue("receiver", pattern(filter.get("receiver")))
                .addValue("sender", pattern(filter.get("sender")))
                .addValue("note", pattern(filter.get("note")))
                .addValue("offset", (page - 1) * size)
                .addValue("size", size);

        String source = """
                FROM public.fn_anbar_qaime_siyahisi(
                    :clinic,
                    :warehouse,
                    :year,
                    CAST(:direction AS varchar)
                ) q
                WHERE (CAST(:from AS date) IS NULL OR qaime_tarixi >= CAST(:from AS date))
                  AND (CAST(:to AS date) IS NULL OR qaime_tarixi <= CAST(:to AS date))
                  AND (CAST(:operation AS bigint) IS NULL OR emeliyyat_novu_id = CAST(:operation AS bigint))
                  AND (CAST(:company AS text) IS NULL OR geldiyi_yer ILIKE CAST(:company AS text))
                  AND (CAST(:document AS text) IS NULL OR sened_no ILIKE CAST(:document AS text))
                  AND (CAST(:receiver AS text) IS NULL OR teslim_alan ILIKE CAST(:receiver AS text))
                  AND (CAST(:sender AS text) IS NULL OR teslim_eden ILIKE CAST(:sender AS text))
                  AND (CAST(:note AS text) IS NULL OR aciqlama ILIKE CAST(:note AS text))
                """;

        Long total = jdbc.queryForObject("SELECT count(*) " + source, p, Long.class);

        var rows = rows("""
                SELECT q.*
                """ + source + """
                ORDER BY qaime_tarixi DESC,
                         qaime_saati DESC,
                         sira_no DESC,
                         qaime_id DESC
                LIMIT :size OFFSET :offset
                """, p);

        return Map.of(
                "rows", rows,
                "total", total == null ? 0 : total
        );
    }

    public Optional<Map<String, Object>> invoice(Long clinic, Long warehouse, int year, Long id) {
        var p = new MapSqlParameterSource()
                .addValue("clinic", clinic)
                .addValue("warehouse", warehouse)
                .addValue("year", year)
                .addValue("id", id);

        return rows("""
                SELECT *
                FROM public.fn_anbar_qaime_siyahisi(:clinic,:warehouse,:year,NULL)
                WHERE qaime_id=:id
                """, p).stream().findFirst();
    }

    public List<Map<String, Object>> materials(Long warehouse, Long qaimeId) {
        return rows("""
                SELECT *
                FROM public.fn_anbar_qaime_material_siyahisi(:warehouse,:qaimeId)
                """,
                new MapSqlParameterSource()
                        .addValue("warehouse", warehouse)
                        .addValue("qaimeId", qaimeId));
    }

    public List<Map<String, Object>> catalog(Long warehouse, Long group) {
        return rows("""
                SELECT *
                FROM public.fn_anbar_qrup_material_siyahisi(:warehouse,:group)
                """,
                new MapSqlParameterSource()
                        .addValue("warehouse", warehouse)
                        .addValue("group", group));
    }

    public List<Map<String, Object>> units(Long materialId) {
        return rows("""
                SELECT *
                FROM public.fn_material_vahid_siyahisi(:materialId)
                WHERE aktiv
                """,
                new MapSqlParameterSource("materialId", materialId));
    }

    public Map<String, Object> options(Long clinic, Long warehouse) {
        var p = new MapSqlParameterSource()
                .addValue("clinic", clinic)
                .addValue("warehouse", warehouse);

        return Map.of(
                "vat", rows("""
                        SELECT text_deyer
                        FROM public.fn_parametr_klinika_siyahisi(:clinic)
                        WHERE parametr_kodu='ANBAR_EDV_FAIZI'
                        """, p),

                "operations", rows("""
                        SELECT emeliyyat_novu_id id, ad, istiqamet
                        FROM public.fn_anbar_emeliyyat_novu_siyahisi(:clinic,NULL,true)
                        """, p),

                "groups", rows("""
                        SELECT mehsul_qrupu_id id, qrup_adi ad
                        FROM public.fn_anbar_mehsul_qrup_siyahisi(:warehouse)
                        WHERE secilib
                        """, p),

                "companies", rows("""
                        SELECT firma_id id, ad
                        FROM public.fn_firma_siyahisi(
                            p_klinika_id=>:clinic,
                            p_aktiv=>true
                        )
                        ORDER BY ad
                        """, p),

                "receivers", rows("""
                        SELECT personal_id id, personal_adi ad, secilib
                        FROM public.fn_anbar_mesul_sexs_siyahisi(
                            p_anbar_id=>CAST(:warehouse AS bigint)
                        )
                        WHERE secilib
                        ORDER BY personal_adi
                        """, p)
        );
    }

    public Map<String, Object> create(Map<String, Object> v) {
        return query("""
                SELECT *
                FROM public.fn_anbar_qaime_yarat(
                    p_anbar_id=>CAST(:anbar_id AS bigint),
                    p_emeliyyat_novu_id=>CAST(:emeliyyat_novu_id AS bigint),
                    p_qaime_tarixi=>CAST(:qaime_tarixi AS date),
                    p_qaime_saati=>CAST(:qaime_saati AS time),
                    p_firma_id=>CAST(:firma_id AS bigint),
                    p_teslim_alan_personal_id=>CAST(:teslim_alan_personal_id AS bigint),
                    p_teslim_eden=>CAST(:teslim_eden AS varchar),
                    p_sened_novu=>CAST(:sened_novu AS varchar),
                    p_sened_tarixi=>CAST(:sened_tarixi AS date),
                    p_sened_no=>CAST(:sened_no AS varchar),
                    p_aciqlama=>CAST(:aciqlama AS text),
                    p_materiallar=>CAST(:materiallar AS jsonb),
                    p_yaradan_personal_id=>CAST(:yaradan_personal_id AS bigint)
                )
                """, v);
    }

    public Map<String, Object> update(Map<String, Object> v) {
        return query("""
                SELECT *
                FROM public.fn_anbar_qaime_yenile(
                    p_qaime_id=>CAST(:qaime_id AS bigint),
                    p_anbar_id=>CAST(:anbar_id AS bigint),

                    p_firma_id=>CAST(:firma_id AS bigint),
                    p_firma_deyisdirilsin=>CAST(:firma_deyisdirilsin AS boolean),

                    p_teslim_alan_personal_id=>CAST(:teslim_alan_personal_id AS bigint),
                    p_teslim_alan_deyisdirilsin=>CAST(:teslim_alan_deyisdirilsin AS boolean),

                    p_teslim_eden=>CAST(:teslim_eden AS varchar),
                    p_teslim_eden_deyisdirilsin=>CAST(:teslim_eden_deyisdirilsin AS boolean),

                    p_sened_novu=>CAST(:sened_novu AS varchar),
                    p_sened_novu_deyisdirilsin=>CAST(:sened_novu_deyisdirilsin AS boolean),

                    p_sened_tarixi=>CAST(:sened_tarixi AS date),
                    p_sened_tarixi_deyisdirilsin=>CAST(:sened_tarixi_deyisdirilsin AS boolean),

                    p_sened_no=>CAST(:sened_no AS varchar),
                    p_sened_no_deyisdirilsin=>CAST(:sened_no_deyisdirilsin AS boolean),

                    p_aciqlama=>CAST(:aciqlama AS text),
                    p_aciqlama_deyisdirilsin=>CAST(:aciqlama_deyisdirilsin AS boolean),

                    p_yenileyen_personal_id=>CAST(:yenileyen_personal_id AS bigint)
                )
                """, v);
    }

    public Map<String, Object> addMaterial(Map<String, Object> v) {
        return query("""
                SELECT *
                FROM public.fn_anbar_qaime_material_elave_et(
                    p_qaime_id=>CAST(:qaime_id AS bigint),
                    p_anbar_id=>CAST(:anbar_id AS bigint),
                    p_mehsul_qrupu_id=>CAST(:mehsul_qrupu_id AS bigint),
                    p_material_id=>CAST(:material_id AS bigint),
                    p_vahid_id=>CAST(:vahid_id AS bigint),
                    p_miqdar=>CAST(:miqdar AS numeric),
                    p_alis_qiymeti=>CAST(:alis_qiymeti AS numeric),
                    p_edv_faizi=>CAST(:edv_faizi AS numeric),
                    p_satis_baza_qiymeti=>CAST(:satis_baza_qiymeti AS numeric),
                    p_satis_faizi=>CAST(:satis_faizi AS numeric),
                    p_son_istifade_tarixi=>CAST(:son_istifade_tarixi AS date),
                    p_seriya_no=>CAST(:seriya_no AS varchar),
                    p_aciqlama=>CAST(:aciqlama AS text),
                    p_yaradan_personal_id=>CAST(:yaradan_personal_id AS bigint)
                )
                """, v);
    }

    public Map<String, Object> updateMaterial(Map<String, Object> v) {
        return query("""
                SELECT *
                FROM public.fn_anbar_qaime_material_yenile(
                    p_qaime_material_id=>CAST(:qaime_material_id AS bigint),
                    p_anbar_id=>CAST(:anbar_id AS bigint),

                    p_vahid_id=>CAST(:vahid_id AS bigint),
                    p_miqdar=>CAST(:miqdar AS numeric),

                    p_alis_qiymeti=>CAST(:alis_qiymeti AS numeric),
                    p_edv_faizi=>CAST(:edv_faizi AS numeric),
                    p_satis_baza_qiymeti=>CAST(:satis_baza_qiymeti AS numeric),
                    p_satis_faizi=>CAST(:satis_faizi AS numeric),

                    p_son_istifade_tarixi=>CAST(:son_istifade_tarixi AS date),
                    p_son_istifade_tarixi_deyisdirilsin=>CAST(:son_istifade_tarixi_deyisdirilsin AS boolean),

                    p_seriya_no=>CAST(:seriya_no AS varchar),
                    p_seriya_no_deyisdirilsin=>CAST(:seriya_no_deyisdirilsin AS boolean),

                    p_aciqlama=>CAST(:aciqlama AS text),
                    p_aciqlama_deyisdirilsin=>CAST(:aciqlama_deyisdirilsin AS boolean),

                    p_yenileyen_personal_id=>CAST(:yenileyen_personal_id AS bigint)
                )
                """, v);
    }

    public Map<String, Object> deleteMaterial(Long qaimeMaterialId,
                                              Long warehouse,
                                              Long personal) {
        return jdbc.queryForMap("""
                SELECT *
                FROM public.fn_anbar_qaime_material_sil(
                    p_qaime_material_id=>:qaimeMaterialId,
                    p_anbar_id=>:warehouse,
                    p_yenileyen_personal_id=>:personal
                )
                """,
                new MapSqlParameterSource()
                        .addValue("qaimeMaterialId", qaimeMaterialId)
                        .addValue("warehouse", warehouse)
                        .addValue("personal", personal));
    }

    private Map<String, Object> query(String sql, Map<String, Object> values) {
        return jdbc.queryForMap(sql, params(values));
    }

    private MapSqlParameterSource params(Map<String, Object> values) {
        var p = new MapSqlParameterSource();

        values.forEach(p::addValue);

        for (String key : List.of(
                "firma_id",
                "teslim_alan_personal_id",
                "teslim_eden",
                "sened_novu",
                "sened_tarixi",
                "sened_no",
                "aciqlama",
                "son_istifade_tarixi",
                "seriya_no"
        )) {
            if (!p.hasValue(key))
                p.addValue(key, null);
        }

        return p;
    }

    private List<Map<String, Object>> rows(String sql, MapSqlParameterSource p) {
        return jdbc.queryForList(sql, p).stream().map(row -> {
            Map<String, Object> result = new LinkedHashMap<>(row);

            result.replaceAll((key, value) ->
                    value instanceof java.sql.Date d
                            ? d.toLocalDate().toString()
                            : value instanceof java.sql.Time t
                            ? t.toLocalTime().toString()
                            : value
            );

            return result;
        }).toList();
    }

    private static String blank(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String pattern(String value) {
        String s = blank(value);

        return s == null
                ? null
                : "%" + s
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_") + "%";
    }
}