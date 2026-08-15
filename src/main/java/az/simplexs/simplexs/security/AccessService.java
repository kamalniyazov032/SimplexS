package az.simplexs.simplexs.security;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

@Service("access")
public class AccessService {
    private static final long ROUTE_CACHE_NANOS=30_000_000_000L;
    private final NamedParameterJdbcTemplate jdbc;
    private final MessageSource messages;
    private final Map<String,TimedValue<Boolean>> registeredRouteCache=new ConcurrentHashMap<>();
    private final Map<String,TimedValue<String>> permissionCache=new ConcurrentHashMap<>();

    public AccessService(NamedParameterJdbcTemplate jdbc, MessageSource messages) {
        this.jdbc=jdbc;
        this.messages=messages;
    }

    public Long firstClinicId(Authentication authentication) {
        List<Long> ids=clinicIds(authentication);
        return ids.isEmpty()?null:ids.getFirst();
    }

    public List<Long> clinicIds(Authentication authentication) {
        Long personalId=personalId(authentication);if(personalId==null)return List.of();
        return requestCached("simplexs.clinicIds."+personalId,()->clinicIdsFromDb(personalId));
    }

    private List<Long> clinicIdsFromDb(Long personalId) {
        if(isSystemAdmin(personalId)){
            return readWithConnectionRetry(() -> jdbc.query(
                "SELECT id FROM public.rn_klinikalar WHERE aktiv ORDER BY sira_no NULLS LAST,id",
                (rs,row)->rs.getLong(1)));
        }
        return readWithConnectionRetry(() -> jdbc.query(
            "SELECT klinika_id FROM public.rn_personal_klinikalar WHERE personal_id=:pid AND aktiv ORDER BY sira_no NULLS LAST,id",
            new MapSqlParameterSource("pid",personalId),(rs,row)->rs.getLong(1)));
    }

    public boolean hasClinic(Authentication authentication, Long clinicId) {
        return clinicId!=null&&clinicIds(authentication).contains(clinicId);
    }

    public boolean hasFullAccess(Authentication authentication) {
        Long personalId=personalId(authentication);
        return personalId!=null&&isSystemAdmin(personalId);
    }

    public List<MenuModule> menu(Authentication authentication, Long clinicId) {
        return menuSystems(authentication, clinicId).stream()
                .flatMap(system -> system.getModules().stream()).toList();
    }

