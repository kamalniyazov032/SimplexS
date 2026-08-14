package az.simplexs.simplexs.repository.rol;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.function.Supplier;

import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import az.simplexs.simplexs.dto.rol.Rol;
import az.simplexs.simplexs.dto.rol.RolModul;
import az.simplexs.simplexs.dto.rol.RolPermissionSaveResult;
import az.simplexs.simplexs.dto.rol.RolSelahiyyet;

@Repository
public class RolRepository {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public RolRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Rol> findAll(Long klinikaId) {
        return retryOnBrokenConnection(() -> jdbcTemplate.query("""
            SELECT rol_id, klinika_id, rol_adi, aciqlama, sistem_roludur, sira_no, aktiv, yaranma_tarixi
            FROM public.fn_rol_siyahisi(p_klinika_id=>CAST(:klinikaId AS bigint), p_aktiv=>CAST(:aktiv AS boolean))
            ORDER BY sira_no NULLS LAST, rol_adi, rol_id
            """, new MapSqlParameterSource("aktiv", null).addValue("klinikaId", klinikaId), (rs, rowNum) -> new Rol(
                rs.getObject("rol_id", Long.class), rs.getObject("klinika_id", Long.class),
                rs.getString("rol_adi"), rs.getString("aciqlama"),
                rs.getObject("sistem_roludur", Boolean.class), rs.getObject("sira_no", Integer.class),
                rs.getObject("aktiv", Boolean.class), rs.getObject("yaranma_tarixi", java.time.LocalDateTime.class)
            )));
    }

    public List<RolModul> findModullar(Long rolId) {
        return retryOnBrokenConnection(() -> jdbcTemplate.query("""
            SELECT t.sistem_id, t.sistem_kodu, t.sistem_adi, t.modul_id, t.parent_id, t.modul_kodu,
                   t.modul_adi, t.modul_aciqlamasi, t.route, t.ikon, t.menyuda_gorunsun, t.sira_no,
                   t.seviyye, t.secilib
            FROM public.fn_rol_modul_siyahisi(:rolId) AS t
            ORDER BY t.sira_no NULLS LAST, t.modul_adi
            """, new MapSqlParameterSource("rolId", rolId), (rs, rowNum) -> new RolModul(
                rs.getObject("sistem_id", Long.class), rs.getString("sistem_kodu"), rs.getString("sistem_adi"),
                rs.getObject("modul_id", Long.class), rs.getObject("parent_id", Long.class),
                rs.getString("modul_kodu"), rs.getString("modul_adi"), rs.getString("modul_aciqlamasi"),
                rs.getString("route"), rs.getString("ikon"), rs.getObject("menyuda_gorunsun", Boolean.class),
                rs.getObject("sira_no", Integer.class),
                rs.getObject("seviyye", Integer.class), rs.getObject("secilib", Boolean.class)
            )));
    }

    public List<RolSelahiyyet> findSelahiyyetler(Long rolId) {
        return retryOnBrokenConnection(() -> jdbcTemplate.query("""
            SELECT t.selahiyyet_id, t.modul_id, t.modul_adi, t.selahiyyet_kodu,
                   t.selahiyyet_adi, t.aciqlama, t.secilib
            FROM public.fn_rol_selahiyyet_siyahisi(CAST(:rolId AS bigint)) AS t
            ORDER BY t.modul_adi, t.selahiyyet_adi
            """, new MapSqlParameterSource("rolId", rolId), (rs, rowNum) -> new RolSelahiyyet(
                rs.getObject("selahiyyet_id", Long.class), rs.getObject("modul_id", Long.class),
                rs.getString("modul_adi"), rs.getString("selahiyyet_kodu"), rs.getString("selahiyyet_adi"),
                rs.getString("aciqlama"), rs.getObject("secilib", Boolean.class)
            )));
    }

    public void create(Long klinikaId, String ad, String aciqlama, boolean sistemRoludur) {
        jdbcTemplate.queryForList("""
            SELECT status_kodu, rol_id, mesaj
            FROM public.fn_rol_yarat(p_klinika_id=>:klinikaId, p_ad=>:ad, p_aciqlama=>:aciqlama,
                p_sistem_roludur=>:sistemRoludur, p_aktiv=>true)
            """, new MapSqlParameterSource()
            .addValue("ad", ad.trim()).addValue("klinikaId", klinikaId)
            .addValue("aciqlama", blankToNull(aciqlama)).addValue("sistemRoludur", sistemRoludur));
    }

    public void update(Long rolId, String ad, String aciqlama, boolean sistemRoludur, boolean aktiv) {
        jdbcTemplate.queryForList("""
            SELECT * FROM public.fn_rol_yenile(p_rol_id=>:rolId, p_ad=>:ad,
              p_aciqlama=>:aciqlama, p_aciqlama_deyisdirilsin=>true,
              p_sistem_roludur=>:sistemRoludur, p_aktiv=>:aktiv)
            """, new MapSqlParameterSource().addValue("rolId", rolId).addValue("ad", ad.trim())
            .addValue("aciqlama", blankToNull(aciqlama)).addValue("sistemRoludur", sistemRoludur)
            .addValue("aktiv", aktiv));
    }

    public void deactivate(Long rolId) {
        jdbcTemplate.queryForList("SELECT * FROM public.fn_rol_yenile(p_rol_id => :rolId, p_aktiv => false)",
            new MapSqlParameterSource("rolId", rolId));
    }

    public RolPermissionSaveResult savePermissions(Long rolId, List<Long> modulIds, List<Long> selahiyyetIds) {
        return jdbcTemplate.getJdbcTemplate().execute((Connection connection) -> {
            try (PreparedStatement statement = connection.prepareStatement("""
                SELECT status_kodu, modul_sayi, selahiyyet_sayi, mesaj
                FROM public.fn_rola_modul_ve_selahiyyet_ver(?, ?, ?)
                """)) {
                statement.setLong(1, rolId);
                statement.setArray(2, connection.createArrayOf("bigint", modulIds.toArray(Long[]::new)));
                statement.setArray(3, connection.createArrayOf("bigint", selahiyyetIds.toArray(Long[]::new)));
                try (var resultSet = statement.executeQuery()) {
                    if (!resultSet.next()) {
                        return new RolPermissionSaveResult("NETICE_YOXDUR", 0, 0,
                            "Verilənlər bazası əməliyyat nəticəsi qaytarmadı.");
                    }
                    return new RolPermissionSaveResult(
                        resultSet.getString("status_kodu"),
                        resultSet.getObject("modul_sayi", Integer.class),
                        resultSet.getObject("selahiyyet_sayi", Integer.class),
                        resultSet.getString("mesaj")
                    );
                }
            }
        });
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private <T> T retryOnBrokenConnection(Supplier<T> operation) {
        try {
            return operation.get();
        } catch (DataAccessResourceFailureException firstFailure) {
            return operation.get();
        }
    }
}
