WITH data(modul_kodu,en,ru) AS (VALUES
('HIS_BILL_INVOICE','Billing invoices','Счета-фактуры'),
('HIS_DET_WARE_OPE','Detailed warehouse operations','Детальные складские операции'),
('HIS_GEN_WARE_OPE','General warehouse operations','Общие складские операции'),
('HIS_MONTHLY_REPORT','Monthly report','Ежемесячный отчёт'),
('HIS_PAT_PRESEN','Patient discharge summaries','Выписки пациентов'),
('LIS_SETTINGS_ANALYSIS','Analysis settings','Настройки анализов'),
('LIS_SETTINGS_BARCODE','Barcode settings','Настройки штрихкодов'),
('LIS_SETTINGS_DEVICE','Device settings','Настройки оборудования'),
('LIS_SETTINGS_MIK_BIO','Microbiology settings','Настройки микробиологии')
), values_by_language AS (
    SELECT modul_kodu,'en' dil_kodu,en deyer FROM data
    UNION ALL
    SELECT modul_kodu,'ru' dil_kodu,ru deyer FROM data
)
INSERT INTO public.kn_melumat_tercumeleri(melumat_novu,menbe_id,saha,dil_id,deyer)
SELECT 'MODUL',m.id,'ad',d.id,v.deyer
FROM values_by_language v
JOIN public.rn_modullar m ON m.kod=v.modul_kodu AND m.aktiv
JOIN public.kn_diller d ON d.kod=v.dil_kodu AND d.aktiv
ON CONFLICT(melumat_novu,menbe_id,saha,dil_id) DO NOTHING;
