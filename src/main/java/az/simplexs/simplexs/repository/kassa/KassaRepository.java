package az.simplexs.simplexs.repository.kassa;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import az.simplexs.simplexs.dto.kassa.Kassa;

@Repository
public class KassaRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public KassaRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Kassa> findAll(Long klinikaId, Boolean aktiv) {
        if (klinikaId == null) return List.of();
        return jdbc.query("""
                SELECT * FROM public.fn_kassa_siyahisi(
                    p_klinika_id=>CAST(:klinika AS bigint), p_aktiv=>CAST(:aktiv AS boolean))
                ORDER BY sira_no NULLS LAST, kassa_adi, kassa_id
                """, new MapSqlParameterSource("klinika", klinikaId).addValue("aktiv", aktiv),
                (r, n) -> new Kassa(l(r, "kassa_id"), l(r, "klinika_id"),
                        r.getString("kassa_kodu"), r.getString("kassa_adi"), r.getString("aciqlama"),
                        i(r, "sira_no"), r.getObject("aktiv", Boolean.class),
                        r.getObject("yaranma_tarixi", java.time.LocalDateTime.class),
                        l(r, "yaradan_personal_id"),
                        r.getObject("yenilenme_tarixi", java.time.LocalDateTime.class),
                        l(r, "yenileyen_personal_id")));
    }

    public Map<String, Object> update(Long id, String kod, String ad, String aciqlama,
            boolean aktiv, Long personalId) {
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT * FROM public.fn_kassa_yenile(
                    p_kassa_id=>CAST(:id AS bigint), p_kod=>:kod, p_ad=>:ad,
                    p_aciqlama=>:aciqlama, p_aciqlama_deyisdirilsin=>true,
                    p_aktiv=>:aktiv, p_yenileyen_personal_id=>CAST(:personal AS bigint))
                """, new MapSqlParameterSource("id", id).addValue("kod", normalized(kod))
                        .addValue("ad", ad.trim()).addValue("aciqlama", normalized(aciqlama))
                        .addValue("aktiv", aktiv).addValue("personal", personalId));
        return rows.isEmpty() ? Map.of() : rows.getFirst();
    }

    private static String normalized(String value) {
        return value == null || value.isBlank() ? null : value.trim();
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
