package az.simplexs.simplexs.repository.sobe;

import java.util.List;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import az.simplexs.simplexs.dto.sobe.PersonalOption;
import az.simplexs.simplexs.dto.sobe.SobeListItem;
import az.simplexs.simplexs.dto.sobe.SobeOperationResult;
import az.simplexs.simplexs.dto.sobe.SobeOption;

@Repository
public class SobeRepository {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public SobeRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<SobeListItem> findByKlinikaId(Long klinikaId) {
        if (klinikaId == null) return List.of();
        String sql = """
            SELECT sobe_id, klinika_id, klinika_adi, sobe_adi, sira_no,
                   sobe_tipi_id, sobe_tipi_kodu, sobe_tipi_adi,
                   hekim_secim_qaydasi_id, hekim_secim_qaydasi_kodu, hekim_secim_qaydasi_adi,
                   sobe_mudiri_personal_id, sobe_mudiri_kodu, sobe_mudiri_adi,
                   boyuk_tibb_bacisi_personal_id, boyuk_tibb_bacisi_kodu, boyuk_tibb_bacisi_adi,
                   cins_id, cins_kodu, cins_adi, aktiv, yaranma_tarixi,
                   yaradan_personal_id, yaradan_personal_adi, yenilenme_tarixi,
                   yenileyen_personal_id, yenileyen_personal_adi
            FROM public.fn_sobe_siyahisi(
                p_klinika_id => CAST(:klinikaId AS bigint),
                p_aktiv => CAST(:aktiv AS boolean)
            )
            ORDER BY sira_no NULLS LAST, sobe_adi, sobe_id
            """;
        var params = new MapSqlParameterSource().addValue("klinikaId", klinikaId).addValue("aktiv", null);
        return jdbcTemplate.query(sql, params, (rs, rowNum) -> new SobeListItem(
            lng(rs.getObject("sobe_id")), lng(rs.getObject("klinika_id")), rs.getString("klinika_adi"),
            rs.getString("sobe_adi"), integer(rs.getObject("sira_no")), lng(rs.getObject("sobe_tipi_id")),
            rs.getString("sobe_tipi_kodu"), rs.getString("sobe_tipi_adi"), lng(rs.getObject("hekim_secim_qaydasi_id")),
            rs.getString("hekim_secim_qaydasi_kodu"), rs.getString("hekim_secim_qaydasi_adi"),
            lng(rs.getObject("sobe_mudiri_personal_id")), rs.getString("sobe_mudiri_kodu"), rs.getString("sobe_mudiri_adi"),
            lng(rs.getObject("boyuk_tibb_bacisi_personal_id")), rs.getString("boyuk_tibb_bacisi_kodu"), rs.getString("boyuk_tibb_bacisi_adi"),
            lng(rs.getObject("cins_id")), rs.getString("cins_kodu"), rs.getString("cins_adi"), rs.getObject("aktiv", Boolean.class),
            rs.getObject("yaranma_tarixi", java.time.LocalDateTime.class), lng(rs.getObject("yaradan_personal_id")),
            rs.getString("yaradan_personal_adi"), rs.getObject("yenilenme_tarixi", java.time.LocalDateTime.class),
            lng(rs.getObject("yenileyen_personal_id")), rs.getString("yenileyen_personal_adi")
        ));
    }

    public List<SobeOption> findSobeTipleri() { return options("SELECT sobe_tipi_id id, sobe_tipi_kodu kod, sobe_tipi_adi ad FROM public.fn_sobe_tipi_siyahisi() ORDER BY sobe_tipi_adi"); }
    public List<SobeOption> findHekimSecimQaydalari() { return options("SELECT hekim_secim_qaydasi_id id, hekim_secim_qaydasi_kodu kod, hekim_secim_qaydasi_adi ad FROM public.fn_hekim_secim_qaydasi_siyahisi() ORDER BY hekim_secim_qaydasi_adi"); }
    public List<SobeOption> findCinsler() { return options("SELECT id, kod, ad FROM public.fn_cins_siyahisi() ORDER BY ad"); }

