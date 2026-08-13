package az.simplexs.simplexs.security;

import java.util.List;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsPasswordService;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class DatabaseUserDetailsService implements UserDetailsService, UserDetailsPasswordService {
    private final NamedParameterJdbcTemplate jdbc;

    public DatabaseUserDetailsService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        var users = jdbc.query("""
            SELECT p.id, p.kod, p.sifre, concat_ws(' ', p.ad, p.soyad) AS tam_ad,
                   p.aktiv AND NOT p.hesab_kilidlidir AND NOT p.isden_ayrilib
                   AND EXISTS (SELECT 1 FROM public.rn_personal_klinikalar pk
                               WHERE pk.personal_id=p.id AND pk.aktiv) AS giris_aktivdir
            FROM public.rn_personallar p
            WHERE upper(p.kod)=upper(:username)
               OR (p.email IS NOT NULL AND lower(p.email)=lower(:username))
            ORDER BY CASE WHEN upper(p.kod)=upper(:username) THEN 0 ELSE 1 END
            LIMIT 1
            """, new MapSqlParameterSource("username", username.trim()), (rs, row) ->
                new AuthenticatedPersonal(
                    rs.getObject("id", Long.class), rs.getString("kod"), rs.getString("sifre"),
                    rs.getString("tam_ad"), rs.getBoolean("giris_aktivdir"),
                    List.of(new SimpleGrantedAuthority("ROLE_USER"))));
        if (users.isEmpty() || users.getFirst().password() == null || users.getFirst().password().isBlank()) {
            throw new UsernameNotFoundException("İstifadəçi tapılmadı");
        }
        return users.getFirst();
    }

    @Override
    public UserDetails updatePassword(UserDetails user, String newPassword) {
        jdbc.update("UPDATE public.rn_personallar SET sifre=:password, sifre_deyisme_tarixi=now() WHERE id=:id",
            new MapSqlParameterSource("password", newPassword)
                .addValue("id", ((AuthenticatedPersonal) user).personalId()));
        var personal = (AuthenticatedPersonal) user;
        return new AuthenticatedPersonal(personal.personalId(), personal.username(), newPassword,
            personal.fullName(), personal.enabled(), personal.authorities());
    }
}
