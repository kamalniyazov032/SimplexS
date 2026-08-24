CREATE OR REPLACE FUNCTION public.fn_xeste_gelisi_siyahisi(
    p_klinika_id bigint, p_xeste_id bigint, p_gelis_novu_id bigint,
    p_teskilat_id bigint, p_tarix_baslama date, p_tarix_bitme date,
    p_randevudur boolean, p_aktiv boolean, p_axtaris varchar,
    p_son_gelis_id bigint, p_limit integer
)
RETURNS TABLE(
    gelis_id bigint, klinika_id bigint, xeste_id bigint, xeste_kodu varchar,
    xeste_ad varchar, xeste_soyad varchar, xeste_ata_adi varchar, fin_kodu varchar,
    sexsiyyet_vesiqesi_nomresi varchar, mobil_nomre varchar, gelis_novu_id bigint,
    gelis_novu_kodu varchar, gelis_novu_adi varchar, teskilat_id bigint,
    teskilat_adi varchar, protokol_kodu varchar, gelis_tarixi date,
    gelis_saati time, randevudur boolean, gonderen_hekim_id bigint,
    gonderen_hekim_kodu varchar, gonderen_hekim_ad varchar,
    gonderen_hekim_soyad varchar, gonderen_hekim_ata_adi varchar, mesaj text,
    aciqlama text, aktiv boolean, yaranma_tarixi timestamp,
    yaradan_personal_id bigint, yenilenme_tarixi timestamp, yenileyen_personal_id bigint
)
LANGUAGE plpgsql AS $function$
BEGIN
    IF p_klinika_id IS NULL OR p_klinika_id <= 0 THEN
        RAISE EXCEPTION 'Klinika ID-si düzgün göndərilməyib';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM public.rn_klinikalar k WHERE k.id=p_klinika_id) THEN
        RAISE EXCEPTION 'Klinika tapılmadı';
    END IF;
    IF p_tarix_baslama IS NOT NULL AND p_tarix_bitme IS NOT NULL
       AND p_tarix_baslama > p_tarix_bitme THEN
        RAISE EXCEPTION 'Başlama tarixi bitmə tarixindən böyük ola bilməz';
    END IF;
    IF p_limit IS NULL OR p_limit < 1 OR p_limit > 200 THEN
        RAISE EXCEPTION 'Səhifə ölçüsü 1 və 200 arasında olmalıdır';
    END IF;

    RETURN QUERY
    SELECT g.id, g.klinika_id, x.id, x.kod, x.ad, x.soyad, x.ata_adi,
           x.fin_kodu, x.sexsiyyet_vesiqesi_nomresi, x.mobil_nomre,
           gn.id, gn.kod, gn.ad, t.id, t.ad, g.protokol_kodu,
           g.gelis_tarixi, g.gelis_saati, g.randevudur,
           gh.id, gh.kod, gh.ad, gh.soyad, gh.ata_adi,
           g.mesaj, g.aciqlama, g.aktiv, g.yaranma_tarixi,
           g.yaradan_personal_id, g.yenilenme_tarixi, g.yenileyen_personal_id
    FROM public.rn_xeste_gelisleri g
    JOIN public.rn_xesteler x ON x.id=g.xeste_id AND x.klinika_id=g.klinika_id
    JOIN public.rn_xeste_gelis_novleri gn ON gn.id=g.gelis_novu_id
    JOIN public.rn_teskilatlar t ON t.id=g.teskilat_id
    LEFT JOIN public.rn_personallar gh ON gh.id=g.gonderen_hekim_id
    WHERE g.klinika_id=p_klinika_id
      AND (p_xeste_id IS NULL OR g.xeste_id=p_xeste_id)
      AND (p_gelis_novu_id IS NULL OR g.gelis_novu_id=p_gelis_novu_id)
      AND (p_teskilat_id IS NULL OR g.teskilat_id=p_teskilat_id)
      AND (p_tarix_baslama IS NULL OR g.gelis_tarixi>=p_tarix_baslama)
      AND (p_tarix_bitme IS NULL OR g.gelis_tarixi<=p_tarix_bitme)
      AND (p_randevudur IS NULL OR g.randevudur=p_randevudur)
      AND (p_aktiv IS NULL OR g.aktiv=p_aktiv)
      AND (p_son_gelis_id IS NULL OR g.id<p_son_gelis_id)
      AND (
          NULLIF(trim(p_axtaris),'') IS NULL
          OR g.protokol_kodu ILIKE '%'||trim(p_axtaris)||'%'
          OR x.kod ILIKE '%'||trim(p_axtaris)||'%'
          OR x.fin_kodu ILIKE '%'||trim(p_axtaris)||'%'
          OR x.sexsiyyet_vesiqesi_nomresi ILIKE '%'||trim(p_axtaris)||'%'
          OR x.ad ILIKE '%'||trim(p_axtaris)||'%'
          OR x.soyad ILIKE '%'||trim(p_axtaris)||'%'
          OR COALESCE(x.ata_adi,'') ILIKE '%'||trim(p_axtaris)||'%'
          OR COALESCE(x.mobil_nomre,'') ILIKE '%'||trim(p_axtaris)||'%'
          OR (x.ad||' '||x.soyad||' '||COALESCE(x.ata_adi,'')) ILIKE '%'||trim(p_axtaris)||'%'
      )
    ORDER BY g.id DESC
    LIMIT p_limit;
END;
$function$;
