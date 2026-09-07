document.addEventListener('DOMContentLoaded', () => {
    const form = document.getElementById('bulkPriceForm');
    if (!form) return;
    const tr = document.getElementById('tariffI18n').dataset;
    const rows = [...form.querySelectorAll('.price-row')];
    const fields = {xeste_payi: 'xestePayi', sigorta_payi: 'sigortaPayi', xeste_endirim: 'xesteEndirim', sigorta_endirim: 'sigortaEndirim'};
    const read = row => ({
        xidmet_id: Number(row.dataset.serviceId),
        qiymet: row.querySelector('.js-price').value === '' ? NaN : Number(row.querySelector('.js-price').value),
        edv_aktivdir: row.querySelector('.js-vat').checked,
        ...Object.fromEntries(Object.entries(fields).map(([key, data]) => [key, Number(row.dataset[data])]))
    });
    const valid = value => Number.isFinite(value.qiymet) && value.qiymet >= 0 &&
        Object.keys(fields).every(key => Number.isFinite(value[key]) && value[key] >= 0 && value[key] <= 100) &&
        Math.abs(value.xeste_payi + value.sigorta_payi - 100) < 0.000001;
    const initial = new Map(rows.map(row => [row, JSON.stringify(read(row))]));
    const dirty = row => {
        row.dataset.dirty = String(JSON.stringify(read(row)) !== initial.get(row));
        row.classList.toggle('table-warning', row.dataset.dirty === 'true');
        const count = rows.filter(r => r.dataset.dirty === 'true').length;
        document.getElementById('changedCount').textContent = count;
        document.getElementById('saveChanges').disabled = count === 0;
    };
    const apply = (row, value) => {
        row.querySelector('.js-price').value = value.qiymet;
        row.querySelector('.js-vat').checked = value.edv_aktivdir;
        Object.entries(fields).forEach(([key, data]) => row.dataset[data] = value[key]);
        row.querySelector('.js-patient-share').textContent = `${value.xeste_payi}%`;
        row.querySelector('.js-insurance-share').textContent = `${value.sigorta_payi}%`;
        row.querySelector('.js-discounts').textContent = tr.discounts.replace('{0}', value.xeste_endirim).replace('{1}', value.sigorta_endirim);
        dirty(row);
    };
    rows.forEach(row => row.querySelectorAll('.js-value').forEach(input => {
        ['input', 'change'].forEach(event => input.addEventListener(event, () => dirty(row)));
    }));
    form.addEventListener('submit', event => {
        const changed = rows.filter(row => row.dataset.dirty === 'true').map(read);
        if (!changed.length || !changed.every(valid)) {
            event.preventDefault();
            if (changed.length) window.alert(tr.invalid);
            return;
        }
        document.getElementById('qiymetlerJson').value = JSON.stringify(changed);
    });
    document.getElementById('selectAll').addEventListener('change', event => {
        rows.forEach(row => row.querySelector('.js-select').checked = event.target.checked);
    });
    for (const [modalId, formId, bulk] of [['tariffEditModal', 'tariffEditForm', false], ['bulkModal', 'bulkUpdateForm', true]]) {
        const modal = document.getElementById(modalId), editor = document.getElementById(formId);
        const error = editor.querySelector('.js-tariff-error');
        let activeRow;
        modal.addEventListener('show.bs.modal', event => {
            editor.reset();
            error.classList.add('d-none');
            if (bulk) return;
            activeRow = event.relatedTarget?.closest('.price-row');
            if (!activeRow) { event.preventDefault(); return; }
            document.getElementById('tariffEditName').textContent = activeRow.querySelector('b').textContent;
            const value = read(activeRow);
            ['qiymet', ...Object.keys(fields)].forEach(key => editor.elements[key].value = value[key]);
            editor.elements.edv_aktivdir.checked = value.edv_aktivdir;
        });
        editor.addEventListener('submit', event => {
            event.preventDefault();
            const targets = bulk ? rows.filter(row => row.querySelector('.js-select').checked) : [activeRow];
            const changes = targets.map(row => {
                const value = read(row);
                ['qiymet', ...Object.keys(fields)].forEach(key => {
                    if (editor.elements[key].value !== '') value[key] = Number(editor.elements[key].value);
                });
                if (!bulk) value.edv_aktivdir = editor.elements.edv_aktivdir.checked;
                else if (editor.elements.edv_aktivdir.value !== '') value.edv_aktivdir = editor.elements.edv_aktivdir.value === 'true';
                return value;
            });
            if (!targets.length || !editor.checkValidity() || !changes.every(valid)) {
                error.textContent = targets.length ? tr.invalid : tr.selectOne;
                error.classList.remove('d-none');
                return;
            }
            targets.forEach((row, i) => apply(row, changes[i]));
            bootstrap.Modal.getInstance(modal)?.hide();
        });
    }
});
