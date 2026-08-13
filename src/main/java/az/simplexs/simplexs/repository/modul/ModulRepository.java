package az.simplexs.simplexs.repository.modul;

import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import az.simplexs.simplexs.dto.modul.ModulListItem;

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
            SELECT id,parent_id,kod,ad,aciqlama,route,ikon,menyuda_gorunsun,aktiv,sira_no,seviyye,tam_yol
            FROM tree ORDER BY yol
            """,(rs,row)->new ModulListItem(rs.getLong("id"),rs.getObject("parent_id",Long.class),
                rs.getString("kod"),rs.getString("ad"),rs.getString("aciqlama"),rs.getString("route"),
                rs.getString("ikon"),rs.getBoolean("menyuda_gorunsun"),rs.getBoolean("aktiv"),
                rs.getObject("sira_no",Integer.class),rs.getInt("seviyye"),rs.getString("tam_yol")));
    }

    @Transactional
    public Map<String,Object> update(Long id,Long parentId,String ad,String aciqlama,String ikon,
            Integer siraNo,boolean menyudaGorunsun,boolean aktiv){
        if(id.equals(parentId))return error("Modul özünün alt modulu ola bilməz.");
        Boolean descendant=jdbc.queryForObject("""
            WITH RECURSIVE descendants AS (
              SELECT id FROM public.rn_modullar WHERE parent_id=:id
              UNION ALL SELECT m.id FROM public.rn_modullar m JOIN descendants d ON m.parent_id=d.id
            ) SELECT EXISTS(SELECT 1 FROM descendants WHERE id=:parentId)
            """,new MapSqlParameterSource("id",id).addValue("parentId",parentId),Boolean.class);
        if(Boolean.TRUE.equals(descendant))return error("Modul öz alt modulunun daxilinə keçirilə bilməz.");
        int changed=jdbc.update("""
            UPDATE public.rn_modullar SET parent_id=:parentId,ad=:ad,aciqlama=:aciqlama,
              ikon=:ikon,sira_no=:siraNo,menyuda_gorunsun=:visible,aktiv=:active
            WHERE id=:id
            """,new MapSqlParameterSource("id",id).addValue("parentId",parentId)
                .addValue("ad",ad.trim()).addValue("aciqlama",blank(aciqlama)).addValue("ikon",blank(ikon))
                .addValue("siraNo",siraNo).addValue("visible",menyudaGorunsun).addValue("active",aktiv));
        return changed==1?Map.of("status_kodu","UGURLU","mesaj","Modul strukturu yeniləndi.")
            :error("Modul tapılmadı.");
    }
    private Map<String,Object> error(String message){return Map.of("status_kodu","XETA","mesaj",message);}
    private String blank(String value){return value==null||value.isBlank()?null:value.trim();}
}
