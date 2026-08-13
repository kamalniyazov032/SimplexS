WITH defs(modul_kodu,kod,ad) AS (VALUES
 ('APP_BUILDINGS','BINA_YENILE','Binaları yenilə'),
 ('APP_BUILDING_PARAMS','BINA_PARAMETR_YENILE','Bina parametrlərini yenilə'),
 ('APP_ACCOUNT_CODES','MUHASIBAT_KODU_YARAT','Mühasibat kodu yarat'),
 ('APP_ACCOUNT_CODES','MUHASIBAT_KODU_YENILE','Mühasibat kodunu yenilə'),
 ('APP_DEPARTMENT_SERVICES','SOBE_XIDMET_IDARE_ET','Şöbə xidmətlərini idarə et'),
 ('MIS_EXAM','XIDMET_YARAT','Xidmət yarat'),
 ('MIS_EXAM','XIDMET_YENILE','Xidməti yenilə'),
 ('MIS_SERVICE','SOBE_YARAT','Şöbə yarat'),
 ('MIS_SERVICE','SOBE_YENILE','Şöbəni yenilə'),
 ('MIS_ORGANIZATION','TESKILAT_YARAT','Təşkilat yarat'),
 ('MIS_ORGANIZATION','TESKILAT_YENILE','Təşkilatı yenilə'),
 ('APP_POSITIONS','VEZIFE_YARAT','Vəzifə yarat'),
 ('APP_POSITIONS','VEZIFE_YENILE','Vəzifəni yenilə'),
 ('APP_SERVICE_GROUPS','XIDMET_QRUPU_YARAT','Xidmət qrupu yarat'),
 ('APP_SERVICE_GROUPS','XIDMET_QRUPU_YENILE','Xidmət qrupunu yenilə'),
 ('APP_PRICES','QIYMET_BASLIQ_YENILE','Qiymət başlığını yenilə'),
 ('HIS_INPATIENT','AMBULATOR_PASIYENT_YARAT','Ambulator pasiyent yarat')
)
INSERT INTO public.rn_selahiyyetler(modul_id,kod,ad,aktiv)
SELECT m.id,d.kod,d.ad,true FROM defs d JOIN public.rn_modullar m ON m.kod=d.modul_kodu
ON CONFLICT(kod) DO UPDATE SET modul_id=EXCLUDED.modul_id,ad=EXCLUDED.ad,aktiv=true;

WITH routes(route,method,permission) AS (VALUES
 ('/binalar/yenile','POST','BINA_YENILE'),
 ('/bina-parametrleri/yadda-saxla','POST','BINA_PARAMETR_YENILE'),
 ('/muhasibatKodu/yeni','POST','MUHASIBAT_KODU_YARAT'),
 ('/muhasibatKodu/yenile','POST','MUHASIBAT_KODU_YENILE'),
 ('/parShobeXidmet/elave','POST','SOBE_XIDMET_IDARE_ET'),
 ('/parShobeXidmet/cixar','POST','SOBE_XIDMET_IDARE_ET'),
 ('/parXidmet/yeni','POST','XIDMET_YARAT'),
 ('/parXidmet/yenile','POST','XIDMET_YENILE'),
 ('/shobe/yeni','POST','SOBE_YARAT'),
 ('/shobe/yenile','POST','SOBE_YENILE'),
 ('/teskilatlar/yeni','POST','TESKILAT_YARAT'),
 ('/teskilatlar/yenile','POST','TESKILAT_YENILE'),
 ('/vezifeler/yeni','POST','VEZIFE_YARAT'),
 ('/vezifeler/yenile','POST','VEZIFE_YENILE'),
 ('/xidmetQruplari/yeni','POST','XIDMET_QRUPU_YARAT'),
 ('/xidmetQruplari/yenile','POST','XIDMET_QRUPU_YENILE'),
 ('/xidmetQiymetleri/basliq/yenile','POST','QIYMET_BASLIQ_YENILE'),
 ('/ambulatorQebul/patient','POST','AMBULATOR_PASIYENT_YARAT')
)
INSERT INTO public.rn_route_selahiyyetleri(route,http_metod,selahiyyet_kodu,aktiv)
SELECT route,method,permission,true FROM routes
ON CONFLICT(route,http_metod) DO UPDATE SET selahiyyet_kodu=EXCLUDED.selahiyyet_kodu,aktiv=true;
