$(document).ready(function () {
	$('#addCustomerModal').on('shown.bs.modal', function () {
		$(this).find('.select2').select2({
			dropdownParent: $('#addCustomerModal'),
			width: '100%',
			placeholder: "Seçin",
			allowClear: true
		});
	});
});