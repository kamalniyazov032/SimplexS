package az.simplexs.simplexs.repository.qiymet;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import az.simplexs.simplexs.dto.qiymet.HekimXidmetQiymeti;
import az.simplexs.simplexs.dto.qiymet.QiymetBasligi;
import az.simplexs.simplexs.dto.qiymet.QiymetCedveli;
import az.simplexs.simplexs.dto.qiymet.QiymetQrupu;
import az.simplexs.simplexs.dto.qiymet.QiymetXidmeti;

@Repository
public class QiymetRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public QiymetRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<QiymetBasligi> basliqlar(Long klinikaId, Boolean aktiv) {
        return jdbc.query("""
                SELECT qiymet_basligi_id, qiymet_basligi_adi, aciqlama, sira_no
                FROM public.fn_qiymet_basligi_siyahisi(p_klinika_id=>CAST(:klinika AS bigint))
                ORDER BY sira_no NULLS LAST, qiymet_basligi_adi
                """, new MapSqlParameterSource("klinika", klinikaId),
                (r, n) -> new QiymetBasligi(l(r, "qiymet_basligi_id"), r.getString("qiymet_basligi_adi"),
                        r.getString("aciqlama"), i(r, "sira_no"), true));
    }

    public List<QiymetQrupu> qruplar(Long klinikaId, Long basliqId, Boolean aktiv) {
        return jdbc.query("""
                SELECT * FROM public.fn_qiymet_qrupu_siyahisi(
                    p_klinika_id=>CAST(:klinika AS bigint),
                    p_qiymet_basligi_id=>CAST(:basliq AS bigint),
                    p_aktiv=>CAST(:aktiv AS boolean))
                ORDER BY sira_no NULLS LAST, qiymet_qrupu_adi
                """, new MapSqlParameterSource("klinika", klinikaId).addValue("basliq", basliqId)
                        .addValue("aktiv", aktiv), (r, n) -> new QiymetQrupu(
                        l(r, "qiymet_qrupu_id"), l(r, "klinika_id"), l(r, "qiymet_basligi_id"),
                        r.getString("qiymet_basligi_adi"), r.getString("qiymet_qrupu_adi"),
                        r.getString("aciqlama"), r.getObject("standartdir", Boolean.class), i(r, "sira_no"),
                        r.getObject("aktiv", Boolean.class), r.getObject("yaranma_tarixi", java.time.LocalDateTime.class),
                        l(r, "yaradan_personal_id"), r.getObject("yenilenme_tarixi", java.time.LocalDateTime.class),
                        l(r, "yenileyen_personal_id")));
    }

    public QiymetQrupu qrup(Long klinikaId, Long qrupId) {
        return qruplar(klinikaId, null, null).stream().filter(q -> q.id().equals(qrupId)).findFirst().orElseThrow();
    }

    public List<QiymetCedveli> cedveller(Long klinikaId, Long qrupId, Boolean aktiv, Boolean tarixdeAktivdir) {
        return jdbc.query("""
                SELECT * FROM public.fn_qiymet_cedveli_siyahisi(
                    p_klinika_id=>CAST(:klinika AS bigint), p_qiymet_qrupu_id=>CAST(:qrup AS bigint),
                    p_aktiv=>CAST(:aktiv AS boolean), p_tarixde_aktivdir=>CAST(:tarix AS boolean))
                ORDER BY baslama_tarixi DESC, qiymet_cedveli_id DESC
                """, new MapSqlParameterSource("klinika", klinikaId).addValue("qrup", qrupId)
                        .addValue("aktiv", aktiv).addValue("tarix", tarixdeAktivdir),
                (r, n) -> new QiymetCedveli(l(r, "qiymet_cedveli_id"), l(r, "klinika_id"),
                        l(r, "qiymet_basligi_id"), r.getString("qiymet_basligi_adi"),
                        l(r, "qiymet_qrupu_id"), r.getString("qiymet_qrupu_adi"),
                        r.getObject("baslama_tarixi", LocalDate.class), r.getObject("bitme_tarixi", LocalDate.class),
                        r.getObject("tarixde_aktivdir", Boolean.class), r.getBigDecimal("xeste_payi"),
                        r.getBigDecimal("sigorta_payi"), r.getBigDecimal("xeste_endirim"),
                        r.getBigDecimal("sigorta_endirim"), r.getObject("aktiv", Boolean.class),
                        r.getObject("yaranma_tarixi", java.time.LocalDateTime.class), l(r, "yaradan_personal_id"),
                        r.getObject("yenilenme_tarixi", java.time.LocalDateTime.class), l(r, "yenileyen_personal_id")));
    }

    public QiymetCedveli cedvel(Long klinikaId, Long cedvelId) {
        return cedveller(klinikaId, null, null, null).stream().filter(c -> c.id().equals(cedvelId))
                .findFirst().orElseThrow();
    }

    public List<QiymetXidmeti> xidmetler(Long cedvelId, Long xidmetQrupuId, String query, String status,
            String hekimQiymetStatus, Long hekimPersonalId, int limit, int offset) {
        String sql = """
                SELECT f.*,
                    COALESCE(f.xeste_payi, qc.xeste_payi) AS effektiv_xeste_pay,
                    COALESCE(f.sigorta_payi, qc.sigorta_payi) AS effektiv_qurum_payi,
                    COALESCE(f.xeste_endirim, qc.xeste_endirim) AS effektiv_xeste_endirim,
                    COALESCE(f.sigorta_endirim, qc.sigorta_endirim) AS effektiv_qurum_endirim
                FROM public.fn_qiymet_cedveli_xidmet_siyahisi(
                    p_qiymet_cedveli_id=>CAST(:cedvel AS bigint),
                    p_xidmet_qrupu_id=>CAST(:xidmetQrupu AS bigint),
                    p_alt_qruplar_daxil=>true, p_aktiv=>true) f
                JOIN public.rn_qiymet_cedveli qc ON qc.id=CAST(:cedvel AS bigint)
                WHERE (CAST(:axtar AS text) IS NULL OR f.xidmet_adi ILIKE '%%'||CAST(:axtar AS text)||'%%'
                    OR f.xidmet_kodu ILIKE '%%'||CAST(:axtar AS text)||'%%')
                  AND (CAST(:status AS text) IS NULL
                    OR (CAST(:status AS text)='teyin' AND f.qiymet_teyin_edilib=true)
                    OR (CAST(:status AS text)='teyin_deyil' AND f.qiymet_teyin_edilib=false))
                  AND (CAST(:hekim AS bigint) IS NULL OR EXISTS (
                    SELECT 1 FROM public.rn_hekim_xidmet_qiymetleri hxq
                    WHERE hxq.qiymet_cedveli_id=CAST(:cedvel AS bigint)
                      AND hxq.xidmet_id=f.xidmet_id AND hxq.hekim_personal_id=CAST(:hekim AS bigint)
                      AND hxq.aktiv=true))
                  AND (CAST(:hekimStatus AS text) IS NULL
                    OR (CAST(:hekimStatus AS text)='var' AND EXISTS (
                      SELECT 1 FROM public.rn_hekim_xidmet_qiymetleri hxq
                      WHERE hxq.qiymet_cedveli_id=CAST(:cedvel AS bigint)
                        AND hxq.xidmet_id=f.xidmet_id AND hxq.aktiv=true)))
                ORDER BY f.xidmet_qrupu_adi, f.xidmet_sira_no NULLS LAST, f.xidmet_adi
                LIMIT :limit OFFSET :offset
                """;
        return jdbc.query(sql, filterParams(cedvelId, xidmetQrupuId, query, status, hekimQiymetStatus, hekimPersonalId)
                .addValue("limit", limit).addValue("offset", offset), (r, n) -> mapXidmet(r));
    }

    public long xidmetSayi(Long cedvelId, Long xidmetQrupuId, String query, String status,
            String hekimQiymetStatus, Long hekimPersonalId) {
        return jdbc.queryForObject("""
                SELECT count(*) FROM public.fn_qiymet_cedveli_xidmet_siyahisi(
                    p_qiymet_cedveli_id=>CAST(:cedvel AS bigint),
                    p_xidmet_qrupu_id=>CAST(:xidmetQrupu AS bigint),
                    p_alt_qruplar_daxil=>true, p_aktiv=>true) f
                WHERE (CAST(:axtar AS text) IS NULL OR f.xidmet_adi ILIKE '%%'||CAST(:axtar AS text)||'%%'
                    OR f.xidmet_kodu ILIKE '%%'||CAST(:axtar AS text)||'%%')
                  AND (CAST(:status AS text) IS NULL
                    OR (CAST(:status AS text)='teyin' AND f.qiymet_teyin_edilib=true)
                    OR (CAST(:status AS text)='teyin_deyil' AND f.qiymet_teyin_edilib=false))
                  AND (CAST(:hekim AS bigint) IS NULL OR EXISTS (
                    SELECT 1 FROM public.rn_hekim_xidmet_qiymetleri hxq
                    WHERE hxq.qiymet_cedveli_id=CAST(:cedvel AS bigint)
                      AND hxq.xidmet_id=f.xidmet_id AND hxq.hekim_personal_id=CAST(:hekim AS bigint)
                      AND hxq.aktiv=true))
                  AND (CAST(:hekimStatus AS text) IS NULL
                    OR (CAST(:hekimStatus AS text)='var' AND EXISTS (
                      SELECT 1 FROM public.rn_hekim_xidmet_qiymetleri hxq
                      WHERE hxq.qiymet_cedveli_id=CAST(:cedvel AS bigint)
                        AND hxq.xidmet_id=f.xidmet_id AND hxq.aktiv=true)))
                """, filterParams(cedvelId, xidmetQrupuId, query, status, hekimQiymetStatus, hekimPersonalId), Long.class);
    }

    public Map<String, Object> basliqYarat(Long klinikaId, String ad, String aciqlama, Long personalId) {
        return one("SELECT * FROM public.fn_qiymet_basligi_yarat(p_klinika_id=>:klinika,p_ad=>:ad,p_aciqlama=>:aciq,p_yaradan_personal_id=>:personal)",
                new MapSqlParameterSource("klinika", klinikaId).addValue("ad", ad.trim())
                        .addValue("aciq", blank(aciqlama)).addValue("personal", personalId));
    }

    public Map<String, Object> basliqYenile(Long id, String ad, String aciqlama, boolean aktiv, Long personalId) {
        return one("SELECT * FROM public.fn_qiymet_basligi_yenile(p_qiymet_basligi_id=>:id,p_ad=>:ad,p_aciqlama=>:aciq,p_aciqlama_deyisdirilsin=>true,p_aktiv=>:aktiv,p_yenileyen_personal_id=>:personal)",
                new MapSqlParameterSource("id", id).addValue("ad", ad.trim()).addValue("aciq", blank(aciqlama))
                        .addValue("aktiv", aktiv).addValue("personal", personalId));
    }

    public Map<String, Object> qrupYarat(Long klinikaId, Long basliqId, String ad, String aciqlama,
            boolean standartdir, List<Long> teskilatIds, Long personalId) {
        return one("SELECT * FROM public.fn_qiymet_qrupu_yarat(p_klinika_id=>:klinika,p_qiymet_basligi_id=>:basliq,p_ad=>:ad,p_aciqlama=>:aciq,p_standartdir=>:standart,p_teskilat_idleri=>CAST(:teskilatlar AS bigint[]),p_yaradan_personal_id=>:personal)",
                new MapSqlParameterSource("klinika", klinikaId).addValue("basliq", basliqId)
                        .addValue("ad", ad.trim()).addValue("aciq", blank(aciqlama))
                        .addValue("standart", standartdir).addValue("teskilatlar", arrayLiteral(teskilatIds))
                        .addValue("personal", personalId));
    }

    public Map<String, Object> qrupYenile(Long id, String ad, String aciqlama, boolean standartdir,
            boolean aktiv, List<Long> teskilatIds, Long personalId) {
        return one("SELECT * FROM public.fn_qiymet_qrupu_yenile(p_qiymet_qrupu_id=>:id,p_ad=>:ad,p_aciqlama=>:aciq,p_aciqlama_deyisdirilsin=>true,p_standartdir=>:standart,p_standartdir_deyisdirilsin=>true,p_aktiv=>:aktiv,p_teskilat_idleri=>CAST(:teskilatlar AS bigint[]),p_teskilatlar_deyisdirilsin=>true,p_yenileyen_personal_id=>:personal)",
                new MapSqlParameterSource("id", id).addValue("ad", ad.trim()).addValue("aciq", blank(aciqlama))
                        .addValue("standart", standartdir).addValue("aktiv", aktiv)
                        .addValue("teskilatlar", arrayLiteral(teskilatIds)).addValue("personal", personalId));
    }

    public Map<String, Object> cedvelYarat(Long klinikaId, Long qrupId, LocalDate baslamaTarixi,
            LocalDate bitmeTarixi, BigDecimal xestePayi, BigDecimal xesteEndirim,
            BigDecimal sigortaEndirim, Long personalId) {
        return one("SELECT * FROM public.fn_qiymet_cedveli_yarat(p_klinika_id=>:klinika,p_qiymet_qrupu_id=>:qrup,p_baslama_tarixi=>:bas,p_bitme_tarixi=>:bit,p_xeste_payi=>:xp,p_xeste_endirim=>:xe,p_sigorta_endirim=>:se,p_yaradan_personal_id=>:personal)",
                new MapSqlParameterSource("klinika", klinikaId).addValue("qrup", qrupId)
                        .addValue("bas", baslamaTarixi).addValue("bit", bitmeTarixi)
                        .addValue("xp", xestePayi).addValue("xe", xesteEndirim)
                        .addValue("se", sigortaEndirim).addValue("personal", personalId));
    }

    @Transactional
    public Map<String, Object> cedvelYaratVeKlonla(Long klinikaId, Long qrupId, LocalDate baslamaTarixi,
            LocalDate bitmeTarixi, BigDecimal xestePayi, BigDecimal xesteEndirim,
            BigDecimal sigortaEndirim, Long menbeCedvelId, Long personalId) {
        if (menbeCedvelId != null) {
            Boolean menbeUygundur = jdbc.queryForObject("""
                    SELECT EXISTS(SELECT 1 FROM public.rn_qiymet_cedveli
                        WHERE id=CAST(:menbe AS bigint) AND klinika_id=CAST(:klinika AS bigint))
                    """, new MapSqlParameterSource("menbe", menbeCedvelId).addValue("klinika", klinikaId),
                    Boolean.class);
            if (!Boolean.TRUE.equals(menbeUygundur)) {
                return Map.of("status_kodu", "MENBE_CEDVEL_UYGUN_DEYIL",
                        "mesaj", "Klonlanacaq qiymət cədvəli seçilmiş klinikaya aid deyil");
            }
        }

        Map<String, Object> yaradildi = cedvelYarat(klinikaId, qrupId, baslamaTarixi, bitmeTarixi,
                xestePayi, xesteEndirim, sigortaEndirim, personalId);
        if (!successful(yaradildi) || menbeCedvelId == null) return yaradildi;

        Object idValue = yaradildi.get("qiymet_cedveli_id");
        if (!(idValue instanceof Number number)) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return Map.of("status_kodu", "SISTEM_XETASI", "mesaj", "Yeni qiymət cədvəlinin ID-si alınmadı");
        }
        String qiymetlerJson = jdbc.queryForObject("""
                SELECT COALESCE(jsonb_agg(jsonb_build_object(
                    'xidmet_id', xidmet_id, 'qiymet', qiymet,
                    'edv_aktivdir', COALESCE(edv_aktivdir,false))), '[]'::jsonb)::text
                FROM public.fn_xidmet_qiymet_siyahisi(CAST(:menbe AS bigint),CAST(NULL AS boolean))
                WHERE qiymet IS NOT NULL
                """, new MapSqlParameterSource("menbe", menbeCedvelId), String.class);
        if (qiymetlerJson == null || "[]".equals(qiymetlerJson)) return yaradildi;

        Map<String, Object> klonlandi = qiymetleriSaxla(number.longValue(), qiymetlerJson, personalId);
        if (!successful(klonlandi)) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return klonlandi;
        }
        return Map.of("status_kodu", "UGURLU", "qiymet_cedveli_id", number.longValue(),
                "mesaj", "Qiymət cədvəli yaradıldı və xidmət qiymətləri klonlandı");
    }

    public Map<String, Object> cedvelYenile(Long id, LocalDate baslamaTarixi, LocalDate bitmeTarixi,
            BigDecimal xestePayi, BigDecimal xesteEndirim, BigDecimal sigortaEndirim,
            boolean aktiv, Long personalId) {
        return one("SELECT * FROM public.fn_qiymet_cedveli_yenile(p_qiymet_cedveli_id=>:id,p_baslama_tarixi=>:bas,p_bitme_tarixi=>:bit,p_xeste_payi=>:xp,p_xeste_endirim=>:xe,p_sigorta_endirim=>:se,p_aktiv=>:aktiv,p_yenileyen_personal_id=>:personal)",
                new MapSqlParameterSource("id", id).addValue("bas", baslamaTarixi).addValue("bit", bitmeTarixi)
                        .addValue("xp", xestePayi).addValue("xe", xesteEndirim)
                        .addValue("se", sigortaEndirim).addValue("aktiv", aktiv).addValue("personal", personalId));
    }

    public Map<String, Object> qiymetleriSaxla(Long cedvelId, String json, Long personalId) {
        return one("SELECT * FROM public.fn_xidmet_qiymetlerini_yadda_saxla(p_qiymet_cedveli_id=>:cedvel,p_qiymetler=>CAST(:json AS jsonb),p_emel_eden_personal_id=>:personal)",
                new MapSqlParameterSource("cedvel", cedvelId).addValue("json", json).addValue("personal", personalId));
    }

    public Map<String, Object> qiymetleriTopluYenile(Long cedvelId, List<Long> xidmetIds,
            BigDecimal xestePayi, BigDecimal sigortaPayi, BigDecimal xesteEndirim,
            BigDecimal sigortaEndirim, Long personalId) {
        return one("SELECT * FROM public.fn_xidmet_qiymetlerini_toplu_yenile(p_qiymet_cedveli_id=>:cedvel,p_xidmet_idleri=>CAST(:xidmetler AS bigint[]),p_xeste_payi=>:xp,p_sigorta_payi=>:sp,p_xeste_endirim=>:xe,p_sigorta_endirim=>:se,p_yenileyen_personal_id=>:personal)",
                new MapSqlParameterSource("cedvel", cedvelId).addValue("xidmetler", arrayLiteral(xidmetIds))
                        .addValue("xp", xestePayi).addValue("sp", sigortaPayi).addValue("xe", xesteEndirim)
                        .addValue("se", sigortaEndirim).addValue("personal", personalId));
    }

    public Map<String, Object> hekimQiymetiSaxla(Long klinikaId, Long cedvelId, Long xidmetId,
            Long hekimId, BigDecimal qiymet, boolean aktiv, Long personalId) {
        return one("SELECT * FROM public.fn_hekim_xidmet_qiymeti_yadda_saxla(p_klinika_id=>:klinika,p_qiymet_cedveli_id=>:cedvel,p_xidmet_id=>:xidmet,p_hekim_personal_id=>:hekim,p_qiymet=>:qiymet,p_aktiv=>:aktiv,p_emel_eden_personal_id=>:personal)",
                new MapSqlParameterSource("klinika", klinikaId).addValue("cedvel", cedvelId)
                        .addValue("xidmet", xidmetId).addValue("hekim", hekimId).addValue("qiymet", qiymet)
                        .addValue("aktiv", aktiv).addValue("personal", personalId));
    }

    @Transactional
    public Map<String, Object> hekimQiymetleriniTopluSaxla(Long klinikaId, Long cedvelId, Long xidmetId,
            List<Long> hekimIds, BigDecimal qiymet, boolean aktiv, Long personalId) {
        if (hekimIds == null || hekimIds.isEmpty()) {
            return Map.of("status_kodu", "HEKIM_SECILMEYIB", "mesaj", "Ən azı bir həkim seçilməlidir");
        }
        Map<String, Object> last = Map.of();
        for (Long hekimId : hekimIds.stream().distinct().toList()) {
            last = hekimQiymetiSaxla(klinikaId, cedvelId, xidmetId, hekimId, qiymet, aktiv, personalId);
            String status = String.valueOf(last.getOrDefault("status_kodu", ""));
            if (!status.toUpperCase().contains("UGUR") && !"1".equals(status)) {
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                return last;
            }
        }
        return last;
    }

    public List<HekimXidmetQiymeti> hekimQiymetleri(Long cedvelId, Long xidmetId, Boolean aktiv) {
        return jdbc.query("""
                SELECT hxq.id hekim_xidmet_qiymeti_id,hxq.qiymet_cedveli_id,hxq.xidmet_id,
                       x.kod xidmet_kodu,x.ad xidmet_adi,p.id hekim_personal_id,p.kod hekim_kodu,
                       concat_ws(' ',p.ad,p.soyad)::varchar hekim_ad_soyad,xqi.qiymet umumi_qiymet,
                       hxq.qiymet hekim_qiymeti,hxq.aktiv,hxq.yaranma_tarixi,hxq.yaradan_personal_id,
                       hxq.yenilenme_tarixi,hxq.yenileyen_personal_id
                FROM public.rn_hekim_xidmet_qiymetleri hxq
                JOIN public.rn_xidmetler x ON x.id=hxq.xidmet_id
                JOIN public.rn_personallar p ON p.id=hxq.hekim_personal_id
                LEFT JOIN public.rn_xidmet_qiymetleri xqi
                  ON xqi.qiymet_cedveli_id=hxq.qiymet_cedveli_id AND xqi.xidmet_id=hxq.xidmet_id
                WHERE hxq.qiymet_cedveli_id=CAST(:cedvel AS bigint)
                  AND (CAST(:xidmet AS bigint) IS NULL OR hxq.xidmet_id=CAST(:xidmet AS bigint))
                  AND (CAST(:aktiv AS boolean) IS NULL OR hxq.aktiv=CAST(:aktiv AS boolean))
                ORDER BY x.ad,p.ad,p.soyad,p.kod
                """,
                new MapSqlParameterSource("cedvel", cedvelId).addValue("xidmet", xidmetId).addValue("aktiv", aktiv),
                (r, n) -> new HekimXidmetQiymeti(l(r, "hekim_xidmet_qiymeti_id"), l(r, "qiymet_cedveli_id"),
                        l(r, "xidmet_id"), r.getString("xidmet_kodu"), r.getString("xidmet_adi"),
                        l(r, "hekim_personal_id"), r.getString("hekim_kodu"), r.getString("hekim_ad_soyad"),
                        r.getBigDecimal("umumi_qiymet"), r.getBigDecimal("hekim_qiymeti"),
                        r.getObject("aktiv", Boolean.class), r.getObject("yaranma_tarixi", java.time.LocalDateTime.class),
                        l(r, "yaradan_personal_id"), r.getObject("yenilenme_tarixi", java.time.LocalDateTime.class),
                        l(r, "yenileyen_personal_id")));
    }

    public Map<Long, List<Long>> qruplarinTeskilatIdleri(List<Long> qrupIds) {
        Map<Long, List<Long>> result = new LinkedHashMap<>();
        if (qrupIds == null) return result;
        for (Long id : qrupIds) {
            result.put(id, jdbc.query("SELECT teskilat_id FROM public.fn_qiymet_qrupu_teskilat_siyahisi(p_qiymet_qrupu_id=>CAST(:id AS bigint),p_aktiv=>true)",
                    new MapSqlParameterSource("id", id), (r, n) -> r.getLong("teskilat_id")));
        }
        return result;
    }

    private MapSqlParameterSource filterParams(Long cedvel, Long qrup, String axtar, String status,
            String hekimQiymetStatus, Long hekimPersonalId) {
        return new MapSqlParameterSource("cedvel", cedvel).addValue("xidmetQrupu", qrup)
                .addValue("axtar", blank(axtar)).addValue("status", blank(status))
                .addValue("hekimStatus", blank(hekimQiymetStatus)).addValue("hekim", hekimPersonalId);
    }

    private QiymetXidmeti mapXidmet(ResultSet r) throws SQLException {
        return new QiymetXidmeti(l(r, "xidmet_id"), r.getString("xidmet_kodu"), r.getString("xidmet_adi"),
                l(r, "xidmet_qrupu_id"), r.getString("xidmet_qrupu_kodu"), r.getString("xidmet_qrupu_adi"),
                l(r, "xidmet_tipi_id"), r.getString("xidmet_tipi_kodu"), r.getString("xidmet_tipi_adi"),
                l(r, "xidmet_qiymeti_id"), r.getBigDecimal("qiymet"), r.getBigDecimal("effektiv_xeste_pay"),
                r.getBigDecimal("effektiv_qurum_payi"), r.getBigDecimal("effektiv_xeste_endirim"), r.getBigDecimal("effektiv_qurum_endirim"),
                r.getObject("edv_aktivdir", Boolean.class), r.getObject("qiymet_aktiv", Boolean.class),
                r.getObject("qiymet_teyin_edilib", Boolean.class), i(r, "xidmet_sira_no"));
    }

    private Map<String, Object> one(String sql, MapSqlParameterSource params) {
        List<Map<String, Object>> rows = jdbc.queryForList(sql, params);
        return rows.isEmpty() ? Map.of() : rows.getFirst();
    }

    private String arrayLiteral(List<Long> ids) {
        return ids == null || ids.isEmpty() ? "{}"
                : ids.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(",", "{", "}"));
    }

    private static String blank(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static boolean successful(Map<String, Object> result) {
        String status = String.valueOf(result.getOrDefault("status_kodu", ""));
        return status.toUpperCase().contains("UGUR") || "1".equals(status);
    }

    private static Long l(ResultSet r, String column) throws SQLException {
        Object value = r.getObject(column);
        return value instanceof Number n ? n.longValue() : null;
    }

    private static Integer i(ResultSet r, String column) throws SQLException {
        Object value = r.getObject(column);
        return value instanceof Number n ? n.intValue() : null;
    }
}
