const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const path = require('node:path');
function income({ values = ['200.00', '50.00'], accountId = '5', advance = false, fetcher = async () => ({ ok: true, redirected: false, text: async () => '<table></table>' }) } = {}) {
    const node = extra => ({ textContent: '', value: '', dataset: {}, listeners: {}, classList: { add() {}, remove() {}, toggle() {} }, setAttribute() {}, focus() {}, addEventListener(name, callback) { this.listeners[name] = callback; }, ...extra });
    const ids = Object.fromEntries(['other-income-modal', 'income-feedback', 'income-total', 'income-account-description', 'income-cancel', 'income-confirm', 'income-confirm-account', 'income-confirm-total', 'other-income-open'].map(id => [id, node()]));
    for (const id of ['advance-visit-id', 'advance-search', 'advance-search-results', 'advance-search-feedback', 'advance-selected', 'advance-query', 'advance-card-type', 'advance-from', 'advance-to', 'advance-search-button', 'advance-clear', 'advance-open', 'advance-selected-name', 'advance-selected-card', 'advance-selected-visit-card', 'advance-selected-dob', 'advance-selected-date', 'advance-selected-details', 'advance-confirm-patient']) ids[id] = node();
    ids['advance-search'].dataset.url = 'http://localhost/kassa/avans/xesteler?kassaId=7';
    ids['advance-search-results'].replaceChildren = function () { this.innerHTML = ''; };
    ids['advance-search-results'].contains = () => true;
    ids['advance-search-results'].querySelectorAll = () => [];
    const amounts = values.map(value => node({ value }));
    const account = ids['income-account'] = node({ value: accountId, selectedOptions: [{ textContent: 'Other income', dataset: { description: 'Accounting description' } }] });
    const dialog = ids['income-confirmation'] = node({ open: false, showModal() { this.open = true; }, close() { this.open = false; this.listeners.close(); } });
    ids['income-submit'] = node({ disabled: false, querySelector: () => node() });
    const form = ids['other-income-form'] = node({ dataset: { advance: String(advance) }, querySelectorAll: () => amounts });
    let submissions = 0;
    let openings = 0;
    function submit() {
        let prevented = false;
        form.listeners.submit({ preventDefault() { prevented = true; } });
        if (!prevented) submissions++;
        return prevented;
    }
    form.requestSubmit = submit;
    const windowEvents = {};
    vm.runInNewContext(fs.readFileSync(path.join(__dirname, '../../main/resources/static/custom/js/kassa-other-income.js'), 'utf8'), {
        document: { getElementById: id => ids[id], documentElement: { lang: 'en' }, body: { appendChild() {} } },
        URL, AbortController, fetch: fetcher,
        window: node({ addEventListener(name, callback) { windowEvents[name] = callback; }, location: { href: 'http://localhost/kassa/avans?kassaId=7' }, KassaIncomeI18n: { invalidAccount: 'account', invalidPayment: 'amount', processing: 'processing', selectPatient: 'patient', searchHint: 'search hint', searching: 'searching', searchFailed: 'failed', invalidSearch: 'invalid search' }, bootstrap: { Modal: { getOrCreateInstance: () => ({ show() { openings++; } }) } } })
    });
    return { windowEvents, ids, amounts, account, dialog, submit, get submissions() { return submissions; }, get openings() { return openings; }, confirm() { ids['income-confirm'].listeners.click(); } };
}
test('popup totals split income and requires custom confirmation before one submission', () => {
    const page = income();
    assert.equal(page.openings, 1);
    assert.equal(page.ids['income-total'].textContent, '250.00');
    assert.equal(page.submit(), true);
    assert.equal(page.submissions, 0);
    assert.equal(page.dialog.open, true);
    assert.equal(page.ids['income-confirm-total'].textContent, '250.00');
    assert.equal(page.ids['income-confirm-account'].textContent, 'Other income');
    page.confirm(); page.confirm(); page.submit();
    assert.equal(page.submissions, 1);
    assert.equal(page.ids['income-submit'].disabled, true);
});
test('invalid account or money cannot reach confirmation', () => {
    for (const options of [{ accountId: '' }, { values: ['0'] }, { values: ['-1'] }, { values: ['0.001'] }, { values: ['999999999999.99', '0.01'] }]) {
        const page = income(options);
        page.submit();
        assert.equal(page.dialog.open, false);
        assert.equal(page.submissions, 0);
    }
});
test('cancel and popup reopen preserve values without sending income', () => {
    const page = income();
    page.submit(); page.ids['income-cancel'].listeners.click();
    assert.equal(page.dialog.open, false);
    assert.equal(page.submissions, 0);
    assert.equal(page.amounts[0].value, '200.00');
    let prevented = false;
    page.ids['other-income-open'].listeners.click({ preventDefault() { prevented = true; } });
    assert.equal(prevented, true);
    assert.equal(page.openings, 2);
});
test('confirmation revalidates and blocks modal dismissal while confirming', () => {
    const page = income();
    page.submit();
    let prevented = false;
    page.ids['other-income-modal'].listeners['hide.bs.modal']({ preventDefault() { prevented = true; } });
    assert.equal(prevented, true);
    page.amounts[0].value = '-1';
    page.confirm();
    assert.equal(page.submissions, 0);
    assert.equal(page.ids['income-feedback'].textContent, 'amount');
});

