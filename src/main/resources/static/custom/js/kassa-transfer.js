(() => {
    'use strict';
    const get = id => document.getElementById('transfer-' + id);
    const form = get('form');
    if (!form) return;
    const modal = get('modal'), dialog = get('confirmation'), submit = get('submit');
    const recipient = get('recipient'), account = get('account'), amount = get('amount');
    const i18n = window.KassaTransferI18n;
    let submitting = false, confirmed = false, dirty = false, popupOpen = true;
    modal.addEventListener('hide.bs.modal', event => { if (submitting || dialog.open) event.preventDefault(); });
    modal.addEventListener('hidden.bs.modal', () => { popupOpen = false; document.getElementById('transfer-open')?.focus(); });
    modal.addEventListener('show.bs.modal', () => { popupOpen = true; });
    if (window.bootstrap?.Modal) {
        document.body.appendChild(modal);
        const popup = window.bootstrap.Modal.getOrCreateInstance(modal);
        popup.show();
        document.getElementById('transfer-open')?.addEventListener('click', event => { event.preventDefault(); popup.show(); });
    }
    form.addEventListener('input', () => { dirty = true; get('feedback').textContent = ''; });
    form.addEventListener('change', () => { dirty = true; });
    if (window.jQuery) window.jQuery([recipient, account]).on('change.kassaTransfer', () => { dirty = true; });
    get('cancel').addEventListener('click', () => dialog.close());
    dialog.addEventListener('close', () => submit.focus());
    get('confirm').addEventListener('click', () => {
        if (submitting || !dialog.open) return;
        dialog.close(); confirmed = true;
        try { form.requestSubmit(submit); } finally { confirmed = false; }
    });
    form.addEventListener('submit', event => {
        if (submitting) { event.preventDefault(); return; }
        const validAmount = /^\d+(\.\d{1,2})?$/.test(amount.value) && Number(amount.value) > 0 && Number(amount.value) <= 999999999999.99;
        const error = !recipient.value ? i18n.invalidRecipient : !account.value ? i18n.invalidAccount : !validAmount ? i18n.invalidAmount : Number(amount.value) > Number(amount.max) ? i18n.insufficientBalance : '';
        if (error) { event.preventDefault(); get('feedback').textContent = error; return; }
        if (!confirmed) {
            event.preventDefault();
            get('confirm-recipient').textContent = recipient.selectedOptions[0]?.textContent || '';
            get('confirm-account').textContent = account.selectedOptions[0]?.textContent || '';
            get('confirm-amount').textContent = Number(amount.value).toLocaleString(document.documentElement.lang || 'az', {minimumFractionDigits: 2, maximumFractionDigits: 2});
            if (!dialog.open) dialog.showModal();
            return;
        }
        submitting = true; dirty = false; submit.disabled = true;
        submit.querySelector('span').textContent = i18n.processing;
    });
    window.addEventListener('beforeunload', event => { if (popupOpen && dirty && !submitting) { event.preventDefault(); event.returnValue = ''; } });
    window.addEventListener('pageshow', event => { if (event.persisted) window.location.reload(); });
})();
