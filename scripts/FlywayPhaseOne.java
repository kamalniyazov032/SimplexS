import java.nio.file.*;
import java.util.*;
import java.sql.*;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;

public class FlywayPhaseOne {
    private static String property(Properties p,String name){
        String raw=p.getProperty(name);if(raw==null)throw new IllegalStateException(name+" yoxdur");
        if(!raw.startsWith("${"))return raw;
        int colon=raw.indexOf(':');String env=raw.substring(2,colon);String configured=System.getenv(env);
        return configured==null||configured.isBlank()?raw.substring(colon+1,raw.length()-1):configured;
    }
    public static void main(String[] args)throws Exception{
        Properties p=new Properties();try(var in=Files.newInputStream(Path.of("src/main/resources/application.properties"))){p.load(in);}
        String url=property(p,"spring.datasource.url"),user=property(p,"spring.datasource.username"),password=property(p,"spring.datasource.password");
        Flyway flyway=Flyway.configure().dataSource(url,user,password)
                .locations("filesystem:src/main/resources/db/migration").baselineOnMigrate(true).baselineVersion("0").target("34").load();
        List<String> pending=Arrays.stream(flyway.info().pending()).map(MigrationInfo::getVersion).map(String::valueOf).toList();
        System.out.println("Pending migration-lar: "+pending);
        if(args.length>0&&"verify".equals(args[0])){try(Connection c=DriverManager.getConnection(url,user,password);Statement s=c.createStatement()){
            try(ResultSet r=s.executeQuery("SELECT d.kod,count(t.id) say FROM public.kn_diller d LEFT JOIN public.kn_interfeys_tercumeleri t ON t.dil_id=d.id WHERE d.kod IN ('az','en','ru') GROUP BY d.kod ORDER BY d.kod")){while(r.next())System.out.println(r.getString(1)+": "+r.getLong(2));}
            try(ResultSet r=s.executeQuery("SELECT count(*) FROM public.rn_modullar WHERE kod='APP_TRANSLATIONS' AND aktiv AND route='/tercumeler'")){r.next();System.out.println("Tərcümələr modulu: "+r.getLong(1));}
            try(ResultSet r=s.executeQuery("SELECT count(*),count(DISTINCT COALESCE(modul_id,0)),count(DISTINCT acar) FROM public.kn_tercume_modul_elaqeleri")){r.next();System.out.println("Modul əlaqələri: "+r.getLong(1)+", modul: "+r.getLong(2)+", açar: "+r.getLong(3));}
            try(ResultSet r=s.executeQuery("SELECT count(*) FROM public.kn_melumat_tercumeleri")){r.next();System.out.println("Məlumat tərcümələri: "+r.getLong(1));}
            try(ResultSet r=s.executeQuery("SELECT d.kod,count(t.id) FROM public.kn_diller d LEFT JOIN public.kn_melumat_tercumeleri t ON t.dil_id=d.id AND t.melumat_novu='MODUL' WHERE d.kod IN ('en','ru') GROUP BY d.kod ORDER BY d.kod")){while(r.next())System.out.println("Modul tərcümələri "+r.getString(1)+": "+r.getLong(2));}
            try(ResultSet r=s.executeQuery("SELECT count(*) FROM public.rn_modullar m WHERE m.aktiv AND EXISTS(SELECT 1 FROM public.kn_diller d WHERE d.kod IN ('en','ru') AND d.aktiv AND NOT EXISTS(SELECT 1 FROM public.kn_melumat_tercumeleri t WHERE t.melumat_novu='MODUL' AND t.menbe_id=m.id AND t.saha='ad' AND t.dil_id=d.id))")){r.next();System.out.println("Tərcüməsi çatışmayan aktiv modul: "+r.getLong(1));}
            try(ResultSet r=s.executeQuery("SELECT m.kod,m.ad FROM public.rn_modullar m WHERE m.aktiv AND EXISTS(SELECT 1 FROM public.kn_diller d WHERE d.kod IN ('en','ru') AND d.aktiv AND NOT EXISTS(SELECT 1 FROM public.kn_melumat_tercumeleri t WHERE t.melumat_novu='MODUL' AND t.menbe_id=m.id AND t.saha='ad' AND t.dil_id=d.id)) ORDER BY m.kod")){while(r.next())System.out.println("missing-module|"+r.getString(1)+"|"+r.getString(2));}
            try(ResultSet r=s.executeQuery("SELECT (SELECT count(*) FROM public.fn_cins_siyahisi())+(SELECT count(*) FROM public.fn_tehsil_siyahisi())+(SELECT count(*) FROM public.fn_aile_veziyyeti_siyahisi())+(SELECT count(*) FROM public.fn_sexsiyyet_vesiqesi_novu_siyahisi())+(SELECT count(*) FROM public.fn_qan_qrupu_siyahisi())+(SELECT count(*) FROM public.fn_olke_siyahisi())+(SELECT count(*) FROM public.fn_seher_siyahisi(NULL))")){r.next();System.out.println("Tərcümə edilə bilən ümumi məlumat sətrləri: "+r.getLong(1));}
            try(ResultSet r=s.executeQuery("SELECT count(*) FROM public.rn_xidmetler WHERE aktiv")){r.next();System.out.println("Tərcümə edilə bilən xidmətlər: "+r.getLong(1));}
            try(ResultSet r=s.executeQuery("SELECT 'system' nov,kod,ad FROM public.rn_sistemler WHERE aktiv UNION ALL SELECT 'module',kod,ad FROM public.rn_modullar WHERE aktiv AND menyuda_gorunsun ORDER BY 1,2")){while(r.next())System.out.println(r.getString(1)+"|"+r.getString(2)+"|"+r.getString(3));}
        }return;}
        if(args.length==0||!"migrate".equals(args[0]))return;
        if(!pending.equals(List.of("34")))throw new IllegalStateException("Təhlükəsizlik dayandırması: yalnız V34 gözlənilirdi, alındı "+pending);
        var result=flyway.migrate();System.out.println("Tətbiq edilən migration sayı: "+result.migrationsExecuted);
    }
}
