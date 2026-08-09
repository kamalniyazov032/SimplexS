package az.simplexs.simplexs.repository.xidmet;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.stereotype.Repository;
import az.simplexs.simplexs.dto.xidmet.SobeXidmeti;

@Repository
public class SobeXidmetRepository {
    private final JdbcTemplate jdbc;
    public SobeXidmetRepository(DataSource dataSource){this.jdbc=new JdbcTemplate(dataSource);}
    public List<SobeXidmeti> find(Long sobeId){if(sobeId==null)return List.of();return jdbc.query("SELECT * FROM public.fn_xidmet_sobe_siyahisi(?,CAST(? AS boolean)) ORDER BY xidmet_qrupu_adi,xidmet_adi",(ps)->{ps.setLong(1,sobeId);ps.setObject(2,null);},(r,n)->new SobeXidmeti(l(r,"elaqe_id"),l(r,"sobe_id"),l(r,"klinika_id"),r.getString("sobe_adi"),l(r,"xidmet_id"),r.getString("xidmet_kodu"),r.getString("xidmet_adi"),l(r,"xidmet_qrupu_id"),r.getString("xidmet_qrupu_kodu"),r.getString("xidmet_qrupu_adi"),l(r,"xidmet_tipi_id"),r.getString("xidmet_tipi_kodu"),r.getString("xidmet_tipi_adi"),l(r,"muhasibat_kodu_id"),r.getString("muhasibat_kodu_adi"),r.getObject("elaqe_aktiv",Boolean.class),r.getObject("xidmet_aktiv",Boolean.class)));}
    public Map<String,Object> add(Long sobeId,List<Long> ids){return operation("SELECT * FROM public.fn_xidmetleri_sobeye_elave_et(?,?,NULL)",sobeId,ids);}
    public Map<String,Object> remove(Long sobeId,List<Long> ids){return operation("SELECT * FROM public.fn_xidmetleri_sobeden_cixar(?,?,NULL)",sobeId,ids);}
    private Map<String,Object> operation(String sql,Long sobeId,List<Long> ids){return jdbc.execute((ConnectionCallback<Map<String,Object>>)connection->{try(var ps=connection.prepareStatement(sql)){ps.setLong(1,sobeId);Array array=connection.createArrayOf("bigint",ids.toArray(Long[]::new));ps.setArray(2,array);try(var rs=ps.executeQuery()){if(!rs.next())return Map.<String,Object>of();var meta=rs.getMetaData();Map<String,Object> row=new LinkedHashMap<>();for(int i=1;i<=meta.getColumnCount();i++)row.put(meta.getColumnLabel(i),rs.getObject(i));return row;}}});}
    private static Long l(ResultSet r,String c)throws SQLException{Object x=r.getObject(c);return x instanceof Number n?n.longValue():null;}
}
