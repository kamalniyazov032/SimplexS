package az.simplexs.simplexs.repository.klinika;

import java.util.List;
import java.util.function.Supplier;

import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import az.simplexs.simplexs.dto.klinika.KlinikaListItem;

@Repository
public class KlinikaRepository {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public KlinikaRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<KlinikaListItem> findAll() {
        String sql = """
            SELECT klinika_id, sira_no, klinika_adi, email, vergi_nomresi,
                   direktor_id, direktor_adi, direktor_soyadi, direktor_ata_adi,
                   direktor_tam_adi, bas_hekim_id, bas_hekim_adi, bas_hekim_soyadi,
                   bas_hekim_ata_adi, bas_hekim_tam_adi, aktiv, yaranma_tarixi
            FROM public.fn_klinika_siyahisi()
            ORDER BY sira_no NULLS LAST, klinika_adi, klinika_id
            """;

        return retryOnBrokenConnection(() -> jdbcTemplate.query(sql, (rs, rowNum) -> new KlinikaListItem(
            numberAsLong(rs.getObject("klinika_id")),
            numberAsInteger(rs.getObject("sira_no")),
            rs.getString("klinika_adi"),
            rs.getString("email"),
            rs.getString("vergi_nomresi"),
            numberAsLong(rs.getObject("direktor_id")),
            rs.getString("direktor_adi"),
            rs.getString("direktor_soyadi"),
            rs.getString("direktor_ata_adi"),
            rs.getString("direktor_tam_adi"),
            numberAsLong(rs.getObject("bas_hekim_id")),
            rs.getString("bas_hekim_adi"),
            rs.getString("bas_hekim_soyadi"),
            rs.getString("bas_hekim_ata_adi"),
            rs.getString("bas_hekim_tam_adi"),
            rs.getObject("aktiv", Boolean.class),
            rs.getObject("yaranma_tarixi", java.time.LocalDateTime.class)
        )));
    }

    private static Long numberAsLong(Object value) {
        return value instanceof Number number ? number.longValue() : null;
    }

    private static Integer numberAsInteger(Object value) {
        return value instanceof Number number ? number.intValue() : null;
    }

    private <T> T retryOnBrokenConnection(Supplier<T> operation) {
        try {
            return operation.get();
        } catch (DataAccessResourceFailureException firstFailure) {
            // Yalnız read-only siyahı sorğusu qırıq socket olduqda yeni connection ilə bir dəfə təkrarlanır.
            return operation.get();
        }
    }
}
