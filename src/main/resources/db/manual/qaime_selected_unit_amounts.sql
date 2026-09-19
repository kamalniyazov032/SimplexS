-- Apply only with explicit approval for the live database.
-- Amounts use the selected unit; stock conversion and JSON fields remain unchanged.
BEGIN;

CREATE OR REPLACE FUNCTION public.fn_anbar_qaime_material_elave_et(p_qaime_id bigint, p_anbar_id bigint, p_mehsul_qrupu_id bigint, p_material_id bigint, p_vahid_id bigint, p_miqdar numeric, p_alis_qiymeti numeric, p_satis_baza_qiymeti numeric DEFAULT NULL::numeric, p_satis_faizi numeric DEFAULT NULL::numeric, p_son_istifade_tarixi date DEFAULT NULL::date, p_seriya_no character varying DEFAULT NULL::character varying, p_aciqlama text DEFAULT NULL::text, p_yaradan_personal_id bigint DEFAULT NULL::bigint, p_edv_faizi numeric DEFAULT NULL::numeric)
 RETURNS TABLE(status_kodu character varying, qaime_material_id bigint, mesaj character varying)
 LANGUAGE plpgsql
AS $function$
DECLARE
    p_klinika_id bigint;
    v_qaime_tarixi date;
    v_h record;
    v_emsal numeric(18,6);
    v_ana_miqdar numeric(18,3);
    v_edv numeric(7,3);
    v_edvsiz numeric(18,4);
    v_edv_mebleg numeric(18,4);
    v_alis_mebleg numeric(18,4);
    v_satis_qiymeti numeric(18,4);
    v_satis_meblegi numeric(18,4);
    v_sira_no int;
    v_id bigint;
