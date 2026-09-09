(() => {
    'use strict';
    document.getElementById('receipt-print')?.addEventListener('click', () => window.print());
    const form = document.getElementById('receipt-cancel-form');
    const modal = document.getElementById('receipt-detail-modal');
    if (modal && window.bootstrap?.Modal) {
        document.body.appendChild(modal);
        modal.addEventListener('shown.bs.modal', () => {
            if (modal.dataset.cancel === 'true' && form) {
                form.scrollIntoView({block: 'start'});
                document.getElementById('receipt-reason').focus();
            }
        });
        window.bootstrap.Modal.getOrCreateInstance(modal).show();
    }
    if (!form) return;
    const dialog = document.getElementById('receipt-cancel-dialog');
    const submit = document.getElementById('receipt-cancel-submit');
    const reason = document.getElementById('receipt-reason');
    let confirmed = false, submitting = false;
    modal?.addEventListener('hide.bs.modal', event => { if (submitting || dialog.open) event.preventDefault(); });
    document.getElementById('receipt-cancel-no').addEventListener('click', () => dialog.close());
    dialog.addEventListener('close', () => submit.focus());
    document.getElementById('receipt-cancel-yes').addEventListener('click', () => {
        if (submitting || !dialog.open) return;
        dialog.close(); confirmed = true;
        try { form.requestSubmit(submit); } finally { confirmed = false; }
    });
    form.addEventListener('submit', event => {
        if (submitting || !reason.value.trim()) { event.preventDefault(); return; }
        if (!confirmed && modal?.dataset.cancel !== 'true') {
            event.preventDefault();
            document.getElementById('receipt-confirm-reason').textContent = reason.selectedOptions[0]?.textContent || '';
            if (!dialog.open) dialog.showModal();
            return;
        }
        submitting = true; submit.disabled = true;
    });
    window.addEventListener('pageshow', event => { if (event.persisted) window.location.reload(); });
})();
