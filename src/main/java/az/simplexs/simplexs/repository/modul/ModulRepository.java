package az.simplexs.simplexs.repository.modul;

import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import az.simplexs.simplexs.dto.modul.ModulListItem;
import az.simplexs.simplexs.dto.modul.ModulSistemi;

@Repository
public class ModulRepository {
    private final NamedParameterJdbcTemplate jdbc;
    public ModulRepository(NamedParameterJdbcTemplate jdbc){this.jdbc=jdbc;}

    public List<ModulListItem> findAll(){
        return jdbc.query("""
            WITH RECURSIVE tree AS (
              SELECT m.*,0 AS seviyye,m.ad::text AS tam_yol,ARRAY[m.id] AS yol
              FROM public.rn_modullar m WHERE m.parent_id IS NULL
              UNION ALL
              SELECT m.*,t.seviyye+1,(t.tam_yol||' / '||m.ad)::text,t.yol||m.id
              FROM public.rn_modullar m JOIN tree t ON m.parent_id=t.id
              WHERE NOT m.id=ANY(t.yol)
            )
            SELECT s.id AS sistem_id,s.kod AS sistem_kodu,s.ad AS sistem_adi,s.ikon AS sistem_ikonu,
                   t.id,t.parent_id,t.kod,t.ad,t.aciqlama,t.route,t.ikon,t.menyuda_gorunsun,
                   t.aktiv,t.sira_no,t.seviyye,t.tam_yol
            FROM tree t JOIN public.rn_sistemler s ON s.id=t.sistem_id
            ORDER BY s.sira_no NULLS LAST,s.ad,t.yol
            """,(rs,row)->new ModulListItem(rs.getObject("sistem_id",Long.class),
                rs.getString("sistem_kodu"),rs.getString("sistem_adi"),rs.getString("sistem_ikonu"),
                rs.getLong("id"),rs.getObject("parent_id",Long.class),
                rs.getString("kod"),rs.getString("ad"),rs.getString("aciqlama"),rs.getString("route"),
                rs.getString("ikon"),rs.getBoolean("menyuda_gorunsun"),rs.getBoolean("aktiv"),
                rs.getObject("sira_no",Integer.class),rs.getInt("seviyye"),rs.getString("tam_yol")));
    }

    public List<ModulSistemi> findSystems(){
        return jdbc.query("""
            SELECT id,kod,ad,ikon,sira_no,aktiv FROM public.rn_sistemler
            ORDER BY sira_no NULLS LAST,ad
            """,(rs,row)->new ModulSistemi(rs.getLong("id"),rs.getString("kod"),rs.getString("ad"),
                rs.getString("ikon"),rs.getObject("sira_no",Integer.class),rs.getBoolean("aktiv")));
    }

    public Map<String,Object> update(Long id,Long sistemId,Long parentId,String ad,String aciqlama,String ikon,
            Integer siraNo,boolean menyudaGorunsun,boolean aktiv){
        return jdbc.queryForMap("""
            SELECT status_kodu,modul_id,mesaj FROM public.kn_modul_yenile(
              CAST(:id AS bigint),CAST(:sistemId AS bigint),CAST(:parentId AS bigint),
              CAST(:ad AS varchar),CAST(:aciqlama AS varchar),CAST(:ikon AS varchar),
              CAST(:siraNo AS integer),CAST(:visible AS boolean),CAST(:active AS boolean))
            """,new MapSqlParameterSource("id",id).addValue("sistemId",sistemId)
                .addValue("parentId",parentId).addValue("ad",ad).addValue("aciqlama",aciqlama)
                .addValue("ikon",ikon).addValue("siraNo",siraNo)
                .addValue("visible",menyudaGorunsun).addValue("active",aktiv));
    }

    public Map<String,Object> createSystem(String kod,String ad,String ikon,Integer siraNo){
        return jdbc.queryForMap("""
            SELECT status_kodu,sistem_id,mesaj FROM public.kn_sistem_yarat(
              CAST(:kod AS varchar),CAST(:ad AS varchar),CAST(:ikon AS varchar),CAST(:siraNo AS integer))
            """,new MapSqlParameterSource("kod",kod).addValue("ad",ad)
                .addValue("ikon",ikon).addValue("siraNo",siraNo));
    }

    public Map<String,Object> createGroup(Long sistemId,String kod,String ad,String aciqlama,
            String ikon,Integer siraNo){
        return jdbc.queryForMap("""
            SELECT status_kodu,modul_id,mesaj FROM public.kn_modul_qrupu_yarat(
              CAST(:sistemId AS bigint),CAST(:kod AS varchar),CAST(:ad AS varchar),
              CAST(:aciqlama AS varchar),CAST(:ikon AS varchar),CAST(:siraNo AS integer))
            """,new MapSqlParameterSource("sistemId",sistemId).addValue("kod",kod)
                .addValue("ad",ad).addValue("aciqlama",aciqlama)
                .addValue("ikon",ikon).addValue("siraNo",siraNo));
    }
}
