INSERT INTO public.rn_route_selahiyyetleri(route,http_metod,selahiyyet_kodu,aktiv)
VALUES('/emekdash/klinika','POST','PERSONAL_IDARE_ET',true)
ON CONFLICT(route,http_metod) DO UPDATE
SET selahiyyet_kodu=EXCLUDED.selahiyyet_kodu,aktiv=true;
