const {test} = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const path = require('node:path');

function workspace(catalogItems = [], existingRows = [], departments = [], doctors = []) {
    const nodes = new Map();
    const element = () => ({
        value: '', dataset: {}, options: [], selectedOptions: [], children: [], listeners: {},
        firstChild: {}, setAttribute(name, value) { this[name] = value; },
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
    if (existingRows.length) get('patientServiceWorkspace').dataset.istekId = '23';
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
            if (url.endsWith('/sobeler')) return {ok: true, json: async () => departments};
            if (url.includes('/hekimler?')) return {ok: true, json: async () => doctors};
            if (!options.body) return {ok: true, json: async () => url.includes('/siyahi?') ? existingRows : ({items: catalogItems, hasMore: false})};
            requests.push(JSON.parse(options.body));
            return {ok: true, json: async () => ({ugurlu: true})};
        }
    });
    const source = fs.readFileSync(path.join(__dirname, '../../main/resources/static/custom/js/patient-services.js'), 'utf8');
    vm.runInContext(source.replace('    initialize();',
        '    globalThis.api = {populateDetails, initialize, syncDetails, newService, payload, validDate, selected, renderSelected, addService, setDraft(row) { draft = row; editingId = String(row.id); }, setRoutine(id) { activeCollection = id; }};'), context);
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
    get('referringDoctor').value = '18';
    get('referringDoctor').selectedOptions = [{textContent: 'Referring Doctor'}];
    get('requestingDoctor').value = '19';
    get('requestingDoctor').selectedOptions = [{textContent: 'Requesting Doctor'}];
    get('performingDoctor').value = '17';
    get('performingDoctor').selectedIndex = 0;
    get('performingDoctor').options = [{dataset: {doctorName: 'Performing Doctor', finalPrice: 25}}];
    const button = element();
    button.closest = () => null;
    await api.addService({id: 7831}, button);
    assert.equal(requests[0].xidmet_tarixi, '2026-09-05');
    assert.equal(requests[0].rutin_id, 2);
    const cells = get('selectedBody').children[0].children;
    assert.equal(cells[3].textContent, '2026-09-05');
    assert.equal(cells[4].textContent, 2);
    assert.equal(cells[9].textContent, 'Performing Doctor');
    assert.equal(cells[10].textContent, 'Referring Doctor');
    assert.equal(cells[11].textContent, 'Requesting Doctor');
    assert.equal(requests[0].icra_eden_hekim_id, 17);
    assert.equal(requests[0].gonderen_hekim_id, 18);
    assert.equal(requests[0].isteyen_hekim_id, 19);
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


test('editing request renders all saved doctor names and retains their IDs', async () => {
    const {api, get} = workspace([], [{
        xeste_xidmet_id: 3, xidmet_id: 7384, xidmet_tarixi: '2026-08-29', miqdar: 1,
        icra_eden_hekim_id: 17, icra_eden_hekim_adi: 'Performing Doctor',
        gonderen_hekim_id: 18, gonderen_hekim_adi: 'Referring Doctor',
        isteyen_hekim_id: 19, isteyen_hekim_adi: 'Requesting Doctor'
    }]);
    await api.initialize();
    const cells = get('selectedBody').children[0].children;
    assert.equal(cells[9].textContent, 'Performing Doctor');
    assert.equal(cells[10].textContent, 'Referring Doctor');
    assert.equal(cells[11].textContent, 'Requesting Doctor');
    const payload = api.payload(api.selected.get('existing-3'));
    assert.equal(payload.icra_eden_hekim_id, 17);
    assert.equal(payload.gonderen_hekim_id, 18);
    assert.equal(payload.isteyen_hekim_id, 19);
});


for (const scenario of [
    {name: 'one department is selected automatically', departments: [{sobe_id: 12, sobe_adi: 'Department'}], initial: null, expected: 12},
    {name: 'multiple departments require user selection', departments: [{sobe_id: 12}, {sobe_id: 44}], initial: null, expected: null},
    {name: 'saved department is preserved during editing', departments: [{sobe_id: 12}, {sobe_id: 44}], initial: 44, expected: 44},
    {name: 'empty department list stays unselected', departments: [], initial: null, expected: null}
]) {
    test(scenario.name, async () => {
        const {api, get, urls} = workspace([], [], scenario.departments);
        const row = api.newService({id: 7831});
        row.sobeId = scenario.initial;
        api.setDraft(row);
        await api.populateDetails(row);
        assert.equal(row.sobeId, scenario.expected);
        assert.equal(get('serviceDepartment').value, scenario.expected ?? '');
        assert.equal(api.payload(row).sobe_id, scenario.expected);
        assert.equal(urls.some(url => url.includes('/hekimler?')), scenario.expected != null);
        if (scenario.expected === 12) assert.equal(row.sobeAdi, 'Department');
    });
}


