ALTER TABLE public.kn_tercume_modul_elaqeleri
    ADD COLUMN modul_id bigint;

UPDATE public.kn_tercume_modul_elaqeleri e
SET modul_id=m.id
FROM public.rn_modullar m
WHERE e.modul_kodu<>'GLOBAL' AND m.kod=e.modul_kodu;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM public.kn_tercume_modul_elaqeleri
        WHERE modul_kodu<>'GLOBAL' AND modul_id IS NULL
    ) THEN
        RAISE EXCEPTION 'Bəzi tərcümə əlaqələri modul ID-si ilə uyğunlaşdırılmadı';
    END IF;
END $$;

ALTER TABLE public.kn_tercume_modul_elaqeleri
    ADD CONSTRAINT fk_kn_tercume_modul_elaqeleri_modul
    FOREIGN KEY(modul_id) REFERENCES public.rn_modullar(id);

DROP INDEX IF EXISTS public.ix_kn_tercume_modul_elaqeleri_modul;
ALTER TABLE public.kn_tercume_modul_elaqeleri
    DROP CONSTRAINT uq_kn_tercume_modul_elaqeleri;
ALTER TABLE public.kn_tercume_modul_elaqeleri
    DROP COLUMN modul_kodu;

CREATE UNIQUE INDEX uq_kn_tercume_modul_elaqeleri
    ON public.kn_tercume_modul_elaqeleri(acar,COALESCE(modul_id,0),ekran);
CREATE INDEX ix_kn_tercume_modul_elaqeleri_modul
    ON public.kn_tercume_modul_elaqeleri(modul_id,acar);
