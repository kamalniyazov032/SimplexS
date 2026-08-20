package az.simplexs.simplexs.repository.tercume;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import az.simplexs.simplexs.dto.tercume.Dil;
import az.simplexs.simplexs.dto.tercume.TercumeModulu;
import az.simplexs.simplexs.dto.tercume.MelumatNovu;
import az.simplexs.simplexs.dto.tercume.MelumatTercumeSetri;

@Repository
public class TercumeRepository {
    private static final Map<String,String> MELUMAT_SQL=Map.ofEntries(
        Map.entry("CINS","SELECT id,kod,ad,'ad' saha FROM public.fn_cins_siyahisi()"),
        Map.entry("TEHSIL","SELECT id,kod,ad,'ad' saha FROM public.fn_tehsil_siyahisi()"),
        Map.entry("AILE_VEZIYYETI","SELECT id,kod,ad,'ad' saha FROM public.fn_aile_veziyyeti_siyahisi()"),
        Map.entry("SENED_NOVU","SELECT id,kod,ad,'ad' saha FROM public.fn_sexsiyyet_vesiqesi_novu_siyahisi()"),
        Map.entry("QAN_QRUPU","SELECT id,kod,ad,'ad' saha FROM public.fn_qan_qrupu_siyahisi()"),
        Map.entry("OLKE","SELECT id,iso2_kod kod,ad,'ad' saha FROM public.fn_olke_siyahisi()"),
        Map.entry("SEHER","SELECT id,id::text kod,ad,'ad' saha FROM public.fn_seher_siyahisi(NULL)"),
        Map.entry("MODUL","SELECT id,kod,ad,'ad' saha FROM public.rn_modullar WHERE aktiv"),
        Map.entry("XIDMET","SELECT id,kod,ad,'ad' saha FROM public.rn_xidmetler WHERE aktiv"),
        Map.entry("TESKILAT","SELECT id,COALESCE(qisa_ad,id::text) kod,ad,'ad' saha FROM public.rn_teskilatlar WHERE aktiv"));
    private final NamedParameterJdbcTemplate jdbc;
    private volatile boolean schemaAvailable=true;
    private final AtomicLong version=new AtomicLong();
    public long version(){return version.get();}
    public TercumeRepository(NamedParameterJdbcTemplate jdbc){this.jdbc=jdbc;}

