package az.simplexs.simplexs.repository.kassa;

import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class KassaEmeliyyatRepository {
    private final NamedParameterJdbcTemplate jdbc;
    public KassaEmeliyyatRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }
    private MapSqlParameterSource scope(Long clinic, Long cash) {
        return new MapSqlParameterSource("clinic", clinic).addValue("cash", cash);
    }
    public List<Map<String,Object>> cashRegisters(Long clinic, Long personal) {
        return jdbc.queryForList("""
            SELECT * FROM public.fn_personal_is_kassalari(
                p_klinika_id=>CAST(:clinic AS bigint), p_personal_id=>CAST(:personal AS bigint))
            """, scope(clinic, null).addValue("personal", personal));
    }
    public List<Map<String,Object>> paymentTypes() {
        return jdbc.queryForList("SELECT * FROM public.fn_odenis_novleri_siyahisi() WHERE aktiv = true", new MapSqlParameterSource());
    }
    public List<Map<String,Object>> visits(Long clinic, Long cash) {
        return jdbc.queryForList("""
            SELECT * FROM public.fn_kassa_odenis_gozleyen_gelisler_siyahisi(
                p_klinika_id=>CAST(:clinic AS bigint), p_kassa_id=>CAST(:cash AS bigint))
            """, scope(clinic,cash));
    }
    public List<Map<String,Object>> services(Long clinic, Long cash, Long visit) {
        return jdbc.queryForList("""
            SELECT * FROM public.fn_kassa_gelis_odenis_xidmetleri(
                p_klinika_id=>CAST(:clinic AS bigint), p_kassa_id=>CAST(:cash AS bigint), p_gelis_id=>CAST(:target AS bigint))
            """, scope(clinic,cash).addValue("target", visit));
    }
    public List<Map<String,Object>> receipts(Long clinic, Long cash, Long visit) {
        return jdbc.queryForList("""
            SELECT * FROM public.fn_kassa_gelis_qebz_siyahisi(
                p_klinika_id=>CAST(:clinic AS bigint), p_kassa_id=>CAST(:cash AS bigint), p_gelis_id=>CAST(:target AS bigint))
            """, scope(clinic,cash).addValue("target", visit));
    }
    public List<Map<String,Object>> debtors(Long clinic, Long cash) {
        return jdbc.queryForList("""
            SELECT * FROM public.fn_kassa_borclular_siyahisi(
                p_klinika_id=>CAST(:clinic AS bigint), p_kassa_id=>CAST(:cash AS bigint))
            """, scope(clinic,cash));
    }
    public List<Map<String,Object>> debtServices(Long clinic, Long cash, Long patient) {
        // The list function returns the debt header ID, whereas payment needs the detail ID.
        // Resolve it by both header and patient-service IDs; never substitute the header ID.
        return jdbc.queryForList("""
            SELECT s.*, bx.id AS borclandirma_xidmet_id
            FROM public.fn_kassa_borclu_xidmetler_siyahisi(
                p_klinika_id=>CAST(:clinic AS bigint), p_kassa_id=>CAST(:cash AS bigint), p_xeste_id=>CAST(:target AS bigint)) s
            JOIN public.rn_borclandirma_xidmetleri bx
                ON bx.borclandirma_id=s.borclandirma_id AND bx.xeste_xidmet_id=s.xeste_xidmet_id AND bx.aktiv=true
            ORDER BY s.borclandirma_tarixi DESC, bx.id
            """, scope(clinic,cash).addValue("target", patient));
    }
    public boolean lockPatient(Long clinic, Long target, boolean debt) {
        // Serialize this application's payments for the same patient across sessions and cash desks.
        String patient = debt ? "CAST(:target AS bigint)" : """
            (SELECT xeste_id FROM public.rn_xeste_gelisleri
             WHERE id=:target AND klinika_id=:clinic AND aktiv=true)
            """;
        return !jdbc.queryForList("SELECT id FROM public.rn_xesteler WHERE klinika_id=:clinic AND aktiv=true AND id="
                +patient+" FOR UPDATE", scope(clinic,null).addValue("target",target)).isEmpty();
    }
    public Map<String,Object> pay(Long clinic, Long cash, Long personal, Long target,
            boolean debt, String services, String payments, boolean borrow, String note) {
        var args = scope(clinic,cash).addValue("personal",personal).addValue("target",target)
                .addValue("services",services).addValue("payments",payments).addValue("borrow",borrow).addValue("note",note);
        String sql = debt ? """
            SELECT status_kodu, ugurlu, mesaj FROM public.fn_kassa_borc_odenisi_qebul_et(
                p_klinika_id=>CAST(:clinic AS bigint), p_kassa_id=>CAST(:cash AS bigint),
                p_personal_id=>CAST(:personal AS bigint), p_xeste_id=>CAST(:target AS bigint),
                p_borclu_xidmetler=>CAST(:services AS jsonb), p_odenisler=>CAST(:payments AS jsonb), p_aciqlama=>:note)
            """ : """
            SELECT status_kodu, ugurlu, mesaj FROM public.fn_kassa_xesteden_odenis_qebul_et(
                p_klinika_id=>CAST(:clinic AS bigint), p_kassa_id=>CAST(:cash AS bigint),
                p_personal_id=>CAST(:personal AS bigint), p_gelis_id=>CAST(:target AS bigint),
                p_xidmetler=>CAST(:services AS jsonb), p_odenisler=>CAST(:payments AS jsonb),
                p_borclandir=>:borrow, p_aciqlama=>:note)
            """;
        return jdbc.queryForMap(sql,args);
    }
}
