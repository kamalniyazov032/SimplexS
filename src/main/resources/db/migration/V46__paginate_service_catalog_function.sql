CREATE OR REPLACE FUNCTION public.fn_xidmet_siyahisi(
    p_klinika_id bigint,
    p_xidmet_qrupu_id bigint DEFAULT NULL,
    p_aktiv boolean DEFAULT true,
    p_alt_qruplar_daxil boolean DEFAULT true,
    p_xidmet_tipi_id bigint DEFAULT NULL,
    p_muhasibat_kodu_id bigint DEFAULT NULL,
    p_paket_xidmet boolean DEFAULT NULL,
    p_axtaris text DEFAULT NULL,
    p_dil_kodu varchar DEFAULT 'az',
    p_limit integer DEFAULT NULL,
    p_offset integer DEFAULT 0
)
RETURNS TABLE(
    xidmet_id bigint, klinika_id bigint, xidmet_kodu varchar, xidmet_adi varchar,
    xidmet_qrupu_id bigint, xidmet_qrupu_kodu varchar, xidmet_qrupu_adi varchar,
    muhasibat_kodu_id bigint, muhasibat_kodu_adi varchar, xidmet_tipi_id bigint,
    xidmet_tipi_kodu varchar, xidmet_tipi_adi varchar, beynelxalq_kod varchar,
    beynelxalq_ad varchar, hesabat_novu_id bigint, hesabat_novu_kodu varchar,
    hesabat_novu_adi varchar, hesabat_mecburiyyeti_id bigint,
    hesabat_mecburiyyeti_kodu varchar, hesabat_mecburiyyeti_adi varchar,
    paket_xidmet boolean, sira_no integer, aktiv boolean, yaranma_tarixi timestamp
)
LANGUAGE plpgsql
AS $function$
BEGIN
    IF p_klinika_id IS NULL OR p_klinika_id <= 0 THEN
        RAISE EXCEPTION 'Klinika ID-si düzgün göndərilməyib';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM public.rn_klinikalar k WHERE k.id = p_klinika_id) THEN
        RAISE EXCEPTION 'Klinika tapılmadı';
    END IF;
    IF p_xidmet_qrupu_id IS NOT NULL AND (p_xidmet_qrupu_id <= 0 OR NOT EXISTS (
        SELECT 1 FROM public.rn_xidmet_qruplari xq
        WHERE xq.id = p_xidmet_qrupu_id AND xq.klinika_id = p_klinika_id
    )) THEN
        RAISE EXCEPTION 'Xidmət qrupu tapılmadı və ya seçilmiş klinikaya aid deyil';
    END IF;
    IF p_limit IS NOT NULL AND (p_limit < 1 OR p_limit > 500) THEN
        RAISE EXCEPTION 'Səhifə ölçüsü 1-500 aralığında olmalıdır';
    END IF;
    IF COALESCE(p_offset, 0) < 0 THEN
        RAISE EXCEPTION 'Səhifə başlanğıcı mənfi ola bilməz';
    END IF;

    RETURN QUERY
    WITH RECURSIVE secilmis_qruplar AS (
        SELECT xq.id, xq.klinika_id
        FROM public.rn_xidmet_qruplari xq
        WHERE xq.id = p_xidmet_qrupu_id AND xq.klinika_id = p_klinika_id
        UNION ALL
        SELECT alt.id, alt.klinika_id
        FROM public.rn_xidmet_qruplari alt
        JOIN secilmis_qruplar ust ON ust.id = alt.parent_id AND ust.klinika_id = alt.klinika_id
        WHERE alt.klinika_id = p_klinika_id AND COALESCE(p_alt_qruplar_daxil, true)
    )
    SELECT
        x.id, x.klinika_id, x.kod,
        COALESCE(NULLIF(mt.deyer, ''), x.ad)::varchar,
        xq.id, xq.kod, xq.ad, mk.id, mk.ad, xt.id, xt.kod, xt.ad,
        x.beynelxalq_kod, x.beynelxalq_ad, hn.id, hn.kod, hn.ad,
        hm.id, hm.kod, hm.ad, x.paket_xidmet, x.sira_no, x.aktiv, x.yaranma_tarixi
    FROM public.rn_xidmetler x
    JOIN public.rn_xidmet_qruplari xq ON xq.id = x.xidmet_qrupu_id AND xq.klinika_id = x.klinika_id
    JOIN public.rn_muhasibat_kodlari mk ON mk.id = x.muhasibat_kodu_id AND mk.klinika_id = x.klinika_id
    JOIN public.rn_xidmet_tipleri xt ON xt.id = x.xidmet_tipi_id
    LEFT JOIN public.rn_hesabat_novleri hn ON hn.id = x.hesabat_novu_id
    LEFT JOIN public.rn_hesabat_mecburiyyetleri hm ON hm.id = x.hesabat_mecburiyyeti_id
    LEFT JOIN public.kn_diller d ON d.kod = COALESCE(NULLIF(p_dil_kodu, ''), 'az') AND d.aktiv
    LEFT JOIN public.kn_melumat_tercumeleri mt
        ON mt.melumat_novu = 'XIDMET' AND mt.menbe_id = x.id AND mt.saha = 'ad' AND mt.dil_id = d.id
    WHERE x.klinika_id = p_klinika_id
      AND (p_aktiv IS NULL OR x.aktiv = p_aktiv)
      AND (p_xidmet_tipi_id IS NULL OR x.xidmet_tipi_id = p_xidmet_tipi_id)
      AND (p_muhasibat_kodu_id IS NULL OR x.muhasibat_kodu_id = p_muhasibat_kodu_id)
      AND (p_paket_xidmet IS NULL OR COALESCE(x.paket_xidmet, false) = p_paket_xidmet)
      AND (NULLIF(BTRIM(p_axtaris), '') IS NULL
           OR x.kod ILIKE '%' || BTRIM(p_axtaris) || '%'
           OR x.ad ILIKE '%' || BTRIM(p_axtaris) || '%'
           OR mt.deyer ILIKE '%' || BTRIM(p_axtaris) || '%'
           OR x.beynelxalq_kod ILIKE '%' || BTRIM(p_axtaris) || '%'
           OR x.beynelxalq_ad ILIKE '%' || BTRIM(p_axtaris) || '%')
      AND (p_xidmet_qrupu_id IS NULL
           OR (NOT COALESCE(p_alt_qruplar_daxil, true) AND x.xidmet_qrupu_id = p_xidmet_qrupu_id)
           OR (COALESCE(p_alt_qruplar_daxil, true) AND x.xidmet_qrupu_id IN (SELECT sq.id FROM secilmis_qruplar sq)))
    ORDER BY COALESCE(xq.sira_no, 2147483647), xq.ad,
             COALESCE(x.sira_no, 2147483647), COALESCE(NULLIF(mt.deyer, ''), x.ad), x.id
    LIMIT p_limit OFFSET COALESCE(p_offset, 0);
END;
$function$;
