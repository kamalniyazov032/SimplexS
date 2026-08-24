WITH qeydiyyat_acarlari(acar) AS (
    SELECT unnest(string_to_array(trim($keys$
common.aktiv
common.axtar
common.axtaris
common.bagla
common.filter
common.hamisi
common.legv_et
common.passiv
common.select
common.status
common.temizle
common.yadda_saxla
patients.actions
patients.active_card
patients.additional_info
patients.address
patients.address_info
patients.birth_city
patients.birth_country
patients.birth_date
patients.birth_date_range
patients.blood_group
patients.city
patients.contact_work
patients.country
patients.default_organization
patients.document_info
patients.document_number
patients.document_type
patients.edit
patients.education
patients.email
patients.empty
patients.end_date
patients.father_name
patients.fin
patients.first_name
patients.gender
patients.last_name
patients.marital_status
patients.mobile
patients.new
patients.new_hint
patients.new_patient
patients.next_records
patients.note
patients.organization
patients.patient_code
patients.patient_list
patients.personal_info
patients.position
patients.profession
patients.result_count
patients.search
patients.search_label
patients.second_mobile
patients.social_card
patients.start_date
patients.title
patients.workplace
visits.new_card
visits.patient
$keys$), E'\n'))
), ambulator_acarlari(acar) AS (
    SELECT unnest(string_to_array(trim($keys$
common.active
common.aktiv
common.all
common.axtar
common.axtaris
common.bagla
common.beli
common.filter
common.filteri_temizle
common.hamisi
common.passiv
common.passive
common.search
common.select
common.show_more
common.status
common.temizle
common.xeyr
common.yadda_saxla
patients.default_organization
patients.document_fin
patients.edit
patients.empty
patients.full_name
patients.new
patients.organization
patients.patient_code
patients.patient_list
patients.search
patients.search_label
patients.status
visits.appointment
visits.date
visits.date_from
visits.date_to
visits.description
visits.detailed_search
visits.diagnostic_examination
visits.doctor_examination
visits.filter_result
visits.list
visits.list_description
visits.message
visits.new
visits.new_for_patient
visits.patient
visits.protocol
visits.referring_doctor
visits.repeat_examination
visits.search_placeholder
visits.select_patient
visits.time
visits.title
visits.today_total
visits.type
$keys$), E'\n'))
), elaqeler(acar, modul_kodu, ekran) AS (
    SELECT acar, 'APP_PATIENT_REGISTRATION', 'pages/pasienQebulu/xesteQeydiyyati.html'
    FROM qeydiyyat_acarlari
    UNION ALL
    SELECT acar, 'HIS_INPATIENT', 'pages/pasienQebulu/ambulator'
    FROM ambulator_acarlari
    UNION ALL
    SELECT acar, 'HIS_PATIENT', 'pages/pasienQebulu'
    FROM (
        SELECT acar FROM qeydiyyat_acarlari
        UNION
        SELECT acar FROM ambulator_acarlari
    ) ana_modul_acarlari
)
INSERT INTO public.kn_tercume_modul_elaqeleri(acar, modul_id, ekran)
SELECT e.acar, m.id, e.ekran
FROM elaqeler e
JOIN public.rn_modullar m ON m.kod=e.modul_kodu
ON CONFLICT DO NOTHING;
