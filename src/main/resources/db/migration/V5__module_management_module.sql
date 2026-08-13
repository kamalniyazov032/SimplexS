WITH parent AS (SELECT id,sistem_id FROM public.rn_modullar WHERE kod='MIS_PARAM')
INSERT INTO public.rn_modullar(sistem_id,parent_id,kod,ad,aciqlama,route,ikon,menyuda_gorunsun,aktiv,sira_no)
SELECT sistem_id,id,'APP_MODULES','Modulların idarə edilməsi','Menyu modul ağacının idarə edilməsi','/modullar','ti ti-sitemap',true,true,90 FROM parent
ON CONFLICT(kod) DO UPDATE SET parent_id=EXCLUDED.parent_id,ad=EXCLUDED.ad,aciqlama=EXCLUDED.aciqlama,route=EXCLUDED.route,ikon=EXCLUDED.ikon,menyuda_gorunsun=true,aktiv=true,sira_no=EXCLUDED.sira_no;

INSERT INTO public.rn_selahiyyetler(modul_id,kod,ad,aciqlama,aktiv)
SELECT id,'MODUL_IDARE_ET','Modulları idarə et','Modul adı, parent, ikon, sıra və statusu dəyişmək',true FROM public.rn_modullar WHERE kod='APP_MODULES'
ON CONFLICT(kod) DO UPDATE SET modul_id=EXCLUDED.modul_id,ad=EXCLUDED.ad,aciqlama=EXCLUDED.aciqlama,aktiv=true;

INSERT INTO public.rn_route_selahiyyetleri(route,http_metod,selahiyyet_kodu,aktiv)
VALUES('/modullar/yenile','POST','MODUL_IDARE_ET',true)
ON CONFLICT(route,http_metod) DO UPDATE SET selahiyyet_kodu=EXCLUDED.selahiyyet_kodu,aktiv=true;
