const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const path = require('node:path');
function checkout({ debt = false, due = '0.30', paid = ['0.10', '0.20'], selected = true, count = 1, cashIndex = -1 } = {}) {
    const node = (extra = {}) => ({ textContent: '', listeners: {}, focus() {}, addEventListener(name, fn) { this.listeners[name] = fn; }, ...extra });
    const ids = Object.fromEntries(['select-all-services', 'borrow-remainder', 'payment-feedback', 'selected-count', 'selected-total', 'payment-total', 'remaining-total', 'confirm-payment', 'cancel-payment', 'confirm-payment-total', 'confirm-payment-remaining', 'cash-change-row', 'cash-change-total', 'confirm-cash-change-row', 'confirm-cash-change-total'].map(id => [id, node()]));
    const dialog = node({ open: false, showModal() { this.open = true; }, close() { this.open = false; this.listeners.close?.(); } });
    ids['payment-confirmation'] = dialog;
    const buttonText = node();
    ids['submit-payment'] = node({ disabled: false, querySelector: () => buttonText });
    const choices = Array.from({ length: count }, () => node({ checked: selected, dataset: { amount: due } }));
    const amounts = paid.map((value, index) => node({ value, dataset: { paymentName: index === cashIndex ? 'Nağd' : 'Kart' } }));
    let submissions = 0;
    const form = node({ dataset: { debt: String(debt) }, querySelectorAll: selector => selector.startsWith('.service-choice') ? choices : amounts });
    const submit = () => {
        let prevented = false;
        form.listeners.submit({ preventDefault() { prevented = true; } });
        if (!prevented) submissions++;
        return prevented;
    };
    form.requestSubmit = submit;
    ids['cash-payment-form'] = form;
    const win = node({ KassaI18n: { selectServices: 'select', invalidPayment: 'amount', overpayment: 'over', borrowRequired: 'borrow', processing: 'processing' }, confirm: () => assert.fail('Native confirmation must not be used') });
    vm.runInNewContext(fs.readFileSync(path.join(__dirname, '../../main/resources/static/custom/js/kassa-center.js'), 'utf8'), {
        document: { documentElement: { lang: 'en' }, getElementById: id => ids[id] }, window: win
    });
    return { ids, choices, amounts, dialog, submit, get submissions() { return submissions; },
        confirm() { ids['confirm-payment'].listeners.click(); },
        cancel() { ids['cancel-payment'].listeners.click(); },
        selectAll(checked) {
            const all = ids['select-all-services'];
            all.checked = checked;
            form.listeners.input({ target: all }); // Browser order: input bubbles before change.
            all.listeners.change();
        },
        selectOne(index, checked) { choices[index].checked = checked; form.listeners.input({ target: choices[index] }); }
    };
}
test('split payment shows exact cents in custom dialog and submits only once after confirmation', () => {
    const page = checkout();
    assert.equal(page.ids['remaining-total'].textContent, '0.00');
    assert.equal(page.submit(), true);
    assert.equal(page.submissions, 0);
    assert.equal(page.dialog.open, true);
    assert.equal(page.ids['confirm-payment-total'].textContent, '0.30');
    assert.equal(page.ids['confirm-payment-remaining'].textContent, '0.00');
    page.confirm();
    assert.equal(page.submissions, 1);
    assert.equal(page.ids['submit-payment'].disabled, true);
    page.confirm();
    assert.equal(page.submit(), true);
    assert.equal(page.submissions, 1);
});
test('partial service payment requires borrowing; debt repayment allows a remainder', () => {
    const page = checkout({ due: '10.00', paid: ['5.00'] });
    page.submit();
    assert.equal(page.dialog.open, false);
    assert.equal(page.ids['payment-feedback'].textContent, 'borrow');
    page.ids['borrow-remainder'].checked = true;
    page.submit();
    assert.equal(page.dialog.open, true);
    const debt = checkout({ debt: true, due: '10.00', paid: ['5.00'] });
    debt.submit();
    assert.equal(debt.dialog.open, true);
});
test('invalid amounts and empty selections never open confirmation', () => {
    for (const input of [{ paid: ['1.00'] }, { paid: ['0.301'] }, { selected: false }]) {
        const page = checkout(input);
        assert.equal(page.submit(), true);
        assert.equal(page.dialog.open, false);
        assert.equal(page.submissions, 0);
    }
});
test('cancelling keeps values and allows reopening without submitting', () => {
    const page = checkout();
    page.submit(); page.cancel();
    assert.equal(page.dialog.open, false);
    assert.equal(page.submissions, 0);
    assert.equal(page.amounts[0].value, '0.10');
    assert.equal(page.ids['submit-payment'].disabled, false);
    page.submit();
    assert.equal(page.dialog.open, true);
});
test('select all handles input/change ordering, clearing and partial selection', () => {
    const page = checkout({ count: 3, selected: false });
    page.selectAll(true);
    assert.ok(page.choices.every(choice => choice.checked));
    assert.equal(page.ids['selected-count'].textContent, 3);
    assert.equal(page.ids['selected-total'].textContent, '0.90');
    page.selectOne(0, false);
    assert.equal(page.ids['select-all-services'].indeterminate, true);
    page.selectAll(true);
    assert.ok(page.choices.every(choice => choice.checked));
    page.selectAll(false);
    assert.ok(page.choices.every(choice => !choice.checked));
    assert.equal(page.ids['selected-total'].textContent, '0.00');
    assert.equal(page.ids['select-all-services'].indeterminate, false);
});
test('no eligible services disables select all', () => {
    assert.equal(checkout({ count: 0 }).ids['select-all-services'].disabled, true);
});
test('confirmation revalidates the payment before submitting', () => {
    const page = checkout();
    page.submit();
    page.amounts[0].value = '100';
    page.confirm();
    assert.equal(page.submissions, 0);
    assert.equal(page.ids['payment-feedback'].textContent, 'over');
});

test('cash tender displays change immediately and submits only retained cash', () => {
    const page = checkout({ due: '27480.00', paid: ['27500.00'], cashIndex: 0 });
    assert.equal(page.ids['cash-change-row'].hidden, false);
    assert.equal(page.ids['cash-change-total'].textContent, '20.00');
    assert.equal(page.ids['remaining-total'].textContent, '0.00');
    page.submit();
    assert.equal(page.ids['confirm-cash-change-total'].textContent, '20.00');
    page.cancel();
    assert.equal(page.amounts[0].value, '27500.00');
    page.submit(); page.confirm();
    assert.equal(page.submissions, 1);
    assert.equal(page.amounts[0].value, '27480.00');
});
test('split payment returns excess from cash only; excess card payment stays invalid', () => {
    const page = checkout({ due: '100.00', paid: ['80.00', '30.00'], cashIndex: 0 });
    assert.equal(page.ids['cash-change-total'].textContent, '10.00');
    page.submit(); page.confirm();
    assert.equal(page.amounts[0].value, '70.00');
    assert.equal(page.amounts[1].value, '30.00');
    const invalid = checkout({ due: '100.00', paid: ['5.00', '110.00'], cashIndex: 0 });
    invalid.submit();
    assert.equal(invalid.dialog.open, false);
    assert.equal(invalid.ids['cash-change-row'].hidden, true);
});
test('cash change updates when the service selection changes and preserves exact cents', () => {
    const page = checkout({ due: '0.10', count: 2, paid: ['0.30'], cashIndex: 0 });
    assert.equal(page.ids['cash-change-total'].textContent, '0.10');
    page.selectOne(0, false);
    assert.equal(page.ids['cash-change-total'].textContent, '0.20');
});
