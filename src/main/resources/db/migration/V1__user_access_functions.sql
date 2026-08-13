CREATE OR REPLACE FUNCTION public.fn_personal_modul_siyahisi(
    p_personal_id bigint,
    p_klinika_id bigint
)
RETURNS TABLE(
    modul_id bigint,
    parent_id bigint,
    modul_kodu varchar,
    modul_adi varchar,
    route varchar,
    ikon varchar,
    sira_no integer,
    seviyye integer
)
LANGUAGE sql
STABLE
AS $$
    WITH RECURSIVE icazeli AS (
        SELECT DISTINCT m.id
        FROM public.rn_personal_klinikalar pk
        JOIN public.rn_personal_klinika_rollari pr
          ON pr.personal_klinika_id = pk.id AND pr.aktiv
        JOIN public.rn_rollar r
          ON r.id = pr.rol_id AND r.aktiv
        JOIN public.rn_rol_modullari rm
          ON rm.rol_id = r.id AND rm.aktiv
        JOIN public.rn_modullar m
          ON m.id = rm.modul_id AND m.aktiv
        WHERE pk.personal_id = p_personal_id
          AND pk.klinika_id = p_klinika_id
          AND pk.aktiv
    ), modul_agaci AS (
        SELECT m.id, m.parent_id
        FROM public.rn_modullar m
        JOIN icazeli i ON i.id = m.id
        UNION
        SELECT parent.id, parent.parent_id
        FROM public.rn_modullar parent
        JOIN modul_agaci child ON child.parent_id = parent.id
        WHERE parent.aktiv
    )
    SELECT m.id, m.parent_id, m.kod, m.ad, m.route, m.ikon, m.sira_no,
           CASE WHEN m.parent_id IS NULL THEN 0 ELSE 1 END
    FROM modul_agaci a
    JOIN public.rn_modullar m ON m.id = a.id
    WHERE m.aktiv AND m.menyuda_gorunsun
    ORDER BY m.parent_id NULLS FIRST, m.sira_no NULLS LAST, m.ad;
$$;

CREATE OR REPLACE FUNCTION public.fn_personal_selahiyyet_siyahisi(
    p_personal_id bigint,
    p_klinika_id bigint
)
RETURNS TABLE(selahiyyet_kodu varchar)
LANGUAGE sql
STABLE
AS $$
    SELECT DISTINCT s.kod
    FROM public.rn_personal_klinikalar pk
    JOIN public.rn_personal_klinika_rollari pr
      ON pr.personal_klinika_id = pk.id AND pr.aktiv
    JOIN public.rn_rollar r ON r.id = pr.rol_id AND r.aktiv
    JOIN public.rn_rol_selahiyyetleri rs ON rs.rol_id = r.id AND rs.aktiv
    JOIN public.rn_selahiyyetler s ON s.id = rs.selahiyyet_id AND s.aktiv
    JOIN public.rn_modullar m ON m.id = s.modul_id AND m.aktiv
    WHERE pk.personal_id = p_personal_id
      AND pk.klinika_id = p_klinika_id
      AND pk.aktiv
    ORDER BY s.kod;
$$;

CREATE OR REPLACE FUNCTION public.fn_personal_route_icazesi_var(
    p_personal_id bigint,
    p_klinika_id bigint,
    p_route varchar
)
RETURNS boolean
LANGUAGE sql
STABLE
AS $$
    SELECT EXISTS (
        SELECT 1
        FROM public.fn_personal_modul_siyahisi(p_personal_id, p_klinika_id) m
        WHERE m.route IS NOT NULL
          AND (p_route = m.route OR p_route LIKE rtrim(m.route, '/') || '/%')
    );
$$;
