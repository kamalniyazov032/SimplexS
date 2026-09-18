package az.simplexs.simplexs.repository.eczaxana;

import java.time.LocalDate;
import java.util.List;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class AnbarKontekstRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public AnbarKontekstRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record Warehouse(Long anbarId, String anbarAdi, Long anbarNovuId, String anbarNovuAdi,
                            LocalDate istifadeyeBaslamaTarixi, Boolean telebAnbaridir, Boolean telebdeStokGorunsun,
                            String cixisUsulu, String effektivCixisUsulu) {
    }

    public record Year(Integer il, Boolean cariIldir, LocalDate baslamaTarixi) {
    }

    public boolean canAccess(Long clinic, Long personal) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS(SELECT 1 FROM public.fn_personal_modul_siyahisi(:personal,:clinic)
                    WHERE modul_kodu='HIS_PHARMACY_INVOICES')
                """, new MapSqlParameterSource("clinic", clinic).addValue("personal", personal), Boolean.class));
    }

    public List<Warehouse> warehouses(Long clinic, Long personal) {
        return jdbc.query("""
                        SELECT anbar_id,anbar_adi,anbar_novu_id,anbar_novu_adi,istifadeye_baslama_tarixi,
                               teleb_anbaridir,telebde_stok_gorunsun,cixis_usulu,effektiv_cixis_usulu
                        FROM public.fn_anbar_kontekst_anbar_siyahisi(p_klinika_id=>:clinic,p_personal_id=>:personal)
                        """, new MapSqlParameterSource("clinic", clinic).addValue("personal", personal),
                (r, n) -> new Warehouse(r.getObject("anbar_id", Long.class), r.getString("anbar_adi"),
                        r.getObject("anbar_novu_id", Long.class), r.getString("anbar_novu_adi"),
                        r.getObject("istifadeye_baslama_tarixi", LocalDate.class), r.getObject("teleb_anbaridir", Boolean.class),
                        r.getObject("telebde_stok_gorunsun", Boolean.class), r.getString("cixis_usulu"), r.getString("effektiv_cixis_usulu")));
    }

    public List<Year> years(Long clinic, Long warehouse) {
        return jdbc.query("""
                        SELECT il,cari_ildir,baslama_tarixi FROM public.fn_anbar_kontekst_il_siyahisi(
                            p_anbar_id=>:warehouse)
                        """, new MapSqlParameterSource("clinic", clinic).addValue("warehouse", warehouse),
                (r, n) -> new Year(r.getObject("il", Integer.class), r.getObject("cari_ildir", Boolean.class), r.getObject("baslama_tarixi", LocalDate.class)));
    }
}
