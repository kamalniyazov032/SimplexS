-- rn_ metadata/functions: live application requires separate explicit approval.
-- Modul idarəetmə ekranından 1-ci səviyyə sistem başlığı yaradır.
CREATE OR REPLACE FUNCTION public.kn_sistem_yarat(
    p_kod varchar,
    p_ad varchar,
    p_ikon varchar,
    p_sira_no integer
)
RETURNS TABLE(status_kodu varchar, sistem_id bigint, mesaj varchar)
LANGUAGE plpgsql
AS $$
DECLARE
    v_kod varchar;
    v_id bigint;
BEGIN
    v_kod := upper(regexp_replace(trim(coalesce(p_kod,'')), '[^A-Za-z0-9_]', '_', 'g'));
    IF v_kod = '' THEN RETURN QUERY SELECT 'XETA'::varchar,NULL::bigint,'Sistem kodu tələb olunur.'::varchar; RETURN; END IF;
    IF nullif(trim(p_ad),'') IS NULL THEN RETURN QUERY SELECT 'XETA'::varchar,NULL::bigint,'Sistem adı tələb olunur.'::varchar; RETURN; END IF;
    IF EXISTS(SELECT 1 FROM public.rn_sistemler WHERE kod=v_kod) THEN
        RETURN QUERY SELECT 'XETA'::varchar,NULL::bigint,'Bu sistem kodu artıq mövcuddur.'::varchar; RETURN;
    END IF;

    INSERT INTO public.rn_sistemler(kod,ad,ikon,sira_no,aktiv)
    VALUES(v_kod,trim(p_ad),nullif(trim(p_ikon),''),p_sira_no,true)
    RETURNING id INTO v_id;
    RETURN QUERY SELECT 'UGURLU'::varchar,v_id,'Yeni sistem başlığı yaradıldı.'::varchar;
EXCEPTION WHEN unique_violation THEN
    RETURN QUERY SELECT 'XETA'::varchar,NULL::bigint,'Bu sistem kodu artıq mövcuddur.'::varchar;
END;
$$;

COMMENT ON FUNCTION public.kn_sistem_yarat(varchar,varchar,varchar,integer)
IS 'Modul idarəetmə ekranı üçün menyunun 1-ci səviyyə sistem başlığını yaradır.';

-- Seçilmiş sistemin altında route-u olmayan 2-ci səviyyə modul qrupu yaradır.
CREATE OR REPLACE FUNCTION public.kn_modul_qrupu_yarat(
    p_sistem_id bigint,
    p_kod varchar,
    p_ad varchar,
    p_aciqlama varchar,
    p_ikon varchar,
    p_sira_no integer
)
RETURNS TABLE(status_kodu varchar, modul_id bigint, mesaj varchar)
LANGUAGE plpgsql
AS $$
DECLARE
    v_kod varchar;
    v_id bigint;
BEGIN
    v_kod := upper(regexp_replace(trim(coalesce(p_kod,'')), '[^A-Za-z0-9_]', '_', 'g'));
    IF NOT EXISTS(SELECT 1 FROM public.rn_sistemler WHERE id=p_sistem_id AND aktiv) THEN
        RETURN QUERY SELECT 'XETA'::varchar,NULL::bigint,'Sistem tapılmadı.'::varchar; RETURN;
    END IF;
    IF v_kod = '' THEN RETURN QUERY SELECT 'XETA'::varchar,NULL::bigint,'Modul kodu tələb olunur.'::varchar; RETURN; END IF;
    IF nullif(trim(p_ad),'') IS NULL THEN RETURN QUERY SELECT 'XETA'::varchar,NULL::bigint,'Qrup adı tələb olunur.'::varchar; RETURN; END IF;
    IF EXISTS(SELECT 1 FROM public.rn_modullar WHERE kod=v_kod) THEN
        RETURN QUERY SELECT 'XETA'::varchar,NULL::bigint,'Bu modul kodu artıq mövcuddur.'::varchar; RETURN;
    END IF;

    INSERT INTO public.rn_modullar(sistem_id,parent_id,kod,ad,aciqlama,route,ikon,
        menyuda_gorunsun,aktiv,sira_no)
    VALUES(p_sistem_id,NULL,v_kod,trim(p_ad),nullif(trim(p_aciqlama),''),NULL,
        nullif(trim(p_ikon),''),true,true,p_sira_no)
    RETURNING id INTO v_id;
    RETURN QUERY SELECT 'UGURLU'::varchar,v_id,'Yeni 2-ci səviyyə qrup modulu yaradıldı.'::varchar;
