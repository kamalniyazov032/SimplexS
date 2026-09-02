package az.simplexs.simplexs.repository.xestexidmet;

import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class XesteXidmetRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public XesteXidmetRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> qruplar(Long klinikaId) {
        return jdbc.queryForList("""
            WITH RECURSIVE qruplar AS MATERIALIZED (
                SELECT * FROM public.fn_xidmet_qrupu_siyahisi(CAST(:klinika AS bigint), true)
            ), agac AS (
                SELECT q.*, ARRAY[COALESCE(q.sira_no, 999999)::bigint, q.xidmet_qrupu_id] sira_yolu
                  FROM qruplar q
                 WHERE q.parent_id IS NULL
                    OR NOT EXISTS (SELECT 1 FROM qruplar parent WHERE parent.xidmet_qrupu_id = q.parent_id)
                UNION ALL
                SELECT q.*, a.sira_yolu || ARRAY[COALESCE(q.sira_no, 999999)::bigint, q.xidmet_qrupu_id]
                  FROM qruplar q
                  JOIN agac a ON a.xidmet_qrupu_id = q.parent_id
            )
            SELECT xidmet_qrupu_id id, parent_id, kod, ad, seviye, tam_yol, alt_qrupu_var
              FROM agac
             ORDER BY sira_yolu
            """, p().addValue("klinika", klinikaId));
    }

    public List<Map<String, Object>> xidmetler(Long gelisId, Long qrupId, String query, int offset) {
        return jdbc.queryForList("""
            SELECT xidmet_id id, xidmet_kodu kod, xidmet_adi ad,
                   xidmet_qrupu_id qrup_id, xidmet_qrupu_adi qrup_adi,
                   xidmet_tipi_adi tip_adi, standart_qiymet qiymet
              FROM public.fn_gelis_ucun_xidmet_siyahisi(
                   p_gelis_id => CAST(:gelis AS bigint), p_istek_id => NULL)
             WHERE (CAST(:qrup AS bigint) IS NULL OR xidmet_qrupu_id = CAST(:qrup AS bigint))
               AND (CAST(:q AS text) IS NULL
                    OR regexp_replace(xidmet_adi, '[[:space:]]+', '', 'g')
                       ILIKE '%' || regexp_replace(btrim(CAST(:q AS text)), '[[:space:]]+', '', 'g') || '%'
                    OR xidmet_kodu ILIKE '%' || CAST(:q AS text) || '%')
             ORDER BY xidmet_adi
             LIMIT 101 OFFSET :offset
            """, p().addValue("gelis", gelisId).addValue("qrup", qrupId)
                .addValue("q", blank(query)).addValue("offset", offset));
    }

    public List<Map<String, Object>> paketler(Long klinikaId, String query, int offset) {
        return jdbc.queryForList("""
            SELECT xidmet_id id, xidmet_kodu kod, xidmet_adi ad
              FROM public.fn_xidmet_siyahisi(CAST(:klinika AS bigint), NULL, true, true)
             WHERE COALESCE(paket_xidmet, false)
               AND (CAST(:q AS text) IS NULL
                    OR regexp_replace(xidmet_adi, '[[:space:]]+', '', 'g')
                       ILIKE '%' || regexp_replace(btrim(CAST(:q AS text)), '[[:space:]]+', '', 'g') || '%'
                    OR xidmet_kodu ILIKE '%' || CAST(:q AS text) || '%')
             ORDER BY xidmet_adi LIMIT 101 OFFSET :offset
            """, p().addValue("klinika", klinikaId).addValue("q", blank(query)).addValue("offset", offset));
    }

    public List<Map<String, Object>> paketTerkibi(Long gelisId, Long paketId) {
        return jdbc.queryForList("""
            SELECT s.xidmet_id id, s.xidmet_kodu kod, s.xidmet_adi ad,
                   s.xidmet_qrupu_adi qrup_adi, s.xidmet_tipi_adi tip_adi,
                   0::numeric qiymet, COALESCE(s.miqdar, 1) miqdar
              FROM public.fn_paket_xidmet_siyahisi(CAST(:paket AS bigint), true) s
             ORDER BY s.sira_no NULLS LAST, s.xidmet_adi
            """, p().addValue("gelis", gelisId).addValue("paket", paketId));
    }

    public List<Map<String, Object>> rutinler(Long klinikaId, String query, int offset) {
        return jdbc.queryForList("""
            SELECT rutin_id id, kod, ad, xidmet_sayi
              FROM public.fn_rutin_siyahisi(CAST(:klinika AS bigint), true, CAST(:q AS varchar))
             ORDER BY sira_no NULLS LAST, ad LIMIT 101 OFFSET :offset
            """, p().addValue("klinika", klinikaId).addValue("q", blank(query)).addValue("offset", offset));
    }

    public List<Map<String, Object>> rutinTerkibi(Long gelisId, Long rutinId) {
        return jdbc.queryForList("""
            SELECT s.xidmet_id id, s.xidmet_kodu kod, s.xidmet_adi ad,
                   s.xidmet_qrupu_adi qrup_adi, s.xidmet_tipi_adi tip_adi,
                   COALESCE(s.qiymet, 0) qiymet, 1 miqdar
              FROM public.fn_rutin_xidmet_siyahisi(CAST(:rutin AS bigint), true) s
             ORDER BY s.sira_no NULLS LAST, s.xidmet_adi
            """, p().addValue("gelis", gelisId).addValue("rutin", rutinId));
    }

    public List<Map<String, Object>> sobeler(Long gelisId, Long xidmetId) {
        return jdbc.queryForList("SELECT * FROM public.fn_xidmet_sobeleri_siyahisi(CAST(:gelis AS bigint), CAST(:xidmet AS bigint)) ORDER BY sira_no NULLS LAST, sobe_adi",
            p().addValue("gelis", gelisId).addValue("xidmet", xidmetId));
    }

    public List<Map<String, Object>> sobeHekimleri(Long gelisId, Long xidmetId, Long sobeId) {
        return jdbc.queryForList("SELECT * FROM public.fn_xidmet_sobe_hekimleri_siyahisi(CAST(:gelis AS bigint), CAST(:xidmet AS bigint), CAST(:sobe AS bigint)) ORDER BY hekim_ad, hekim_soyad",
            p().addValue("gelis", gelisId).addValue("xidmet", xidmetId).addValue("sobe", sobeId));
    }

    public List<Map<String, Object>> isteyenHekimler(Long gelisId) {
        return jdbc.queryForList("SELECT * FROM public.fn_gelis_isteyen_hekimleri_siyahisi(CAST(:gelis AS bigint)) ORDER BY hekim_ad, hekim_soyad",
            p().addValue("gelis", gelisId));
    }

    public Map<String, Object> gonderenHekim(Long gelisId) {
        var rows = jdbc.queryForList("SELECT * FROM public.fn_gelis_gonderen_hekim_getir(CAST(:gelis AS bigint))",
            p().addValue("gelis", gelisId));
        return rows.isEmpty() ? Map.of() : rows.getFirst();
    }

    public List<Map<String, Object>> secilmisXidmetler(Long gelisId) {
        return jdbc.queryForList("SELECT * FROM public.fn_xeste_xidmetleri_siyahisi(CAST(:gelis AS bigint), NULL, NULL, true) ORDER BY yaranma_tarixi DESC",
            p().addValue("gelis", gelisId));
    }

    public Map<String, Object> yarat(Long gelisId, String xidmetler, String aciqlama, Long personalId) {
        var rows = jdbc.queryForList("""
            SELECT * FROM public.fn_xeste_xidmetlerini_yarat(
                p_gelis_id => CAST(:gelis AS bigint), p_xidmetler => CAST(:xidmetler AS jsonb),
                p_istek_id => NULL, p_istek_aciqlama => CAST(:aciqlama AS text),
                p_yaradan_personal_id => CAST(:personal AS bigint))
            """, p().addValue("gelis", gelisId).addValue("xidmetler", xidmetler)
                .addValue("aciqlama", blank(aciqlama)).addValue("personal", personalId));
        return rows.isEmpty() ? Map.of() : rows.getFirst();
    }

    private static MapSqlParameterSource p() { return new MapSqlParameterSource(); }
    private static String blank(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
