WITH parametr AS (
    SELECT id, sistem_id FROM public.rn_modullar WHERE kod='MIS_PARAM'
)
INSERT INTO public.rn_modullar(
    sistem_id,parent_id,kod,ad,aciqlama,route,ikon,menyuda_gorunsun,aktiv,sira_no)
SELECT sistem_id,id,'APP_BEDS','Yataqlar','Mərtəbə, palata və yataqların idarəsi',
       '/yataqlar','ti ti-bed',true,true,65
FROM parametr
ON CONFLICT(kod) DO UPDATE SET
    parent_id=EXCLUDED.parent_id,ad=EXCLUDED.ad,aciqlama=EXCLUDED.aciqlama,
    route=EXCLUDED.route,ikon=EXCLUDED.ikon,menyuda_gorunsun=true,aktiv=true,
    sira_no=EXCLUDED.sira_no;

WITH defs(kod,ad) AS (VALUES
    ('YATAQ_BAX','Mərtəbə, palata və yataqlara bax'),
    ('MERTEBE_YARAT','Mərtəbə yarat'),
    ('MERTEBE_YENILE','Mərtəbəni yenilə'),
    ('PALATA_YARAT','Palata yarat'),
    ('PALATA_YENILE','Palatanı yenilə'),
    ('YATAQ_YARAT','Yataq yarat'),
    ('YATAQ_YENILE','Yatağı yenilə')
)
INSERT INTO public.rn_selahiyyetler(modul_id,kod,ad,aktiv)
SELECT m.id,d.kod,d.ad,true FROM defs d
JOIN public.rn_modullar m ON m.kod='APP_BEDS'
ON CONFLICT(kod) DO UPDATE SET modul_id=EXCLUDED.modul_id,ad=EXCLUDED.ad,aktiv=true;

WITH routes(route,method,permission) AS (VALUES
    ('/yataqlar/mertebe/yeni','POST','MERTEBE_YARAT'),
    ('/yataqlar/mertebe/yenile','POST','MERTEBE_YENILE'),
    ('/yataqlar/palata/yeni','POST','PALATA_YARAT'),
    ('/yataqlar/palata/yenile','POST','PALATA_YENILE'),
    ('/yataqlar/yeni','POST','YATAQ_YARAT'),
    ('/yataqlar/yenile','POST','YATAQ_YENILE')
)
INSERT INTO public.rn_route_selahiyyetleri(route,http_metod,selahiyyet_kodu,aktiv)
SELECT route,method,permission,true FROM routes
ON CONFLICT(route,http_metod) DO UPDATE SET selahiyyet_kodu=EXCLUDED.selahiyyet_kodu,aktiv=true;

INSERT INTO public.rn_rol_modullari(rol_id,modul_id,aktiv)
SELECT r.id,m.id,true FROM public.rn_rollar r CROSS JOIN public.rn_modullar m
WHERE r.sistem_roludur AND r.aktiv AND m.kod='APP_BEDS'
ON CONFLICT(rol_id,modul_id) DO UPDATE SET aktiv=true;

INSERT INTO public.rn_rol_selahiyyetleri(rol_id,selahiyyet_id,aktiv)
SELECT r.id,s.id,true FROM public.rn_rollar r CROSS JOIN public.rn_selahiyyetler s
WHERE r.sistem_roludur AND r.aktiv AND s.kod IN (
    'YATAQ_BAX','MERTEBE_YARAT','MERTEBE_YENILE','PALATA_YARAT',
    'PALATA_YENILE','YATAQ_YARAT','YATAQ_YENILE')
ON CONFLICT(rol_id,selahiyyet_id) DO UPDATE SET aktiv=true;
