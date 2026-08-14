-- Alt modul verilmiş mövcud rollarda yeni əməliyyat düymələrinin işləməsi üçün
-- həmin modulun bütün aktiv səlahiyyətlərini də tamamla.
INSERT INTO public.rn_rol_selahiyyetleri(rol_id,selahiyyet_id,aktiv)
SELECT rm.rol_id,s.id,true
FROM public.rn_rol_modullari rm
JOIN public.rn_modullar m ON m.id=rm.modul_id AND m.kod IN ('APP_BEDS','APP_CASH_REGISTERS')
JOIN public.rn_selahiyyetler s ON s.modul_id=m.id AND s.aktiv
WHERE rm.aktiv
ON CONFLICT(rol_id,selahiyyet_id) DO UPDATE SET aktiv=true;
