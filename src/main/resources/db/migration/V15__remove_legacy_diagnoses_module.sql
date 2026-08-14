CREATE TEMP TABLE legacy_diaqnoz_modullari ON COMMIT DROP AS
WITH RECURSIVE modul_agaci AS (
    SELECT id
    FROM public.rn_modullar
    WHERE ad = 'Diaqnozlar'
      AND kod <> 'APP_DIAGNOSES'

    UNION

    SELECT m.id
    FROM public.rn_modullar m
    JOIN modul_agaci parent ON m.parent_id = parent.id
    WHERE m.kod <> 'APP_DIAGNOSES'
)
SELECT id FROM modul_agaci;

DELETE FROM public.rn_route_selahiyyetleri route_icazesi
USING public.rn_selahiyyetler selahiyyet
WHERE route_icazesi.selahiyyet_kodu = selahiyyet.kod
  AND selahiyyet.modul_id IN (SELECT id FROM legacy_diaqnoz_modullari);

DELETE FROM public.rn_rol_selahiyyetleri rol_icazesi
USING public.rn_selahiyyetler selahiyyet
WHERE rol_icazesi.selahiyyet_id = selahiyyet.id
  AND selahiyyet.modul_id IN (SELECT id FROM legacy_diaqnoz_modullari);

DELETE FROM public.rn_selahiyyetler
WHERE modul_id IN (SELECT id FROM legacy_diaqnoz_modullari);

DELETE FROM public.rn_rol_modullari
WHERE modul_id IN (SELECT id FROM legacy_diaqnoz_modullari);

DELETE FROM public.rn_modullar
WHERE id IN (SELECT id FROM legacy_diaqnoz_modullari);
