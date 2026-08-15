WITH data(modul_kodu,en,ru) AS (VALUES
('APP_ACCOUNT_CODES','Accounting codes','Бухгалтерские коды'),
('APP_BEDS','Beds','Койки'),
('APP_BUILDING_PARAMS','General parameters','Общие параметры'),
('APP_BUILDINGS','Buildings','Здания'),
('APP_CASH_REGISTERS','Cash registers','Кассы'),
('APP_DEPARTMENT_SERVICES','Department services','Услуги отделений'),
('APP_DIAGNOSES','Diagnoses','Диагнозы'),
('APP_MODULES','Module management','Управление модулями'),
('APP_PACKAGES','Packages','Пакеты'),
('APP_POSITIONS','Positions','Должности'),
('APP_PRICES','Service prices','Цены на услуги'),
('APP_ROLES','Roles and permissions','Роли и разрешения'),
('APP_SERVICE_GROUPS','Service groups','Группы услуг'),
('APP_TRANSLATIONS','Translations','Переводы'),
('FIS_BONUSES','Doctor bonuses','Бонусы врачей'),
('FIS_CASH','Cash desk','Касса'),
('FIS_INOVICE','Invoices','Счета'),
('HIS_INPATIENT','Outpatient admission','Амбулаторный приём'),
('HIS_OUTPATIENT','Inpatient admission','Стационарный приём'),
('HIS_PATIENT','Patient admission','Приём пациентов'),
('HIS_PERSONAL','Personnel management','Управление персоналом'),
('HIS_PHARMACY','Pharmacy','Аптека'),
('HIS_POLIKNIK','Polyclinic','Поликлиника'),
('HIS_PROCEDURE','Procedures','Процедуры'),
('HIS_RANDEVU','Appointments','Запись на приём'),
('HIS_STASIONAR','Department','Отделение'),
('HIS_STOM','Dentistry','Стоматология'),
('HIS_SURGENCY','Operating room','Операционная'),
('LIS_BARCODE','Barcode printing','Печать штрихкодов'),
('LIS_LAB_RESULT','Laboratory results','Лабораторные результаты'),
('LIS_RESULT_CONFIRMATION','Result confirmation','Подтверждение результатов'),
('LIS_SAMPLE_ACCEPT','Sample acceptance','Приём образцов'),
('LIS_SETTINGS','Settings','Настройки'),
('LIS_STATISTIC','Statistics','Статистика'),
('LIS_WAREHOUSE','Laboratory warehouse','Лабораторный склад'),
('MIS_ERROR_LOG','Error log','Журнал ошибок'),
('MIS_EXAM','Services','Услуги'),
('MIS_ORGANIZATION','Organizations','Организации'),
('MIS_PARAM','Parameters','Параметры'),
('MIS_PERSONAL','Doctors and employees','Врачи и сотрудники'),
('MIS_SERVICE','Departments','Отделения'),
('MIS_STATISTIC','Statistics','Статистика'),
('MIS_WAREHOUSE','Warehouses','Склады'),
('RIS_PACS','PACS','PACS'),
('RIS_RADIOLOGY','Radiology','Радиология')
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
