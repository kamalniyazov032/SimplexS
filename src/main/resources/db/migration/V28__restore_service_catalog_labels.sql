-- Xidmətlər ekranında istifadə olunan, lakin aktiv kataloqdan düşmüş sabit UI mətnləri.
-- Dəyişən xidmət və qrup adları bu migration-a daxil edilmir.
WITH data(dil_kodu, acar, deyer) AS (VALUES
('az','services.title','Xidmətlər'),
('az','services.subtitle','Ümumi tibbi xidmət kataloqu'),
('az','services.new','Yeni xidmət'),
('az','services.groups','Xidmət qrupları'),
('az','services.groupHint','Siyahını qrupa görə seçin'),
('az','common.all','Hamısı'),
('az','common.active','Aktiv'),
('en','services.title','Services'),
('en','services.subtitle','General medical service catalog'),
('en','services.new','New service'),
('en','services.groups','Service groups'),
('en','services.groupHint','Select a group to filter the list'),
('en','common.all','All'),
('en','common.active','Active'),
('ru','services.title','Услуги'),
('ru','services.subtitle','Общий каталог медицинских услуг'),
('ru','services.new','Новая услуга'),
('ru','services.groups','Группы услуг'),
('ru','services.groupHint','Выберите группу для фильтрации списка'),
('ru','common.all','Все'),
('ru','common.active','Активен')
)
INSERT INTO public.kn_interfeys_tercumeleri(dil_id, acar, deyer)
SELECT d.id, data.acar, data.deyer
FROM data
JOIN public.kn_diller d ON d.kod = data.dil_kodu
ON CONFLICT(dil_id, acar) DO NOTHING;

INSERT INTO kn_tercume_modul_elaqeleri (acar, modul_kodu, ekran)
VALUES
('services.title','MIS_EXAM','pages/xidmet.html'),
('services.subtitle','MIS_EXAM','pages/xidmet.html'),
('services.new','MIS_EXAM','pages/xidmet.html'),
('services.groups','MIS_EXAM','pages/xidmet.html'),
('services.groupHint','MIS_EXAM','pages/xidmet.html'),
('common.all','MIS_EXAM','pages/xidmet.html'),
('common.active','MIS_EXAM','pages/xidmet.html')
ON CONFLICT(acar, modul_kodu, ekran) DO NOTHING;