EXCEPTION WHEN unique_violation THEN
    RETURN QUERY SELECT 'XETA'::varchar,NULL::bigint,'Bu modul kodu artıq mövcuddur.'::varchar;
END;
$$;

COMMENT ON FUNCTION public.kn_modul_qrupu_yarat(bigint,varchar,varchar,varchar,varchar,integer)
IS 'Sistem altında route-u olmayan 2-ci səviyyə modul qrupu yaradır.';

-- Serializes hierarchy edits so concurrent moves cannot create a cycle.
CREATE OR REPLACE FUNCTION public.kn_modul_yerlesme_xetasi(
    p_modul_id bigint, p_sistem_id bigint, p_parent_id bigint
) RETURNS varchar LANGUAGE plpgsql AS $$
DECLARE
    v_depth integer := -1;
    v_height integer := 0;
    v_cycle boolean := false;
    v_parent public.rn_modullar%ROWTYPE;
BEGIN
    LOCK TABLE public.rn_modullar IN SHARE ROW EXCLUSIVE MODE;
    IF NOT EXISTS(SELECT 1 FROM public.rn_sistemler WHERE id=p_sistem_id AND aktiv) THEN
        RETURN 'modules.invalid_system';
    END IF;
    IF p_modul_id IS NOT NULL THEN
        IF NOT EXISTS(SELECT 1 FROM public.rn_modullar WHERE id=p_modul_id) THEN
            RETURN 'modules.not_found';
        END IF;
        WITH RECURSIVE subtree AS (
            SELECT id,0 depth,ARRAY[id] path FROM public.rn_modullar WHERE id=p_modul_id
            UNION ALL
            SELECT m.id,s.depth+1,s.path||m.id FROM public.rn_modullar m
            JOIN subtree s ON m.parent_id=s.id WHERE NOT m.id=ANY(s.path)
        ) SELECT max(depth),bool_or(id=p_parent_id) INTO v_height,v_cycle FROM subtree;
        IF coalesce(v_cycle,false) THEN RETURN 'modules.cycle'; END IF;
    END IF;
    IF p_parent_id IS NOT NULL THEN
        SELECT * INTO v_parent FROM public.rn_modullar WHERE id=p_parent_id;
        IF NOT FOUND OR v_parent.sistem_id<>p_sistem_id THEN RETURN 'modules.invalid_parent'; END IF;
        IF nullif(trim(v_parent.route),'') IS NOT NULL THEN RETURN 'modules.route_parent'; END IF;
        WITH RECURSIVE ancestors AS (
            SELECT id,parent_id,0 depth,ARRAY[id] path FROM public.rn_modullar WHERE id=p_parent_id
            UNION ALL
            SELECT m.id,m.parent_id,a.depth+1,a.path||m.id FROM public.rn_modullar m
            JOIN ancestors a ON m.id=a.parent_id WHERE NOT m.id=ANY(a.path)
        ) SELECT max(depth) FILTER (WHERE parent_id IS NULL) INTO v_depth FROM ancestors;
        IF v_depth IS NULL THEN RETURN 'modules.cycle'; END IF;
    END IF;
    IF v_depth+1+v_height>2 THEN RETURN 'modules.depth_exceeded'; END IF;
    RETURN NULL;