    public List<Dil> diller(){
        if(!schemaAvailable)return List.of(new Dil(null,"az","Azərbaycan dili","Azərbaycan dili",true,true,1));
        try{return jdbc.query("SELECT id,kod,ad,yerli_ad,standartdir,aktiv,sira_no FROM public.kn_diller ORDER BY sira_no,ad",Map.of(),
                (r,n)->new Dil(r.getLong("id"),r.getString("kod"),r.getString("ad"),r.getString("yerli_ad"),r.getBoolean("standartdir"),r.getBoolean("aktiv"),r.getInt("sira_no")));
        }catch(DataAccessException e){schemaAvailable=false;return List.of(new Dil(null,"az","Azərbaycan dili","Azərbaycan dili",true,true,1));}
    }
    public Map<String,String> tercumeler(String dilKodu){
        if(!schemaAvailable)return Map.of();
        try{return jdbc.query("SELECT t.acar,t.deyer FROM public.kn_interfeys_tercumeleri t JOIN public.kn_diller d ON d.id=t.dil_id WHERE d.kod=:kod AND d.aktiv",Map.of("kod",dilKodu),
                r->{var result=new java.util.LinkedHashMap<String,String>();while(r.next())result.put(r.getString(1),r.getString(2));return result;});
        }catch(DataAccessException e){schemaAvailable=false;return Map.of();}
    }
    public List<TercumeModulu> modullar(){
        if(!schemaAvailable)return List.of();
        try{return jdbc.query("""
            WITH RECURSIVE agac AS (
              SELECT m.id,m.parent_id,m.kod,m.ad,m.ikon,m.sistem_id,0 seviyye,
                     lpad(COALESCE(m.sira_no,0)::text,6,'0')||'-'||m.id yol
              FROM public.rn_modullar m WHERE m.parent_id IS NULL AND m.aktiv AND m.menyuda_gorunsun
              UNION ALL
              SELECT m.id,m.parent_id,m.kod,m.ad,m.ikon,m.sistem_id,a.seviyye+1,
                     a.yol||'/'||lpad(COALESCE(m.sira_no,0)::text,6,'0')||'-'||m.id
              FROM public.rn_modullar m JOIN agac a ON a.id=m.parent_id
              WHERE m.aktiv AND m.menyuda_gorunsun
            ), secimler AS (
              SELECT NULL::bigint modul_id,'GLOBAL'::varchar modul_kodu,'Ümumi'::varchar modul_adi,NULL::varchar qrup_adi,0 seviyye,
                     'ti ti-world'::varchar ikon,NULL::varchar sistem_kodu,NULL::varchar sistem_adi,NULL::varchar sistem_ikonu,
                     '000000'::text sistem_yolu,'000000'::text modul_yolu
              UNION ALL
              SELECT a.id,a.kod,a.ad,p.ad,a.seviyye,a.ikon,s.kod,s.ad,s.ikon,
                     lpad(COALESCE(s.sira_no,0)::text,6,'0')||'-'||s.id,a.yol
              FROM agac a JOIN public.rn_sistemler s ON s.id=a.sistem_id AND s.aktiv
              LEFT JOIN public.rn_modullar p ON p.id=a.parent_id
            )
            SELECT s.*,count(DISTINCT e.acar) acar_sayi
            FROM secimler s LEFT JOIN public.kn_tercume_modul_elaqeleri e ON e.modul_id IS NOT DISTINCT FROM s.modul_id
            GROUP BY s.modul_id,s.modul_kodu,s.modul_adi,s.qrup_adi,s.seviyye,s.ikon,s.sistem_kodu,s.sistem_adi,s.sistem_ikonu,s.sistem_yolu,s.modul_yolu
            ORDER BY s.sistem_yolu,s.modul_yolu
            """,Map.of(),(r,n)->new TercumeModulu(r.getString("modul_kodu"),r.getString("modul_adi"),r.getString("qrup_adi"),r.getInt("seviyye"),r.getLong("acar_sayi"),r.getString("ikon"),r.getString("sistem_kodu"),r.getString("sistem_adi"),r.getString("sistem_ikonu")));
        }catch(DataAccessException e){return List.of();}
    }
    public List<String> acarlar(String modulKodu){
        if(!schemaAvailable)return List.of();
        try{return jdbc.query("""
                SELECT DISTINCT e.acar FROM public.kn_tercume_modul_elaqeleri e
                WHERE (:kod='GLOBAL' AND e.modul_id IS NULL)
                   OR e.modul_id=(SELECT id FROM public.rn_modullar WHERE kod=:kod)
                ORDER BY e.acar
                """,
                Map.of("kod",modulKodu),(r,n)->r.getString(1));
        }catch(DataAccessException e){return List.of();}
    }
    public void dilYarat(String kod,String ad,String yerliAd){jdbc.update("INSERT INTO public.kn_diller(kod,ad,yerli_ad,standartdir,aktiv,sira_no) VALUES(lower(:kod),:ad,:yerli,false,true,(SELECT COALESCE(max(sira_no),0)+1 FROM public.kn_diller))",new MapSqlParameterSource().addValue("kod",kod.trim()).addValue("ad",ad.trim()).addValue("yerli",yerliAd.trim()));version.incrementAndGet();}
    public void dilYenile(Long id,String ad,String yerliAd,boolean aktiv){jdbc.update("UPDATE public.kn_diller SET ad=:ad,yerli_ad=:yerli,aktiv=:aktiv,yenilenme_tarixi=now() WHERE id=:id AND NOT standartdir",new MapSqlParameterSource().addValue("id",id).addValue("ad",ad.trim()).addValue("yerli",yerliAd.trim()).addValue("aktiv",aktiv));version.incrementAndGet();}
    public void tercumeYaddaSaxla(String dilKodu,String acar,String deyer){jdbc.update("""
        INSERT INTO public.kn_interfeys_tercumeleri(dil_id,acar,deyer)
        SELECT id,:acar,:deyer FROM public.kn_diller WHERE kod=:kod AND NOT standartdir
        ON CONFLICT(dil_id,acar) DO UPDATE SET deyer=EXCLUDED.deyer,yenilenme_tarixi=now()
        """,new MapSqlParameterSource().addValue("kod",dilKodu).addValue("acar",acar).addValue("deyer",deyer.trim()));version.incrementAndGet();}

