-- Tətbiqin təhlükəsizlik hadisəsini audit jurnalına yazır.
CREATE OR REPLACE FUNCTION public.kn_tehlukesizlik_auditi_yaz(
    p_personal_id bigint,
    p_istifadeci_adi varchar,
    p_klinika_id bigint,
    p_hadise_kodu varchar,
    p_route varchar,
    p_http_metod varchar,
    p_ip_unvan varchar,
    p_ugurlu boolean
)
RETURNS bigint
LANGUAGE plpgsql
AS $$
DECLARE
    v_audit_id bigint;
BEGIN
    IF nullif(trim(p_hadise_kodu), '') IS NULL THEN
        RAISE EXCEPTION 'Hadisə kodu tələb olunur.';
    END IF;

    INSERT INTO public.rn_tehlukesizlik_auditi(
        personal_id, istifadeci_adi, klinika_id, hadise_kodu,
        route, http_metod, ip_unvan, ugurlu
    )
    VALUES(
        p_personal_id, nullif(trim(p_istifadeci_adi), ''), p_klinika_id,
        trim(p_hadise_kodu), nullif(trim(p_route), ''),
        nullif(upper(trim(p_http_metod)), ''), nullif(trim(p_ip_unvan), ''), p_ugurlu
    )
    RETURNING id INTO v_audit_id;

    RETURN v_audit_id;
END;
$$;

COMMENT ON FUNCTION public.kn_tehlukesizlik_auditi_yaz(
    bigint, varchar, bigint, varchar, varchar, varchar, varchar, boolean
) IS 'Login və icazə yoxlamaları zamanı yaranan təhlükəsizlik hadisəsini audit jurnalına yazır və audit qeydinin ID-sini qaytarır.';
