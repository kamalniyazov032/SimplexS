package az.simplexs.simplexs.repository.ambulator;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import az.simplexs.simplexs.dto.ambulator.AmbulatorLookups;
import az.simplexs.simplexs.dto.ambulator.LookupOption;
import az.simplexs.simplexs.dto.ambulator.PatientDocumentFilter;
import az.simplexs.simplexs.dto.ambulator.PatientDocumentForm;
import az.simplexs.simplexs.dto.ambulator.PatientDocumentListItem;
import az.simplexs.simplexs.dto.ambulator.Gelis;
import az.simplexs.simplexs.dto.ambulator.GelisForm;
import az.simplexs.simplexs.dto.ambulator.GelisOption;

@Repository
public class AmbulatorRepository {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public AmbulatorRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<GelisOption> xesteAxtar(Long klinika,Boolean aktiv,String q,Long cursor,int limit){return jdbcTemplate.query("SELECT xeste_id id,xeste_kodu kod,concat_ws(' ',ad,soyad,ata_adi) ad,concat_ws(' · ',fin_kodu,mobil_nomre,sexsiyyet_vesiqesi_nomresi) meta,default_teskilat_id FROM public.fn_xeste_siyahisi_sehifeli(CAST(:k AS bigint),CAST(:a AS boolean),CAST(:q AS varchar),NULL,CAST(:cursor AS bigint),:limit)",new MapSqlParameterSource("k",klinika).addValue("a",aktiv).addValue("q",blankToNull(q)).addValue("cursor",cursor).addValue("limit",limit),(r,n)->new GelisOption(toLong(r.getObject("id")),r.getString("kod"),r.getString("ad"),r.getString("meta"),toLong(r.getObject("default_teskilat_id"))));}
    public GelisOption xeste(Long klinika,Long id){return jdbcTemplate.query("SELECT xeste_id id,xeste_kodu kod,concat_ws(' ',ad,soyad,ata_adi) ad,concat_ws(' · ',fin_kodu,mobil_nomre,sexsiyyet_vesiqesi_nomresi) meta,default_teskilat_id FROM public.fn_xeste_siyahisi(CAST(:k AS bigint),NULL,NULL) WHERE xeste_id=:id",new MapSqlParameterSource("k",klinika).addValue("id",id),(r,n)->new GelisOption(toLong(r.getObject("id")),r.getString("kod"),r.getString("ad"),r.getString("meta"),toLong(r.getObject("default_teskilat_id")))).stream().findFirst().orElseThrow();}
    public List<GelisOption> gelisNovleri(){return jdbcTemplate.query("SELECT id,kod,ad,NULL::text meta FROM public.rn_xeste_gelis_novleri WHERE aktiv ORDER BY id",(r,n)->new GelisOption(toLong(r.getObject("id")),r.getString("kod"),r.getString("ad"),null,null));}
    public List<GelisOption> teskilatlar(Long klinika){return jdbcTemplate.query("SELECT teskilat_id id,teskilat_tipi_kodu kod,ad,NULL::text meta FROM public.fn_teskilat_siyahisi(CAST(:k AS bigint),true) ORDER BY ad",new MapSqlParameterSource("k",klinika),(r,n)->new GelisOption(toLong(r.getObject("id")),r.getString("kod"),r.getString("ad"),null,null));}
    public List<GelisOption> hekimler(Long klinika){return jdbcTemplate.query("SELECT personal_id id,personal_kodu kod,tam_ad ad,vezife_adi meta FROM public.fn_personal_siyahisi(CAST(:k AS bigint),true) WHERE hekimdir ORDER BY tam_ad",new MapSqlParameterSource("k",klinika),(r,n)->new GelisOption(toLong(r.getObject("id")),r.getString("kod"),r.getString("ad"),r.getString("meta"),null));}
    public List<Gelis> gelisler(Long klinika,Long xeste,Long nov,Long teskilat,java.time.LocalDate baslama,java.time.LocalDate bitme,Boolean randevu,Boolean aktiv,String q,Long cursor,int limit){return jdbcTemplate.query("SELECT * FROM public.fn_xeste_gelisi_siyahisi(CAST(:k AS bigint),CAST(:x AS bigint),CAST(:n AS bigint),CAST(:t AS bigint),CAST(:b AS date),CAST(:bt AS date),CAST(:r AS boolean),CAST(:a AS boolean),CAST(:q AS varchar),CAST(:cursor AS bigint),:limit)",new MapSqlParameterSource("k",klinika).addValue("x",xeste).addValue("n",nov).addValue("t",teskilat).addValue("b",baslama).addValue("bt",bitme).addValue("r",randevu).addValue("a",aktiv).addValue("q",blankToNull(q)).addValue("cursor",cursor).addValue("limit",limit),this::mapGelis);}
    public Gelis gelis(Long klinika,Long id){return jdbcTemplate.query("SELECT * FROM public.fn_xeste_gelisi_siyahisi(CAST(:k AS bigint),NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL) WHERE gelis_id=:id",new MapSqlParameterSource("k",klinika).addValue("id",id),this::mapGelis).stream().findFirst().orElseThrow();}
    public Map<String,Object> gelisYarat(Long klinika,GelisForm f,Long personal){return one("SELECT * FROM public.fn_xeste_gelisi_yarat(p_klinika_id=>:k,p_xeste_id=>:x,p_gelis_novu_id=>:n,p_teskilat_id=>:t,p_gelis_tarixi=>:d,p_gelis_saati=>:s,p_randevudur=>:r,p_gonderen_hekim_id=>:h,p_mesaj=>:m,p_aciqlama=>:ac,p_yaradan_personal_id=>:p)",gelisParams(f).addValue("k",klinika).addValue("x",f.xesteId).addValue("p",personal));}
    public Map<String,Object> gelisYenile(Long klinika,GelisForm f,Long personal){return one("SELECT * FROM public.fn_xeste_gelisi_yenile(p_gelis_id=>:id,p_klinika_id=>:k,p_gelis_novu_id=>:n,p_teskilat_id=>:t,p_gelis_tarixi=>:d,p_gelis_saati=>:s,p_randevudur=>:r,p_gonderen_hekim_id=>:h,p_gonderen_hekim_deyisdirilsin=>true,p_mesaj=>:m,p_mesaj_deyisdirilsin=>true,p_aciqlama=>:ac,p_aciqlama_deyisdirilsin=>true,p_aktiv=>:a,p_yenileyen_personal_id=>:p)",gelisParams(f).addValue("id",f.gelisId).addValue("k",klinika).addValue("a",f.aktiv).addValue("p",personal));}
    private MapSqlParameterSource gelisParams(GelisForm f){return new MapSqlParameterSource("n",f.gelisNovuId).addValue("t",f.teskilatId).addValue("d",f.gelisTarixi).addValue("s",f.gelisSaati).addValue("r",f.randevudur).addValue("h",f.gonderenHekimId).addValue("m",blankToNull(f.mesaj)).addValue("ac",blankToNull(f.aciqlama));}
    private Map<String,Object> one(String sql,MapSqlParameterSource p){var rows=jdbcTemplate.queryForList(sql,p);return rows.isEmpty()?Map.of():rows.getFirst();}
    private Gelis mapGelis(ResultSet r,int n)throws SQLException{return new Gelis(toLong(r.getObject("gelis_id")),toLong(r.getObject("xeste_id")),r.getString("xeste_kodu"),r.getString("xeste_ad"),r.getString("xeste_soyad"),r.getString("xeste_ata_adi"),r.getString("fin_kodu"),r.getString("sexsiyyet_vesiqesi_nomresi"),r.getString("mobil_nomre"),toLong(r.getObject("gelis_novu_id")),r.getString("gelis_novu_adi"),toLong(r.getObject("teskilat_id")),r.getString("teskilat_adi"),r.getString("protokol_kodu"),r.getObject("gelis_tarixi",java.time.LocalDate.class),r.getObject("gelis_saati",java.time.LocalTime.class),r.getObject("randevudur",Boolean.class),toLong(r.getObject("gonderen_hekim_id")),String.join(" ",java.util.stream.Stream.of(r.getString("gonderen_hekim_ad"),r.getString("gonderen_hekim_soyad"),r.getString("gonderen_hekim_ata_adi")).filter(x->x!=null&&!x.isBlank()).toList()),r.getString("mesaj"),r.getString("aciqlama"),r.getObject("aktiv",Boolean.class));}

