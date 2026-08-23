-- İstifadəçinin açıq tələbi ilə rn_ menyu iyerarxiyası düzəldilir.
UPDATE public.rn_modullar child
SET parent_id=parent.id,sistem_id=parent.sistem_id,sira_no=1
FROM public.rn_modullar parent
WHERE child.kod='APP_PATIENT_REGISTRATION' AND parent.kod='HIS_PATIENT';

UPDATE public.rn_modullar SET sira_no=2 WHERE kod='HIS_INPATIENT';
UPDATE public.rn_modullar SET sira_no=3 WHERE kod='HIS_OUTPATIENT';
