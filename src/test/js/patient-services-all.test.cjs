const {test} = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

async function page(result, confirmed = true, packageValue = null) {
  const nodes = new Map();
  const element = () => ({
    value: '', textContent: '', dataset: {}, children: [],
    classList: {hidden: false, add() {this.hidden = true;}, remove() {this.hidden = false;}, toggle() {}},
    append(...items) {this.children.push(...items);},
    replaceChildren(...items) {this.children = items;},
    add(item) {this.children.push(item);}, setAttribute() {}
  });
  const get = id => {if (!nodes.has(id)) nodes.set(id, element()); return nodes.get(id);};
  get('allServices').dataset.gelisId = '2';
  get('allI18n').dataset = {cancelSuccess: 'Cancelled', loadError: 'Error', currency: 'AZN'};
  get('allSuccess').textContent = 'Created request and added 2 services';
  get('allError').textContent = 'Old error';
  const calls = [];
  let serverRow = {xeste_xidmet_id: 8, istek_id: 3, aktiv: true, paket_daxildir: false, vahid_qiymet: 10, yekun_mebleg: 10};
  vm.runInNewContext(fs.readFileSync(path.join(__dirname, '../../main/resources/static/custom/js/patient-services-all.js'), 'utf8'), {
    document: {getElementById: get, createElement: element, querySelector: () => ({value: 'csrf-test'}), querySelectorAll: () => [],
      addEventListener(event, fn) {if (event === 'DOMContentLoaded') fn();}},
    URLSearchParams, window: {confirm: () => confirmed}, Option: function() {},
    fetch: async (url, options) => {
      calls.push({url, options});
      if (options.method === 'POST' && result.status_kodu === 'UGURLU') serverRow = {...serverRow, ...result};
      return {ok: true, json: async () => options.method === 'POST' ? result : [serverRow]};
    }
  });
  await new Promise(resolve => setImmediate(resolve));
  if (packageValue === null) {
    const remove = get('allBody').children[0].children.at(-1).children[2];
    await remove.onclick();
  } else {
    const checkbox = get('allBody').children[0].children.at(-2).children[0];
    checkbox.checked = packageValue;
    await checkbox.onchange();
  }
  return {get, calls};
}

test('cancellation replaces old creation message with database cancellation message', async () => {
  const {get, calls} = await page({status_kodu: 'UGURLU', mesaj: 'DB cancellation message'});
  assert.equal(get('allSuccess').textContent, 'DB cancellation message');
  assert.equal(get('allSuccess').classList.hidden, false);
  assert.equal(get('allError').textContent, '');
  assert.equal(calls.filter(x => x.options.method === 'POST').length, 1);
  assert.equal(calls.at(-1).url, '/xeste-xidmetleri/2/siyahi');
});
test('failed cancellation clears old success and shows its error', async () => {
  const {get} = await page({status_kodu: 'XETA', mesaj: 'Cannot cancel'});
  assert.equal(get('allSuccess').textContent, '');
  assert.equal(get('allSuccess').classList.hidden, true);
  assert.equal(get('allError').textContent, 'Cannot cancel');
});
test('cancellation without database text uses localized fallback', async () => {
  const {get} = await page({status_kodu: 'UGURLU'});
  assert.equal(get('allSuccess').textContent, 'Cancelled');
});
test('declining confirmation preserves existing message and sends no cancellation', async () => {
  const {get, calls} = await page({}, false);
  assert.equal(get('allSuccess').textContent, 'Created request and added 2 services');
  assert.equal(calls.length, 1);
});


test('package checkbox posts both checked and unchecked values and reloads amounts', async () => {
  for (const checked of [true, false]) {
    const {get, calls} = await page({status_kodu: 'UGURLU', paket_daxildir: checked, vahid_qiymet: 0, yekun_mebleg: 0, mesaj: 'Updated'}, true, checked);
    const post = calls.find(call => call.options.method === 'POST');
    assert.equal(post.url, '/xeste-xidmetleri/2/paket-statusu/8');
    assert.equal(post.options.body, `paketDaxildir=${checked}`);
    assert.equal(calls.at(-1).url, '/xeste-xidmetleri/2/siyahi');
    assert.equal(get('allSuccess').textContent, 'Updated');
    assert.equal(post.options.headers['X-CSRF-TOKEN'], 'csrf-test');
    assert.equal(get('allBody').children[0].children.at(-2).children[0].checked, checked);
    assert.equal(get('allTotal').textContent, '0.00 AZN');
  }
});
test('failed package change restores checkbox and displays the database error', async () => {
  const {get, calls} = await page({status_kodu: 'XETA', mesaj: 'Locked'}, true, true);
  assert.equal(get('allBody').children[0].children.at(-2).children[0].checked, false);
  assert.equal(get('allError').textContent, 'Locked');
  assert.equal(calls.length, 2);
});