    public AmbulatorLookups getLookups() {
        return new AmbulatorLookups(
            queryLookup("SELECT rb.id, NULL AS code, rb.name, NULL AS short_name, NULL AS country_id FROM public.rn_buildings rb WHERE rb.is_active IS TRUE ORDER BY rb.id"),
            queryLookup("SELECT ro.id, NULL AS code, ro.name, ro.short_name, NULL AS country_id FROM public.rn_organizations ro WHERE ro.is_active IS TRUE ORDER BY ro.id"),
            queryLookup("SELECT rit.id, rit.code, rit.name, NULL AS short_name, NULL AS country_id FROM public.rn_id_types rit WHERE rit.is_active IS TRUE ORDER BY rit.id"),
            queryLookup("SELECT rg.id, rg.code, rg.name, NULL AS short_name, NULL AS country_id FROM public.rn_genders rg WHERE rg.is_active IS TRUE ORDER BY rg.id"),
            queryLookup("SELECT rc.id, rc.code, rc.name, NULL AS short_name, NULL AS country_id FROM public.rn_countries rc WHERE rc.is_active IS TRUE ORDER BY rc.id"),
            queryLookup("SELECT rc.id, rc.code, rc.name, NULL AS short_name, rc.country_id FROM public.rn_cities rc WHERE rc.is_active IS TRUE ORDER BY rc.id"),
            queryLookup("SELECT rbg.id, rbg.code, rbg.name, NULL AS short_name, NULL AS country_id FROM public.rn_blood_groups rbg WHERE rbg.is_active IS TRUE ORDER BY rbg.id"),
            queryLookup("SELECT rms.id, rms.code, rms.name, NULL AS short_name, NULL AS country_id FROM public.rn_marital_statuses rms WHERE rms.is_active IS TRUE ORDER BY rms.id"),
            queryLookup("SELECT rn.id, rn.code, rn.name, NULL AS short_name, NULL AS country_id FROM public.rn_nationalities rn WHERE rn.is_active IS TRUE ORDER BY rn.id"),
            queryLookup("SELECT re.id, re.code, re.name, NULL AS short_name, NULL AS country_id FROM public.rn_educations re WHERE re.is_active IS TRUE ORDER BY re.id")
        );
    }

