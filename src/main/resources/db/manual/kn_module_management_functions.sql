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

-- Modulu sistem və ya 2-ci səviyyə parent arasında daşıyır, maksimum 3 səviyyəni qoruyur.
CREATE OR REPLACE FUNCTION public.kn_modul_yenile(
    p_modul_id bigint,
    p_sistem_id bigint,
    p_parent_id bigint,
    p_ad varchar,
    p_aciqlama varchar,
    p_ikon varchar,
    p_sira_no integer,
    p_menyuda_gorunsun boolean,
    p_aktiv boolean
)
RETURNS TABLE(status_kodu varchar, modul_id bigint, mesaj varchar)
LANGUAGE plpgsql
AS $$
DECLARE
    v_sistem_id bigint := p_sistem_id;
BEGIN
    IF NOT EXISTS(SELECT 1 FROM public.rn_modullar WHERE id=p_modul_id) THEN
        RETURN QUERY SELECT 'XETA'::varchar,NULL::bigint,'Modul tapılmadı.'::varchar; RETURN;
    END IF;
    IF p_modul_id=p_parent_id THEN
        RETURN QUERY SELECT 'XETA'::varchar,p_modul_id,'Modul özünün alt modulu ola bilməz.'::varchar; RETURN;
    END IF;

    IF p_parent_id IS NOT NULL THEN
        SELECT sistem_id INTO v_sistem_id FROM public.rn_modullar
        WHERE id=p_parent_id AND parent_id IS NULL;
        IF v_sistem_id IS NULL THEN
            RETURN QUERY SELECT 'XETA'::varchar,p_modul_id,'Üst modul yalnız 2-ci səviyyə modulu ola bilər.'::varchar; RETURN;
        END IF;
        IF EXISTS(SELECT 1 FROM public.rn_modullar WHERE parent_id=p_modul_id) THEN
            RETURN QUERY SELECT 'XETA'::varchar,p_modul_id,'Alt modulları olan modul 3-cü səviyyəyə keçirilə bilməz.'::varchar; RETURN;
        END IF;
    END IF;

    IF v_sistem_id IS NULL OR NOT EXISTS(SELECT 1 FROM public.rn_sistemler WHERE id=v_sistem_id AND aktiv) THEN
        RETURN QUERY SELECT 'XETA'::varchar,p_modul_id,'Seçilmiş sistem aktiv deyil və ya tapılmadı.'::varchar; RETURN;
    END IF;

    -- Qrup başqa sistemə keçiriləndə bütün alt modullar da həmin sistemə keçirilir.
    WITH RECURSIVE subtree AS (
        SELECT id FROM public.rn_modullar WHERE parent_id=p_modul_id
        UNION ALL
        SELECT m.id FROM public.rn_modullar m JOIN subtree s ON m.parent_id=s.id
    )
    UPDATE public.rn_modullar SET sistem_id=v_sistem_id WHERE id IN(SELECT id FROM subtree);

    UPDATE public.rn_modullar
    SET sistem_id=v_sistem_id,parent_id=p_parent_id,ad=trim(p_ad),
        aciqlama=nullif(trim(p_aciqlama),''),ikon=nullif(trim(p_ikon),''),sira_no=p_sira_no,
        menyuda_gorunsun=p_menyuda_gorunsun,aktiv=p_aktiv
    WHERE id=p_modul_id;

    RETURN QUERY SELECT 'UGURLU'::varchar,p_modul_id,'Modul strukturu yeniləndi.'::varchar;
END;
$$;

COMMENT ON FUNCTION public.kn_modul_yenile(bigint,bigint,bigint,varchar,varchar,varchar,integer,boolean,boolean)
IS 'Modulu 1-3 səviyyəli menyu qaydasına uyğun daşıyır və görünüş məlumatlarını yeniləyir.';
