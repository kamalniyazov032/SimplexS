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

    public List<Map<String, Object>> xidmetler(Long gelisId, Long qrupId, String query, int offset, Long istekId) {
        return jdbc.queryForList("""
                SELECT xidmet_id id, xidmet_kodu kod, xidmet_adi ad,
                       xidmet_qrupu_id qrup_id, xidmet_qrupu_adi qrup_adi,
                       xidmet_tipi_adi tip_adi, standart_qiymet qiymet, istekde_var, istekde_say
                  FROM public.fn_gelis_ucun_xidmet_siyahisi(
                       p_gelis_id => CAST(:gelis AS bigint), p_istek_id => CAST(:istek AS bigint))
                 WHERE (CAST(:qrup AS bigint) IS NULL OR xidmet_qrupu_id = CAST(:qrup AS bigint))
                   AND (CAST(:q AS text) IS NULL
                        OR regexp_replace(xidmet_adi, '[[:space:]]+', '', 'g')
                           ILIKE '%' || regexp_replace(btrim(CAST(:q AS text)), '[[:space:]]+', '', 'g') || '%'
                        OR xidmet_kodu ILIKE '%' || CAST(:q AS text) || '%')
                 ORDER BY xidmet_adi
                 LIMIT 101 OFFSET :offset
                """, p().addValue("gelis", gelisId).addValue("qrup", qrupId).addValue("istek", istekId)
                .addValue("q", blank(query)).addValue("offset", offset));
    }

    public List<Map<String, Object>> paketler(Long gelisId, String query, int offset) {
        return jdbc.queryForList("""
                SELECT paket_xidmet_id id, paket_kodu kod, paket_adi ad, standart_qiymet qiymet
                  FROM public.fn_gelis_ucun_paket_siyahisi(p_gelis_id => CAST(:gelis AS bigint))
                 WHERE (CAST(:q AS text) IS NULL
                        OR regexp_replace(paket_adi, '[[:space:]]+', '', 'g')
                           ILIKE '%' || regexp_replace(btrim(CAST(:q AS text)), '[[:space:]]+', '', 'g') || '%'
                        OR paket_kodu ILIKE '%' || CAST(:q AS text) || '%')
                 ORDER BY paket_adi LIMIT 101 OFFSET :offset
                """, p().addValue("gelis", gelisId).addValue("q", blank(query)).addValue("offset", offset));
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

    public List<Map<String, Object>> rutinTerkibi(Long gelisId, Long rutinId, java.time.LocalDate tarix) {
        return jdbc.queryForList("""
                SELECT s.xidmet_id id, s.xidmet_kodu kod, s.xidmet_adi ad,
                       s.xidmet_qrupu_adi qrup_adi, s.xidmet_tipi_adi tip_adi,
                       s.effektiv_qiymet qiymet, s.rutin_id, 1 miqdar
                  FROM public.fn_gelis_rutin_xidmet_siyahisi(p_gelis_id => CAST(:gelis AS bigint), p_rutin_id => CAST(:rutin AS bigint), p_xidmet_tarixi => CAST(:tarix AS date)) s
                 ORDER BY s.sira_no NULLS LAST, s.xidmet_adi
                """, p().addValue("gelis", gelisId).addValue("rutin", rutinId).addValue("tarix", tarix));
    }

    public List<Map<String, Object>> sobeler(Long gelisId, Long xidmetId) {
        return jdbc.queryForList("SELECT * FROM public.fn_xidmet_sobeleri_siyahisi(CAST(:gelis AS bigint), CAST(:xidmet AS bigint)) ORDER BY sira_no NULLS LAST, sobe_adi",
                p().addValue("gelis", gelisId).addValue("xidmet", xidmetId));
    }

    public List<Map<String, Object>> sobeHekimleri(Long gelisId, Long xidmetId, Long sobeId, java.time.LocalDate tarix) {
        // The legacy department-doctor wrapper omits the now-required pricing date.
        // Keep its department/clinic eligibility checks and call the current pricing function directly.
        return jdbc.queryForList("""
                SELECT DISTINCT p.id hekim_id, p.kod hekim_kodu, p.ad hekim_ad,
                       p.soyad hekim_soyad, p.ata_adi hekim_ata_adi,
                       q.standart_qiymet, q.hekim_qiymeti, q.yekun_qiymet,
                       q.ferdi_hekim_qiymeti_var ferdi_qiymet_var
                  FROM public.fn_xidmet_sobeleri_siyahisi(
                       CAST(:gelis AS bigint), CAST(:xidmet AS bigint)) s
                  JOIN public.rn_xeste_gelisleri g ON g.id = :gelis AND g.aktiv
                  JOIN public.rn_personal_sobeler ps ON ps.sobe_id = s.sobe_id
                       AND ps.klinika_id = g.klinika_id AND ps.aktiv AND ps.islesin
                  JOIN public.rn_personallar p ON p.id = ps.personal_id
                       AND p.aktiv AND p.hekimdir AND p.isden_ayrilib = false
                  JOIN public.rn_personal_klinikalar pk ON pk.personal_id = p.id
                       AND pk.klinika_id = g.klinika_id AND pk.aktiv
                 CROSS JOIN LATERAL public.fn_hpl_gelis_xidmet_qiymetini_hesabla(
                       p_gelis_id => CAST(:gelis AS bigint), p_xidmet_id => CAST(:xidmet AS bigint),
                       p_icra_eden_hekim_id => p.id, p_xidmet_tarixi => CAST(:tarix AS date)) q
                 WHERE s.sobe_id = :sobe AND COALESCE(s.hekim_secim_qaydasi_kodu, '') <> 'SECILMIR'
                 ORDER BY hekim_ad, hekim_soyad, hekim_id
                """, p().addValue("gelis", gelisId).addValue("xidmet", xidmetId)
                .addValue("sobe", sobeId).addValue("tarix", tarix));
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
        return jdbc.queryForList("SELECT * FROM public.fn_xeste_xidmetleri_siyahisi(p_gelis_id => CAST(:gelis AS bigint), p_aktiv => NULL) ORDER BY yaranma_tarixi DESC",
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


    public List<Map<String, Object>> redakte(Long gelisId, Long istekId) {
        return jdbc.queryForList("SELECT * FROM public.fn_xeste_xidmet_isteyi_redakte_siyahisi(p_gelis_id => CAST(:gelis AS bigint), p_istek_id => CAST(:istek AS bigint))", p().addValue("gelis", gelisId).addValue("istek", istekId));
    }

    public Map<String, Object> yenile(Long gelisId, Long istekId, String xidmetler, Long personalId) {
        return jdbc.queryForMap("SELECT * FROM public.fn_xeste_xidmet_isteyini_yenile(p_gelis_id => CAST(:gelis AS bigint), p_istek_id => CAST(:istek AS bigint), p_xidmetler => CAST(:xidmetler AS jsonb), p_yenileyen_personal_id => CAST(:personal AS bigint))", p().addValue("gelis", gelisId).addValue("istek", istekId).addValue("xidmetler", xidmetler).addValue("personal", personalId));
    }

    public Map<String, Object> legv(Long xidmetId, Long personalId) {
        return jdbc.queryForMap("SELECT * FROM public.fn_xeste_xidmetini_legv_et(p_xeste_xidmet_id => CAST(:id AS bigint), p_legv_eden_personal_id => CAST(:personal AS bigint))", p().addValue("id", xidmetId).addValue("personal", personalId));
    }

    public Map<String, Object> hazirla(
            Long gelisId,
            Map<String, Object> row,
            Long istekId,
            Long personalId) {

        String sql = """
                SELECT *
                FROM public.fn_xeste_xidmetini_hazirla(
                    p_gelis_id              => CAST(:gelis AS bigint),
                    p_xidmet_id             => CAST(:xidmet_id AS bigint),
                    p_sobe_id               => CAST(:sobe_id AS bigint),
                    p_xidmet_tarixi         => CAST(:xidmet_tarixi AS date),
                    p_rutin_id              => CAST(:rutin_id AS bigint),
                    p_gonderen_hekim_id     => CAST(:gonderen_hekim_id AS bigint),
                    p_isteyen_hekim_id      => CAST(:isteyen_hekim_id AS bigint),
                    p_icra_eden_hekim_id    => CAST(:icra_eden_hekim_id AS bigint),
                    p_miqdar                => CAST(:miqdar AS numeric),
                    p_tecili                => CAST(:tecili AS boolean),
                    p_istek_id              => CAST(:istek AS bigint),
                    p_emel_eden_personal_id => CAST(:personal AS bigint)
                )
                """;

        return jdbc.queryForMap(
                sql,
                new MapSqlParameterSource(row)
                        .addValue("gelis", gelisId)
                        .addValue("istek", istekId)
                        .addValue("personal", personalId)
        );
    }

    private static MapSqlParameterSource p() {
        return new MapSqlParameterSource();
    }

    private static String blank(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
