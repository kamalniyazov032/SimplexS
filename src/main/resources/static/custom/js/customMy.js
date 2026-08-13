function initSelect2Fields(context, forceReinit) {
    if (!$.fn.select2) return;

    const $context = context ? $(context) : $(document);
    $context.find('.select2').each(function () {
        const $select = $(this);
        if ($select.hasClass('select2-hidden-accessible')) {
            if (!forceReinit) return;
            $select.select2('destroy');
        }

        const $modalParent = $select.closest('.modal');
        const $offcanvasParent = $select.closest('.offcanvas');
        const options = {
            width: '100%',
            placeholder: $select.data('placeholder') || 'Seçin',
            allowClear: false,
            minimumResultsForSearch: 0
        };

        if ($modalParent.length) {
            options.dropdownParent = $modalParent;
        }

        // Qeyd: offcanvas daxilində olan .select2 elementləri ID-yə bağlı deyil.
        // Gələcək səhifələrdə offcanvas içində select-ə sadəcə .select2 class-ı verilsə,
        // dropdown həmin offcanvas parent-in içindən açılacaq.
        if ($offcanvasParent.length) {
            options.dropdownParent = $offcanvasParent;
        }

        $select.select2(options);
    });
}

$(document).ready(function () {
    initSelect2Fields(document);
});

$(document).on('shown.bs.modal shown.bs.offcanvas', '.modal, .offcanvas', function () {
    initSelect2Fields(this, true);
});

$(document).ready(function () {
    $('.datatable_ka_tam').DataTable({
        "paging": false,          // Səhifələməni tamamilə söndürür
        "scrollY": "65vh",       // Cədvəlin hündürlüyünü təyin edir (istədiyin qədər dəyişə bilərsən)
        "scrollCollapse": true,   // Əgər data azdırsa, cədvəlin boş sahə saxlamayıb daralmasını təmin edir
        "fixedHeader": true,      // Başlığın yerində qalmasını qarantiləyir
        "dom": 'ft',              // 'f' - search box, 't' - table. Paging və info hissələrini yığışdırır.
        "language": {
            "search": "",
            "searchPlaceholder": "Axtarış..."
        }
    });
});

$(document).ready(function () {
    $('.datatable_ka_tam_30').DataTable({
        "paging": false,          // Səhifələməni tamamilə söndürür
        "scrollY": "30vh",       // Cədvəlin hündürlüyünü təyin edir (istədiyin qədər dəyişə bilərsən)
        "scrollCollapse": true,   // Əgər data azdırsa, cədvəlin boş sahə saxlamayıb daralmasını təmin edir
        "fixedHeader": true,      // Başlığın yerində qalmasını qarantiləyir
        "dom": 'ft',              // 'f' - search box, 't' - table. Paging və info hissələrini yığışdırır.
        "language": {
            "search": "",
            "searchPlaceholder": "Axtarış..."
        }
    });
});


$(document).ready(function () {
    // Əvvəlcə köhnəni silirik (xəta verməsin deyə)
    if ($.fn.DataTable.isDataTable('#kassaTable')) {
        $('#kassaTable').DataTable().destroy();
    }

    $('#kassaTable').DataTable({
        "paging": false,
        "scrollY": "65vh",
        "scrollX": true,
        "searching": true,
        "info": false,
        "autoWidth": false,
        "dom": 'ft',
        "language": {
            "search": "",
            "searchPlaceholder": "Cədvəldə axtar..."
        }
    });

    $('.btn-search-firma').on('click', function () {
        const year = $.trim($('[name="year"]').val());
        const dateRangeElement = document.querySelector('[data-provider="flatpickr"]');
        const dateRange = $.trim(dateRangeElement?._flatpickr?.altInput?.value || $(dateRangeElement).val());
        const workType = $.trim($('[name="isleyis_novu"] option:selected').text());
        const invoiceNo = $.trim($('[name="qaimeno"]').val()).toLowerCase();
        const firma = $.trim($('[name="firma"] option:selected').text());

        const dateRangeParts = dateRange.split(' - ').map(function (value) {
            return $.trim(value);
        });
        const startDate = parseDate(dateRangeParts[0]);
        const endDate = parseDate(dateRangeParts[1]);

        $('.table tbody tr').each(function () {
            const $cells = $(this).find('td');
            const rowDateText = $cells.eq(0).text().replace('▼', '').trim();
            const rowDate = parseDate(rowDateText);
            const rowInvoice = $cells.eq(1).text().trim().toLowerCase();
            const rowYear = $cells.eq(2).text().trim();
            const rowType = $cells.eq(3).text().trim();
            const rowFirma = $cells.eq(4).text().trim();

            let visible = true;
            if (year && year !== '') {
                visible = visible && rowYear === year;
            }
            if (workType && workType !== '-- Seçin --') {
                visible = visible && rowType === workType;
            }
            if (invoiceNo) {
                visible = visible && rowInvoice.indexOf(invoiceNo) !== -1;
            }
            if (firma && firma !== '-- Firma seçin --') {
                visible = visible && rowFirma === firma;
            }
            if (startDate && endDate && rowDate) {
                visible = visible && rowDate >= startDate && rowDate <= endDate;
            }

            $(this).toggle(visible);
        });
    });

    function parseDate(dateText) {
        if (!dateText) {
            return null;
        }
        const parts = dateText.split('.');
        if (parts.length !== 3) {
            return null;
        }
        const day = parseInt(parts[0], 10);
        const month = parseInt(parts[1], 10) - 1;
        const year = parseInt(parts[2], 10);
        if (isNaN(day) || isNaN(month) || isNaN(year)) {
            return null;
        }
        return new Date(year, month, day);
    }
});

