package az.simplexs.simplexs.repository.personal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import az.simplexs.simplexs.dto.personal.PersonalListItem;
import az.simplexs.simplexs.dto.personal.PersonalKlinika;
import az.simplexs.simplexs.dto.personal.PersonalRol;

@Repository
public class PersonalRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public PersonalRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<PersonalListItem> find(Long klinikaId) {
        if (klinikaId == null) {
            return List.of();
        }
        return jdbc.query("""
                SELECT *
                FROM public.fn_personal_siyahisi(CAST(:kid AS bigint), CAST(:aktiv AS boolean))
                ORDER BY sira_no NULLS LAST, tam_ad
                """, new MapSqlParameterSource().addValue("kid", klinikaId).addValue("aktiv", null),
                (r, n) -> new PersonalListItem(l(r, "personal_klinika_id"), l(r, "personal_id"),
                        r.getString("personal_kodu"), l(r, "klinika_id"), r.getString("klinika_adi"),
                        l(r, "vezife_id"), r.getString("vezife_kodu"), r.getString("vezife_adi"),
                        r.getString("ad"), r.getString("soyad"), r.getString("ata_adi"), r.getString("tam_ad"),
                        r.getObject("hekimdir", Boolean.class), r.getString("mobil_nomre"),
                        r.getString("daxili_nomre"), r.getString("is_nomresi"), r.getString("email"),
                        r.getObject("personal_aktiv", Boolean.class),
                        r.getObject("klinika_elagesi_aktiv", Boolean.class), i(r, "sira_no"),
                        r.getObject("klinikaya_baglanma_tarixi", java.time.LocalDateTime.class),
                        r.getObject("personal_yaranma_tarixi", java.time.LocalDateTime.class)));
    }

    @Transactional
    public Map<String, Object> create(Long kid, Long vid, String ad, String soyad, String ataAdi,
            String mobilNomre, String daxiliNomre, String isNomresi, String email, boolean hekimdir,
            boolean aktiv, String sifre) {
        List<Map<String, Object>> created = jdbc.queryForList("""
                SELECT * FROM public.fn_personal_yarat(
                    p_klinika_id=>:kid, p_vezife_id=>:vid, p_ad=>:ad,
                    p_soyad=>:soyad, p_hekimdir=>:hekim, p_yaradan_personal_id=>NULL)
                """, new MapSqlParameterSource().addValue("kid", kid).addValue("vid", vid)
                .addValue("ad", ad.trim()).addValue("soyad", soyad.trim()).addValue("hekim", hekimdir));
        if (created.isEmpty() || !"UGURLU".equals(String.valueOf(created.getFirst().get("status_kodu")))) {
            return created.isEmpty()
                    ? Map.of("status_kodu", "SISTEM_XETASI", "mesaj", "Personal yaradıla bilmədi")
                    : created.getFirst();
        }
        Number personalId = (Number) created.getFirst().get("personal_id");
        return update(personalId.longValue(), vid, ad, soyad, ataAdi, mobilNomre, daxiliNomre,
                isNomresi, email, hekimdir, aktiv, sifre);
    }

    public Map<String, Object> update(Long personalId, Long vezifeId, String ad, String soyad, String ataAdi,
            String mobilNomre, String daxiliNomre, String isNomresi, String email, boolean hekimdir,
            boolean aktiv, String sifre) {
        String sql = """
                SELECT netice.*
                FROM public.rn_personallar movcud
                CROSS JOIN LATERAL public.fn_personal_yenile(
                    p_personal_id => movcud.id,
                    p_sexsiyyet_vesiqesi_nomresi => movcud.sexsiyyet_vesiqesi_nomresi,
                    p_fin_kodu => movcud.fin_kodu,
                    p_ad => :ad,
                    p_soyad => :soyad,
                    p_ata_adi => :ataAdi,
                    p_cins_id => movcud.cins_id,
                    p_dogum_tarixi => movcud.dogum_tarixi,
                    p_doguldugu_olke_id => movcud.doguldugu_olke_id,
                    p_doguldugu_seher_id => movcud.doguldugu_seher_id,
                    p_qan_qrupu_id => movcud.qan_qrupu_id,
                    p_mobil_nomre => :mobilNomre,
                    p_daxili_nomre => :daxiliNomre,
                    p_is_nomresi => :isNomresi,
                    p_email => :email,
                    p_unvan => movcud.unvan,
                    p_vezife_id => :vezifeId,
                    p_hekimdir => :hekimdir,
                    p_kart_id => movcud.kart_id,
                    p_barmaq_izi_id => movcud.barmaq_izi_id,
                    p_uz_tanima_id => movcud.uz_tanima_id,
                    p_sifre => :sifre,
                    p_sifre_deyisdirilmelidir => NULL,
                    p_sifre_deyise_bilmez => NULL,
                    p_hesab_kilidlidir => NULL,
                    p_muqavile_baslama_tarixi => movcud.muqavile_baslama_tarixi,
                    p_muqavile_bitme_tarixi => movcud.muqavile_bitme_tarixi,
                    p_isden_ayrilib => movcud.isden_ayrilib,
                    p_isden_ayrilma_tarixi => movcud.isden_ayrilma_tarixi,
                    p_aktiv => :aktiv,
                    p_yenileyen_personal_id => NULL
                ) netice
                WHERE movcud.id = :personalId
                """;
        var params = new MapSqlParameterSource()
                .addValue("personalId", personalId).addValue("vezifeId", vezifeId)
                .addValue("ad", ad).addValue("soyad", soyad).addValue("ataAdi", ataAdi)
                .addValue("mobilNomre", mobilNomre).addValue("daxiliNomre", daxiliNomre)
                .addValue("isNomresi", isNomresi).addValue("email", email)
                .addValue("hekimdir", hekimdir).addValue("aktiv", aktiv).addValue("sifre", sifre);
        List<Map<String, Object>> result = jdbc.queryForList(sql, params);
        return result.isEmpty()
                ? Map.of("status_kodu", "PERSONAL_TAPILMADI", "mesaj", "Personal tapılmadı")
                : result.getFirst();
    }

    public void role(Long personalKlinikaId, Long rid, boolean add, boolean primary) {
        jdbc.queryForList("""
                SELECT * FROM public.fn_personal_klinika_rol_yenile(
                    p_personal_klinika_id=>:pkid, p_rol_id=>:rid, p_elave_edilsin=>:add,
                    p_esas_rol=>:primary, p_emel_eden_personal_id=>NULL)
                """, new MapSqlParameterSource().addValue("pkid", personalKlinikaId).addValue("rid", rid)
                .addValue("add", add).addValue("primary", primary));
    }

    public Map<String, Object> clinic(Long personalId, Long klinikaId, boolean add) {
        List<Map<String, Object>> result = jdbc.queryForList("""
                SELECT * FROM public.fn_personal_klinika_yenile(
                    p_personal_id=>:personalId, p_klinika_id=>:klinikaId,
                    p_elave_edilsin=>:add, p_emel_eden_personal_id=>NULL)
                """, new MapSqlParameterSource().addValue("personalId", personalId)
                .addValue("klinikaId", klinikaId).addValue("add", add));
        return result.isEmpty()
                ? Map.of("status_kodu", "SISTEM_XETASI", "mesaj", "Klinika icazəsi dəyişdirilə bilmədi")
                : result.getFirst();
    }

    public Map<Long, List<PersonalKlinika>> clinicsByPersonal(Collection<Long> personalIds,
            Collection<Long> manageableClinicIds) {
        if (personalIds == null || personalIds.isEmpty() || manageableClinicIds == null
                || manageableClinicIds.isEmpty()) {
            return Map.of();
        }
        List<PersonalKlinika> rows = jdbc.query("""
                SELECT p.id AS personal_id, pk.id AS personal_klinika_id,
                       k.id AS klinika_id, k.ad AS klinika_adi,
                       COALESCE(pk.aktiv, false) AS aktiv
                FROM public.rn_personallar p
                CROSS JOIN public.rn_klinikalar k
                LEFT JOIN public.rn_personal_klinikalar pk
                       ON pk.personal_id=p.id AND pk.klinika_id=k.id
                WHERE p.id IN (:personalIds) AND k.id IN (:clinicIds) AND k.aktiv=true
                ORDER BY p.id, k.sira_no NULLS LAST, k.ad, k.id
                """, new MapSqlParameterSource("personalIds", personalIds)
                .addValue("clinicIds", manageableClinicIds),
                (rs, n) -> new PersonalKlinika(l(rs, "personal_id"), l(rs, "personal_klinika_id"),
                        l(rs, "klinika_id"), rs.getString("klinika_adi"),
                        rs.getObject("aktiv", Boolean.class)));
        Map<Long, List<PersonalKlinika>> result = new LinkedHashMap<>();
        for (PersonalKlinika row : rows) {
            result.computeIfAbsent(row.personalId(), key -> new ArrayList<>()).add(row);
        }
        return result;
    }

    public Map<Long, List<PersonalRol>> rolesByPersonal(Collection<Long> personalIds, Long klinikaId) {
        if (personalIds == null || personalIds.isEmpty() || klinikaId == null) {
            return Map.of();
        }
        List<PersonalRol> rows = jdbc.query("""
                SELECT pk.personal_id, pr.rol_id, r.ad AS rol_adi, r.aciqlama, pr.esas_rol
                FROM public.rn_personal_klinika_rollari pr
                JOIN public.rn_personal_klinikalar pk ON pk.id=pr.personal_klinika_id
                JOIN public.rn_rollar r ON r.id=pr.rol_id
                WHERE pk.personal_id IN (:ids) AND pk.klinika_id=:klinikaId
                      AND pk.aktiv=true AND pr.aktiv=true AND r.aktiv=true
                ORDER BY pk.personal_id, pr.esas_rol DESC, r.sira_no NULLS LAST, r.ad
                """, new MapSqlParameterSource("ids", personalIds).addValue("klinikaId", klinikaId),
                (rs, n) -> new PersonalRol(l(rs, "personal_id"), l(rs, "rol_id"), rs.getString("rol_adi"),
                        rs.getString("aciqlama"), rs.getObject("esas_rol", Boolean.class)));
        Map<Long, List<PersonalRol>> result = new LinkedHashMap<>();
        for (PersonalRol row : rows) {
            result.computeIfAbsent(row.personalId(), key -> new ArrayList<>()).add(row);
        }
        return result;
    }

    private static Long l(java.sql.ResultSet r, String c) throws java.sql.SQLException {
        Object x = r.getObject(c);
        return x instanceof Number n ? n.longValue() : null;
    }

    private static Integer i(java.sql.ResultSet r, String c) throws java.sql.SQLException {
        Object x = r.getObject(c);
        return x instanceof Number n ? n.intValue() : null;
    }
}
