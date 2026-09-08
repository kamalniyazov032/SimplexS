(() => {
    'use strict';
    const form = document.getElementById('other-income-form');
    if (!form) return;
    const modal = document.getElementById('other-income-modal');
    const account = document.getElementById('income-account');
    const amounts = [...form.querySelectorAll('.income-amount')];
    const submit = document.getElementById('income-submit');
    const feedback = document.getElementById('income-feedback');
    const dialog = document.getElementById('income-confirmation');
    const i18n = window.KassaIncomeI18n;
    const advance = form.dataset?.advance === 'true';
    const visit = advance ? document.getElementById('advance-visit-id') : null;
    let confirmed = false;
    let submitting = false;
    let dirty = false;
    const cents = value => {
        if (!/^\d+(\.\d{1,2})?$/.test(value || '0')) return NaN;
        const [whole, fraction = ''] = (value || '0').split('.');
        const amount = Number(whole) * 100 + Number(fraction.padEnd(2, '0'));
        return Number.isSafeInteger(amount) ? amount : NaN;
    };
    const display = value => (value / 100).toLocaleString(document.documentElement.lang || 'az', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    function totals() {
        const total = amounts.reduce((sum, input) => sum + cents(input.value), 0);
        document.getElementById('income-total').textContent = Number.isFinite(total) ? display(total) : '—';
        return total;
    }
    function accountChanged() {
        document.getElementById('income-account-description').textContent = account.selectedOptions[0]?.dataset.description || '';
        feedback.textContent = '';
    }
    form.addEventListener('input', () => { dirty = true; feedback.textContent = ''; totals(); });
    account.addEventListener('change', () => { dirty = true; accountChanged(); });
    // Select2 emits jQuery change events; keep its business description in sync too.
    if (window.jQuery) window.jQuery(account).on('change.kassaIncome', () => { dirty = true; accountChanged(); });
    document.getElementById('income-cancel').addEventListener('click', () => dialog.close());
    dialog.addEventListener('close', () => submit.focus());
    document.getElementById('income-confirm').addEventListener('click', () => {
        if (submitting || !dialog.open) return;
        dialog.close();
        confirmed = true;
        try { form.requestSubmit(submit); } finally { confirmed = false; }
    });
    form.addEventListener('submit', event => {
        if (submitting) { event.preventDefault(); return; }
        const total = totals();
        const error = advance && !visit.value ? i18n.selectPatient : !account.value ? i18n.invalidAccount
            : !Number.isFinite(total) || total <= 0 || total > 99999999999999 ? i18n.invalidPayment : '';
        if (error) { event.preventDefault(); feedback.textContent = error; return; }
        if (!confirmed) {
            event.preventDefault();
            if (advance) document.getElementById('advance-confirm-patient').textContent =
                document.getElementById('advance-selected-details').textContent.trim().replace(/\s+/g, ' ');
            document.getElementById('income-confirm-account').textContent = account.selectedOptions[0]?.textContent || '';
            document.getElementById('income-confirm-total').textContent = display(total);
            if (!dialog.open) dialog.showModal();
            return;
        }
        submitting = true;
        dirty = false;
        submit.disabled = true;
        submit.querySelector('span').textContent = i18n.processing;
    });
    modal.addEventListener('hide.bs.modal', event => {
        if (submitting || dialog.open) event.preventDefault();
    });
    modal.addEventListener('hidden.bs.modal', () => document.getElementById(advance ? 'advance-open' : 'other-income-open')?.focus());
    const open = document.getElementById(advance ? 'advance-open' : 'other-income-open');
    if (window.bootstrap?.Modal) {
        document.body.appendChild(modal);
        const popup = window.bootstrap.Modal.getOrCreateInstance(modal);
        popup.show();
        open?.addEventListener('click', event => { event.preventDefault(); popup.show(); });
    }
    if (advance) {
        const search = document.getElementById('advance-search');
        const results = document.getElementById('advance-search-results');
        const status = document.getElementById('advance-search-feedback');
        const selected = document.getElementById('advance-selected');
        const query = document.getElementById('advance-query');
        const card = document.getElementById('advance-card-type');
        const from = document.getElementById('advance-from');
        const to = document.getElementById('advance-to');
        let request = null;
        let revision = 0;
        function clearSelection() {
            visit.value = '';
            selected.classList.add('d-none');
            results.querySelectorAll('[data-select-advance]').forEach(button => {
                button.classList.remove('btn-primary');
                button.classList.add('btn-outline-primary');
                button.setAttribute('aria-pressed', 'false');
            });
        }
        function invalidateSearch() {
            revision++;
            request?.abort();
            results.replaceChildren();
            results.setAttribute('aria-busy', 'false');
            status.textContent = i18n.searchHint;
            clearSelection();
        }
        async function findPatients(page = 1) {
            invalidateSearch();
            const current = revision;
            if (from.value && to.value && from.value > to.value) { status.textContent = i18n.invalidSearch; return; }
            if (!query.value.trim() && !card.value && !from.value && !to.value) return;
            request = new AbortController();
            const url = new URL(search.dataset.url, window.location.href);
            url.searchParams.set('q', query.value.trim());
            url.searchParams.set('page', String(page));
            if (card.value) url.searchParams.set('cardType', card.value);
            if (from.value) url.searchParams.set('from', from.value);
            if (to.value) url.searchParams.set('to', to.value);
            status.textContent = i18n.searching;
            results.setAttribute('aria-busy', 'true');
            try {
                const response = await fetch(url, { signal: request.signal, headers: { Accept: 'text/html' } });
                if (!response.ok || response.redirected) throw new Error('Search unavailable');
                const html = await response.text();
                if (current !== revision) return;
                // Same-origin Thymeleaf fragment; all patient fields are HTML-escaped server-side.
                results.innerHTML = html;
                status.textContent = '';
            } catch (error) {
                if (current === revision && error.name !== 'AbortError') status.textContent = i18n.searchFailed;
            } finally {
                if (current === revision) results.setAttribute('aria-busy', 'false');
            }
        }
        document.getElementById('advance-search-button').addEventListener('click', () => findPatients(1));
        search.addEventListener('keydown', event => {
            if (event.key === 'Enter') { event.preventDefault(); findPatients(); }
        });
        search.addEventListener('input', invalidateSearch);
        card.addEventListener('change', invalidateSearch);
        if (window.jQuery) window.jQuery(card).on('change.kassaAdvance', invalidateSearch);
        document.getElementById('advance-clear').addEventListener('click', () => {
            clearSelection();
            query.focus();
        });
        results.addEventListener('click', event => {
            const pager = event.target.closest('[data-advance-page]');
            if (pager && results.contains(pager)) {
                if (!pager.disabled) findPatients(Number(pager.dataset.advancePage));
                return;
            }
            const button = event.target.closest('[data-select-advance]');
            if (!button || !results.contains(button)) return;
            visit.value = button.dataset.visitId;
            for (const [id, field] of [['name', 'name'], ['card', 'card'], ['visit-card', 'visitCard'], ['dob', 'dob'], ['date', 'date']]) {
                document.getElementById('advance-selected-' + id).textContent = button.dataset[field] || '—';
            }
            selected.classList.remove('d-none');
            results.querySelectorAll('[data-select-advance]').forEach(item => {
                item.classList.toggle('btn-primary', item === button);
                item.classList.toggle('btn-outline-primary', item !== button);
                item.setAttribute('aria-pressed', String(item === button));
            });
            dirty = true;
            feedback.textContent = '';
        });
    }
    window.addEventListener('beforeunload', event => {
        if (dirty && !submitting) { event.preventDefault(); event.returnValue = ''; }
    });
    window.addEventListener('pageshow', event => { if (event.persisted) window.location.reload(); });
    accountChanged();
    totals();
})();
