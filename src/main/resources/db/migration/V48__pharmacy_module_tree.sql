-- Prepared menu metadata only; no pharmacy business operations or new routes.
-- Changes rn_modullar and rn_rol_modullari. Do not run on live DB without explicit approval.
DO $$
DECLARE v_pharmacy public.rn_modullar%ROWTYPE; v_parent bigint; v_item record;
BEGIN
    LOCK TABLE public.rn_modullar IN SHARE ROW EXCLUSIVE MODE;
    SELECT * INTO STRICT v_pharmacy FROM public.rn_modullar WHERE kod='HIS_PHARMACY';
    IF v_pharmacy.parent_id IS NOT NULL OR nullif(trim(v_pharmacy.route),'') IS NOT NULL THEN
        RAISE EXCEPTION 'HIS_PHARMACY must be a level-two heading without an existing route; review before applying';
    END IF;
    UPDATE public.rn_modullar SET ad='Əczaxana',aktiv=true,menyuda_gorunsun=true WHERE id=v_pharmacy.id;
    FOR v_item IN SELECT * FROM (VALUES
        ('HIS_PHARMACY_INVOICES','Qaimələr',NULL::text,10),
        ('HIS_PHARMACY_REQUESTS','Anbar tələbləri',NULL::text,20),
        ('HIS_PHARMACY_REQUESTS_SENT','Göndərdiklərim','HIS_PHARMACY_REQUESTS',10),
        ('HIS_PHARMACY_REQUESTS_INCOMING','Gələn tələblər','HIS_PHARMACY_REQUESTS',20),
        ('HIS_PHARMACY_TRANSFER','Anbarlararası transfer',NULL::text,30),
        ('HIS_PHARMACY_RETURNS','Geri qaytarma',NULL::text,40),
        ('HIS_PHARMACY_RETURNS_SENT','Göndərdiklərim','HIS_PHARMACY_RETURNS',10),
        ('HIS_PHARMACY_RETURNS_INCOMING','Qəbul edəcəklərim','HIS_PHARMACY_RETURNS',20),
        ('HIS_PHARMACY_DISPOSAL','Məhv əməliyyatları',NULL::text,50),
        ('HIS_PHARMACY_DISPOSAL_SENT','Məhvə göndərilənlər','HIS_PHARMACY_DISPOSAL',10),
        ('HIS_PHARMACY_DISPOSAL_INCOMING','Məhv anbarına gələnlər','HIS_PHARMACY_DISPOSAL',20),
        ('HIS_PHARMACY_DISPOSAL_ACTUAL','Faktiki məhv','HIS_PHARMACY_DISPOSAL',30),
        ('HIS_PHARMACY_STOCK','Stok və qalıqlar',NULL::text,60),
        ('HIS_PHARMACY_MOVEMENTS','Material hərəkətləri',NULL::text,70),
        ('HIS_PHARMACY_REPORTS','Hesabatlar',NULL::text,80),
        ('HIS_PHARMACY_REPORTS_MONTHLY','Aylıq dövriyyə','HIS_PHARMACY_REPORTS',10)
    ) AS items(kod,ad,parent_kod,sira_no)
    LOOP
        v_parent := v_pharmacy.id;
        IF v_item.parent_kod IS NOT NULL THEN
            SELECT id INTO STRICT v_parent FROM public.rn_modullar WHERE kod=v_item.parent_kod;
        END IF;
        INSERT INTO public.rn_modullar(sistem_id,parent_id,kod,ad,route,ikon,menyuda_gorunsun,aktiv,sira_no)
        VALUES(v_pharmacy.sistem_id,v_parent,v_item.kod,v_item.ad,NULL,'ti ti-point',true,true,v_item.sira_no)
        ON CONFLICT(kod) DO NOTHING;
    END LOOP;
END;
$$;

-- Existing pharmacy roles receive its new navigation entries, without operation permissions.
WITH RECURSIVE pharmacy AS (
    SELECT id FROM public.rn_modullar WHERE kod='HIS_PHARMACY'
    UNION
    SELECT m.id FROM public.rn_modullar m JOIN pharmacy p ON m.parent_id=p.id
)
INSERT INTO public.rn_rol_modullari(rol_id,modul_id,aktiv)
SELECT access.rol_id,child.id,true FROM public.rn_rol_modullari access
JOIN public.rn_modullar root ON root.id=access.modul_id AND root.kod='HIS_PHARMACY'
CROSS JOIN pharmacy child WHERE access.aktiv AND child.id<>root.id
ON CONFLICT(rol_id,modul_id) DO NOTHING;