    public List<MenuSystem> menuSystems(Authentication authentication, Long clinicId) {
        Long personalId=personalId(authentication); if(personalId==null||clinicId==null)return List.of();
        String dil=LocaleContextHolder.getLocale().getLanguage();
        List<MenuRow> rows=readWithConnectionRetry(() -> jdbc.query("""
                SELECT menu.*, sistem.id AS sistem_id, sistem.kod AS sistem_kodu,
                       sistem.ad AS sistem_adi, sistem.ikon AS sistem_ikonu,
                       mt.deyer AS tercume_modul_adi
                FROM public.fn_personal_modul_siyahisi(:pid,:kid) menu
                JOIN public.rn_modullar modul ON modul.id=menu.modul_id
                JOIN public.rn_sistemler sistem ON sistem.id=modul.sistem_id AND sistem.aktiv
                LEFT JOIN public.kn_diller dil ON dil.kod=:dil AND dil.aktiv
                LEFT JOIN public.kn_melumat_tercumeleri mt ON mt.melumat_novu='MODUL'
                     AND mt.menbe_id=menu.modul_id AND mt.saha='ad' AND mt.dil_id=dil.id
                ORDER BY sistem.sira_no NULLS LAST, menu.sira_no NULLS LAST, menu.modul_adi
                """,
            new MapSqlParameterSource("pid",personalId).addValue("kid",clinicId).addValue("dil",dil),
            (rs,row)->new MenuRow(rs.getObject("sistem_id",Long.class),rs.getString("sistem_kodu"),
                localized("menu.system.",rs.getString("sistem_kodu"),rs.getString("sistem_adi")),rs.getString("sistem_ikonu"),
                new MenuModule(rs.getLong("modul_id"),rs.getObject("parent_id",Long.class),
                    rs.getString("modul_kodu"),firstNonBlank(rs.getString("tercume_modul_adi"),localized("menu.module.",rs.getString("modul_kodu"),rs.getString("modul_adi"))),rs.getString("route"),rs.getString("ikon")))));
        List<MenuModule> flat=rows.stream().map(row -> row.module()).toList();
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

    private String localized(String prefix,String code,String fallback) {
        if(code==null||code.isBlank())return fallback;
        return messages.getMessage(prefix+code.toLowerCase(java.util.Locale.ROOT),null,fallback,LocaleContextHolder.getLocale());
    }
    private static String firstNonBlank(String value,String fallback){return value==null||value.isBlank()?fallback:value;}

    private record MenuRow(Long systemId, String systemCode, String systemName,
            String systemIcon, MenuModule module) {}

    public boolean hasPermission(Authentication authentication, Long clinicId, String code) {
        Long personalId=personalId(authentication); if(personalId==null||clinicId==null)return false;
        if(isSystemAdmin(personalId))return true;
        Boolean result=readWithConnectionRetry(() -> jdbc.queryForObject("""
            SELECT EXISTS(SELECT 1 FROM public.fn_personal_selahiyyet_siyahisi(:pid,:kid) WHERE selahiyyet_kodu=:code)
            """,new MapSqlParameterSource("pid",personalId).addValue("kid",clinicId).addValue("code",code),Boolean.class));
        return Boolean.TRUE.equals(result);
    }

    public boolean isRegisteredRoute(String path) {
        Boolean result=shortCache(registeredRouteCache,path,() -> readWithConnectionRetry(() -> jdbc.queryForObject("""
                SELECT EXISTS(SELECT 1 FROM public.rn_modullar WHERE aktiv AND route IS NOT NULL
                  AND (:path=route OR :path LIKE rtrim(route,'/')||'/%'))
                """,new MapSqlParameterSource("path",path),Boolean.class)));
        return Boolean.TRUE.equals(result);
    }

    public boolean canAccessRoute(Authentication authentication, Long clinicId, String path) {
        Long personalId=personalId(authentication);if(personalId==null||clinicId==null)return false;
        if(isSystemAdmin(personalId))return true;
        Boolean result=readWithConnectionRetry(() -> jdbc.queryForObject(
            "SELECT public.fn_personal_route_icazesi_var(:pid,:kid,:path)",
            new MapSqlParameterSource("pid",personalId).addValue("kid",clinicId).addValue("path",path),Boolean.class));
        return Boolean.TRUE.equals(result);
    }

    public String requiredPermission(String path, String method) {
        String key=method+' '+path;
        return shortCache(permissionCache,key,() -> {
            List<String> codes=readWithConnectionRetry(() -> jdbc.query(
                "SELECT selahiyyet_kodu FROM public.rn_route_selahiyyetleri WHERE route=:path AND http_metod=:method AND aktiv",
                new MapSqlParameterSource("path",path).addValue("method",method),(rs,row)->rs.getString(1)));
            return codes.isEmpty()?null:codes.getFirst();
        });
    }

    public void audit(Authentication authentication, Long clinicId, String event, String path,
            String method, String ipAddress, boolean successful) {
        Long personalId=personalId(authentication);
        String username=authentication==null?null:authentication.getName();
        jdbc.queryForObject("""
            SELECT public.kn_tehlukesizlik_auditi_yaz(
              CAST(:pid AS bigint),CAST(:username AS varchar),CAST(:kid AS bigint),
              CAST(:event AS varchar),CAST(:path AS varchar),CAST(:method AS varchar),
              CAST(:ip AS varchar),CAST(:successful AS boolean))
            """,new MapSqlParameterSource("pid",personalId).addValue("username",username)
                .addValue("kid",clinicId).addValue("event",event).addValue("path",path)
                .addValue("method",method).addValue("ip",ipAddress).addValue("successful",successful),Long.class);
    }

    private Long personalId(Authentication authentication) {
        return authentication!=null&&authentication.getPrincipal() instanceof AuthenticatedPersonal p?p.personalId():null;
    }

    private boolean isSystemAdmin(Long personalId) {
        Boolean result=requestCached("simplexs.systemAdmin."+personalId,() -> readWithConnectionRetry(() -> jdbc.queryForObject("""
                SELECT EXISTS(
                    SELECT 1 FROM public.rn_personal_klinikalar pk
                    JOIN public.rn_personal_klinika_rollari pr
                      ON pr.personal_klinika_id=pk.id AND pr.aktiv
                    JOIN public.rn_rollar r ON r.id=pr.rol_id AND r.aktiv AND r.sistem_roludur
                    WHERE pk.personal_id=:pid
                )
                """,new MapSqlParameterSource("pid",personalId),Boolean.class)));
        return Boolean.TRUE.equals(result);
    }

    @SuppressWarnings("unchecked")
    private <T> T requestCached(String key,Supplier<T> loader) {
        RequestAttributes attributes=RequestContextHolder.getRequestAttributes();
        if(attributes==null)return loader.get();
        Object cached=attributes.getAttribute(key,RequestAttributes.SCOPE_REQUEST);
        if(cached!=null)return (T)cached;
        T value=loader.get();
        attributes.setAttribute(key,value,RequestAttributes.SCOPE_REQUEST);
        return value;
    }

    private <T> T shortCache(Map<String,TimedValue<T>> cache,String key,Supplier<T> loader) {
        long now=System.nanoTime();
        TimedValue<T> cached=cache.get(key);
        if(cached!=null&&cached.expiresAt()>now)return cached.value();
        T value=loader.get();
        // Route quruluşu nadir dəyişir; qısa cache hər HTTP sorğusunda eyni metadata-nı DB-dən oxumağın qarşısını alır.
        cache.put(key,new TimedValue<>(value,now+ROUTE_CACHE_NANOS));
        return value;
    }

    private record TimedValue<T>(T value,long expiresAt) {}

    private <T> T readWithConnectionRetry(Supplier<T> operation) {
        try {
            return operation.get();
        } catch (DataAccessResourceFailureException firstFailure) {
            // Read-only təhlükəsizlik sorğusu qırıq socket olduqda yeni connection ilə bir dəfə təkrarlanır.
            return operation.get();
        }
    }
}
