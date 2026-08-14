package az.simplexs.simplexs.repository.diaqnoz;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import az.simplexs.simplexs.dto.diaqnoz.Diaqnoz;
import az.simplexs.simplexs.dto.diaqnoz.DiaqnozSistemi;

@Repository
public class DiaqnozRepository {
    private static final String SOURCE = """
            public.fn_diaqnoz_terif_siyahisi(
                p_diaqnoz_sistemi_id=>CAST(:sistem AS bigint),
                p_parent_id=>CAST(:parent AS bigint),
                p_secile_biler=>CAST(:secileBiler AS boolean),
                p_aktiv=>CAST(:aktiv AS boolean),
                p_axtaris=>CAST(:axtaris AS text))
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public DiaqnozRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<DiaqnozSistemi> sistemler() {
        return jdbc.query("""
                SELECT DISTINCT diaqnoz_sistemi_id, diaqnoz_sistemi_kodu, diaqnoz_sistemi_adi
                FROM public.fn_diaqnoz_terif_siyahisi(
                    p_diaqnoz_sistemi_id=>NULL, p_parent_id=>NULL,
                    p_secile_biler=>NULL, p_aktiv=>NULL, p_axtaris=>NULL)
                ORDER BY diaqnoz_sistemi_adi
                """, (r, n) -> new DiaqnozSistemi(l(r, "diaqnoz_sistemi_id"),
                        r.getString("diaqnoz_sistemi_kodu"), r.getString("diaqnoz_sistemi_adi")));
    }

    public List<Diaqnoz> find(Long sistemId, Long parentId, Boolean secileBiler, Boolean aktiv,
            String axtaris, Boolean kateqoriyadir, String cins, int limit, int offset) {
        String sql = "SELECT * FROM " + SOURCE + " f " + extraWhere(kateqoriyadir, cins)
                + " ORDER BY f.sira_no NULLS LAST, f.kod, f.ad LIMIT :limit OFFSET :offset";
        return jdbc.query(sql, params(sistemId, parentId, secileBiler, aktiv, axtaris, cins)
                .addValue("kateqoriya", kateqoriyadir).addValue("limit", limit).addValue("offset", offset),
                (r, n) -> map(r));
    }

    public long count(Long sistemId, Long parentId, Boolean secileBiler, Boolean aktiv,
            String axtaris, Boolean kateqoriyadir, String cins) {
        String sql = "SELECT count(*) FROM " + SOURCE + " f " + extraWhere(kateqoriyadir, cins);
        Long result = jdbc.queryForObject(sql, params(sistemId, parentId, secileBiler, aktiv, axtaris, cins)
                .addValue("kateqoriya", kateqoriyadir), Long.class);
        return result == null ? 0 : result;
    }

    private String extraWhere(Boolean kateqoriyadir, String cins) {
        return """
                WHERE (CAST(:kateqoriya AS boolean) IS NULL OR f.kateqoriyadir=:kateqoriya)
                  AND ('qadin'<>CAST(:cins AS text) OR f.qadina_verile_biler=true)
                  AND ('kisi'<>CAST(:cins AS text) OR f.kisiye_verile_biler=true)
                """;
    }

    private MapSqlParameterSource params(Long sistemId, Long parentId, Boolean secileBiler,
            Boolean aktiv, String axtaris, String cins) {
        return new MapSqlParameterSource("sistem", sistemId).addValue("parent", parentId)
                .addValue("secileBiler", secileBiler).addValue("aktiv", aktiv)
                .addValue("axtaris", blank(axtaris)).addValue("cins", blank(cins) == null ? "hamisi" : cins);
    }

    private Diaqnoz map(ResultSet r) throws SQLException {
        return new Diaqnoz(l(r, "diaqnoz_id"), l(r, "diaqnoz_sistemi_id"),
                r.getString("diaqnoz_sistemi_kodu"), r.getString("diaqnoz_sistemi_adi"),
                l(r, "parent_id"), r.getString("parent_kodu"), r.getString("parent_adi"),
                r.getString("kod"), r.getString("ad"), r.getString("aciqlama"),
                r.getObject("kateqoriyadir", Boolean.class), r.getObject("secile_biler", Boolean.class),
                r.getObject("qadina_verile_biler", Boolean.class), r.getObject("kisiye_verile_biler", Boolean.class),
                i(r, "sira_no"), r.getObject("aktiv", Boolean.class),
                r.getObject("yaranma_tarixi", java.time.LocalDateTime.class),
                r.getObject("yenilenme_tarixi", java.time.LocalDateTime.class));
    }

    private static String blank(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private static Long l(ResultSet r, String column) throws SQLException { Object x = r.getObject(column); return x instanceof Number n ? n.longValue() : null; }
    private static Integer i(ResultSet r, String column) throws SQLException { Object x = r.getObject(column); return x instanceof Number n ? n.intValue() : null; }
}