END;
$$;

CREATE OR REPLACE FUNCTION public.kn_modul_qrupu_yarat_4(
    p_sistem_id bigint, p_parent_id bigint, p_kod varchar, p_ad varchar,
    p_aciqlama varchar, p_ikon varchar, p_sira_no integer
) RETURNS TABLE(status_kodu varchar, modul_id bigint, mesaj varchar)
LANGUAGE plpgsql AS $$
DECLARE v_error varchar; v_kod varchar; v_id bigint;
BEGIN
    v_error := public.kn_modul_yerlesme_xetasi(NULL,p_sistem_id,p_parent_id);
    IF v_error IS NOT NULL THEN RETURN QUERY SELECT 'XETA'::varchar,NULL::bigint,v_error; RETURN; END IF;
    IF nullif(trim(p_ad),'') IS NULL THEN
        RETURN QUERY SELECT 'XETA'::varchar,NULL::bigint,'modules.invalid_name'::varchar; RETURN;
    END IF;
    v_kod := upper(regexp_replace(trim(coalesce(p_kod,'')), '[^A-Za-z0-9_]', '_', 'g'));
    IF v_kod='' THEN RETURN QUERY SELECT 'XETA'::varchar,NULL::bigint,'modules.invalid_code'::varchar; RETURN; END IF;
    INSERT INTO public.rn_modullar(sistem_id,parent_id,kod,ad,aciqlama,route,ikon,menyuda_gorunsun,aktiv,sira_no)
    VALUES(p_sistem_id,p_parent_id,v_kod,trim(p_ad),nullif(trim(p_aciqlama),''),NULL,nullif(trim(p_ikon),''),true,true,p_sira_no)
    RETURNING id INTO v_id;
    RETURN QUERY SELECT 'UGURLU'::varchar,v_id,'modules.saved'::varchar;
EXCEPTION WHEN unique_violation THEN
    RETURN QUERY SELECT 'XETA'::varchar,NULL::bigint,'modules.duplicate_code'::varchar;
END;
$$;

CREATE OR REPLACE FUNCTION public.kn_modul_yenile(
    p_modul_id bigint, p_sistem_id bigint, p_parent_id bigint, p_ad varchar,
    p_aciqlama varchar, p_ikon varchar, p_sira_no integer,
    p_menyuda_gorunsun boolean, p_aktiv boolean
) RETURNS TABLE(status_kodu varchar, modul_id bigint, mesaj varchar)
LANGUAGE plpgsql AS $$
DECLARE v_error varchar;
BEGIN
    v_error := public.kn_modul_yerlesme_xetasi(p_modul_id,p_sistem_id,p_parent_id);
    IF v_error IS NOT NULL THEN RETURN QUERY SELECT 'XETA'::varchar,p_modul_id,v_error; RETURN; END IF;
    IF nullif(trim(p_ad),'') IS NULL THEN
        RETURN QUERY SELECT 'XETA'::varchar,p_modul_id,'modules.invalid_name'::varchar; RETURN;
    END IF;
    WITH RECURSIVE subtree AS (
        SELECT id FROM public.rn_modullar WHERE id=p_modul_id
        UNION
        SELECT m.id FROM public.rn_modullar m JOIN subtree s ON m.parent_id=s.id
    ) UPDATE public.rn_modullar SET sistem_id=p_sistem_id WHERE id IN(SELECT id FROM subtree);
    UPDATE public.rn_modullar SET parent_id=p_parent_id,ad=trim(p_ad),
        aciqlama=nullif(trim(p_aciqlama),''),ikon=nullif(trim(p_ikon),''),sira_no=p_sira_no,
        menyuda_gorunsun=p_menyuda_gorunsun,aktiv=p_aktiv WHERE id=p_modul_id;
    RETURN QUERY SELECT 'UGURLU'::varchar,p_modul_id,'modules.saved'::varchar;
END;
$$;
