
$(document).on('shown.bs.modal', '.modal', function () {
	$(this).find('.select2').each(function () {
		if ($(this).hasClass('select2-hidden-accessible')) return;

		$(this).select2({
			dropdownParent: $(this).closest('.modal'),
			width: '100%',
			placeholder: 'Seçin',
			allowClear: false,

		});
	});
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
        const dateRange = $.trim($('[data-provider="flatpickr"]').val());
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