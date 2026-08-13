UPDATE public.rn_modullar SET route='/ambulatorQebul', ikon='ti ti-stethoscope' WHERE kod='HIS_INPATIENT';
UPDATE public.rn_modullar SET route='/stasionar', ikon='ti ti-building-hospital' WHERE kod='HIS_OUTPATIENT';
UPDATE public.rn_modullar SET route='/randevu', ikon='ti ti-calendar-event' WHERE kod='HIS_RANDEVU';
UPDATE public.rn_modullar SET route='/emekdash', ikon='ti ti-users' WHERE kod='MIS_PERSONAL';
UPDATE public.rn_modullar SET route='/shobe', ikon='ti ti-hierarchy-2' WHERE kod='MIS_SERVICE';
UPDATE public.rn_modullar SET route='/parXidmet', ikon='ti ti-stethoscope' WHERE kod='MIS_EXAM';
UPDATE public.rn_modullar SET route='/teskilatlar', ikon='ti ti-buildings' WHERE kod='MIS_ORGANIZATION';
UPDATE public.rn_modullar SET ikon='ti ti-settings' WHERE kod='MIS_PARAM';

WITH parametr AS (SELECT id, sistem_id FROM public.rn_modullar WHERE kod='MIS_PARAM'),
new_modules(kod, ad, route, ikon, sira_no) AS (VALUES
 ('APP_SERVICE_GROUPS','Xidmət qrupları','/xidmetQruplari','ti ti-folders',10),
 ('APP_PRICES','Xidmət qiymətləri','/xidmetQiymetleri','ti ti-currency-manat',20),
 ('APP_ACCOUNT_CODES','Mühasibat kodları','/muhasibatKodu','ti ti-file-invoice',30),
 ('APP_DEPARTMENT_SERVICES','Şöbə xidmətləri','/parShobeXidmet','ti ti-list-details',40),
 ('APP_POSITIONS','Vəzifələr','/vezifeler','ti ti-id-badge-2',50),
 ('APP_BUILDINGS','Binalar','/binalar','ti ti-building',60),
 ('APP_BUILDING_PARAMS','Ümumi parametrlər','/bina-parametrleri','ti ti-adjustments',70),
 ('APP_ROLES','Rollar və səlahiyyətlər','/rollar','ti ti-shield-lock',80)
)
INSERT INTO public.rn_modullar(sistem_id,parent_id,kod,ad,route,ikon,menyuda_gorunsun,aktiv,sira_no)
SELECT p.sistem_id,p.id,n.kod,n.ad,n.route,n.ikon,true,true,n.sira_no
FROM parametr p CROSS JOIN new_modules n
ON CONFLICT (kod) DO UPDATE SET parent_id=EXCLUDED.parent_id, ad=EXCLUDED.ad,
 route=EXCLUDED.route, ikon=EXCLUDED.ikon, menyuda_gorunsun=true, aktiv=true, sira_no=EXCLUDED.sira_no;

WITH permission_defs(modul_kodu, kod, ad) AS (VALUES
 ('APP_PRICES','QIYMET_BAX','Qiymətlərə bax'),
 ('APP_PRICES','QIYMET_YARAT','Qiymət qrupu yarat'),
 ('APP_PRICES','QIYMET_YENILE','Qiyməti yenilə'),
 ('APP_ROLES','ROL_BAX','Rollara bax'),
 ('APP_ROLES','ROL_IDARE_ET','Rol və icazələri idarə et'),
 ('MIS_PERSONAL','PERSONAL_BAX','Personala bax'),
 ('MIS_PERSONAL','PERSONAL_IDARE_ET','Personal və rolları idarə et')
)
INSERT INTO public.rn_selahiyyetler(modul_id,kod,ad,aktiv)
SELECT m.id,p.kod,p.ad,true
FROM permission_defs p JOIN public.rn_modullar m ON m.kod=p.modul_kodu
ON CONFLICT (kod) DO UPDATE SET modul_id=EXCLUDED.modul_id, ad=EXCLUDED.ad, aktiv=true;

-- Sistem rolları idarəetmə ekranına çıxışı itirməsin.
INSERT INTO public.rn_rol_modullari(rol_id,modul_id,aktiv)
SELECT r.id,m.id,true
FROM public.rn_rollar r CROSS JOIN public.rn_modullar m
WHERE r.sistem_roludur AND r.aktiv AND m.aktiv
ON CONFLICT (rol_id,modul_id) DO UPDATE SET aktiv=true;

INSERT INTO public.rn_rol_selahiyyetleri(rol_id,selahiyyet_id,aktiv)
SELECT r.id,s.id,true
FROM public.rn_rollar r CROSS JOIN public.rn_selahiyyetler s
WHERE r.sistem_roludur AND r.aktiv AND s.aktiv
ON CONFLICT (rol_id,selahiyyet_id) DO UPDATE SET aktiv=true;