document.addEventListener('DOMContentLoaded', function () {
    const calendarEl = document.getElementById('calendar');
    if (!calendarEl) return;

    const addCustomerModalEl = document.getElementById('addCustomerModal');
    const addCustomerForm = addCustomerModalEl
        ? addCustomerModalEl.querySelector('form')
        : null;

    let selectedStart = null;
    let selectedEnd = null;
    let eventCounter = 1;

    const calendar = new FullCalendar.Calendar(calendarEl, {
        initialView: 'timeGridWeek',
        height: '100%',
        headerToolbar: {
            left: 'prev,next today',
            center: 'title',
            right: 'dayGridMonth,timeGridWeek,timeGridDay,listWeek'
        },

        buttonText: {
            today: 'Bu gün',
            month: 'Ay',
            week: 'Həftə',
            day: 'Gün',
            list: 'Siyahı'
        },

        dayHeaderContent: function (arg) {
            const days = ['Bazar', 'B.e', 'Ç.a', 'Ç', 'C.a', 'C', 'Şənbə'];
            return days[arg.date.getDay()];
        },

        slotDuration: '00:15:00',
        slotLabelInterval: '01:00:00',

        slotLabelFormat: {
            hour: '2-digit',
            minute: '2-digit',
            hour12: false
        },

        eventTimeFormat: {
            hour: '2-digit',
            minute: '2-digit',
            hour12: false
        },

        slotMinTime: '08:00:00',
        slotMaxTime: '18:00:00',

        selectable: true,
        editable: true,
        nowIndicator: true,
        allDaySlot: false,



        events: [
            {
                daysOfWeek: [1, 2, 3, 4, 5],
                startTime: '13:00:00',
                endTime: '14:00:00',
                display: 'background',
                color: '#230a0a'
            },
            {
                title: 'Fasilə',
                daysOfWeek: [1, 2, 3, 4, 5],
                startTime: '13:00:00',
                endTime: '14:00:00',
                backgroundColor: '#4e159d',
                textColor: '#ac1313',
                editable: false
            },
            {
                title: 'Randevu',
                start: '2026-04-22T10:00:00',
                end: '2026-04-22T10:15:00',
                backgroundColor: '#28a745',
                borderColor: '#28a745',
                textColor: '#ac1313',
                extendedProps: {
                    doctorName: 'Dr. Elvin',
                    patientName: 'Said',
                    serviceName: 'Diş müayinəsi',
                    departmentName: 'Stomatologiya',
                    organizationName: 'Pasha Sığorta'
                }
            }
        ],

        // select edəndə hazır modal açılır
        select: function (info) {
            selectedStart = info.start;
            selectedEnd = info.end;

            if (addCustomerModalEl) {
                const modal = new bootstrap.Modal(addCustomerModalEl);
                modal.show();
            }
        },

        // event üstünə gələndə detal görünsün
        eventDidMount: function (info) {
            const p = info.event.extendedProps || {};

            info.el.title =
                'Pasiyent: ' + (p.patientName || info.event.title || '') + '\n' +
                'Təşkilat: ' + (p.organizationName || '') + '\n' +
                'Telefon: ' + (p.phone || '') + '\n' +
                'FİN: ' + (p.fin || '') + '\n' +
                'Başlama: ' + (info.event.start ? info.event.start.toLocaleString('az-AZ') : '') + '\n' +
                'Bitmə: ' + (info.event.end ? info.event.end.toLocaleString('az-AZ') : '');
        },

        // klik edəndə detal + silmə
        eventClick: function (info) {
            const event = info.event;
            const p = event.extendedProps || {};

            selectedEvent = event;

            document.getElementById('detailPatient').value = p.patientName || event.title || '';
            document.getElementById('detailOrganization').value = p.organizationName || '';
            document.getElementById('detailPhone').value = p.phone || '';
            document.getElementById('detailFin').value = p.fin || '';
            document.getElementById('detailSv').value = p.svNomresi || '';
            document.getElementById('detailPassport').value = p.passportNo || '';
            document.getElementById('detailStart').value = event.start ? event.start.toLocaleString('az-AZ') : '';
            document.getElementById('detailEnd').value = event.end ? event.end.toLocaleString('az-AZ') : '';

            const detailModal = new bootstrap.Modal(document.getElementById('appointmentDetailModal'));
            detailModal.show();
        }
    });

    calendar.render();

    if (addCustomerForm) {
        addCustomerForm.addEventListener('submit', function (e) {
            e.preventDefault();

            if (!selectedStart || !selectedEnd) {
                alert('Əvvəlcə calendar-da saat seçin.');
                return;
            }

            const ad = addCustomerForm.querySelector('[name="ad"]')?.value.trim() || '';
            const soyad = addCustomerForm.querySelector('[name="soyad"]')?.value.trim() || '';
            const ataAdi = addCustomerForm.querySelector('[name="ataAdi"]')?.value.trim() || '';
            const teskilat = addCustomerForm.querySelector('[name="teskilat"]')?.value.trim() || '';
            const telefon = addCustomerForm.querySelector('[name="telefon"]')?.value.trim() || '';
            const fin = addCustomerForm.querySelector('[name="fin"]')?.value.trim() || '';
            const svNomresi = addCustomerForm.querySelector('[name="svNomresi"]')?.value.trim() || '';
            const passportNo = addCustomerForm.querySelector('[name="passportNo"]')?.value.trim() || '';
            const email = addCustomerForm.querySelector('[name="email"]')?.value.trim() || '';
            const dogumTarixi = addCustomerForm.querySelector('[name="dogumTarixi"]')?.value.trim() || '';
            const cins = addCustomerForm.querySelector('[name="cins"]')?.value.trim() || '';
            const aileVeziyyeti = addCustomerForm.querySelector('[name="aileVeziyyeti"]')?.value.trim() || '';
            const qanQrupu = addCustomerForm.querySelector('[name="qanQrupu"]')?.value.trim() || '';
            const isYeri = addCustomerForm.querySelector('[name="isYeri"]')?.value.trim() || '';
            const milliyyet = addCustomerForm.querySelector('[name="milliyyet"]')?.value.trim() || '';
            const unvan = addCustomerForm.querySelector('[name="unvan"]')?.value.trim() || '';
            const yasadigiUnvan = addCustomerForm.querySelector('[name="yasadigiUnvan"]')?.value.trim() || '';

            if (!ad) {
                alert('Ad sahəsini doldurun.');
                return;
            }

            const fullName = [ad, soyad, ataAdi].filter(Boolean).join(' ');

            calendar.addEvent({
                id: 'temp-' + eventCounter++,
                title: fullName,
                start: selectedStart,
                end: selectedEnd,
                backgroundColor: '#198754',
                borderColor: '#198754',
                textColor: '#ffffff',
                extendedProps: {
                    patientName: fullName,
                    organizationName: teskilat,
                    phone: telefon,
                    fin: fin,
                    svNomresi: svNomresi,
                    passportNo: passportNo,
                    email: email,
                    dogumTarixi: dogumTarixi,
                    gender: cins,
                    familyStatus: aileVeziyyeti,
                    bloodGroup: qanQrupu,
                    workPlace: isYeri,
                    nationality: milliyyet,
                    address: unvan,
                    livingAddress: yasadigiUnvan
                }
            });

            const modalInstance = bootstrap.Modal.getInstance(addCustomerModalEl);
            if (modalInstance) {
                modalInstance.hide();
            }

            addCustomerForm.reset();
            calendar.unselect();
            selectedStart = null;
            selectedEnd = null;
        });
    }

    if (addCustomerModalEl) {
        addCustomerModalEl.addEventListener('hidden.bs.modal', function () {
            calendar.unselect();
            selectedStart = null;
            selectedEnd = null;
        });
    }
});
