CREATE TABLE IF NOT EXISTS public.kn_melumat_tercumeleri (
    id bigserial PRIMARY KEY,
    melumat_novu varchar(60) NOT NULL,
    menbe_id bigint NOT NULL,
    saha varchar(40) NOT NULL,
    dil_id bigint NOT NULL REFERENCES public.kn_diller(id),
    deyer text NOT NULL,
    yaranma_tarixi timestamp NOT NULL DEFAULT now(),
    yenilenme_tarixi timestamp NOT NULL DEFAULT now(),
    CONSTRAINT uq_kn_melumat_tercumeleri UNIQUE(melumat_novu,menbe_id,saha,dil_id),
    CONSTRAINT ck_kn_melumat_tercumeleri_nov CHECK (melumat_novu IN ('CINS','TEHSIL','AILE_VEZIYYETI','SENED_NOVU','QAN_QRUPU','OLKE','SEHER')),
    CONSTRAINT ck_kn_melumat_tercumeleri_saha CHECK (saha IN ('ad','aciqlama'))
);
CREATE INDEX IF NOT EXISTS ix_kn_melumat_tercumeleri_axtaris
    ON public.kn_melumat_tercumeleri(melumat_novu,dil_id,menbe_id);
