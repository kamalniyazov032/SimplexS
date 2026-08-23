-- Bu migration rn_ təhlükəsizlik metadata-sına toxunur; ayrıca razılıq olmadan canlı DB-yə tətbiq edilməməlidir.
WITH parent AS (SELECT id,sistem_id FROM public.rn_modullar WHERE kod='MIS_PARAM')
INSERT INTO public.rn_modullar(sistem_id,parent_id,kod,ad,aciqlama,route,ikon,menyuda_gorunsun,aktiv,sira_no)
SELECT sistem_id,id,'APP_ROUTINES','Rutinlər','Toplu tətbiq edilən xidmət rutinlərinin idarə edilməsi','/parametrler/rutinler','ti ti-list-check',true,true,77 FROM parent
ON CONFLICT(kod) DO UPDATE SET parent_id=EXCLUDED.parent_id,ad=EXCLUDED.ad,aciqlama=EXCLUDED.aciqlama,route=EXCLUDED.route,ikon=EXCLUDED.ikon,menyuda_gorunsun=true,aktiv=true,sira_no=EXCLUDED.sira_no;

INSERT INTO public.rn_selahiyyetler(modul_id,kod,ad,aciqlama,aktiv)
SELECT m.id,v.kod,v.ad,v.aciqlama,true FROM public.rn_modullar m CROSS JOIN (VALUES
 ('RUTIN_BAX','Rutinlərə bax','Rutin siyahısını və tərkibini görmək'),
 ('RUTIN_IDARE_ET','Rutinləri idarə et','Rutin yaratmaq, yeniləmək və tərkibini dəyişmək')
)v(kod,ad,aciqlama) WHERE m.kod='APP_ROUTINES'
ON CONFLICT(kod) DO UPDATE SET modul_id=EXCLUDED.modul_id,ad=EXCLUDED.ad,aciqlama=EXCLUDED.aciqlama,aktiv=true;

INSERT INTO public.rn_route_selahiyyetleri(route,http_metod,selahiyyet_kodu,aktiv) VALUES
('/parametrler/rutinler/yeni','POST','RUTIN_IDARE_ET',true),('/parametrler/rutinler/yenile','POST','RUTIN_IDARE_ET',true),('/parametrler/rutinler/xidmetler','POST','RUTIN_IDARE_ET',true)
ON CONFLICT(route,http_metod) DO UPDATE SET selahiyyet_kodu=EXCLUDED.selahiyyet_kodu,aktiv=true;

INSERT INTO public.rn_rol_modullari(rol_id,modul_id,aktiv) SELECT r.id,m.id,true FROM public.rn_rollar r CROSS JOIN public.rn_modullar m WHERE r.sistem_roludur AND r.aktiv AND m.kod='APP_ROUTINES' ON CONFLICT(rol_id,modul_id) DO UPDATE SET aktiv=true;
INSERT INTO public.rn_rol_selahiyyetleri(rol_id,selahiyyet_id,aktiv) SELECT r.id,s.id,true FROM public.rn_rollar r CROSS JOIN public.rn_selahiyyetler s WHERE r.sistem_roludur AND r.aktiv AND s.kod IN('RUTIN_BAX','RUTIN_IDARE_ET') ON CONFLICT(rol_id,selahiyyet_id) DO UPDATE SET aktiv=true;
