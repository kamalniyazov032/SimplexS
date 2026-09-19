package az.simplexs.simplexs.repository.eczaxana;

import java.time.LocalTime;
import java.util.*;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.stereotype.Repository;

@Repository
public class BirbasaTransferRepository {
    private final NamedParameterJdbcTemplate jdbc;
    public BirbasaTransferRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc=jdbc; }

    public boolean canAccess(Long clinic, Long personal) {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM public.fn_personal_modul_siyahisi(:personal,:clinic) WHERE modul_kodu='HIS_PHARMACY_TRANSFER')",new MapSqlParameterSource("clinic",clinic).addValue("personal",personal),Boolean.class));
    }
    public List<Map<String,Object>> warehouses(Long clinic) {
        return jdbc.queryForList("SELECT anbar_id,anbar_adi FROM public.fn_anbar_birbasa_transfer_anbar_siyahisi(:clinic)",new MapSqlParameterSource("clinic",clinic));
    }
    public List<Map<String,Object>> materials(Long warehouse, String date) {
        return jdbc.queryForList("SELECT * FROM public.fn_anbar_birbasa_transfer_material_siyahisi(:warehouse,CAST(:date AS date))",new MapSqlParameterSource("warehouse",warehouse).addValue("date",date));
    }
    public List<Map<String,Object>> units(Long material) {
        return jdbc.queryForList("SELECT * FROM public.fn_material_vahid_siyahisi(:material) WHERE aktiv",new MapSqlParameterSource("material",material));
    }
    public Map<String,Object> save(Long sender, Long receiver, String date, String materials, String note, Long personal) {
        return jdbc.queryForMap("""
                SELECT * FROM public.fn_anbar_birbasa_transfer_yarat(
                    p_gonderen_anbar_id=>:sender,p_qebul_eden_anbar_id=>:receiver,p_transfer_tarixi=>CAST(:date AS date),
                    p_materiallar=>CAST(:materials AS jsonb),p_transfer_saati=>CAST(:time AS time),
                    p_aciqlama=>:note,p_yaradan_personal_id=>:personal)
                """,new MapSqlParameterSource("sender",sender).addValue("receiver",receiver).addValue("date",date)
                .addValue("materials",materials).addValue("time",LocalTime.now().toString()).addValue("note",note).addValue("personal",personal));
    }
}
