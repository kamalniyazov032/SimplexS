CREATE OR REPLACE FUNCTION public.fn_personal_modul_siyahisi(
    p_personal_id bigint,
    p_klinika_id bigint
)
RETURNS TABLE(
    modul_id bigint, parent_id bigint, modul_kodu varchar, modul_adi varchar,
    route varchar, ikon varchar, sira_no integer, seviyye integer
)
LANGUAGE sql STABLE AS $$
    WITH RECURSIVE personal_rollari AS (
        SELECT r.id, r.sistem_roludur
        FROM public.rn_personal_klinikalar pk
        JOIN public.rn_personal_klinika_rollari pr ON pr.personal_klinika_id=pk.id AND pr.aktiv
        JOIN public.rn_rollar r ON r.id=pr.rol_id AND r.aktiv
        WHERE pk.personal_id=p_personal_id AND pk.klinika_id=p_klinika_id AND pk.aktiv
    ), icazeli AS (
        SELECT DISTINCT m.id
        FROM public.rn_modullar m
        WHERE m.aktiv AND (
            EXISTS (SELECT 1 FROM personal_rollari WHERE sistem_roludur)
            OR EXISTS (SELECT 1 FROM personal_rollari r
                       JOIN public.rn_rol_modullari rm ON rm.rol_id=r.id AND rm.aktiv
                       WHERE rm.modul_id=m.id)
        )
    ), modul_agaci AS (
        SELECT m.id,m.parent_id FROM public.rn_modullar m JOIN icazeli i ON i.id=m.id
        UNION
        SELECT p.id,p.parent_id FROM public.rn_modullar p
        JOIN modul_agaci c ON c.parent_id=p.id WHERE p.aktiv
    )
    SELECT m.id,m.parent_id,m.kod,m.ad,m.route,m.ikon,m.sira_no,
           CASE WHEN m.parent_id IS NULL THEN 0 ELSE 1 END
    FROM modul_agaci a JOIN public.rn_modullar m ON m.id=a.id
    WHERE m.aktiv AND m.menyuda_gorunsun
    ORDER BY m.parent_id NULLS FIRST,m.sira_no NULLS LAST,m.ad;
$$;

CREATE OR REPLACE FUNCTION public.fn_personal_selahiyyet_siyahisi(
    p_personal_id bigint,
    p_klinika_id bigint
)
RETURNS TABLE(selahiyyet_kodu varchar)
LANGUAGE sql STABLE AS $$
    WITH personal_rollari AS (
        SELECT r.id,r.sistem_roludur
        FROM public.rn_personal_klinikalar pk
        JOIN public.rn_personal_klinika_rollari pr ON pr.personal_klinika_id=pk.id AND pr.aktiv
        JOIN public.rn_rollar r ON r.id=pr.rol_id AND r.aktiv
        WHERE pk.personal_id=p_personal_id AND pk.klinika_id=p_klinika_id AND pk.aktiv
    )
    SELECT DISTINCT s.kod
    FROM public.rn_selahiyyetler s
    JOIN public.rn_modullar m ON m.id=s.modul_id AND m.aktiv
    WHERE s.aktiv AND (
        EXISTS (SELECT 1 FROM personal_rollari WHERE sistem_roludur)
        OR EXISTS (SELECT 1 FROM personal_rollari r
                   JOIN public.rn_rol_selahiyyetleri rs ON rs.rol_id=r.id AND rs.aktiv
                   WHERE rs.selahiyyet_id=s.id)
    )
    ORDER BY s.kod;
$$;
