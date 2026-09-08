(() => {
    'use strict';
    const search = document.getElementById('patient-search');
    if (search) search.addEventListener('input', () => {
        const query = search.value.trim().toLocaleLowerCase(document.documentElement.lang || 'az');
        let visible = 0;
        document.querySelectorAll('[data-patient-row]').forEach(row => {
            const match = row.textContent.toLocaleLowerCase(document.documentElement.lang || 'az').includes(query);
            row.hidden = !match;
            if (match) visible++;
        });
        document.getElementById('patient-search-empty').classList.toggle('d-none', !query || visible > 0);
    });
    const form = document.getElementById('cash-payment-form');
    if (!form) return;
    const i18n = window.KassaI18n;
    const refund = form.dataset.refund === 'true';
    const refundAccount = refund ? document.getElementById('refund-account') : null;
    const refundType = refund ? document.getElementById('refund-payment-type') : null;
    const choices = [...form.querySelectorAll('.service-choice:not(:disabled)')];
    const amounts = [...form.querySelectorAll('.payment-amount')];
    const all = document.getElementById('select-all-services');
    const borrow = document.getElementById('borrow-remainder');
    const feedback = document.getElementById('payment-feedback');
    const submit = document.getElementById('submit-payment');
    const confirmation = document.getElementById('payment-confirmation');
    const confirmButton = document.getElementById('confirm-payment');
    const cancelButton = document.getElementById('cancel-payment');
    let confirmed = false;
    let submitting = false;
    let dirty = false;
    const modal = document.getElementById('cash-payment-modal');
    if (modal && window.bootstrap?.Modal) {
        // Keep the popup above the application sidebar and header stacking contexts.
        document.body.appendChild(modal);
        modal.addEventListener('hide.bs.modal', event => {
            if (submitting || confirmation.open) event.preventDefault();
        });
        modal.addEventListener('hidden.bs.modal', () => {
            document.querySelector('[data-patient-row].is-selected a')?.focus();
        });
        window.bootstrap.Modal.getOrCreateInstance(modal).show();
    }
    // Work in integer cents to avoid binary floating point money comparisons.
    const cents = value => {
        if (!/^\d+(\.\d{1,2})?$/.test(String(value))) return NaN;
        const [whole, fraction = ''] = String(value).split('.');
        const result = Number(whole) * 100 + Number(fraction.padEnd(2, '0'));
        return Number.isSafeInteger(result) ? result : NaN;
    };
    const display = value => (value / 100).toLocaleString(document.documentElement.lang || 'az', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    function totals() {
        const selected = choices.filter(input => input.checked);
        const due = selected.reduce((sum, input) => sum + cents(input.dataset.amount), 0);
        const paid = refund ? due : amounts.reduce((sum, input) => sum + cents(input.value || '0'), 0);
        document.getElementById('selected-count').textContent = selected.length;
        document.getElementById('selected-total').textContent = display(due);
        document.getElementById('payment-total').textContent = Number.isFinite(paid) ? display(paid) : '—';
        document.getElementById('remaining-total').textContent = Number.isFinite(paid) ? display(due - paid) : '—';
        all.checked = choices.length > 0 && selected.length === choices.length;
        all.indeterminate = selected.length > 0 && selected.length < choices.length;
        all.disabled = choices.length === 0;
        let error = '';
        if (!selected.length) error = i18n.selectServices;
        else if (refund && !refundAccount.value) error = i18n.invalidAccount;
        else if (refund && !refundType.value) error = i18n.invalidPayment;
        else if (!Number.isFinite(paid) || paid <= 0) error = i18n.invalidPayment;
        else if (paid > due) error = i18n.overpayment;
        else if (form.dataset.debt !== 'true' && paid < due && !borrow?.checked) error = i18n.borrowRequired;
        return { error, due, paid };
    }
    form.addEventListener('input', event => {
        dirty = true;
        feedback.textContent = '';
        // Checkbox input bubbles before change. Do not reset the master checkbox
        // from the old service selection before its change handler runs.
        if (event.target !== all) totals();
    });
    all.addEventListener('change', () => {
        const checked = all.checked;
        choices.forEach(input => { input.checked = checked; });
        dirty = true;
        feedback.textContent = '';
        totals();
    });
    cancelButton.addEventListener('click', () => confirmation.close());
    confirmation.addEventListener('close', () => submit.focus());
    confirmButton.addEventListener('click', () => {
        if (submitting || !confirmation.open) return;
        confirmation.close();
        confirmed = true;
        try { form.requestSubmit(submit); }
        finally { confirmed = false; }
    });
    form.addEventListener('submit', event => {
        if (submitting) { event.preventDefault(); return; }
        const state = totals();
        if (state.error) { event.preventDefault(); feedback.textContent = state.error; return; }
        if (!confirmed) {
            event.preventDefault();
            if (refund) {
                document.getElementById('refund-confirm-account').textContent = refundAccount.selectedOptions[0]?.textContent || '';
                document.getElementById('refund-confirm-type').textContent = refundType.selectedOptions[0]?.textContent || '';
            }
            document.getElementById('confirm-payment-total').textContent = display(state.paid);
            document.getElementById('confirm-payment-remaining').textContent = display(state.due - state.paid);
            if (!confirmation.open) confirmation.showModal();
            return;
        }
        if (refund) document.getElementById('refund-expected-total').value = (state.due / 100).toFixed(2);
        submitting = true;
        dirty = false;
        submit.disabled = true;
        submit.querySelector('span').textContent = i18n.processing;
    });
    window.addEventListener('beforeunload', event => {
        if (dirty && !submitting) { event.preventDefault(); event.returnValue = ''; }
    });
    window.addEventListener('pageshow', event => { if (event.persisted) window.location.reload(); });
    totals();
})();
