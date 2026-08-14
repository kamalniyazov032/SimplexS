WITH parametr AS (
    SELECT id, sistem_id FROM public.rn_modullar WHERE kod='MIS_PARAM'
)
INSERT INTO public.rn_modullar(
    sistem_id,parent_id,kod,ad,aciqlama,route,ikon,menyuda_gorunsun,aktiv,sira_no)
SELECT sistem_id,id,'APP_CASH_REGISTERS','Kassalar','Kassaların idarəsi',
       '/kassalar','ti ti-cash-register',true,true,66
FROM parametr
ON CONFLICT(kod) DO UPDATE SET
    parent_id=EXCLUDED.parent_id,ad=EXCLUDED.ad,aciqlama=EXCLUDED.aciqlama,
    route=EXCLUDED.route,ikon=EXCLUDED.ikon,menyuda_gorunsun=true,aktiv=true,
    sira_no=EXCLUDED.sira_no;

WITH defs(kod,ad) AS (VALUES
    ('KASSA_BAX','Kassalara bax'),
    ('KASSA_YENILE','Kassanı yenilə')
)
INSERT INTO public.rn_selahiyyetler(modul_id,kod,ad,aktiv)
SELECT m.id,d.kod,d.ad,true FROM defs d
JOIN public.rn_modullar m ON m.kod='APP_CASH_REGISTERS'
ON CONFLICT(kod) DO UPDATE SET modul_id=EXCLUDED.modul_id,ad=EXCLUDED.ad,aktiv=true;

INSERT INTO public.rn_route_selahiyyetleri(route,http_metod,selahiyyet_kodu,aktiv)
VALUES('/kassalar/yenile','POST','KASSA_YENILE',true)
ON CONFLICT(route,http_metod) DO UPDATE SET selahiyyet_kodu=EXCLUDED.selahiyyet_kodu,aktiv=true;

INSERT INTO public.rn_rol_modullari(rol_id,modul_id,aktiv)
SELECT r.id,m.id,true FROM public.rn_rollar r CROSS JOIN public.rn_modullar m
WHERE r.sistem_roludur AND r.aktiv AND m.kod='APP_CASH_REGISTERS'
ON CONFLICT(rol_id,modul_id) DO UPDATE SET aktiv=true;

INSERT INTO public.rn_rol_selahiyyetleri(rol_id,selahiyyet_id,aktiv)
SELECT r.id,s.id,true FROM public.rn_rollar r CROSS JOIN public.rn_selahiyyetler s
WHERE r.sistem_roludur AND r.aktiv AND s.kod IN ('KASSA_BAX','KASSA_YENILE')
ON CONFLICT(rol_id,selahiyyet_id) DO UPDATE SET aktiv=true;
