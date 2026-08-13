INSERT INTO public.rn_selahiyyetler(modul_id,kod,ad,aktiv)
SELECT id,'QIYMET_BASLIQ_SIL','Qiymət başlığını sil',true
FROM public.rn_modullar WHERE kod='APP_PRICES'
ON CONFLICT(kod) DO UPDATE SET modul_id=EXCLUDED.modul_id,ad=EXCLUDED.ad,aktiv=true;

INSERT INTO public.rn_route_selahiyyetleri(route,http_metod,selahiyyet_kodu,aktiv)
VALUES('/xidmetQiymetleri/basliq/sil','POST','QIYMET_BASLIQ_SIL',true)
ON CONFLICT(route,http_metod) DO UPDATE SET selahiyyet_kodu=EXCLUDED.selahiyyet_kodu,aktiv=true;

INSERT INTO public.rn_rol_selahiyyetleri(rol_id,selahiyyet_id,aktiv)
SELECT r.id,s.id,true FROM public.rn_rollar r CROSS JOIN public.rn_selahiyyetler s
WHERE r.sistem_roludur AND r.aktiv AND s.kod='QIYMET_BASLIQ_SIL'
ON CONFLICT(rol_id,selahiyyet_id) DO UPDATE SET aktiv=true;
