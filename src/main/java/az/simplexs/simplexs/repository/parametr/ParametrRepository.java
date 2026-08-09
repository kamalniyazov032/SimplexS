package az.simplexs.simplexs.repository.parametr;

import java.util.List;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import az.simplexs.simplexs.dto.parametr.KlinikaParametr;
import az.simplexs.simplexs.dto.parametr.ParametrSaveResult;
import az.simplexs.simplexs.dto.parametr.ParametrSecim;

@Repository
public class ParametrRepository {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public ParametrRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<KlinikaParametr> findByKlinikaId(Long klinikaId) {
        if (klinikaId == null) {
            return List.of();
        }

        String sql = """
            SELECT parametr_id, parametr_kodu, parametr_adi, aciqlama, parametr_tipi,
                   sira_no, parametr_deyer_id, boolean_deyer, text_deyer,
                   secim_id, secim_kodu, secim_adi, deyer_teyin_edilib,
                   yenileyen_personal_id, yenilenme_tarixi
            FROM public.fn_parametr_klinika_siyahisi(
                p_klinika_id => CAST(:klinikaId AS bigint)
            )
            ORDER BY sira_no NULLS LAST, parametr_adi, parametr_id
            """;

        return jdbcTemplate.query(sql, new MapSqlParameterSource("klinikaId", klinikaId),
            (rs, rowNum) -> {
                Long parametrId = toLong(rs.getObject("parametr_id"));
                return new KlinikaParametr(
                    parametrId,
                    rs.getString("parametr_kodu"),
                    rs.getString("parametr_adi"),
                    rs.getString("aciqlama"),
                    rs.getString("parametr_tipi"),
                    toInteger(rs.getObject("sira_no")),
                    toLong(rs.getObject("parametr_deyer_id")),
                    rs.getObject("boolean_deyer", Boolean.class),
                    rs.getString("text_deyer"),
                    toLong(rs.getObject("secim_id")),
                    rs.getString("secim_kodu"),
                    rs.getString("secim_adi"),
                    rs.getObject("deyer_teyin_edilib", Boolean.class),
                    toLong(rs.getObject("yenileyen_personal_id")),
                    rs.getObject("yenilenme_tarixi", java.time.LocalDateTime.class),
                    findSecimler(parametrId)
                );
            });
    }

    public ParametrSaveResult save(
        Long klinikaId,
        Long parametrId,
        Boolean booleanDeyer,
        String textDeyer,
        Long secimId,
        Long yenileyenPersonalId
    ) {
        String sql = """
            SELECT status_kodu, parametr_deyer_id, parametr_tipi, mesaj
            FROM public.fn_parametr_klinika_deyerini_elave_et(
                p_klinika_id => CAST(:klinikaId AS bigint),
                p_parametr_id => CAST(:parametrId AS bigint),
                p_boolean_deyer => :booleanDeyer,
                p_text_deyer => :textDeyer,
                p_secim_id => CAST(:secimId AS bigint),
                p_yenileyen_personal_id => CAST(:yenileyenPersonalId AS bigint)
            )
            """;

        var params = new MapSqlParameterSource()
            .addValue("klinikaId", klinikaId)
            .addValue("parametrId", parametrId)
            .addValue("booleanDeyer", booleanDeyer)
            .addValue("textDeyer", textDeyer == null || textDeyer.isBlank() ? null : textDeyer.trim())
            .addValue("secimId", secimId)
            .addValue("yenileyenPersonalId", yenileyenPersonalId);

        return jdbcTemplate.queryForObject(sql, params, (rs, rowNum) -> new ParametrSaveResult(
            toInteger(rs.getObject("status_kodu")),
            toLong(rs.getObject("parametr_deyer_id")),
            rs.getString("parametr_tipi"),
            rs.getString("mesaj")
        ));
    }

    private List<ParametrSecim> findSecimler(Long parametrId) {
        String sql = """
            SELECT secim_id, parametr_id, secim_kodu, secim_adi, sira_no
            FROM public.fn_parametr_secim_siyahisi(
                p_parametr_id => CAST(:parametrId AS bigint)
            )
            ORDER BY sira_no NULLS LAST, secim_adi, secim_id
            """;

        return jdbcTemplate.query(sql, new MapSqlParameterSource("parametrId", parametrId),
            (rs, rowNum) -> new ParametrSecim(
                toLong(rs.getObject("secim_id")),
                toLong(rs.getObject("parametr_id")),
                rs.getString("secim_kodu"),
                rs.getString("secim_adi"),
                toInteger(rs.getObject("sira_no"))
            ));
    }

    private static Long toLong(Object value) {
        return value instanceof Number number ? number.longValue() : null;
    }

    private static Integer toInteger(Object value) {
        return value instanceof Number number ? number.intValue() : null;
    }
}
