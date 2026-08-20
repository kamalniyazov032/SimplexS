ALTER TABLE public.kn_melumat_tercumeleri
    DROP CONSTRAINT ck_kn_melumat_tercumeleri_nov;
ALTER TABLE public.kn_melumat_tercumeleri
    ADD CONSTRAINT ck_kn_melumat_tercumeleri_nov
    CHECK (melumat_novu IN ('CINS','TEHSIL','AILE_VEZIYYETI','SENED_NOVU','QAN_QRUPU','OLKE','SEHER','MODUL','XIDMET','TESKILAT'));
