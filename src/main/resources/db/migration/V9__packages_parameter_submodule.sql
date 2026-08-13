WITH parametr AS (
    SELECT id, sistem_id
    FROM public.rn_modullar
    WHERE kod='MIS_PARAM'
)
INSERT INTO public.rn_modullar(
    sistem_id,parent_id,kod,ad,aciqlama,route,ikon,
    menyuda_gorunsun,aktiv,sira_no
)
SELECT sistem_id,id,'APP_PACKAGES','Paketlər',
       'Xidmət paketlərinin idarə edilməsi','/parPaket',
       'ti ti-package',true,true,75
FROM parametr
ON CONFLICT(kod) DO UPDATE SET
    sistem_id=EXCLUDED.sistem_id,
    parent_id=EXCLUDED.parent_id,
    ad=EXCLUDED.ad,
    aciqlama=EXCLUDED.aciqlama,
    route=EXCLUDED.route,
    ikon=EXCLUDED.ikon,
    menyuda_gorunsun=true,
    aktiv=true,
    sira_no=EXCLUDED.sira_no;

INSERT INTO public.rn_selahiyyetler(modul_id,kod,ad,aciqlama,aktiv)
SELECT id,'PAKET_BAX','Paketlərə bax',
       'Paket siyahısını və paket tərkibini görmək',true
FROM public.rn_modullar
WHERE kod='APP_PACKAGES'
ON CONFLICT(kod) DO UPDATE SET
    modul_id=EXCLUDED.modul_id,
    ad=EXCLUDED.ad,
    aciqlama=EXCLUDED.aciqlama,
    aktiv=true;

INSERT INTO public.rn_rol_modullari(rol_id,modul_id,aktiv)
SELECT r.id,m.id,true
FROM public.rn_rollar r
CROSS JOIN public.rn_modullar m
WHERE r.sistem_roludur AND r.aktiv AND m.kod='APP_PACKAGES'
ON CONFLICT(rol_id,modul_id) DO UPDATE SET aktiv=true;

INSERT INTO public.rn_rol_selahiyyetleri(rol_id,selahiyyet_id,aktiv)
SELECT r.id,s.id,true
FROM public.rn_rollar r
CROSS JOIN public.rn_selahiyyetler s
WHERE r.sistem_roludur AND r.aktiv AND s.kod='PAKET_BAX'
ON CONFLICT(rol_id,selahiyyet_id) DO UPDATE SET aktiv=true;