    public List<PatientDocumentListItem> getPatientDocuments(PatientDocumentFilter filter) {
        PatientDocumentFilter safeFilter = filter == null ? new PatientDocumentFilter() : filter;
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("limit", safeFilter.getLimit());

        StringBuilder sql = new StringBuilder("""
            SELECT
                pd.id,
                pd.patient_code,
                pd.id_number,
                pd.fin_code,
                pd.first_name,
                pd.last_name,
                pd.father_name,
                pd.birth_date,
                pd.mobile_phone,
                pd.workplace
            FROM public.rn_patient_documents pd
            WHERE pd.is_active IS TRUE
            """);

        addFilters(sql, params, safeFilter);

        sql.append("""
            ORDER BY pd.created_at DESC NULLS LAST, pd.id DESC
            LIMIT :limit
            """);

        return jdbcTemplate.query(sql.toString(), params, (rs, rowNum) -> new PatientDocumentListItem(
            toLong(rs.getObject("id")),
            rs.getString("patient_code"),
            rs.getString("id_number"),
            rs.getString("fin_code"),
            rs.getString("first_name"),
            rs.getString("last_name"),
            rs.getString("father_name"),
            rs.getObject("birth_date", java.time.LocalDate.class),
            rs.getString("mobile_phone"),
            rs.getString("workplace")
        ));
    }

    private void addFilters(StringBuilder sql, MapSqlParameterSource params, PatientDocumentFilter filter) {
        String q = filter.normalizedQ();
        if (!q.isBlank()) {
            params.addValue("q", containsPattern(q));
            sql.append("""
                AND (
                    pd.patient_code ILIKE :q
                    OR pd.id_number ILIKE :q
                    OR pd.fin_code ILIKE :q
                    OR pd.first_name ILIKE :q
                    OR pd.last_name ILIKE :q
                    OR pd.father_name ILIKE :q
                    OR CONCAT_WS(' ', pd.first_name, pd.last_name, pd.father_name) ILIKE :q
                    OR pd.mobile_phone ILIKE :q
                    OR pd.workplace ILIKE :q
                )
                """);
        }

        addContainsFilter(sql, params, "idNumber", "pd.id_number", filter.normalizedIdNumber());
        addContainsFilter(sql, params, "finCode", "pd.fin_code", filter.normalizedFinCode());
        addContainsFilter(sql, params, "mobilePhone", "pd.mobile_phone", filter.normalizedMobilePhone());
    }

    private void addContainsFilter(
        StringBuilder sql,
        MapSqlParameterSource params,
        String paramName,
        String columnName,
        String value
    ) {
        if (value.isBlank()) {
            return;
        }
        params.addValue(paramName, containsPattern(value));
        sql.append(" AND ").append(columnName).append(" ILIKE :").append(paramName).append('\n');
    }

