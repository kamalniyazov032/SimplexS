WITH fixes(dil_kodu,acar,deyer) AS (VALUES
('az','ui.08_20_am.0d30af','08:20'),('az','ui.09_58_am.1773b7','09:58'),
('az','ui.10_30_am.7d98bd','10:30'),('az','ui.11_35_pm.831276','23:35'),('az','ui.12_10_pm.d79b05','12:10'),
('ru','ui.08_20_am.0d30af','08:20'),('ru','ui.09_58_am.1773b7','09:58'),
('ru','ui.10_30_am.7d98bd','10:30'),('ru','ui.11_35_pm.831276','23:35'),('ru','ui.12_10_pm.d79b05','12:10')
)
UPDATE public.kn_interfeys_tercumeleri t SET deyer=f.deyer,yenilenme_tarixi=now()
FROM fixes f JOIN public.kn_diller d ON d.kod=f.dil_kodu
WHERE t.dil_id=d.id AND t.acar=f.acar;
