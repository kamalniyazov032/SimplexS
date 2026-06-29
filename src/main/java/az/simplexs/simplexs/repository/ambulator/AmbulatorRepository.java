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
import az.simplexs.simplexs.dto.ambulator.PatientDocumentForm;
import az.simplexs.simplexs.dto.ambulator.PatientDocumentListItem;

@Repository
public class AmbulatorRepository {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public AmbulatorRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

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

    public List<PatientDocumentListItem> getPatientDocuments() {
        String sql = """
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
            ORDER BY pd.id DESC
            LIMIT 200
            """;

        return jdbcTemplate.getJdbcTemplate().query(sql, (rs, rowNum) -> new PatientDocumentListItem(
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