    private String containsPattern(String value) {
        return "%" + value + "%";
    }

    public int countActivePatientDocuments() {
        String sql = "SELECT COUNT(*) FROM public.rn_patient_documents WHERE is_active IS TRUE";
        Integer count = jdbcTemplate.getJdbcTemplate().queryForObject(sql, Integer.class);
        return count == null ? 0 : count;
    }

    public int countTodayPatientDocuments() {
        String sql = """
            SELECT COUNT(*)
            FROM public.rn_patient_documents
            WHERE is_active IS TRUE
              AND created_at::date = CURRENT_DATE
            """;
        Integer count = jdbcTemplate.getJdbcTemplate().queryForObject(sql, Integer.class);
        return count == null ? 0 : count;
    }

    public boolean patientDocumentExists(Long patientDocumentId) {
        String sql = """
            SELECT EXISTS (
                SELECT 1
                FROM public.rn_patient_documents
                WHERE id = :patientDocumentId
                  AND is_active IS TRUE
            )
            """;
        Boolean exists = jdbcTemplate.queryForObject(
            sql,
            new MapSqlParameterSource("patientDocumentId", patientDocumentId),
            Boolean.class
        );
        return Boolean.TRUE.equals(exists);
    }

    public List<Map<String, Object>> createPatientDocument(PatientDocumentForm form, String createdBy) {
        String sql = """
            SELECT *
            FROM public.fn_create_patient_document(
                :buildingId,
                :organizationId,
                :idTypeId,
                :idNumber,
                :finCode,
                :firstName,
                :lastName,
                :fatherName,
                :genderId,
                :birthDate,
                :birthPlace,
                :birthCountryId,
                :birthCityId,
                :bloodGroupId,
                :livingCountryId,
                :address,
                :homePhone,
                :mobilePhone,
                :workPhone,
                :socialSecurityNumber,
                :maritalStatusId,
                :nationalityId,
                :educationId,
                :occupation,
                :workplace,
                :position,
                :createdBy
            )
            """;

        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("buildingId", form.getBuildingId())
            .addValue("organizationId", form.getOrganizationId())
            .addValue("idTypeId", form.getIdTypeId())
            .addValue("idNumber", blankToNull(form.getIdNumber()))
            .addValue("finCode", blankToNull(form.getFinCode()))
            .addValue("firstName", blankToNull(form.getFirstName()))
            .addValue("lastName", blankToNull(form.getLastName()))
            .addValue("fatherName", blankToNull(form.getFatherName()))
            .addValue("genderId", form.getGenderId())
            .addValue("birthDate", form.getBirthDate() == null ? null : Date.valueOf(form.getBirthDate()))
            .addValue("birthPlace", blankToNull(form.getBirthPlace()))
            .addValue("birthCountryId", form.getBirthCountryId())
            .addValue("birthCityId", form.getBirthCityId())
            .addValue("bloodGroupId", form.getBloodGroupId())
            .addValue("livingCountryId", form.getLivingCountryId())
            .addValue("address", blankToNull(form.getAddress()))
            .addValue("homePhone", blankToNull(form.getHomePhone()))
            .addValue("mobilePhone", normalizeMobilePhone(form.getMobilePhone()))
            .addValue("workPhone", blankToNull(form.getWorkPhone()))
            .addValue("socialSecurityNumber", blankToNull(form.getSocialSecurityNumber()))
            .addValue("maritalStatusId", form.getMaritalStatusId())
            .addValue("nationalityId", form.getNationalityId())
            .addValue("educationId", form.getEducationId())
            .addValue("occupation", blankToNull(form.getOccupation()))
            .addValue("workplace", blankToNull(form.getWorkplace()))
            .addValue("position", blankToNull(form.getPosition()))
            .addValue("createdBy", createdBy);

        return jdbcTemplate.queryForList(sql, params);
    }

    private List<LookupOption> queryLookup(String sql) {
        return jdbcTemplate.query(sql, (rs, rowNum) -> mapLookup(rs));
    }

    private LookupOption mapLookup(ResultSet rs) throws SQLException {
        return new LookupOption(
            toInteger(rs.getObject("id")),
            rs.getString("code"),
            rs.getString("name"),
            rs.getString("short_name"),
            toInteger(rs.getObject("country_id"))
        );
    }

    private Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.valueOf(value.toString());
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(value.toString());
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String normalizeMobilePhone(String value) {
        String phone = blankToNull(value);
        if (phone == null || phone.startsWith("+")) {
            return phone;
        }
        return "+994" + phone.replace(" ", "");
    }
}