test('user doctor choices persist for new services and packages until changed or cleared', () => {
    const {api, get} = workspace();
    const referring = get('referringDoctor'), requesting = get('requestingDoctor');
    referring.value = '18';
    referring.selectedOptions = [{textContent: 'Referring Doctor'}];
    referring.listeners.change();
    requesting.value = '19';
    requesting.selectedOptions = [{textContent: 'Requesting Doctor'}];
    requesting.listeners.change();
    const first = api.newService({id: 1});
    api.selected.set('1', first);
    for (const type of ['XIDMET', 'RUTIN', 'PAKET']) {
        get('catalogType').value = type;
        const next = api.newService({id: 2});
        assert.equal(next.gonderenHekimId, 18);
        assert.equal(next.isteyenHekimId, 19);
        assert.equal(next.gonderenHekimAdi, 'Referring Doctor');
        assert.equal(next.isteyenHekimAdi, 'Requesting Doctor');
    }
    requesting.value = '21';
    requesting.listeners.change();
    referring.value = '';
    referring.listeners.change();
    const changed = api.newService({id: 3});
    assert.equal(changed.gonderenHekimId, null);
    assert.equal(changed.isteyenHekimId, 21);
    assert.equal(first.gonderenHekimId, 18);
    assert.equal(first.isteyenHekimId, 19);
    // Loading an existing row into controls must not replace explicit user defaults.
    referring.value = '40';
    requesting.value = '41';
    assert.equal(api.newService({id: 4}).isteyenHekimId, 21);
    assert.equal(api.newService({id: 4}).gonderenHekimId, null);
});


test('service search keeps the selected group; all groups and other catalogs omit it', async () => {
    const {get, urls} = workspace();
    get('catalogSearch').value = 'test';
    const selectGroup = async group => {
        get('serviceGroups').listeners.click({target: {closest: () => ({dataset: {group}})}});
        await new Promise(resolve => setImmediate(resolve));
        return new URL(urls.at(-1), 'http://localhost').searchParams;
    };
    let params = await selectGroup('12');
    assert.equal(params.get('qrupId'), '12');
    assert.equal(params.get('q'), 'test');
    params = await selectGroup('');
    assert.equal(params.has('qrupId'), false);
    for (const type of ['PAKET', 'RUTIN']) {
        get('catalogType').value = type;
        params = await selectGroup('12');
        assert.equal(params.has('qrupId'), false);
        assert.equal(params.get('q'), 'test');
    }
});

test('selecting a parent group loads services immediately without a search or child selection', async () => {
    const {get, urls} = workspace([{id: 7, ad: 'Child group service'}]);
    const icon = {classList: {contains: () => true, toggle() {}}};
    const parent = {
        dataset: {group: '21', hasChildren: 'true'},
        querySelector: () => icon
    };
    get('serviceGroups').listeners.click({target: {closest: () => parent}});
    await new Promise(resolve => setImmediate(resolve));
    assert.equal(urls.length, 1);
    const params = new URL(urls[0], 'http://localhost').searchParams;
    assert.equal(params.get('qrupId'), '21');
    assert.equal(params.has('q'), false);
    assert.equal(params.get('page'), '0');
    assert.equal(get('catalogBody').children.length, 1);
    assert.equal(get('catalogBody').children[0].dataset.serviceId, 7);
});

for (const scenario of [
    {name: 'single department without doctors adds on first plus', departments: [{sobe_id: 12}], doctors: [], added: true},
    {name: 'available doctors keep the candidate for selection', departments: [{sobe_id: 12}], doctors: [{hekim_id: 17}], added: false},
    {name: 'multiple departments keep the candidate for selection', departments: [{sobe_id: 12}, {sobe_id: 13}], doctors: [], added: false},
    {name: 'single required doctor is selected with its price on first plus', departments: [{sobe_id: 12, hekim_secim_qaydasi_kodu: 'MECBURI'}], doctors: [{hekim_id: 17, hekim_ad: 'Doctor', yekun_qiymet: 25}], added: true, doctorId: 17},
    {name: 'multiple required doctors need selection', departments: [{sobe_id: 12, hekim_secim_qaydasi_kodu: 'MECBURI'}], doctors: [{hekim_id: 17}, {hekim_id: 18}], added: false},
    {name: 'required doctor without options cannot auto-add', departments: [{sobe_id: 12, hekim_secim_qaydasi_kodu: 'MECBURI'}], doctors: [], added: false}
]) {
    test(scenario.name, async () => {
        const {api, get, element, requests} = workspace([], [], scenario.departments, scenario.doctors);
        // Model the selected option properties used by the browser's select element.
        for (const department of [get('serviceDepartment'), get('performingDoctor')]) {
        Object.defineProperty(department, 'options', {get() { return this.children; }});
        Object.defineProperty(department, 'selectedOptions', {get() {
            return this.children.filter(x => String(x.value) === String(this.value));
        }});
        Object.defineProperty(department, 'selectedIndex', {get() {
            return this.children.findIndex(x => String(x.value) === String(this.value));
        }});
        }
        const button = element();
        button.closest = () => null;
        await api.addService({id: 7831}, button);
        assert.equal(api.selected.has('7831'), scenario.added);
        assert.equal(requests.length, scenario.added ? 1 : 0);
        if (scenario.added) assert.equal(requests[0].sobe_id, 12);
        else assert.equal(get('catalogAlertMessage').textContent, 'Required');
        if (scenario.doctorId) {
            assert.equal(requests[0].icra_eden_hekim_id, scenario.doctorId);
            assert.equal(api.selected.get('7831').icraEdenHekimAdi, 'Doctor');
            assert.equal(api.selected.get('7831').qiymet, 25);
        }
    });
}
