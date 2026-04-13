
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