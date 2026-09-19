package az.simplexs.simplexs.service.eczaxana;

import az.simplexs.simplexs.repository.eczaxana.AnbarKontekstRepository;
import az.simplexs.simplexs.repository.eczaxana.QaimeRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Service
public class QaimeService {

    private final QaimeRepository repo;
    private final AnbarKontekstRepository context;
    private final ObjectMapper json;

    public QaimeService(QaimeRepository repo,
                        AnbarKontekstRepository context,
                        ObjectMapper json) {
        this.repo = repo;
        this.context = context;
        this.json = json;
    }

    public record Scope(Long clinic, Long personal, Long warehouse, int year) {}

    public static class Rejected extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public final int status;
        public Map<String, Object> result;

        public Rejected(int status, String code) {
            super(code);
            this.status = status;
        }
    }

    public void authorize(Long clinic, Long personal) {
        if (!context.canAccess(clinic, personal))
            throw new Rejected(403, "denied");
    }

    public void warehouse(Long clinic, Long personal, Long warehouse) {
        authorize(clinic, personal);

        if (context.warehouses(clinic, personal).stream()
                .noneMatch(w -> Objects.equals(w.anbarId(), warehouse)))
            throw new Rejected(403, "denied");
    }

    public void validate(Scope s) {
        warehouse(s.clinic(), s.personal(), s.warehouse());

        if (context.years(s.clinic(), s.warehouse()).stream()
                .noneMatch(y -> Objects.equals(y.il(), s.year())))
            throw new Rejected(400, "invalidYear");
    }

    public Map<String, Object> invoice(Scope s, Long id) {
        return repo.invoice(s.clinic(), s.warehouse(), s.year(), id)
                .orElseThrow(() -> new Rejected(404, "notFound"));
    }

    public Map<String, Object> details(Scope s, Long id) {
        validate(s);

        var header = invoice(s, id);
        var rows = repo.materials(s.warehouse(), id);

        return Map.of(
                "invoice", header,
                "rows", rows,
                "purchaseTotal", sum(rows, "alis_meblegi"),
                "saleTotal", sum(rows, "satis_meblegi")
        );
    }

    @Transactional
    public Map<String, Object> create(Scope s, Map<String, Object> input) {
        validate(s);
        validateDate(input, s.year());

        if (!(input.get("materiallar") instanceof List<?> materials)
                || materials.isEmpty()
                || materials.size() > 1000)
            throw new Rejected(400, "materialsRequired");

        var values = values(s, input);
        values.put("materiallar", json.writeValueAsString(materials));

        return check(repo.create(values));
    }

    @Transactional
    public Map<String, Object> update(Scope s, Long qaimeId, Map<String, Object> input) {
        validate(s);
        invoice(s, qaimeId);

        var values = values(s, input);
        values.put("qaime_id", qaimeId);

        changeFlags(values, input, false);

        return check(repo.update(values));
    }

    @Transactional
    public Map<String, Object> addMaterial(Scope s, Long qaimeId, Map<String, Object> input) {
        validate(s);
        invoice(s, qaimeId);

        var values = values(s, input);
        values.put("qaime_id", qaimeId);

        return check(repo.addMaterial(values));
    }

    @Transactional
    public Map<String, Object> updateMaterial(Scope s,
                                              Long qaimeId,
                                              Long qaimeMaterialId,
                                              Map<String, Object> input) {
        validate(s);
        invoice(s, qaimeId);
        material(s, qaimeId, qaimeMaterialId, "deyisdirile_biler");

        var values = values(s, input);
        values.put("qaime_material_id", qaimeMaterialId);

        changeFlags(values, input, true);

        return check(repo.updateMaterial(values));
    }

    @Transactional
    public Map<String, Object> deleteMaterial(Scope s,
                                              Long qaimeId,
                                              Long qaimeMaterialId) {
        validate(s);
        invoice(s, qaimeId);
        material(s, qaimeId, qaimeMaterialId, "siline_biler");

        return check(
                repo.deleteMaterial(
                        qaimeMaterialId,
                        s.warehouse(),
                        s.personal()
                )
        );
    }

    private void material(Scope s,
                          Long qaimeId,
                          Long qaimeMaterialId,
                          String permission) {

        var item = repo.materials(s.warehouse(), qaimeId).stream()
                .filter(m ->
                        m.get("qaime_material_id") instanceof Number n
                                && n.longValue() == qaimeMaterialId
                )
                .findFirst()
                .orElseThrow(() -> new Rejected(404, "notFound"));

        if (!Boolean.TRUE.equals(item.get(permission)))
            throw new Rejected(409, "materialLocked");
    }

    private Map<String, Object> values(Scope s, Map<String, Object> input) {
        var values = new HashMap<>(input);

        values.put("klinika_id", s.clinic());
        values.put("anbar_id", s.warehouse());
        values.put("yaradan_personal_id", s.personal());
        values.put("yenileyen_personal_id", s.personal());

        return values;
    }

    private void validateDate(Map<String, Object> input, int year) {
        try {
            if (LocalDate.parse(String.valueOf(input.get("qaime_tarixi"))).getYear() != year)
                throw new Rejected(400, "invalidYear");
        } catch (DateTimeParseException e) {
            throw new Rejected(400, "invalidDate");
        }
    }

    private void changeFlags(Map<String, Object> values,
                             Map<String, Object> input,
                             boolean material) {

        if (material) {
            values.put("son_istifade_tarixi_deyisdirilsin",
                    input.containsKey("son_istifade_tarixi"));

            values.put("seriya_no_deyisdirilsin",
                    input.containsKey("seriya_no"));

            values.put("aciqlama_deyisdirilsin",
                    input.containsKey("aciqlama"));

            return;
        }

        values.put("firma_deyisdirilsin",
                input.containsKey("firma_id"));

        values.put("teslim_alan_deyisdirilsin",
                input.containsKey("teslim_alan_personal_id"));

        values.put("teslim_eden_deyisdirilsin",
                input.containsKey("teslim_eden"));

        values.put("sened_novu_deyisdirilsin",
                input.containsKey("sened_novu"));

        values.put("sened_tarixi_deyisdirilsin",
                input.containsKey("sened_tarixi"));

        values.put("sened_no_deyisdirilsin",
                input.containsKey("sened_no"));

        values.put("aciqlama_deyisdirilsin",
                input.containsKey("aciqlama"));
    }

    private Map<String, Object> check(Map<String, Object> result) {

        if (!"UGURLU".equals(result.get("status_kodu"))) {

            var e = new Rejected(
                    422,
                    String.valueOf(result.get("status_kodu"))
            );

            e.result = result;
            throw e;
        }

        return result;
    }

    private BigDecimal sum(List<Map<String, Object>> rows, String key) {
        return rows.stream()
                .map(r -> r.get(key) instanceof BigDecimal d
                        ? d
                        : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}