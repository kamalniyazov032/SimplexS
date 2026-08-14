package az.simplexs.simplexs.repository.yataq;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import az.simplexs.simplexs.dto.yataq.Mertebe;
import az.simplexs.simplexs.dto.yataq.Palata;
import az.simplexs.simplexs.dto.yataq.Yataq;

@Repository
public class YataqRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public YataqRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Mertebe> mertebeler(Long klinikaId, Long binaId, Boolean aktiv) {
        if (klinikaId == null) return List.of();
        return jdbc.query("""
                SELECT * FROM public.fn_mertebe_siyahisi(
                    p_klinika_id=>CAST(:klinika AS bigint), p_bina_id=>CAST(:bina AS bigint),
                    p_aktiv=>CAST(:aktiv AS boolean))
                ORDER BY sira_no NULLS LAST, mertebe_no, mertebe_id
                """, new MapSqlParameterSource("klinika", klinikaId).addValue("bina", binaId)
                        .addValue("aktiv", aktiv), (r, n) -> new Mertebe(l(r, "mertebe_id"),
                        l(r, "klinika_id"), l(r, "bina_id"), r.getString("bina_adi"),
                        i(r, "mertebe_no"), r.getString("mertebe_adi"), i(r, "sira_no"),
                        r.getObject("aktiv", Boolean.class)));
    }

    public List<Palata> palatalar(Long klinikaId, Long mertebeId, Boolean aktiv) {
        if (klinikaId == null) return List.of();
        return jdbc.query("""
                SELECT * FROM public.fn_palata_siyahisi(
                    p_klinika_id=>CAST(:klinika AS bigint), p_mertebe_id=>CAST(:mertebe AS bigint),
                    p_aktiv=>CAST(:aktiv AS boolean))
                ORDER BY sira_no NULLS LAST, otaq_nomresi, palata_id
                """, new MapSqlParameterSource("klinika", klinikaId).addValue("mertebe", mertebeId)
                        .addValue("aktiv", aktiv), (r, n) -> new Palata(l(r, "palata_id"),
                        l(r, "klinika_id"), l(r, "bina_id"), l(r, "mertebe_id"), i(r, "mertebe_no"),
                        r.getString("otaq_nomresi"), r.getString("palata_adi"), r.getString("aciqlama"),
                        i(r, "sira_no"), r.getObject("aktiv", Boolean.class)));
    }

    public List<Yataq> yataqlar(Long klinikaId, Long mertebeId, Long palataId, Long sobeId, Boolean aktiv) {
        if (klinikaId == null) return List.of();
        return jdbc.query("""
                SELECT * FROM public.fn_yataq_siyahisi(
                    p_klinika_id=>CAST(:klinika AS bigint), p_mertebe_id=>CAST(:mertebe AS bigint),
                    p_palata_id=>CAST(:palata AS bigint), p_sobe_id=>CAST(:sobe AS bigint),
                    p_aktiv=>CAST(:aktiv AS boolean))
                ORDER BY sira_no NULLS LAST, yataq_kodu, yataq_id
                """, new MapSqlParameterSource("klinika", klinikaId).addValue("mertebe", mertebeId)
                        .addValue("palata", palataId).addValue("sobe", sobeId).addValue("aktiv", aktiv),
                (r, n) -> new Yataq(l(r, "yataq_id"), l(r, "klinika_id"), l(r, "bina_id"),
                        r.getString("bina_adi"), l(r, "mertebe_id"), i(r, "mertebe_no"),
                        r.getString("mertebe_adi"), l(r, "palata_id"), r.getString("otaq_nomresi"),
                        r.getString("palata_adi"), l(r, "sobe_id"), r.getString("sobe_adi"),
                        r.getString("yataq_kodu"), r.getString("yataq_adi"), i(r, "sira_no"),
                        r.getObject("aktiv", Boolean.class)));
    }

    public Map<String, Object> mertebeYarat(Long klinikaId, Long binaId, Integer no, String ad, Long personalId) {
        return one("SELECT * FROM public.fn_mertebe_yarat(p_klinika_id=>:klinika,p_bina_id=>:bina,p_mertebe_no=>:no,p_ad=>:ad,p_yaradan_personal_id=>:personal)",
                new MapSqlParameterSource("klinika", klinikaId).addValue("bina", binaId).addValue("no", no)
                        .addValue("ad", ad.trim()).addValue("personal", personalId));
    }

    public Map<String, Object> mertebeYenile(Long id, Integer no, String ad, boolean aktiv, Long personalId) {
        return one("SELECT * FROM public.fn_mertebe_yenile(p_mertebe_id=>:id,p_mertebe_no=>:no,p_ad=>:ad,p_ad_deyisdirilsin=>true,p_aktiv=>:aktiv,p_yenileyen_personal_id=>:personal)",
                new MapSqlParameterSource("id", id).addValue("no", no).addValue("ad", ad.trim())
                        .addValue("aktiv", aktiv).addValue("personal", personalId));
    }

    public Map<String, Object> palataYarat(Long klinikaId, Long mertebeId, String otaqNo,
            String ad, String aciqlama, Long personalId) {
        return one("SELECT * FROM public.fn_palata_yarat(p_klinika_id=>:klinika,p_mertebe_id=>:mertebe,p_otaq_nomresi=>:otaq,p_ad=>:ad,p_aciqlama=>:aciq,p_yaradan_personal_id=>:personal)",
                new MapSqlParameterSource("klinika", klinikaId).addValue("mertebe", mertebeId)
                        .addValue("otaq", otaqNo.trim()).addValue("ad", ad.trim())
                        .addValue("aciq", blank(aciqlama)).addValue("personal", personalId));
    }

    public Map<String, Object> palataYenile(Long id, String otaqNo, String ad, String aciqlama,
            boolean aktiv, Long personalId) {
        return one("SELECT * FROM public.fn_palata_yenile(p_palata_id=>:id,p_otaq_nomresi=>:otaq,p_ad=>:ad,p_ad_deyisdirilsin=>true,p_aciqlama=>:aciq,p_aciqlama_deyisdirilsin=>true,p_aktiv=>:aktiv,p_yenileyen_personal_id=>:personal)",
                new MapSqlParameterSource("id", id).addValue("otaq", otaqNo.trim()).addValue("ad", ad.trim())
                        .addValue("aciq", blank(aciqlama)).addValue("aktiv", aktiv).addValue("personal", personalId));
    }

    public Map<String, Object> yataqYarat(Long klinikaId, Long palataId, Long sobeId, String kod,
            String ad, String aciqlama, Long personalId) {
        return one("SELECT * FROM public.fn_yataq_yarat(p_klinika_id=>:klinika,p_palata_id=>:palata,p_sobe_id=>:sobe,p_kod=>:kod,p_ad=>:ad,p_aciqlama=>:aciq,p_yaradan_personal_id=>:personal)",
                new MapSqlParameterSource("klinika", klinikaId).addValue("palata", palataId)
                        .addValue("sobe", sobeId).addValue("kod", kod.trim()).addValue("ad", ad.trim())
                        .addValue("aciq", blank(aciqlama)).addValue("personal", personalId));
    }

    public Map<String, Object> yataqYenile(Long id, Long sobeId, String kod, String ad,
            boolean aktiv, Long personalId) {
        return one("SELECT * FROM public.fn_yataq_yenile(p_yataq_id=>:id,p_sobe_id=>:sobe,p_kod=>:kod,p_ad=>:ad,p_ad_deyisdirilsin=>true,p_aciqlama=>NULL,p_aciqlama_deyisdirilsin=>false,p_aktiv=>:aktiv,p_yenileyen_personal_id=>:personal)",
                new MapSqlParameterSource("id", id).addValue("sobe", sobeId).addValue("kod", kod.trim())
                        .addValue("ad", ad.trim()).addValue("aktiv", aktiv).addValue("personal", personalId));
    }

    private Map<String, Object> one(String sql, MapSqlParameterSource params) {
        List<Map<String, Object>> rows = jdbc.queryForList(sql, params);
        return rows.isEmpty() ? Map.of() : rows.getFirst();
    }

    private static String blank(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private static Long l(ResultSet r, String c) throws SQLException { Object x = r.getObject(c); return x instanceof Number n ? n.longValue() : null; }
    private static Integer i(ResultSet r, String c) throws SQLException { Object x = r.getObject(c); return x instanceof Number n ? n.intValue() : null; }
}
