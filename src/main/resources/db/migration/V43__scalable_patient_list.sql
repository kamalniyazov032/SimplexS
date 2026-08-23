-- İstifadəçinin açıq razılığı ilə rn_xesteler üçün milyonluq həcm optimallaşdırması.
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX IF NOT EXISTS idx_rn_xesteler_klinika_aktiv_id_desc
    ON public.rn_xesteler (klinika_id, aktiv, id DESC);

CREATE INDEX IF NOT EXISTS idx_rn_xesteler_klinika_teskilat_aktiv_id_desc
    ON public.rn_xesteler (klinika_id, default_teskilat_id, aktiv, id DESC);

CREATE INDEX IF NOT EXISTS idx_rn_xesteler_umumi_axtaris_trgm
    ON public.rn_xesteler USING gin ((lower(
        coalesce(kod, '') || ' ' || coalesce(fin_kodu, '') || ' ' ||
        coalesce(sexsiyyet_vesiqesi_nomresi, '') || ' ' || coalesce(ad, '') || ' ' ||
        coalesce(soyad, '') || ' ' || coalesce(ata_adi, '') || ' ' ||
        coalesce(mobil_nomre, '') || ' ' || coalesce(ikinci_mobil_nomre, '') || ' ' ||
        coalesce(sosial_kart_nomresi, '') || ' ' || coalesce(is_yeri, '') || ' ' ||
        coalesce(vezifesi, '') || ' ' || coalesce(pesesi, '')
    )) gin_trgm_ops);

CREATE OR REPLACE FUNCTION public.fn_xeste_siyahisi_sehifeli(
    p_klinika_id bigint,
    p_aktiv boolean DEFAULT NULL,
    p_axtaris varchar DEFAULT NULL,
    p_teskilat_id bigint DEFAULT NULL,
    p_son_xeste_id bigint DEFAULT NULL,
    p_limit integer DEFAULT 26
)
RETURNS TABLE(
    xeste_id bigint, klinika_id bigint, xeste_kodu varchar,
    default_teskilat_id bigint, default_teskilat_adi varchar,
    ad varchar, soyad varchar, ata_adi varchar,
    sexsiyyet_vesiqesi_novu_id bigint, sexsiyyet_vesiqesi_novu_kodu varchar,
    sexsiyyet_vesiqesi_novu_adi varchar, sexsiyyet_vesiqesi_nomresi varchar, fin_kodu varchar,
    cins_id bigint, cins_kodu varchar, cins_adi varchar, dogum_tarixi date,
    aile_veziyyeti_id bigint, aile_veziyyeti_kodu varchar, aile_veziyyeti_adi varchar,
    tehsil_id bigint, tehsil_kodu varchar, tehsil_adi varchar,
    doguldugu_olke_id bigint, doguldugu_olke_adi varchar,
    doguldugu_seher_id bigint, doguldugu_seher_adi varchar,
    olke_id bigint, olke_adi varchar, seher_id bigint, seher_adi varchar,
    qan_qrupu_id bigint, qan_qrupu_kodu varchar, qan_qrupu_adi varchar,
    mobil_nomre varchar, ikinci_mobil_nomre varchar, email varchar,
    sosial_kart_nomresi varchar, is_yeri varchar, vezifesi varchar, pesesi varchar,
    unvan text, qeyd text, aktiv boolean, yaranma_tarixi timestamp,
    yaradan_personal_id bigint, yenilenme_tarixi timestamp, yenileyen_personal_id bigint
)
LANGUAGE sql STABLE
AS $function$
    SELECT x.id, x.klinika_id, x.kod, x.default_teskilat_id, tes.ad,
           x.ad, x.soyad, x.ata_adi,
           sv.id, sv.kod, sv.ad, x.sexsiyyet_vesiqesi_nomresi, x.fin_kodu,
           c.id, c.kod, c.ad, x.dogum_tarixi,
           av.id, av.kod, av.ad, th.id, th.kod, th.ad,
           do_.id, do_.ad, ds.id, ds.ad, o.id, o.ad, s.id, s.ad,
           q.id, q.kod, q.ad,
           x.mobil_nomre, x.ikinci_mobil_nomre, x.email,
           x.sosial_kart_nomresi, x.is_yeri, x.vezifesi, x.pesesi,
           x.unvan, x.qeyd, x.aktiv, x.yaranma_tarixi, x.yaradan_personal_id,
           x.yenilenme_tarixi, x.yenileyen_personal_id
    FROM public.rn_xesteler x
    JOIN public.rn_teskilatlar tes ON tes.id=x.default_teskilat_id
    JOIN public.rn_sexsiyyet_vesiqesi_novleri sv ON sv.id=x.sexsiyyet_vesiqesi_novu_id
    JOIN public.rn_cinsler c ON c.id=x.cins_id
    LEFT JOIN public.rn_aile_veziyyetleri av ON av.id=x.aile_veziyyeti_id
    LEFT JOIN public.rn_tehsiller th ON th.id=x.tehsil_id
    LEFT JOIN public.rn_olkeler do_ ON do_.id=x.doguldugu_olke_id
    LEFT JOIN public.rn_seherler ds ON ds.id=x.doguldugu_seher_id AND ds.olke_id=x.doguldugu_olke_id
    LEFT JOIN public.rn_olkeler o ON o.id=x.olke_id
    LEFT JOIN public.rn_seherler s ON s.id=x.seher_id AND s.olke_id=x.olke_id
    LEFT JOIN public.rn_qan_qruplari q ON q.id=x.qan_qrupu_id
    WHERE x.klinika_id=p_klinika_id
      AND (p_aktiv IS NULL OR x.aktiv=p_aktiv)
      AND (p_teskilat_id IS NULL OR x.default_teskilat_id=p_teskilat_id)
      AND (p_son_xeste_id IS NULL OR x.id<p_son_xeste_id)
      AND (
          nullif(trim(p_axtaris), '') IS NULL OR
          lower(coalesce(x.kod, '') || ' ' || coalesce(x.fin_kodu, '') || ' ' ||
                coalesce(x.sexsiyyet_vesiqesi_nomresi, '') || ' ' || coalesce(x.ad, '') || ' ' ||
                coalesce(x.soyad, '') || ' ' || coalesce(x.ata_adi, '') || ' ' ||
                coalesce(x.mobil_nomre, '') || ' ' || coalesce(x.ikinci_mobil_nomre, '') || ' ' ||
                coalesce(x.sosial_kart_nomresi, '') || ' ' || coalesce(x.is_yeri, '') || ' ' ||
                coalesce(x.vezifesi, '') || ' ' || coalesce(x.pesesi, ''))
          LIKE '%' || lower(trim(p_axtaris)) || '%'
      )
    ORDER BY x.id DESC
    LIMIT least(greatest(coalesce(p_limit, 26), 1), 101);
$function$;
