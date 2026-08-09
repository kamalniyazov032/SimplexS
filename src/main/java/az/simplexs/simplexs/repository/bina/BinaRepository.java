package az.simplexs.simplexs.repository.bina;

import java.util.List;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import az.simplexs.simplexs.dto.bina.BinaListItem;
import az.simplexs.simplexs.dto.bina.BinaNovu;
import az.simplexs.simplexs.dto.bina.BinaUpdateResult;

@Repository
public class BinaRepository {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public BinaRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<BinaListItem> findByKlinikaId(Long klinikaId) {
        if (klinikaId == null) {
            return List.of();
        }

        String sql = """
            SELECT bina_id, klinika_id, klinika_adi, sira_no, bina_adi, unvan,
                   telefon, mobil_nomre, mertebe_sayi, bina_novu_id,
                   bina_novu_kodu, bina_novu_adi, aktiv, yaranma_tarixi
            FROM public.fn_bina_siyahisi()
            WHERE klinika_id = :klinikaId
            ORDER BY sira_no NULLS LAST, bina_adi, bina_id
            """;

        return jdbcTemplate.query(sql,
            new org.springframework.jdbc.core.namedparam.MapSqlParameterSource("klinikaId", klinikaId),
            (rs, rowNum) -> new BinaListItem(
                toLong(rs.getObject("bina_id")),
                toLong(rs.getObject("klinika_id")),
                rs.getString("klinika_adi"),
                toInteger(rs.getObject("sira_no")),
                rs.getString("bina_adi"),
                rs.getString("unvan"),
                rs.getString("telefon"),
                rs.getString("mobil_nomre"),
                toInteger(rs.getObject("mertebe_sayi")),
                toLong(rs.getObject("bina_novu_id")),
                rs.getString("bina_novu_kodu"),
                rs.getString("bina_novu_adi"),
                rs.getObject("aktiv", Boolean.class),
                rs.getObject("yaranma_tarixi", java.time.LocalDateTime.class)
            ));
    }

    public List<BinaNovu> findBinaNovleri() {
        String sql = """
            SELECT bina_novu_id, bina_novu_kodu, bina_novu_adi, aciqlama
            FROM public.fn_bina_novu_siyahisi()
            ORDER BY bina_novu_adi, bina_novu_id
            """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> new BinaNovu(
            toLong(rs.getObject("bina_novu_id")),
            rs.getString("bina_novu_kodu"),
            rs.getString("bina_novu_adi"),
            rs.getString("aciqlama")
        ));
    }

    public BinaUpdateResult update(
        Long binaId,
        String unvan,
        String telefon,
        String mobilNomre,
        Integer mertebeSayi,
        Long binaNovuId,
        Integer siraNo
    ) {
        String sql = """
            SELECT status_kodu, bina_id, mesaj
            FROM public.fn_bina_yenile(
                p_bina_id => :binaId,
                p_unvan => :unvan,
                p_unvan_deyisdirilsin => true,
                p_telefon => :telefon,
                p_telefon_deyisdirilsin => true,
                p_mobil_nomre => :mobilNomre,
                p_mobil_nomre_deyisdirilsin => true,
                p_mertebe_sayi => :mertebeSayi,
                p_mertebe_sayi_deyisdirilsin => true,
                p_bina_novu_id => :binaNovuId,
                p_sira_no => :siraNo,
                p_sira_no_deyisdirilsin => true
            )
            """;

        var params = new org.springframework.jdbc.core.namedparam.MapSqlParameterSource()
            .addValue("binaId", binaId)
            .addValue("unvan", normalized(unvan))
            .addValue("telefon", normalized(telefon))
            .addValue("mobilNomre", normalized(mobilNomre))
            .addValue("mertebeSayi", mertebeSayi)
            .addValue("binaNovuId", binaNovuId)
            .addValue("siraNo", siraNo);

        return jdbcTemplate.queryForObject(sql, params, (rs, rowNum) -> new BinaUpdateResult(
            toInteger(rs.getObject("status_kodu")),
            toLong(rs.getObject("bina_id")),
            rs.getString("mesaj")
        ));
    }

    private String normalized(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private Long toLong(Object value) {
        return value instanceof Number number ? number.longValue() : null;
    }

    private Integer toInteger(Object value) {
        return value instanceof Number number ? number.intValue() : null;
    }
}
