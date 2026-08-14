UPDATE public.rn_modullar modul
SET parent_id = NULL
FROM public.rn_modullar umumi
WHERE umumi.kod = 'APP_GENERAL_INFORMATION'
  AND modul.parent_id = umumi.id;

DELETE FROM public.rn_rol_modullari rol_modulu
USING public.rn_modullar umumi
WHERE rol_modulu.modul_id = umumi.id
  AND umumi.kod = 'APP_GENERAL_INFORMATION';

DELETE FROM public.rn_modullar
WHERE kod = 'APP_GENERAL_INFORMATION';