BEGIN
    SELECT q.klinika_id INTO p_klinika_id FROM public.rn_anbar_qaimeleri q WHERE q.id=p_qaime_id;
    SELECT q.qaime_tarixi INTO v_qaime_tarixi
    FROM public.rn_anbar_qaimeleri q
    WHERE q.id=p_qaime_id AND q.klinika_id=p_klinika_id AND q.anbar_id=p_anbar_id AND q.aktiv=true;
    IF NOT FOUND THEN
        RETURN QUERY SELECT 'QAIME_TAPILMADI'::varchar,NULL::bigint,'Qaimə tapılmadı və ya seçilmiş anbara aid deyil'::varchar; RETURN;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM public.rn_anbar_qaimeleri q
        WHERE q.id = p_qaime_id
          AND q.kassaya_gonderilib = true
    ) THEN
        RETURN QUERY SELECT
            'QAIME_KASSAYA_GONDERILIB'::varchar,
            NULL::bigint,
            'Qaimə kassaya göndərildiyi üçün material əlavə edilə bilməz'::varchar;
        RETURN;
    END IF;

    SELECT * INTO v_h FROM public.fn_anbar_qaime_material_hazirla(
        p_anbar_id=>p_anbar_id,p_qaime_tarixi=>v_qaime_tarixi,
        p_mehsul_qrupu_id=>p_mehsul_qrupu_id,p_material_id=>p_material_id,
        p_vahid_id=>p_vahid_id,p_miqdar=>p_miqdar,p_alis_qiymeti=>p_alis_qiymeti,
        p_satis_baza_qiymeti=>p_satis_baza_qiymeti,p_satis_faizi=>p_satis_faizi,
        p_son_istifade_tarixi=>p_son_istifade_tarixi,p_seriya_no=>p_seriya_no,p_aciqlama=>p_aciqlama
    );
    IF NOT COALESCE(v_h.ugurlu,false) THEN
        RETURN QUERY SELECT v_h.status_kodu::varchar,NULL::bigint,v_h.mesaj::varchar; RETURN;
    END IF;

    IF p_yaradan_personal_id IS NOT NULL AND NOT EXISTS(
        SELECT 1 FROM public.rn_personallar p WHERE p.id=p_yaradan_personal_id AND p.aktiv=true
    ) THEN
        RETURN QUERY SELECT 'YARADAN_PERSONAL_TAPILMADI'::varchar,NULL::bigint,'Yaradan personal tapılmadı və ya aktiv deyil'::varchar; RETURN;
    END IF;

    SELECT mv.ana_vahide_emsal INTO v_emsal
    FROM public.rn_material_vahidleri mv
    WHERE mv.klinika_id=p_klinika_id AND mv.material_id=p_material_id AND mv.vahid_id=p_vahid_id AND mv.aktiv=true;

    v_ana_miqdar:=round((p_miqdar*v_emsal)::numeric,3);

    v_edv:=p_edv_faizi;
    IF v_edv IS NULL OR v_edv<0 OR v_edv>100 OR round(v_edv,3)<>v_edv THEN
        RETURN QUERY SELECT 'EDV_FAIZI_YANLISDIR'::varchar,NULL::bigint,'ƏDV faizi 0–100 aralığında, ən çox 3 onluqla göndərilməlidir'::varchar; RETURN;
    END IF;
    v_edvsiz:=round(p_miqdar*p_alis_qiymeti,4);
    v_edv_mebleg:=round(v_edvsiz*v_edv/100,4);
    v_alis_mebleg:=round(v_edvsiz+v_edv_mebleg,4);
    IF p_satis_baza_qiymeti IS NOT NULL THEN
        v_satis_qiymeti:=round(p_satis_baza_qiymeti,4);
        v_satis_meblegi:=round(p_miqdar*v_satis_qiymeti,4);
    END IF;

    SELECT COALESCE(MAX(qm.sira_no),0)+1 INTO v_sira_no
    FROM public.rn_anbar_qaime_materiallari qm WHERE qm.qaime_id=p_qaime_id;

    INSERT INTO public.rn_anbar_qaime_materiallari(
        qaime_id,material_id,sira_no,miqdar,secilen_vahid_id,secilen_vahid_miqdari,ana_vahide_emsal,
        alis_qiymeti,satis_qiymeti,satis_meblegi,edv_faizi,edvsiz_mebleg,edv_meblegi,alis_meblegi,
        satis_baza_qiymeti,satis_faizi,son_istifade_tarixi,seriya_no,aciqlama,hereket_gorub,aktiv,yaradan_personal_id
    ) VALUES(
        p_qaime_id,p_material_id,v_sira_no,v_ana_miqdar,p_vahid_id,p_miqdar,v_emsal,
        p_alis_qiymeti,v_satis_qiymeti,v_satis_meblegi,v_edv,v_edvsiz,v_edv_mebleg,v_alis_mebleg,
        p_satis_baza_qiymeti,p_satis_faizi,p_son_istifade_tarixi,NULLIF(trim(p_seriya_no),''),NULLIF(trim(p_aciqlama),''),false,true,p_yaradan_personal_id
    ) RETURNING id INTO v_id;

    RETURN QUERY SELECT 'UGURLU'::varchar,v_id,'Material qaiməyə uğurla əlavə edildi'::varchar;
END;
$function$;

CREATE OR REPLACE FUNCTION public.fn_anbar_qaime_material_yenile(p_qaime_material_id bigint, p_anbar_id bigint, p_vahid_id bigint DEFAULT NULL::bigint, p_miqdar numeric DEFAULT NULL::numeric, p_alis_qiymeti numeric DEFAULT NULL::numeric, p_satis_baza_qiymeti numeric DEFAULT NULL::numeric, p_satis_faizi numeric DEFAULT NULL::numeric, p_son_istifade_tarixi date DEFAULT NULL::date, p_son_istifade_tarixi_deyisdirilsin boolean DEFAULT false, p_seriya_no character varying DEFAULT NULL::character varying, p_seriya_no_deyisdirilsin boolean DEFAULT false, p_aciqlama text DEFAULT NULL::text, p_aciqlama_deyisdirilsin boolean DEFAULT false, p_yenileyen_personal_id bigint DEFAULT NULL::bigint, p_edv_faizi numeric DEFAULT NULL::numeric)
 RETURNS TABLE(status_kodu character varying, qaime_material_id bigint, mesaj character varying)
 LANGUAGE plpgsql
