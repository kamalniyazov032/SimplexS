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