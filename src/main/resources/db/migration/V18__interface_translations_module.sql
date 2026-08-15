CREATE TABLE IF NOT EXISTS public.kn_diller (
    id bigserial PRIMARY KEY,
    kod varchar(10) NOT NULL UNIQUE,
    ad varchar(100) NOT NULL,
    yerli_ad varchar(100) NOT NULL,
    standartdir boolean NOT NULL DEFAULT false,
    aktiv boolean NOT NULL DEFAULT true,
    sira_no integer NOT NULL DEFAULT 0,
    yaranma_tarixi timestamp NOT NULL DEFAULT now(),
    yenilenme_tarixi timestamp NOT NULL DEFAULT now(),
    CONSTRAINT ck_kn_diller_kod CHECK (kod ~ '^[a-z]{2,3}(-[A-Z]{2})?$')
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_kn_diller_standart ON public.kn_diller(standartdir) WHERE standartdir;

CREATE TABLE IF NOT EXISTS public.kn_interfeys_tercumeleri (
    id bigserial PRIMARY KEY,
    dil_id bigint NOT NULL REFERENCES public.kn_diller(id),
    acar varchar(200) NOT NULL,
    deyer text NOT NULL,
    yaranma_tarixi timestamp NOT NULL DEFAULT now(),
    yenilenme_tarixi timestamp NOT NULL DEFAULT now(),
    CONSTRAINT uq_kn_interfeys_tercumeleri UNIQUE(dil_id,acar)
);

INSERT INTO public.kn_diller(kod,ad,yerli_ad,standartdir,aktiv,sira_no)
VALUES('az','Azərbaycan dili','Azərbaycan dili',true,true,1)
ON CONFLICT(kod) DO UPDATE SET ad=EXCLUDED.ad,yerli_ad=EXCLUDED.yerli_ad,standartdir=true,aktiv=true;

WITH parametr AS (SELECT id,sistem_id FROM public.rn_modullar WHERE kod='MIS_PARAM')
INSERT INTO public.rn_modullar(sistem_id,parent_id,kod,ad,aciqlama,route,ikon,menyuda_gorunsun,aktiv,sira_no)
SELECT sistem_id,id,'APP_TRANSLATIONS','Tərcümələr','İnterfeys dilləri və tərcümələrinin idarə edilməsi','/tercumeler','ti ti-language',true,true,95
FROM parametr
ON CONFLICT(kod) DO UPDATE SET parent_id=EXCLUDED.parent_id,ad=EXCLUDED.ad,aciqlama=EXCLUDED.aciqlama,route=EXCLUDED.route,ikon=EXCLUDED.ikon,menyuda_gorunsun=true,aktiv=true,sira_no=EXCLUDED.sira_no;

WITH defs(kod,ad) AS (VALUES ('TERCUME_BAX','Tərcümələrə bax'),('TERCUME_IDARE_ET','Tərcümələri idarə et'))
INSERT INTO public.rn_selahiyyetler(modul_id,kod,ad,aktiv)
SELECT m.id,d.kod,d.ad,true FROM defs d JOIN public.rn_modullar m ON m.kod='APP_TRANSLATIONS'
ON CONFLICT(kod) DO UPDATE SET modul_id=EXCLUDED.modul_id,ad=EXCLUDED.ad,aktiv=true;

INSERT INTO public.rn_route_selahiyyetleri(route,http_metod,selahiyyet_kodu,aktiv) VALUES
('/tercumeler/dil','POST','TERCUME_IDARE_ET',true),
('/tercumeler/yadda-saxla','POST','TERCUME_IDARE_ET',true)
ON CONFLICT(route,http_metod) DO UPDATE SET selahiyyet_kodu=EXCLUDED.selahiyyet_kodu,aktiv=true;

INSERT INTO public.rn_rol_modullari(rol_id,modul_id,aktiv)
SELECT r.id,m.id,true FROM public.rn_rollar r CROSS JOIN public.rn_modullar m WHERE r.sistem_roludur AND r.aktiv AND m.kod='APP_TRANSLATIONS'
ON CONFLICT(rol_id,modul_id) DO UPDATE SET aktiv=true;

INSERT INTO public.rn_rol_selahiyyetleri(rol_id,selahiyyet_id,aktiv)
SELECT r.id,s.id,true FROM public.rn_rollar r CROSS JOIN public.rn_selahiyyetler s WHERE r.sistem_roludur AND r.aktiv AND s.kod IN('TERCUME_BAX','TERCUME_IDARE_ET')
ON CONFLICT(rol_id,selahiyyet_id) DO UPDATE SET aktiv=true;
