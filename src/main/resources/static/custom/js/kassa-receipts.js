(() => {
    'use strict';
    document.getElementById('receipt-print')?.addEventListener('click', () => window.print());
    const form = document.getElementById('receipt-cancel-form');
    if (!form) return;
    const dialog = document.getElementById('receipt-cancel-dialog');
    const submit = document.getElementById('receipt-cancel-submit');
    const reason = document.getElementById('receipt-reason');
    let confirmed = false, submitting = false;
    document.getElementById('receipt-cancel-no').addEventListener('click', () => dialog.close());
    dialog.addEventListener('close', () => submit.focus());
    document.getElementById('receipt-cancel-yes').addEventListener('click', () => {
        if (submitting || !dialog.open) return;
        dialog.close(); confirmed = true;
        try { form.requestSubmit(submit); } finally { confirmed = false; }
    });
    form.addEventListener('submit', event => {
        if (submitting || !reason.value.trim()) { event.preventDefault(); return; }
        if (!confirmed) {
            event.preventDefault();
            document.getElementById('receipt-confirm-reason').textContent = reason.value;
            if (!dialog.open) dialog.showModal();
            return;
        }
        submitting = true; submit.disabled = true;
    });
    window.addEventListener('pageshow', event => { if (event.persisted) window.location.reload(); });
})();