    public List<PersonalOption> findPersonallar(Long klinikaId) {
        if (klinikaId == null) return List.of();
        String sql = "SELECT personal_id, personal_kodu, tam_ad FROM public.fn_personal_siyahisi(CAST(:klinikaId AS bigint), true) ORDER BY tam_ad";
        return jdbcTemplate.query(sql, new MapSqlParameterSource("klinikaId", klinikaId),
            (rs, n) -> new PersonalOption(lng(rs.getObject("personal_id")), rs.getString("personal_kodu"), rs.getString("tam_ad")));
    }

    public SobeOperationResult create(Long klinikaId, String ad, Long sobeTipiId,
                                      Long hekimSecimQaydasiId, Long sobeMudiriPersonalId,
                                      Long boyukTibbBacisiPersonalId, Long cinsId, Long personalId) {
        String sql = """
            SELECT status_kodu, sobe_id, mesaj FROM public.fn_sobe_yarat(
                p_klinika_id => CAST(:klinikaId AS bigint), p_ad => :ad,
                p_sobe_tipi_id => CAST(:sobeTipiId AS bigint),
                p_hekim_secim_qaydasi_id => CAST(:hekimSecimQaydasiId AS bigint),
                p_sobe_mudiri_personal_id => CAST(:sobeMudiriPersonalId AS bigint),
                p_boyuk_tibb_bacisi_personal_id => CAST(:boyukTibbBacisiPersonalId AS bigint),
                p_cins_id => CAST(:cinsId AS bigint),
                p_yaradan_personal_id => CAST(:personalId AS bigint))
            """;
        return result(sql, new MapSqlParameterSource().addValue("klinikaId", klinikaId).addValue("ad", ad.trim())
            .addValue("sobeTipiId", sobeTipiId).addValue("hekimSecimQaydasiId", hekimSecimQaydasiId)
            .addValue("sobeMudiriPersonalId", sobeMudiriPersonalId)
            .addValue("boyukTibbBacisiPersonalId", boyukTibbBacisiPersonalId)
            .addValue("cinsId", cinsId).addValue("personalId", personalId));
    }

    public SobeOperationResult update(Long sobeId, String ad, Long qaydaId, Long mudirId, Long baciId, Long cinsId,
                                      boolean aktiv, Long personalId) {
        String sql = """
            SELECT status_kodu, sobe_id, mesaj FROM public.fn_sobe_yenile(
                p_sobe_id => CAST(:sobeId AS bigint),
                p_ad => :ad, p_ad_deyisdirilsin => true,
                p_hekim_secim_qaydasi_id => CAST(:qaydaId AS bigint), p_hekim_secim_qaydasi_deyisdirilsin => true,
                p_sobe_mudiri_personal_id => CAST(:mudirId AS bigint), p_sobe_mudiri_deyisdirilsin => true,
                p_boyuk_tibb_bacisi_personal_id => CAST(:baciId AS bigint), p_boyuk_tibb_bacisi_deyisdirilsin => true,
                p_cins_id => CAST(:cinsId AS bigint), p_cins_deyisdirilsin => true,
                p_aktiv => :aktiv,
                p_yenileyen_personal_id => CAST(:personalId AS bigint))
            """;
        return result(sql, new MapSqlParameterSource().addValue("sobeId", sobeId).addValue("ad", ad.trim())
            .addValue("qaydaId", qaydaId)
            .addValue("mudirId", mudirId).addValue("baciId", baciId).addValue("cinsId", cinsId)
            .addValue("aktiv", aktiv).addValue("personalId", personalId));
    }

    private List<SobeOption> options(String sql) { return jdbcTemplate.query(sql, (rs,n) -> new SobeOption(lng(rs.getObject("id")), rs.getString("kod"), rs.getString("ad"))); }
    private SobeOperationResult result(String sql, MapSqlParameterSource p) { return jdbcTemplate.queryForObject(sql, p, (rs,n) -> new SobeOperationResult(rs.getString("status_kodu"), lng(rs.getObject("sobe_id")), rs.getString("mesaj"))); }
    private static Long lng(Object v) { return v instanceof Number n ? n.longValue() : null; }
    private static Integer integer(Object v) { return v instanceof Number n ? n.intValue() : null; }
}
