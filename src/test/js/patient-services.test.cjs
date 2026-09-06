const {test} = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const path = require('node:path');

function workspace(catalogItems = []) {
    const nodes = new Map();
    const element = () => ({
        value: '', dataset: {}, options: [], selectedOptions: [], children: [], listeners: {},
        classList: {add() {}, remove() {}, toggle() {}}, style: {},
        addEventListener(event, callback) { this.listeners[event] = callback; },
        append(...children) { this.children.push(...children); },
        replaceChildren(...children) { this.children = children; },
        querySelector(selector) {
            if (selector.includes('_csrf')) return null;
            this.parts ??= {};
            return this.parts[selector] ??= element();
        }, querySelectorAll() { return []; },
        checkValidity() { return true; }, reportValidity() {}, focus() {},
        getBoundingClientRect() { return {top: 0}; }, submit() { this.submitted = true; }
    });
    const get = id => {
        if (!nodes.has(id)) nodes.set(id, element());
        return nodes.get(id);
    };
    get('patientServiceWorkspace').dataset.gelisId = '20';
    get('patientServiceI18n').dataset = {invalidDate: 'Invalid date', selectionRequired: 'Required', page: 'Page {0}'};
    get('serviceDate').value = '2026-09-05';
    get('catalogType').value = 'XIDMET';
    get('serviceQuantity').value = '1';
    const requests = [];
    const urls = [];
    const context = vm.createContext({
        document: {getElementById: get, createElement: element, body: element(),
            addEventListener(event, callback) { callback(); }},
        window: {innerHeight: 900, addEventListener() {}},
        URLSearchParams, AbortController, setTimeout, clearTimeout,
        fetch: async (url, options) => {
            urls.push(url);
            if (!options.body) return {ok: true, json: async () => ({items: catalogItems, hasMore: false})};
            requests.push(JSON.parse(options.body));
            return {ok: true, json: async () => ({ugurlu: true})};
        }
    });
    const source = fs.readFileSync(path.join(__dirname, '../../main/resources/static/custom/js/patient-services.js'), 'utf8');
    vm.runInContext(source.replace('    initialize();',
        '    globalThis.api = {newService, payload, validDate, selected, renderSelected, addService, setDraft(row) { draft = row; editingId = String(row.id); }, setRoutine(id) { activeCollection = id; }};'), context);
    return {api: context.api, get, requests, urls, element};
}

test('selected date and routine ID survive preparation, display and final JSON', async () => {
    const {api, get, requests, element} = workspace();
    get('catalogType').value = 'RUTIN';
    api.setRoutine('2');
    const row = api.newService({id: 7831, kod: 'S', ad: 'Service'});
    api.setDraft(row);
    get('serviceDepartment').value = '12';
    get('serviceDepartment').selectedIndex = 0;
    get('serviceDepartment').options = [{text: 'Department'}];
    get('serviceDepartment').selectedOptions = [{dataset: {doctorRule: 'SECIMLI'}}];
    const button = element();
    button.closest = () => null;
    await api.addService({id: 7831}, button);
    assert.equal(requests[0].xidmet_tarixi, '2026-09-05');
    assert.equal(requests[0].rutin_id, 2);
    const cells = get('selectedBody').children[0].children;
    assert.equal(cells[3].textContent, '2026-09-05');
    assert.equal(cells[4].textContent, 2);
    await get('selectedForm').listeners.submit({preventDefault() {}, target: get('selectedForm')});
    const saved = JSON.parse(get('servicesJson').value)[0];
    assert.equal(saved.xidmet_tarixi, '2026-09-05');
    assert.equal(saved.rutin_id, 2);
});

test('package saves with null department and routine ID', async () => {
    const {api, get} = workspace();
    get('catalogType').value = 'PAKET';
    api.selected.set('234', api.newService({id: 234}));
    await get('selectedForm').listeners.submit({preventDefault() {}, target: get('selectedForm')});
    assert.equal(get('selectedForm').submitted, true);
    const saved = JSON.parse(get('servicesJson').value)[0];
    assert.equal(saved.sobe_id, null);
    assert.equal(saved.rutin_id, null);
    assert.equal(saved.xidmet_tarixi, '2026-09-05');
});

test('invalid dates cannot be submitted; leap dates are checked', async () => {
    const {api, get, requests} = workspace();
    for (const value of ['', 'abc', '2026-02-29', '2026-09-31', '0000-01-01', '12345-01-01']) {
        assert.equal(api.validDate(value), false, value);
        get('serviceDate').value = value;
        await get('selectedForm').listeners.submit({preventDefault() {}, target: get('selectedForm')});
    }
    assert.equal(api.validDate('2028-02-29'), true);
    assert.equal(requests.length, 0);
    assert.equal(get('selectedForm').submitted, undefined);
});


test('switching from a service search loads routines without the old filter', async () => {
    const {get, urls} = workspace([{id: 2, kod: 'PAK001', ad: 'Routine', xidmet_sayi: 1}]);
    get('catalogSearch').value = '7831';
    const type = get('catalogType');
    type.value = 'RUTIN';
    type.selectedIndex = 0;
    type.options = [{text: 'Routines'}];
    type.listeners.change();
    await new Promise(resolve => setImmediate(resolve));
    assert.equal(get('catalogSearch').value, '');
    assert.equal(new URL(urls[0], 'http://localhost').searchParams.get('nov'), 'RUTIN');
    assert.equal(new URL(urls[0], 'http://localhost').searchParams.has('q'), false);
    assert.equal(get('collectionList').children.length, 1);
    assert.equal(get('collectionList').children[0].dataset.id, 2);
    assert.equal(get('collectionList').children[0].querySelector('span').textContent, 'Routine');
});