test('advance requires explicit patient selection and includes their identity in confirmation', () => {
    const page = income({ advance: true });
    page.submit();
    assert.equal(page.dialog.open, false);
    assert.equal(page.ids['income-feedback'].textContent, 'patient');
    const button = { dataset: { visitId: '17', name: 'Kamal Niyazov Ali', card: 'X004', visitCard: 'AMB017', dob: '1985-03-12', date: '2026-09-09' } };
    page.ids['advance-search-results'].listeners.click({ target: { closest: selector => selector === '[data-select-advance]' ? button : null } });
    assert.equal(page.ids['advance-visit-id'].value, '17');
    assert.equal(page.ids['advance-selected-name'].textContent, 'Kamal Niyazov Ali');
    assert.equal(page.ids['advance-selected-dob'].textContent, '1985-03-12');
    page.ids['advance-selected-details'].textContent = 'Kamal Niyazov Ali X004 AMB017';
    page.submit();
    assert.equal(page.dialog.open, true);
    assert.equal(page.ids['advance-confirm-patient'].textContent, 'Kamal Niyazov Ali X004 AMB017');
    page.confirm();
    assert.equal(page.submissions, 1);
});
test('advance search sends full name and filters and never auto-selects a result', async () => {
    let searched;
    const page = income({ advance: true, fetcher: async url => { searched = url; return { ok: true, text: async () => '<table>results</table>' }; } });
    page.ids['advance-query'].value = ' Kamal Niyazov ';
    page.ids['advance-card-type'].value = 'AMBULATOR';
    page.ids['advance-from'].value = '2026-09-01';
    await page.ids['advance-search-button'].listeners.click();
    assert.equal(searched.searchParams.get('q'), 'Kamal Niyazov');
    assert.equal(searched.searchParams.get('cardType'), 'AMBULATOR');
    assert.equal(searched.searchParams.get('from'), '2026-09-01');
    assert.equal(page.ids['advance-search-results'].innerHTML, '<table>results</table>');
    assert.equal(page.ids['advance-visit-id'].value, '');
});
test('changing search invalidates old results and ignores a stale response', async () => {
    let release;
    const page = income({ advance: true, fetcher: () => new Promise(resolve => { release = resolve; }) });
    page.ids['advance-query'].value = 'Kamal';
    const pending = page.ids['advance-search-button'].listeners.click();
    page.ids['advance-visit-id'].value = '17';
    page.ids['advance-query'].value = 'Other';
    page.ids['advance-search'].listeners.input();
    release({ ok: true, text: async () => 'stale result' });
    await pending;
    assert.equal(page.ids['advance-search-results'].innerHTML, '');
    assert.equal(page.ids['advance-visit-id'].value, '');
});

test('advance pagination keeps filters and a new search resets to page one', async () => {
    const urls = [];
    const page = income({ advance: true, fetcher: async url => { urls.push(url); return { ok: true, text: async () => 'rows' }; } });
    page.ids['advance-query'].value = 'Kamal';
    page.ids['advance-card-type'].value = 'AMBULATOR';
    await page.ids['advance-search-button'].listeners.click();
    assert.equal(urls[0].searchParams.get('page'), '1');
    const pager = { disabled: false, dataset: { advancePage: '2' } };
    page.ids['advance-search-results'].listeners.click({ target: { closest: selector => selector === '[data-advance-page]' ? pager : null } });
    assert.equal(urls[1].searchParams.get('page'), '2');
    assert.equal(urls[1].searchParams.get('q'), 'Kamal');
    assert.equal(urls[1].searchParams.get('cardType'), 'AMBULATOR');
    pager.disabled = true;
    page.ids['advance-search-results'].listeners.click({ target: { closest: () => pager } });
    assert.equal(urls.length, 2);
    await page.ids['advance-search-button'].listeners.click();
    assert.equal(urls[2].searchParams.get('page'), '1');
});

test('closing a changed popup allows navigation without the browser unsaved warning', () => {
    const page = income();
    const leaving = () => {
        let blocked = false;
        page.windowEvents.beforeunload({ preventDefault() { blocked = true; } });
        return blocked;
    };
    assert.equal(leaving(), false);
    page.ids['other-income-form'].listeners.input();
    assert.equal(leaving(), true);
    page.ids['other-income-modal'].listeners['hidden.bs.modal']();
    assert.equal(leaving(), false);
    // Select2 events from a hidden form must not reactivate the warning.
    page.account.listeners.change();
    assert.equal(leaving(), false);
    page.ids['other-income-modal'].listeners['show.bs.modal']();
    assert.equal(leaving(), true);
    assert.equal(page.submissions, 0);
});

test('advance selection stays in the table and switches directly to another patient', () => {
    const page = income({ advance: true });
    const makeButton = visitId => {
        const classes = new Set();
        const row = { classList: { toggle(name, enabled) { enabled ? classes.add(name) : classes.delete(name); } } };
        return { dataset: { visitId }, classes, classList: { toggle() {} }, setAttribute(name, value) { this[name] = value; }, closest: () => row };
    };
    const buttons = [makeButton('17'), makeButton('18')];
    page.ids['advance-search-results'].querySelectorAll = () => buttons;
    for (const button of buttons) {
        page.ids['advance-search-results'].listeners.click({ target: { closest: selector => selector === '[data-select-advance]' ? button : null } });
        assert.equal(page.ids['advance-visit-id'].value, button.dataset.visitId);
        assert.equal(button['aria-pressed'], 'true');
        assert.equal(button.classes.has('is-selected'), true);
    }
    assert.equal(buttons[0]['aria-pressed'], 'false');
    assert.equal(buttons[0].classes.has('is-selected'), false);
    assert.equal(page.ids['advance-clear'].listeners.click, undefined);
});