AS $function$
DECLARE
    p_klinika_id bigint;
    v_qaime_tarixi date;
    v_hereket_gorub boolean;
    v_material_id bigint;
    v_vahid_id bigint;
    v_secilen_miqdar numeric;
    v_emsal numeric;
    v_alis numeric;
    v_satis_baza numeric;
    v_satis_faiz numeric;
    v_son date;
    v_edv numeric;
    v_ana_miqdar numeric;
    v_edvsiz numeric;
    v_edv_mebleg numeric;
    v_alis_mebleg numeric;
    v_satis_qiymeti numeric;
    v_satis_meblegi numeric;
BEGIN
    SELECT q.klinika_id INTO p_klinika_id FROM public.rn_anbar_qaime_materiallari qm JOIN public.rn_anbar_qaimeleri q ON q.id=qm.qaime_id WHERE qm.id=p_qaime_material_id;
    SELECT q.qaime_tarixi,qm.hereket_gorub,qm.material_id,qm.secilen_vahid_id,qm.secilen_vahid_miqdari,
           qm.ana_vahide_emsal,qm.alis_qiymeti,qm.satis_baza_qiymeti,qm.satis_faizi,qm.son_istifade_tarixi,qm.edv_faizi
    INTO v_qaime_tarixi,v_hereket_gorub,v_material_id,v_vahid_id,v_secilen_miqdar,
         v_emsal,v_alis,v_satis_baza,v_satis_faiz,v_son,v_edv
    FROM public.rn_anbar_qaime_materiallari qm
    JOIN public.rn_anbar_qaimeleri q ON q.id=qm.qaime_id
    WHERE qm.id=p_qaime_material_id AND qm.aktiv=true AND q.aktiv=true
      AND q.klinika_id=p_klinika_id AND q.anbar_id=p_anbar_id;
    IF NOT FOUND THEN
        RETURN QUERY SELECT 'QAIME_MATERIALI_TAPILMADI'::varchar,p_qaime_material_id,'Qaimə materialı tapılmadı və ya seçilmiş anbara aid deyil'::varchar; RETURN;
    END IF;
    IF EXISTS (
        SELECT 1
        FROM public.rn_anbar_qaime_materiallari qm
        JOIN public.rn_anbar_qaimeleri q ON q.id = qm.qaime_id
        WHERE qm.id = p_qaime_material_id
          AND q.kassaya_gonderilib = true
    ) THEN
        RETURN QUERY SELECT
            'QAIME_KASSAYA_GONDERILIB'::varchar,
            p_qaime_material_id,
            'Qaimə kassaya göndərildiyi üçün material redaktə edilə bilməz'::varchar;
        RETURN;
    END IF;

    IF v_hereket_gorub THEN
        RETURN QUERY SELECT 'MATERIAL_HEREKET_GORUB'::varchar,p_qaime_material_id,'Bu qaimə materialı artıq hərəkət görüb, redaktə edilə bilməz'::varchar; RETURN;
    END IF;
    IF EXISTS(
        SELECT 1 FROM public.rn_anbar_kilidleri k WHERE k.klinika_id=p_klinika_id AND k.anbar_id=p_anbar_id
          AND k.il=EXTRACT(YEAR FROM v_qaime_tarixi)::int AND k.ay=EXTRACT(MONTH FROM v_qaime_tarixi)::int AND k.kilidlidir=true
    ) THEN
        RETURN QUERY SELECT 'QAIME_AYI_KILIDLIDIR'::varchar,p_qaime_material_id,'Qaimənin aid olduğu ay kilidlidir, material redaktə edilə bilməz'::varchar; RETURN;
    END IF;

    v_vahid_id:=COALESCE(p_vahid_id,v_vahid_id);
    v_secilen_miqdar:=COALESCE(p_miqdar,v_secilen_miqdar);
    v_alis:=COALESCE(p_alis_qiymeti,v_alis);
    v_satis_baza:=COALESCE(p_satis_baza_qiymeti,v_satis_baza);
    v_satis_faiz:=COALESCE(p_satis_faizi,v_satis_faiz);
    IF COALESCE(p_son_istifade_tarixi_deyisdirilsin,false) THEN v_son:=p_son_istifade_tarixi; END IF;

    SELECT mv.ana_vahide_emsal INTO v_emsal
    FROM public.rn_material_vahidleri mv
    WHERE mv.klinika_id=p_klinika_id AND mv.material_id=v_material_id AND mv.vahid_id=v_vahid_id AND mv.aktiv=true;
    IF NOT FOUND THEN
        RETURN QUERY SELECT 'MATERIAL_VAHIDI_TAPILMADI'::varchar,p_qaime_material_id,'Seçilmiş vahid materiala bağlı deyil və ya aktiv deyil'::varchar; RETURN;
    END IF;
    IF v_secilen_miqdar IS NULL OR v_secilen_miqdar<=0 THEN
        RETURN QUERY SELECT 'MIQDAR_YANLISDIR'::varchar,p_qaime_material_id,'Miqdar sıfırdan böyük olmalıdır'::varchar; RETURN;
    END IF;
    IF v_alis IS NULL OR v_alis<0 THEN
        RETURN QUERY SELECT 'ALIS_QIYMETI_YANLISDIR'::varchar,p_qaime_material_id,'Alış qiyməti mənfi ola bilməz'::varchar; RETURN;
    END IF;
    IF v_son IS NOT NULL AND v_son<v_qaime_tarixi THEN
        RETURN QUERY SELECT 'SON_ISTIFADE_TARIXI_YANLISDIR'::varchar,p_qaime_material_id,'Son istifadə tarixi qaimə tarixindən əvvəl ola bilməz'::varchar; RETURN;
    END IF;
    IF p_yenileyen_personal_id IS NOT NULL AND NOT EXISTS(SELECT 1 FROM public.rn_personallar p WHERE p.id=p_yenileyen_personal_id AND p.aktiv=true) THEN
        RETURN QUERY SELECT 'YENILEYEN_PERSONAL_TAPILMADI'::varchar,p_qaime_material_id,'Yeniləyən personal tapılmadı və ya aktiv deyil'::varchar; RETURN;
    END IF;

    v_edv:=COALESCE(p_edv_faizi,v_edv);
    IF v_edv IS NULL OR v_edv<0 OR v_edv>100 OR round(v_edv,3)<>v_edv THEN
        RETURN QUERY SELECT 'EDV_FAIZI_YANLISDIR'::varchar,p_qaime_material_id,'ƏDV faizi 0–100 aralığında, ən çox 3 onluqla göndərilməlidir'::varchar; RETURN;
    END IF;
    v_ana_miqdar:=round(v_secilen_miqdar*v_emsal,3);
    v_edvsiz:=round(v_secilen_miqdar*v_alis,4);
    v_edv_mebleg:=round(v_edvsiz*v_edv/100,4);
    v_alis_mebleg:=round(v_edvsiz+v_edv_mebleg,4);
    IF v_satis_baza IS NOT NULL THEN
        v_satis_qiymeti:=round(v_satis_baza,4);
        v_satis_meblegi:=round(v_secilen_miqdar*v_satis_qiymeti,4);
    END IF;

    UPDATE public.rn_anbar_qaime_materiallari qm SET
        secilen_vahid_id=v_vahid_id,secilen_vahid_miqdari=v_secilen_miqdar,ana_vahide_emsal=v_emsal,miqdar=v_ana_miqdar,
        alis_qiymeti=v_alis,edv_faizi=v_edv,edvsiz_mebleg=v_edvsiz,edv_meblegi=v_edv_mebleg,alis_meblegi=v_alis_mebleg,
        satis_baza_qiymeti=v_satis_baza,satis_faizi=v_satis_faiz,satis_qiymeti=v_satis_qiymeti,satis_meblegi=v_satis_meblegi,
        son_istifade_tarixi=v_son,
        seriya_no=CASE WHEN COALESCE(p_seriya_no_deyisdirilsin,false) THEN NULLIF(trim(p_seriya_no),'') ELSE qm.seriya_no END,
        aciqlama=CASE WHEN COALESCE(p_aciqlama_deyisdirilsin,false) THEN NULLIF(trim(p_aciqlama),'') ELSE qm.aciqlama END,
        yenilenme_tarixi=CURRENT_TIMESTAMP,yenileyen_personal_id=p_yenileyen_personal_id
    WHERE qm.id=p_qaime_material_id;

    RETURN QUERY SELECT 'UGURLU'::varchar,p_qaime_material_id,'Qaimə materialı uğurla yeniləndi'::varchar;
END;
$function$;

COMMIT;
