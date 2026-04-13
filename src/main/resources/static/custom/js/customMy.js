/*
$(document).ready(function () {

    // Normal səhifədə olan select2-lər
    $('.select2').not('#addCustomerModal .select2').select2({
        width: '100%',
        placeholder: 'Seçin'
    });

    // Modal açılanda yalnız modal içindəkilər init olunur
    $('#addCustomerModal').on('shown.bs.modal', function () {
        $(this).find('.select2').select2({
            dropdownParent: $(this),
            width: '100%',
            placeholder: 'Seçin',
            allowClear: true
        });
    });

});
*/
/*

$(document).ready(function () {

	// 2. MODAL ÜÇÜN UNIVERSAL QAYDA (Həm təkli, həm multi)
	$(document).on('shown.bs.modal', '.modal', function () {
		const currentModal = $(this);

		currentModal.find('.select2, .mul-select2').each(function () {
			const isMultiple = $(this).prop('multiple');
			$(this).select2({
				placeholder: "Buradan bir neçə seçim edin...",
				allowClear: true,
				width: '100%',
				closeOnSelect: false // Sən istəyən "içində qalsın" məntiqi
			});
		});
	});

	// 3. MODAL BAĞLANDIQA (Yaddaş təmizliyi və bug qarşısını almaq üçün)
	$(document).on('hidden.bs.modal', '.modal', function () {
		$(this).find('.select2, .mul-select2').select2('destroy');
	});
});
*/
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
});