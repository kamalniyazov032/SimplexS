package az.simplexs.simplexs.security;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service("access")
public class AccessService {
    private final NamedParameterJdbcTemplate jdbc;

    public AccessService(NamedParameterJdbcTemplate jdbc) { this.jdbc=jdbc; }

    public Long firstClinicId(Authentication authentication) {
        List<Long> ids=clinicIds(authentication);
        return ids.isEmpty()?null:ids.getFirst();
    }

    public List<Long> clinicIds(Authentication authentication) {
        Long personalId=personalId(authentication);if(personalId==null)return List.of();
        if(isSystemAdmin(personalId)){
            return jdbc.query("SELECT id FROM public.rn_klinikalar WHERE aktiv ORDER BY sira_no NULLS LAST,id",
                (rs,row)->rs.getLong(1));
        }
        return jdbc.query("SELECT klinika_id FROM public.rn_personal_klinikalar WHERE personal_id=:pid AND aktiv ORDER BY sira_no NULLS LAST,id",
            new MapSqlParameterSource("pid",personalId),(rs,row)->rs.getLong(1));
    }

    public boolean hasClinic(Authentication authentication, Long clinicId) {
        return clinicId!=null&&clinicIds(authentication).contains(clinicId);
    }

    public List<MenuModule> menu(Authentication authentication, Long clinicId) {
        return menuSystems(authentication, clinicId).stream()
                .flatMap(system -> system.getModules().stream()).toList();
    }

    public List<MenuSystem> menuSystems(Authentication authentication, Long clinicId) {
        Long personalId=personalId(authentication); if(personalId==null||clinicId==null)return List.of();
        List<MenuRow> rows=jdbc.query("""
                SELECT menu.*, sistem.id AS sistem_id, sistem.kod AS sistem_kodu,
                       sistem.ad AS sistem_adi, sistem.ikon AS sistem_ikonu
                FROM public.fn_personal_modul_siyahisi(:pid,:kid) menu
                JOIN public.rn_modullar modul ON modul.id=menu.modul_id
                JOIN public.rn_sistemler sistem ON sistem.id=modul.sistem_id AND sistem.aktiv
                ORDER BY sistem.sira_no NULLS LAST, menu.sira_no NULLS LAST, menu.modul_adi
                """,
            new MapSqlParameterSource("pid",personalId).addValue("kid",clinicId),
            (rs,row)->new MenuRow(rs.getObject("sistem_id",Long.class),rs.getString("sistem_kodu"),
                rs.getString("sistem_adi"),rs.getString("sistem_ikonu"),
                new MenuModule(rs.getLong("modul_id"),rs.getObject("parent_id",Long.class),
                    rs.getString("modul_kodu"),rs.getString("modul_adi"),rs.getString("route"),rs.getString("ikon"))));
        List<MenuModule> flat=rows.stream().map(MenuRow::module).toList();
        Map<Long,MenuModule> byId=new LinkedHashMap<>(); flat.forEach(m->byId.put(m.getId(),m));
        Map<Long,MenuSystem> systems=new LinkedHashMap<>();
        for(MenuRow row:rows) {
            systems.computeIfAbsent(row.systemId(), id ->
                    new MenuSystem(id,row.systemCode(),row.systemName(),row.systemIcon()));
            MenuModule module=row.module();
            MenuModule parent=byId.get(module.getParentId());
            if(parent==null) systems.get(row.systemId()).getModules().add(module);
            else parent.getChildren().add(module);
        }
        return new ArrayList<>(systems.values());
    }

    private record MenuRow(Long systemId, String systemCode, String systemName,
            String systemIcon, MenuModule module) {}

    public boolean hasPermission(Authentication authentication, Long clinicId, String code) {
        Long personalId=personalId(authentication); if(personalId==null||clinicId==null)return false;
        if(isSystemAdmin(personalId))return true;
        Boolean result=jdbc.queryForObject("""
            SELECT EXISTS(SELECT 1 FROM public.fn_personal_selahiyyet_siyahisi(:pid,:kid) WHERE selahiyyet_kodu=:code)
            """,new MapSqlParameterSource("pid",personalId).addValue("kid",clinicId).addValue("code",code),Boolean.class);
        return Boolean.TRUE.equals(result);
    }

    public boolean isRegisteredRoute(String path) {
        Boolean result=jdbc.queryForObject("""
            SELECT EXISTS(SELECT 1 FROM public.rn_modullar WHERE aktiv AND route IS NOT NULL
              AND (:path=route OR :path LIKE rtrim(route,'/')||'/%'))
            """,new MapSqlParameterSource("path",path),Boolean.class);
        return Boolean.TRUE.equals(result);
    }

    public boolean canAccessRoute(Authentication authentication, Long clinicId, String path) {
        Long personalId=personalId(authentication);if(personalId==null||clinicId==null)return false;
        if(isSystemAdmin(personalId))return true;
        Boolean result=jdbc.queryForObject("SELECT public.fn_personal_route_icazesi_var(:pid,:kid,:path)",
            new MapSqlParameterSource("pid",personalId).addValue("kid",clinicId).addValue("path",path),Boolean.class);
        return Boolean.TRUE.equals(result);
    }

    public String requiredPermission(String path, String method) {
        List<String> codes=jdbc.query("SELECT selahiyyet_kodu FROM public.rn_route_selahiyyetleri WHERE route=:path AND http_metod=:method AND aktiv",
            new MapSqlParameterSource("path",path).addValue("method",method),(rs,row)->rs.getString(1));
        return codes.isEmpty()?null:codes.getFirst();
    }

    public void audit(Authentication authentication, Long clinicId, String event, String path,
            String method, String ipAddress, boolean successful) {
        Long personalId=personalId(authentication);
        String username=authentication==null?null:authentication.getName();
        jdbc.update("""
            INSERT INTO public.rn_tehlukesizlik_auditi
              (personal_id,istifadeci_adi,klinika_id,hadise_kodu,route,http_metod,ip_unvan,ugurlu)
            VALUES(:pid,:username,:kid,:event,:path,:method,:ip,:successful)
            """,new MapSqlParameterSource("pid",personalId).addValue("username",username)
                .addValue("kid",clinicId).addValue("event",event).addValue("path",path)
                .addValue("method",method).addValue("ip",ipAddress).addValue("successful",successful));
    }

    private Long personalId(Authentication authentication) {
        return authentication!=null&&authentication.getPrincipal() instanceof AuthenticatedPersonal p?p.personalId():null;
    }

    private boolean isSystemAdmin(Long personalId) {
        Boolean result=jdbc.queryForObject("""
            SELECT EXISTS(
                SELECT 1 FROM public.rn_personal_klinikalar pk
                JOIN public.rn_personal_klinika_rollari pr
                  ON pr.personal_klinika_id=pk.id AND pr.aktiv
                JOIN public.rn_rollar r ON r.id=pr.rol_id AND r.aktiv AND r.sistem_roludur
                WHERE pk.personal_id=:pid
            )
            """,new MapSqlParameterSource("pid",personalId),Boolean.class);
        return Boolean.TRUE.equals(result);
    }
}