    public List<MelumatNovu> melumatNovleri(){return List.of(
        new MelumatNovu("CINS","Cins siyahısı","ti ti-gender-bigender"),new MelumatNovu("TEHSIL","Təhsil siyahısı","ti ti-school"),
        new MelumatNovu("AILE_VEZIYYETI","Ailə vəziyyəti","ti ti-users"),new MelumatNovu("SENED_NOVU","Sənəd növü","ti ti-id"),
        new MelumatNovu("QAN_QRUPU","Qan qrupları","ti ti-droplet"),new MelumatNovu("OLKE","Ölkələr","ti ti-world"),
        new MelumatNovu("SEHER","Şəhərlər","ti ti-building-community"),new MelumatNovu("MODUL","Modullar","ti ti-apps"),
        new MelumatNovu("XIDMET","Xidmətlər","ti ti-medical-cross"),new MelumatNovu("TESKILAT","Təşkilatlar","ti ti-building-bank"));}

    public List<MelumatTercumeSetri> melumatSetirleri(String nov){
        String sql=MELUMAT_SQL.get(nov);if(sql==null)throw new IllegalArgumentException("Naməlum məlumat növü");
        var translations=jdbc.query("SELECT t.menbe_id,t.saha,d.kod,t.deyer FROM public.kn_melumat_tercumeleri t JOIN public.kn_diller d ON d.id=t.dil_id WHERE t.melumat_novu=:nov",Map.of("nov",nov),r->{var x=new java.util.HashMap<String,String>();while(r.next())x.put(r.getLong(1)+"|"+r.getString(2)+"|"+r.getString(3),r.getString(4));return x;});
        return jdbc.query("SELECT * FROM ("+sql+") x ORDER BY ad",Map.of(),(r,n)->{Long id=r.getLong("id");String saha=r.getString("saha");var values=new java.util.LinkedHashMap<String,String>();diller().stream().filter(d->!Boolean.TRUE.equals(d.standartdir())).forEach(d->values.put(d.kod(),translations.getOrDefault(id+"|"+saha+"|"+d.kod(),"")));return new MelumatTercumeSetri(id,r.getString("kod"),r.getString("ad"),saha,values);});
    }

    public void melumatTercumesiYaddaSaxla(String nov,Long id,String saha,String dilKodu,String deyer){
        if(!MELUMAT_SQL.containsKey(nov)||!"ad".equals(saha))throw new IllegalArgumentException("Yanlış məlumat növü və ya sahə");
        jdbc.update("""
            INSERT INTO public.kn_melumat_tercumeleri(melumat_novu,menbe_id,saha,dil_id,deyer)
            SELECT :nov,:id,:saha,d.id,:deyer FROM public.kn_diller d WHERE d.kod=:dil AND NOT d.standartdir
            ON CONFLICT(melumat_novu,menbe_id,saha,dil_id) DO UPDATE SET deyer=EXCLUDED.deyer,yenilenme_tarixi=now()
            """,
            new MapSqlParameterSource().addValue("nov",nov).addValue("id",id).addValue("saha",saha).addValue("dil",dilKodu).addValue("deyer",deyer.trim()));
    }
}
