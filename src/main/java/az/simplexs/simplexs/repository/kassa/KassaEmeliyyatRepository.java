package az.simplexs.simplexs.repository.kassa;

import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class KassaEmeliyyatRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public KassaEmeliyyatRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private MapSqlParameterSource scope(Long clinic, Long cash) {
        return new MapSqlParameterSource("clinic", clinic).addValue("cash", cash);
    }

    public List<Map<String, Object>> cashRegisters(Long clinic, Long personal) {
        return jdbc.queryForList("""
                SELECT * FROM public.fn_personal_is_kassalari(
                    p_klinika_id=>CAST(:clinic AS bigint), p_personal_id=>CAST(:personal AS bigint))
                """, scope(clinic, null).addValue("personal", personal));
    }

    public Map<String, Object> balance(Long clinic, Long cash) {
        return jdbc.queryForMap("""
                SELECT umumi_giris, umumi_cixis, cari_balans FROM public.fn_kassa_balansi(
                    p_klinika_id=>CAST(:clinic AS bigint), p_kassa_id=>CAST(:cash AS bigint))
                """, scope(clinic, cash));
    }

    public Map<String, Object> balanceByDate(Long clinic, Long cash, java.time.LocalDate from, java.time.LocalDate to) {
        return jdbc.queryForMap("""
                SELECT acilis_balansi, dovr_giris, dovr_cixis, baglanis_balansi
                FROM public.fn_kassa_balansi_tarix_uzre(
                    p_klinika_id=>CAST(:clinic AS bigint), p_kassa_id=>CAST(:cash AS bigint),
                    p_baslangic_tarix=>CAST(:from AS date), p_son_tarix=>CAST(:to AS date))
                """, scope(clinic, cash).addValue("from", from).addValue("to", to));
    }

    public List<Map<String, Object>> transferRecipients(Long clinic, Long cash) {
        return jdbc.queryForList("""
                SELECT kassa_id, kassa_kodu, kassa_adi FROM public.fn_kassa_transfer_qebul_eden_kassalar_siyahisi(
                    p_klinika_id=>CAST(:clinic AS bigint), p_gonderen_kassa_id=>CAST(:cash AS bigint))
                """, scope(clinic, cash));
    }

    public Map<String, Object> transfer(Long clinic, Long cash, Long recipient, Long personal, Long account, java.math.BigDecimal amount, String note) {
        return jdbc.queryForMap("""
                SELECT status_kodu, ugurlu, mesaj FROM public.fn_kassadan_kassaya_transfer(
                    p_klinika_id=>CAST(:clinic AS bigint), p_gonderen_kassa_id=>CAST(:cash AS bigint),
                    p_qebul_eden_kassa_id=>CAST(:recipient AS bigint), p_personal_id=>CAST(:personal AS bigint),
                    p_muhasibat_kodu_id=>CAST(:account AS bigint), p_mebleg=>CAST(:amount AS numeric),
                    p_aciqlama=>CAST(:note AS text))
                """, scope(clinic, cash).addValue("recipient", recipient).addValue("personal", personal)
                .addValue("account", account).addValue("amount", amount).addValue("note", note));
    }

    public List<Map<String, Object>> allReceipts(Long clinic, Long cash, String query, java.time.LocalDate from,
                                                 java.time.LocalDate to, String direction, String operation, Boolean active, int page) {
        return jdbc.queryForList("""
                SELECT * FROM public.fn_kassa_qebzler_siyahisi(
                    p_klinika_id=>CAST(:clinic AS bigint),p_kassa_id=>CAST(:cash AS bigint))
                WHERE (:query='' OR position(lower(:query) in lower(concat_ws(' ',emeliyyat_no,xeste_kodu,xeste_adi_soyadi,
                    protokol_kodu,muhasibat_kodu_adi,yaradan_personal_adi,aciqlama)))>0)
                  AND (CAST(:from AS date) IS NULL OR emeliyyat_tarixi>=CAST(:from AS date))
                  AND (CAST(:to AS date) IS NULL OR emeliyyat_tarixi<CAST(:to AS date)+interval '1 day')
                  AND (:direction='' OR istiqamet=:direction)
                  AND (:operation='' OR emeliyyat_kodu=:operation)
                  AND (CAST(:active AS boolean) IS NULL OR aktiv=CAST(:active AS boolean))
                ORDER BY emeliyyat_tarixi DESC,kassa_emeliyyat_id DESC LIMIT 101 OFFSET :offset
                """, scope(clinic, cash).addValue("query", query).addValue("from", from).addValue("to", to)
                .addValue("direction", direction).addValue("operation", operation).addValue("active", active).addValue("offset", (long) (page - 1) * 100));
    }

    public Map<String, Object> receiptDetail(Long clinic, Long cash, Long receipt) {
        var rows = jdbc.queryForList("""
                SELECT * FROM public.fn_kassa_qebz_detali(p_klinika_id=>CAST(:clinic AS bigint),
                    p_kassa_id=>CAST(:cash AS bigint),p_kassa_emeliyyat_id=>CAST(:receipt AS bigint))
                """, scope(clinic, cash).addValue("receipt", receipt));
        return rows.isEmpty() ? Map.of() : rows.getFirst();
    }

    public Map<String, Object> receiptPrint(Long clinic, Long cash, Long receipt) {
        var rows = jdbc.queryForList("""
                SELECT * FROM public.fn_kassa_qebz_cap_melumati(p_klinika_id=>CAST(:clinic AS bigint),
                    p_kassa_id=>CAST(:cash AS bigint),p_kassa_emeliyyat_id=>CAST(:receipt AS bigint))
                """, scope(clinic, cash).addValue("receipt", receipt));
        return rows.isEmpty() ? Map.of() : rows.getFirst();
    }

    public Map<String, Object> cancelReceipt(Long clinic, Long cash, Long receipt, Long personal, String reason) {
        return jdbc.queryForMap("""
                SELECT status_kodu,ugurlu,eks_emeliyyat_id,mesaj FROM public.fn_kassa_qebzi_legv_et(
                    p_klinika_id=>CAST(:clinic AS bigint),p_kassa_id=>CAST(:cash AS bigint),
                    p_kassa_emeliyyat_id=>CAST(:receipt AS bigint),p_legv_eden_personal_id=>CAST(:personal AS bigint),
                    p_legv_sebebi=>CAST(:reason AS text))
                """, scope(clinic, cash).addValue("receipt", receipt).addValue("personal", personal).addValue("reason", reason));
    }

    public List<Map<String, Object>> paymentTypes() {
        return jdbc.queryForList("SELECT * FROM public.fn_odenis_novleri_siyahisi() WHERE aktiv = true", new MapSqlParameterSource());
    }

    public List<Map<String, Object>> accountingCodes(Long clinic, String operation) {
        return jdbc.queryForList("""
                SELECT muhasibat_kodu_id, emeliyyat_kodu, tip_kodu, ad, aciqlama, sira_no
                FROM public.fn_kassa_muhasibat_kodlari_siyahisi(
                    p_klinika_id=>CAST(:clinic AS bigint), p_emeliyyat_kodu=>CAST(:operation AS varchar))
                ORDER BY sira_no NULLS LAST, ad, muhasibat_kodu_id
                """, scope(clinic, null).addValue("operation", operation));
    }

    public Map<String, Object> otherIncome(Long clinic, Long cash, Long personal, Long account, String payments, String note) {
        return jdbc.queryForMap("""
                SELECT status_kodu, ugurlu, mesaj FROM public.fn_kassa_diger_giris(
                    p_klinika_id=>CAST(:clinic AS bigint), p_kassa_id=>CAST(:cash AS bigint),
                    p_personal_id=>CAST(:personal AS bigint), p_muhasibat_kodu_id=>CAST(:account AS bigint),
                    p_odenisler=>CAST(:payments AS jsonb), p_aciqlama=>CAST(:note AS text))
                """, scope(clinic, cash).addValue("personal", personal).addValue("account", account)
                .addValue("payments", payments).addValue("note", note));
    }

    public Map<String, Object> otherExpense(Long clinic, Long cash, Long personal, Long account, String payments, String note) {
        return jdbc.queryForMap("""
                SELECT status_kodu, ugurlu, mesaj FROM public.fn_kassa_diger_cixis(
                    p_klinika_id=>CAST(:clinic AS bigint), p_kassa_id=>CAST(:cash AS bigint),
                    p_personal_id=>CAST(:personal AS bigint), p_muhasibat_kodu_id=>CAST(:account AS bigint),
                    p_odenisler=>CAST(:payments AS jsonb), p_aciqlama=>CAST(:note AS text))
                """, scope(clinic, cash).addValue("personal", personal).addValue("account", account)
                .addValue("payments", payments).addValue("note", note));
    }

    public List<Map<String, Object>> cardTypes() {
        return jdbc.queryForList("SELECT * FROM public.fn_xeste_kart_novleri_siyahisi()", new MapSqlParameterSource());
    }

    public List<Map<String, Object>> advancePatients(Long clinic, Long cash, String cardType, String query,
                                                     java.time.LocalDate from, java.time.LocalDate to, int page) {
        String[] terms = query == null || query.isBlank() ? new String[0] : query.trim().split("\\s+");
        var params = scope(clinic, cash).addValue("card", cardType).addValue("from", from).addValue("to", to).addValue("offset", (long) (page - 1) * 100);
        StringBuilder sql = new StringBuilder("""
                SELECT gelis_id, xeste_id, l_kart, kart_novu_adi, gelis_karti, ad, soyad, ata_adi,
                       dogum_tarixi, gelis_tarixi, gelis_saati
                FROM public.fn_xeste_avans_almaq_ucun_siyahisi(
                    p_klinika_id=>CAST(:clinic AS bigint), p_kassa_id=>CAST(:cash AS bigint),
                    p_kart_novu_kodu=>CAST(:card AS varchar))
                WHERE aktiv=true
                  AND (CAST(:from AS date) IS NULL OR gelis_tarixi>=CAST(:from AS date))
                  AND (CAST(:to AS date) IS NULL OR gelis_tarixi<CAST(:to AS date)+interval '1 day')
                """);
        for (int i = 0; i < terms.length; i++) {
            sql.append(" AND position(lower(:term").append(i).append(") in lower(concat_ws(' ',l_kart,gelis_karti,ad,soyad,ata_adi)))>0");
            params.addValue("term" + i, terms[i]);
        }
        sql.append(" ORDER BY gelis_tarixi DESC,gelis_saati DESC,gelis_id DESC LIMIT 101 OFFSET :offset");
        return jdbc.queryForList(sql.toString(), params);
    }

    public List<Map<String, Object>> advanceRefundPatients(Long clinic, Long cash, String cardType, String query,
                                                     java.time.LocalDate from, java.time.LocalDate to, int page) {
        String[] terms = query == null || query.isBlank() ? new String[0] : query.trim().split("\\s+");
        var params = scope(clinic, cash).addValue("card", cardType).addValue("from", from).addValue("to", to).addValue("offset", (long) (page - 1) * 100);
        StringBuilder sql = new StringBuilder("""
                SELECT gelis_id, xeste_id, l_kart, kart_novu_adi, gelis_karti, ad, soyad, ata_adi,
                       dogum_tarixi, gelis_tarixi, gelis_saati
                FROM public.fn_kassa_avans_qaytarma_ucun_siyahisi(
                    p_klinika_id=>CAST(:clinic AS bigint), p_kassa_id=>CAST(:cash AS bigint),
                    p_kart_novu_kodu=>CAST(:card AS varchar))
                WHERE aktiv=true
                  AND (CAST(:from AS date) IS NULL OR gelis_tarixi>=CAST(:from AS date))
                  AND (CAST(:to AS date) IS NULL OR gelis_tarixi<CAST(:to AS date)+interval '1 day')
                """);
        for (int i = 0; i < terms.length; i++) {
            sql.append(" AND position(lower(:term").append(i).append(") in lower(concat_ws(' ',l_kart,gelis_karti,ad,soyad,ata_adi)))>0");
            params.addValue("term" + i, terms[i]);
        }
        sql.append(" ORDER BY gelis_tarixi DESC,gelis_saati DESC,gelis_id DESC LIMIT 101 OFFSET :offset");
        return jdbc.queryForList(sql.toString(), params);
    }

    public List<Map<String,Object>> refundableAdvances(Long clinic,Long cash,Long visit) {
        return jdbc.queryForList("""
            SELECT * FROM public.fn_kassa_xeste_avans_qaytarma_siyahisi(
                p_klinika_id=>CAST(:clinic AS bigint),p_kassa_id=>CAST(:cash AS bigint),p_gelis_id=>CAST(:visit AS bigint))
            """,scope(clinic,cash).addValue("visit",visit));
    }
    public Map<String,Object> refundAdvance(Long clinic,Long cash,Long personal,Long visit,Long advance,Long account,Long type,java.math.BigDecimal amount,String note) {
        return jdbc.queryForMap("""
            SELECT status_kodu,ugurlu,mesaj FROM public.fn_kassa_avansdan_qaytarma(
                p_klinika_id=>CAST(:clinic AS bigint),p_kassa_id=>CAST(:cash AS bigint),p_personal_id=>CAST(:personal AS bigint),
                p_gelis_id=>CAST(:visit AS bigint),p_avans_id=>CAST(:advance AS bigint),p_muhasibat_kodu_id=>CAST(:account AS bigint),
                p_odenis_novu_id=>CAST(:type AS bigint),p_mebleg=>CAST(:amount AS numeric),p_aciqlama=>CAST(:note AS text))
            """,scope(clinic,cash).addValue("personal",personal).addValue("visit",visit).addValue("advance",advance)
                .addValue("account",account).addValue("type",type).addValue("amount",amount).addValue("note",note));
    }
    public Map<String, Object> advanceVisit(Long clinic, Long cash, Long visit) {
        var rows = jdbc.queryForList("""
                SELECT gelis_id, xeste_id, l_kart, gelis_karti, ad, soyad, ata_adi, dogum_tarixi, gelis_tarixi
                FROM public.fn_xeste_avans_almaq_ucun_siyahisi(p_klinika_id=>CAST(:clinic AS bigint),
                    p_kassa_id=>CAST(:cash AS bigint),p_kart_novu_kodu=>CAST(NULL AS varchar))
                WHERE gelis_id=CAST(:visit AS bigint) AND aktiv=true
                """, scope(clinic, cash).addValue("visit", visit));
        return rows.isEmpty() ? Map.of() : rows.getFirst();
    }

    public Map<String, Object> advance(Long clinic, Long cash, Long personal, Long patient, Long visit,
                                       Long account, String payments, String note) {
        return jdbc.queryForMap("""
                SELECT status_kodu, ugurlu, mesaj FROM public.fn_kassa_xesteden_avans_qebul_et(
                    p_klinika_id=>CAST(:clinic AS bigint), p_kassa_id=>CAST(:cash AS bigint),
                    p_personal_id=>CAST(:personal AS bigint), p_xeste_id=>CAST(:patient AS bigint),
                    p_odenisler=>CAST(:payments AS jsonb), p_muhasibat_kodu_id=>CAST(:account AS bigint),
                    p_gelis_id=>CAST(:visit AS bigint), p_aciqlama=>CAST(:note AS text))
                """, scope(clinic, cash).addValue("personal", personal).addValue("patient", patient).addValue("visit", visit)
                .addValue("account", account).addValue("payments", payments).addValue("note", note));
    }

    public List<Map<String, Object>> refundPatients(Long clinic, Long cash, String card, String query, int page) {
        var params = scope(clinic, cash).addValue("card", card).addValue("offset", (long) (page - 1) * 100);
        StringBuilder sql = new StringBuilder("""
                SELECT gelis_id,xeste_id,l_kart,kart_novu_adi,gelis_karti,ad,soyad,ata_adi,dogum_tarixi,gelis_tarixi,gelis_saati
                FROM public.fn_kassa_xeste_qaytarma_ucun_siyahisi(
                    p_klinika_id=>CAST(:clinic AS bigint),p_kassa_id=>CAST(:cash AS bigint),p_kart_novu_kodu=>CAST(:card AS varchar))
                WHERE aktiv=true
                """);
        String[] terms = query == null || query.isBlank() ? new String[0] : query.trim().split("\\s+");
        for (int n = 0; n < terms.length; n++) {
            sql.append(" AND position(lower(:word").append(n).append(") in lower(concat_ws(' ',l_kart,gelis_karti,ad,soyad,ata_adi)))>0");
            params.addValue("word" + n, terms[n]);
        }
        sql.append(" ORDER BY gelis_tarixi DESC,gelis_saati DESC,gelis_id DESC LIMIT 101 OFFSET :offset");
        return jdbc.queryForList(sql.toString(), params);
    }

    public List<Map<String, Object>> refundServices(Long clinic, Long cash, Long visit) {
        return jdbc.queryForList("""
                SELECT * FROM public.fn_kassa_xeste_qaytarma_xidmetleri_siyahisi(
                    p_klinika_id=>CAST(:clinic AS bigint),p_kassa_id=>CAST(:cash AS bigint),p_gelis_id=>CAST(:visit AS bigint))
                """, scope(clinic, cash).addValue("visit", visit));
    }

    public Map<String, Object> refund(Long clinic, Long cash, Long personal, String protocol, Long account,
                                      String services, Long paymentType, String note) {
        return jdbc.queryForMap("""
                SELECT status_kodu,ugurlu,mesaj FROM public.fn_kassa_xesteye_qaytarma(
                    p_klinika_id=>CAST(:clinic AS bigint),p_kassa_id=>CAST(:cash AS bigint),p_personal_id=>CAST(:personal AS bigint),
                    p_protokol_kodu=>CAST(:protocol AS varchar),p_muhasibat_kodu_id=>CAST(:account AS bigint),
                    p_xidmetler=>CAST(:services AS jsonb),p_odenis_novu_id=>CAST(:paymentType AS bigint),p_aciqlama=>CAST(:note AS text))
                """, scope(clinic, cash).addValue("personal", personal).addValue("protocol", protocol).addValue("account", account)
                .addValue("services", services).addValue("paymentType", paymentType).addValue("note", note));
    }

    public List<Map<String, Object>> visits(Long clinic, Long cash) {
        return jdbc.queryForList("""
                SELECT * FROM public.fn_kassa_odenis_gozleyen_gelisler_siyahisi(
                    p_klinika_id=>CAST(:clinic AS bigint), p_kassa_id=>CAST(:cash AS bigint))
                """, scope(clinic, cash));
    }

    public List<Map<String, Object>> services(Long clinic, Long cash, Long visit) {
        return jdbc.queryForList("""
                SELECT * FROM public.fn_kassa_gelis_odenis_xidmetleri(
                    p_klinika_id=>CAST(:clinic AS bigint), p_kassa_id=>CAST(:cash AS bigint), p_gelis_id=>CAST(:target AS bigint))
                """, scope(clinic, cash).addValue("target", visit));
    }

    public List<Map<String, Object>> receipts(Long clinic, Long cash, Long visit) {
        return jdbc.queryForList("""
                SELECT * FROM public.fn_kassa_gelis_qebz_siyahisi(
                    p_klinika_id=>CAST(:clinic AS bigint), p_kassa_id=>CAST(:cash AS bigint), p_gelis_id=>CAST(:target AS bigint))
                """, scope(clinic, cash).addValue("target", visit));
    }

    public List<Map<String, Object>> debtors(Long clinic, Long cash) {
        return jdbc.queryForList("""
                SELECT * FROM public.fn_kassa_borclular_siyahisi(
                    p_klinika_id=>CAST(:clinic AS bigint), p_kassa_id=>CAST(:cash AS bigint))
                """, scope(clinic, cash));
    }

    public List<Map<String, Object>> debtServices(Long clinic, Long cash, Long patient) {
        // The list function returns the debt header ID, whereas payment needs the detail ID.
        // Resolve it by both header and patient-service IDs; never substitute the header ID.
        return jdbc.queryForList("""
                SELECT s.*, bx.id AS borclandirma_xidmet_id
                FROM public.fn_kassa_borclu_xidmetler_siyahisi(
                    p_klinika_id=>CAST(:clinic AS bigint), p_kassa_id=>CAST(:cash AS bigint), p_xeste_id=>CAST(:target AS bigint)) s
                JOIN public.rn_borclandirma_xidmetleri bx
                    ON bx.borclandirma_id=s.borclandirma_id AND bx.xeste_xidmet_id=s.xeste_xidmet_id AND bx.aktiv=true
                ORDER BY s.borclandirma_tarixi DESC, bx.id
                """, scope(clinic, cash).addValue("target", patient));
    }

    public boolean lockPatient(Long clinic, Long target, boolean debt) {
        // Serialize this application's payments for the same patient across sessions and cash desks.
        String patient = debt ? "CAST(:target AS bigint)" : """
                (SELECT xeste_id FROM public.rn_xeste_gelisleri
                 WHERE id=:target AND klinika_id=:clinic AND aktiv=true)
                """;
        return !jdbc.queryForList("SELECT id FROM public.rn_xesteler WHERE klinika_id=:clinic AND aktiv=true AND id="
                + patient + " FOR UPDATE", scope(clinic, null).addValue("target", target)).isEmpty();
    }

    public Map<String, Object> pay(Long clinic, Long cash, Long personal, Long target,
                                   boolean debt, String services, String payments, boolean borrow, String note) {
        var args = scope(clinic, cash).addValue("personal", personal).addValue("target", target)
                .addValue("services", services).addValue("payments", payments).addValue("borrow", borrow).addValue("note", note);
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
        return jdbc.queryForMap(sql, args);
    }
}
