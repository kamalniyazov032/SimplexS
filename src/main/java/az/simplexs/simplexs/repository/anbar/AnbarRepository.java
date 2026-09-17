package az.simplexs.simplexs.repository.anbar;

import static az.simplexs.simplexs.dto.anbar.AnbarModels.*;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AnbarRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public AnbarRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private MapSqlParameterSource p() {
        return new MapSqlParameterSource();
    }

    private List<Map<String, Object>> call(String sql, MapSqlParameterSource p) {
        return jdbc.queryForList(sql, p);
    }

    private Map<String, Object> one(String sql, MapSqlParameterSource p) {
        var rows = call(sql, p);
        return rows.isEmpty() ? Map.of() : rows.getFirst();
    }

    private static String n(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    public List<Firma> firmalar(Long klinikaId, Boolean aktiv) {
        return jdbc.query(
                "SELECT * FROM public.fn_firma_siyahisi(" +
                        "p_klinika_id=>:klinikaId, " +
                        "p_aktiv=>CAST(:a AS boolean)) ORDER BY ad",
                p()
                        .addValue("klinikaId", klinikaId)
                        .addValue("a", aktiv),
                (r, i) -> new Firma(
                        l(r, "firma_id"),
                        s(r, "ad"),
                        s(r, "unvan"),
                        s(r, "is_telefonu"),
                        s(r, "faks_nomresi"),
                        s(r, "email"),
                        s(r, "qeyd"),
                        s(r, "bank_adi"),
                        s(r, "bank_hesab_nomresi"),
                        s(r, "vergi_nomresi"),
                        s(r, "vergi_idaresi"),
                        b(r, "aktiv"),
                        dt(r, "yaranma_tarixi"),
                        dt(r, "yenilenme_tarixi")
                )
        );
    }
    public Map<String, Object> firmaYarat(Long k, String ad, String unvan, String telefon, String faks, String email, String qeyd, String bank, String hesab, String vergi, String idare, Long personal) {
        return one("SELECT * FROM public.fn_firma_yarat(p_klinika_id=>CAST(:k AS bigint),p_ad=>:ad,p_unvan=>:u,p_is_telefonu=>:t,p_faks_nomresi=>:f,p_email=>:e,p_qeyd=>:q,p_bank_adi=>:b,p_bank_hesab_nomresi=>:h,p_vergi_nomresi=>:v,p_vergi_idaresi=>:i,p_yaradan_personal_id=>CAST(:p AS bigint))", p().addValue("k", k).addValue("ad", ad.trim()).addValue("u", n(unvan)).addValue("t", n(telefon)).addValue("f", n(faks)).addValue("e", n(email)).addValue("q", n(qeyd)).addValue("b", n(bank)).addValue("h", n(hesab)).addValue("v", n(vergi)).addValue("i", n(idare)).addValue("p", personal));
    }

    public Map<String, Object> firmaYenile(Long k, Long id, String ad, String unvan, String telefon, String faks, String email, String qeyd, String bank, String hesab, String vergi, String idare, boolean aktiv, Long personal) {
        return one("SELECT * FROM public.fn_firma_yenile(p_klinika_id=>CAST(:k AS bigint),p_firma_id=>CAST(:id AS bigint),p_ad=>:ad,p_unvan=>:u,p_unvan_deyisdirilsin=>true,p_is_telefonu=>:t,p_is_telefonu_deyisdirilsin=>true,p_faks_nomresi=>:f,p_faks_nomresi_deyisdirilsin=>true,p_email=>:e,p_email_deyisdirilsin=>true,p_qeyd=>:q,p_qeyd_deyisdirilsin=>true,p_bank_adi=>:b,p_bank_adi_deyisdirilsin=>true,p_bank_hesab_nomresi=>:h,p_bank_hesab_nomresi_deyisdirilsin=>true,p_vergi_nomresi=>:v,p_vergi_nomresi_deyisdirilsin=>true,p_vergi_idaresi=>:i,p_vergi_idaresi_deyisdirilsin=>true,p_aktiv=>:a,p_yenileyen_personal_id=>CAST(:p AS bigint))", p().addValue("k", k).addValue("id", id).addValue("ad", n(ad)).addValue("u", n(unvan)).addValue("t", n(telefon)).addValue("f", n(faks)).addValue("e", n(email)).addValue("q", n(qeyd)).addValue("b", n(bank)).addValue("h", n(hesab)).addValue("v", n(vergi)).addValue("i", n(idare)).addValue("a", aktiv).addValue("p", personal));
    }

    public List<Vahid> vahidler(Long klinikaId, Boolean aktiv, Boolean sifarisdeGorunsun) {
        return jdbc.query(
                "SELECT * FROM public.fn_vahid_siyahisi(" +
                        "p_klinika_id=>:klinikaId, " +
                        "p_aktiv=>CAST(:a AS boolean), " +
                        "p_sifarisde_gorunsun=>CAST(:s AS boolean)) " +
                        "ORDER BY sira_no NULLS LAST, ad",
                p()
                        .addValue("klinikaId", klinikaId)
                        .addValue("a", aktiv)
                        .addValue("s", sifarisdeGorunsun),
                (r, i) -> new Vahid(
                        l(r, "vahid_id"),
                        s(r, "ad"),
                        l(r, "alt_vahid_id"),
                        s(r, "alt_vahid_adi"),
                        d(r, "vurma_emsali"),
                        in(r, "sira_no"),
                        b(r, "sifarisde_gorunsun"),
                        b(r, "aktiv"),
                        dt(r, "yaranma_tarixi"),
                        dt(r, "yenilenme_tarixi")
                )
        );
    }

    public Map<String, Object> vahidYarat(Long k, String ad, Long alt, java.math.BigDecimal emsal, boolean sifaris, Long personal) {
        return one("SELECT * FROM public.fn_vahid_yarat(p_klinika_id=>CAST(:k AS bigint),p_ad=>:ad,p_alt_vahid_id=>CAST(:alt AS bigint),p_vurma_emsali=>CAST(:em AS numeric),p_sifarisde_gorunsun=>:s,p_yaradan_personal_id=>CAST(:p AS bigint))", p().addValue("k", k).addValue("ad", ad.trim()).addValue("alt", alt).addValue("em", emsal).addValue("s", sifaris).addValue("p", personal));
    }

    public Map<String, Object> vahidYenile(Long k, Long id, String ad, Long alt, java.math.BigDecimal emsal, boolean sifaris, boolean aktiv, Long personal) {
        return one("SELECT * FROM public.fn_vahid_yenile(p_klinika_id=>CAST(:k AS bigint),p_vahid_id=>CAST(:id AS bigint),p_ad=>:ad,p_alt_vahid_id=>CAST(:alt AS bigint),p_alt_vahid_deyisdirilsin=>true,p_vurma_emsali=>CAST(:em AS numeric),p_vurma_emsali_deyisdirilsin=>true,p_sifarisde_gorunsun=>:s,p_aktiv=>:a,p_yenileyen_personal_id=>CAST(:p AS bigint))", p().addValue("k", k).addValue("id", id).addValue("ad", ad.trim()).addValue("alt", alt).addValue("em", emsal).addValue("s", sifaris).addValue("a", aktiv).addValue("p", personal));
    }

    public List<QrupNovu> qrupNovleri() {
        return jdbc.query("SELECT * FROM public.fn_mehsul_qrup_novu_siyahisi() ORDER BY sira_no NULLS LAST,ad", (r, i) -> new QrupNovu(l(r, "qrup_novu_id"), s(r, "kod"), s(r, "ad"), s(r, "aciqlama"), in(r, "sira_no"), b(r, "aktiv")));
    }

    public List<MehsulQrupu> qruplar(Long k, Boolean aktiv) {
        return jdbc.query("SELECT * FROM public.fn_mehsul_qrup_siyahisi(p_klinika_id=>CAST(:k AS bigint),p_aktiv=>CAST(:a AS boolean)) ORDER BY sira_no NULLS LAST,ad", p().addValue("k", k).addValue("a", aktiv), (r, i) -> new MehsulQrupu(l(r, "mehsul_qrupu_id"), null, null, s(r, "ad"), s(r, "aciqlama"), b(r, "xesteye_cixis_edile_biler"), b(r, "sifarisde_gorunsun"), b(r, "xeste_material_cixisinda_gorunsun"), b(r, "cihaza_cixis_edile_biler"), b(r, "aktiv")));
    }

    public Map<String, Object> qrupYaddaSaxla(boolean yeni, Long k, Long id, Long nov, String ad, String ac, boolean xeste, boolean sifaris, boolean material, boolean cihaz, boolean aktiv, Long personal) {
        String sql = yeni ? "SELECT * FROM public.fn_mehsul_qrupu_yarat(p_klinika_id=>CAST(:k AS bigint),p_qrup_novu_id=>CAST(:n AS bigint),p_ad=>:ad,p_aciqlama=>:ac,p_xesteye_cixis_edile_biler=>:x,p_sifarisde_gorunsun=>:s,p_xeste_material_cixisinda_gorunsun=>:m,p_cihaza_cixis_edile_biler=>:c,p_yaradan_personal_id=>CAST(:p AS bigint))" : "SELECT * FROM public.fn_mehsul_qrupu_yenile(p_mehsul_qrupu_id=>CAST(:id AS bigint),p_klinika_id=>CAST(:k AS bigint),p_qrup_novu_id=>CAST(:n AS bigint),p_ad=>:ad,p_aciqlama=>:ac,p_aciqlama_deyisdirilsin=>true,p_xesteye_cixis_edile_biler=>:x,p_sifarisde_gorunsun=>:s,p_xeste_material_cixisinda_gorunsun=>:m,p_cihaza_cixis_edile_biler=>:c,p_aktiv=>:a,p_yenileyen_personal_id=>CAST(:p AS bigint))";
        return one(sql, p().addValue("id", id).addValue("k", k).addValue("n", nov).addValue("ad", ad.trim()).addValue("ac", n(ac)).addValue("x", xeste).addValue("s", sifaris).addValue("m", material).addValue("c", cihaz).addValue("a", aktiv).addValue("p", personal));
    }

    public List<Material> materiallar(Long k, Long qrup, Boolean aktiv, String axtaris) {
        return jdbc.query("SELECT * FROM public.fn_material_siyahisi(p_klinika_id=>CAST(:k AS bigint),p_qrup_id=>CAST(:q AS bigint),p_aktiv=>CAST(:a AS boolean),p_axtaris=>:x) ORDER BY sira_no NULLS LAST,ad", p().addValue("k", k).addValue("q", qrup).addValue("a", aktiv).addValue("x", n(axtaris)), (r, i) -> new Material(l(r, "material_id"), l(r, "qrup_id"), null, s(r, "qrup_adi"), l(r, "ana_vahid_id"), s(r, "ana_vahid_adi"), s(r, "ad"), s(r, "qisa_ad"), s(r, "barkod_nomresi"), d(r, "minimum_miqdar"), d(r, "maksimum_miqdar"), null, null, null, b(r, "mehv_edile_biler"), b(r, "paketden_kenar"), b(r, "aktiv"), null, null, null, null, null, null, null, null, null, null, b(r, "vahid_deyisdirile_biler")));
    }

    public Map<String, Object> materialYaddaSaxla(boolean yeni, Long id, Long k, Long q, Long v, String ad, String qisa, String barkod, java.math.BigDecimal min, java.math.BigDecimal max, boolean mehv, boolean paket, boolean aktiv, String farm, String istifade, Map<String,String> fields, Long personal) {
        var p = p().addValue("id", id).addValue("k", k).addValue("q", q).addValue("v", v).addValue("ad", ad.trim()).addValue("qa", n(qisa)).addValue("b", n(barkod)).addValue("min", min).addValue("max", max).addValue("mehv", mehv).addValue("paket", paket).addValue("a", aktiv).addValue("farm", n(farm)).addValue("ist", n(istifade)).addValue("p", personal);
        p.addValue("minimum_doza", n(fields.get("minimum_doza")));
        p.addValue("maksimum_doza", n(fields.get("maksimum_doza")));
        p.addValue("xeste_ucun_maksimum_teleb_muddeti", n(fields.get("xeste_ucun_maksimum_teleb_muddeti")));
        p.addValue("xeste_ucun_maksimum_teleb_miqdari", n(fields.get("xeste_ucun_maksimum_teleb_miqdari")));
        p.addValue("yas_asagi_heddi", n(fields.get("yas_asagi_heddi")));
        p.addValue("yas_yuxari_heddi", n(fields.get("yas_yuxari_heddi")));
        p.addValue("beden_cekisi", n(fields.get("beden_cekisi")));
        p.addValue("clinical", yeni || "true".equals(fields.get("farmasevtikDeyisdirilsin")));
        p.addValue("auto", "true".equals(fields.get("avtomatik_hekim_tesdiqi_olmasin")));
        String sql = yeni ? "SELECT * FROM public.fn_material_yarat(p_klinika_id=>CAST(:k AS bigint),p_qrup_id=>CAST(:q AS bigint),p_ana_vahid_id=>CAST(:v AS bigint),p_ad=>:ad,p_qisa_ad=>:qa,p_barkod_nomresi=>:b,p_minimum_miqdar=>CAST(:min AS numeric),p_maksimum_miqdar=>CAST(:max AS numeric),p_mehv_edile_biler=>:mehv,p_paketden_kenar=>:paket,p_farmasevtik_melumat=>:farm,p_istifade_qaydasi=>:ist,p_minimum_doza=>CAST(:minimum_doza AS numeric),p_maksimum_doza=>CAST(:maksimum_doza AS numeric),p_xeste_ucun_maksimum_teleb_muddeti=>CAST(:xeste_ucun_maksimum_teleb_muddeti AS integer),p_xeste_ucun_maksimum_teleb_miqdari=>CAST(:xeste_ucun_maksimum_teleb_miqdari AS numeric),p_yas_asagi_heddi=>CAST(:yas_asagi_heddi AS integer),p_yas_yuxari_heddi=>CAST(:yas_yuxari_heddi AS integer),p_beden_cekisi=>CAST(:beden_cekisi AS numeric),p_avtomatik_hekim_tesdiqi_olmasin=>:auto,p_yaradan_personal_id=>CAST(:p AS bigint))" : "SELECT * FROM public.fn_material_yenile(p_material_id=>CAST(:id AS bigint),p_klinika_id=>CAST(:k AS bigint),p_qrup_id=>CAST(:q AS bigint),p_ana_vahid_id=>CAST(:v AS bigint),p_ad=>:ad,p_qisa_ad=>:qa,p_qisa_ad_deyisdirilsin=>true,p_barkod_nomresi=>:b,p_barkod_deyisdirilsin=>true,p_minimum_miqdar=>CAST(:min AS numeric),p_minimum_miqdar_deyisdirilsin=>true,p_maksimum_miqdar=>CAST(:max AS numeric),p_maksimum_miqdar_deyisdirilsin=>true,p_mehv_edile_biler=>:mehv,p_paketden_kenar=>:paket,p_aktiv=>:a,p_farmasevtik_deyisdirilsin=>CAST(:clinical AS boolean),p_farmasevtik_melumat=>:farm,p_istifade_qaydasi=>:ist,p_minimum_doza=>CAST(:minimum_doza AS numeric),p_maksimum_doza=>CAST(:maksimum_doza AS numeric),p_xeste_ucun_maksimum_teleb_muddeti=>CAST(:xeste_ucun_maksimum_teleb_muddeti AS integer),p_xeste_ucun_maksimum_teleb_miqdari=>CAST(:xeste_ucun_maksimum_teleb_miqdari AS numeric),p_yas_asagi_heddi=>CAST(:yas_asagi_heddi AS integer),p_yas_yuxari_heddi=>CAST(:yas_yuxari_heddi AS integer),p_beden_cekisi=>CAST(:beden_cekisi AS numeric),p_avtomatik_hekim_tesdiqi_olmasin=>:auto,p_yenileyen_personal_id=>CAST(:p AS bigint))";
        return one(sql, p);
    }

    public List<EmeliyyatKateqoriyasi> kateqoriyalar(String istiqamet) {
        return jdbc.query("SELECT * FROM public.fn_anbar_emeliyyat_kateqoriya_siyahisi(p_istiqamet=>:i) ORDER BY sira_no NULLS LAST,ad", p().addValue("i", istiqamet), (r, i) -> new EmeliyyatKateqoriyasi(l(r, "kateqoriya_id"), null, s(r, "ad"), s(r, "istiqamet"), s(r, "aciqlama"), in(r, "sira_no"), b(r, "aktiv")));
    }

    public List<EmeliyyatNovu> emeliyyatlar(Long k, String istiqamet, Boolean aktiv) {
        return jdbc.query("SELECT * FROM public.fn_anbar_emeliyyat_novu_siyahisi(p_klinika_id=>CAST(:k AS bigint),p_istiqamet=>:i,p_aktiv=>CAST(:a AS boolean)) ORDER BY sira_no NULLS LAST,ad", p().addValue("k", k).addValue("i", istiqamet).addValue("a", aktiv), (r, i) -> new EmeliyyatNovu(l(r, "emeliyyat_novu_id"), l(r, "kateqoriya_id"), null, s(r, "kateqoriya_adi"), s(r, "istiqamet"), null, s(r, "ad"), b(r, "standartdir"), s(r, "aciqlama"), in(r, "sira_no"), b(r, "aktiv")));
    }

    public Map<String, Object> emeliyyatYaddaSaxla(boolean yeni, Long id, Long k, Long kat, String ad, boolean standart, String ac, boolean aktiv, Long personal) {
        String sql = yeni ? "SELECT * FROM public.fn_anbar_emeliyyat_novu_yarat(p_klinika_id=>CAST(:k AS bigint),p_emeliyyat_kateqoriyasi_id=>CAST(:kat AS bigint),p_ad=>:ad,p_standartdir=>:s,p_aciqlama=>:ac,p_yaradan_personal_id=>CAST(:p AS bigint))" : "SELECT * FROM public.fn_anbar_emeliyyat_novu_yenile(p_emeliyyat_novu_id=>CAST(:id AS bigint),p_klinika_id=>CAST(:k AS bigint),p_emeliyyat_kateqoriyasi_id=>CAST(:kat AS bigint),p_ad=>:ad,p_standartdir=>:s,p_aciqlama=>:ac,p_aciqlama_deyisdirilsin=>true,p_aktiv=>:a,p_yenileyen_personal_id=>CAST(:p AS bigint))";
        return one(sql, p().addValue("id", id).addValue("k", k).addValue("kat", kat).addValue("ad", ad.trim()).addValue("s", standart).addValue("ac", n(ac)).addValue("a", aktiv).addValue("p", personal));
    }

    public List<AnbarNovu> anbarNovleri() {
        return jdbc.query("SELECT * FROM public.fn_anbar_novu_siyahisi() ORDER BY sira_no NULLS LAST,ad", (r, i) -> new AnbarNovu(l(r, "anbar_novu_id"), null, s(r, "ad"), s(r, "aciqlama"), in(r, "sira_no"), b(r, "aktiv")));
    }

    public List<Anbar> anbarlar(Long k, Long nov, Boolean aktiv, String axtaris) {
        return jdbc.query("SELECT * FROM public.fn_anbar_siyahisi(p_klinika_id=>CAST(:k AS bigint),p_anbar_novu_id=>CAST(:n AS bigint),p_aktiv=>CAST(:a AS boolean),p_axtaris=>:x) ORDER BY sira_no NULLS LAST,ad", p().addValue("k", k).addValue("n", nov).addValue("a", aktiv).addValue("x", n(axtaris)), (r, i) -> new Anbar(l(r, "anbar_id"), l(r, "anbar_novu_id"), null, s(r, "anbar_novu_adi"), null, s(r, "ad"), s(r, "aciqlama"), b(r, "teleb_anbaridir"), b(r, "telebde_stok_gorunsun"), b(r, "derman_paketi_tetbiq_edilsin"), b(r, "istehsal_cixisi_edile_biler"), b(r, "mehv_cixisi_edile_biler"), null, null, null, null, null, in(r, "sira_no"), b(r, "aktiv"), r.getObject("istifadeye_baslama_tarixi", java.time.LocalDate.class), s(r, "cixis_usulu"), s(r, "effektiv_cixis_usulu")));
    }

    public Map<String, Object> anbarYaddaSaxla(boolean yeni, Long id, Long k, Long nov, String ad, String ac, boolean teleb, boolean stok, boolean paket, boolean istehsal, boolean mehv, boolean aktiv, java.time.LocalDate baslama, String cixis, Long personal) {
        String sql = yeni ? "SELECT * FROM public.fn_anbar_yarat(p_klinika_id=>CAST(:k AS bigint),p_anbar_novu_id=>CAST(:n AS bigint),p_ad=>:ad,p_istifadeye_baslama_tarixi=>CAST(:baslama AS date),p_aciqlama=>:ac,p_teleb_anbaridir=>:t,p_telebde_stok_gorunsun=>:s,p_derman_paketi_tetbiq_edilsin=>:pak,p_istehsal_cixisi_edile_biler=>:i,p_mehv_cixisi_edile_biler=>:m,p_cixis_usulu=>:cixis,p_yaradan_personal_id=>CAST(:p AS bigint))" : "SELECT * FROM public.fn_anbar_yenile(p_anbar_id=>CAST(:id AS bigint),p_klinika_id=>CAST(:k AS bigint),p_anbar_novu_id=>CAST(:n AS bigint),p_ad=>:ad,p_aciqlama=>:ac,p_aciqlama_deyisdirilsin=>true,p_teleb_anbaridir=>:t,p_telebde_stok_gorunsun=>:s,p_derman_paketi_tetbiq_edilsin=>:pak,p_istehsal_cixisi_edile_biler=>:i,p_mehv_cixisi_edile_biler=>:m,p_cixis_usulu=>:cixis,p_cixis_usulu_deyisdirilsin=>true,p_istifadeye_baslama_tarixi=>CAST(:baslama AS date),p_istifadeye_baslama_tarixi_deyisdirilsin=>true,p_aktiv=>:a,p_yenileyen_personal_id=>CAST(:p AS bigint))";
        return one(sql, p().addValue("id", id).addValue("k", k).addValue("n", nov).addValue("baslama", baslama).addValue("cixis", n(cixis)).addValue("ad", ad.trim()).addValue("ac", n(ac)).addValue("t", teleb).addValue("s", stok).addValue("pak", paket).addValue("i", istehsal).addValue("m", mehv).addValue("a", aktiv).addValue("p", personal));
    }

    private static String s(ResultSet r, String c) throws SQLException {
        return r.getString(c);
    }

    private static Long l(ResultSet r, String c) throws SQLException {
        Object o = r.getObject(c);
        return o instanceof Number x ? x.longValue() : null;
    }

    private static Integer in(ResultSet r, String c) throws SQLException {
        Object o = r.getObject(c);
        return o instanceof Number x ? x.intValue() : null;
    }

    private static java.math.BigDecimal d(ResultSet r, String c) throws SQLException {
        return r.getBigDecimal(c);
    }

    private static Boolean b(ResultSet r, String c) throws SQLException {
        return r.getObject(c, Boolean.class);
    }

    private static java.time.LocalDateTime dt(ResultSet r, String c) throws SQLException {
        return r.getObject(c, java.time.LocalDateTime.class);
    }
}
