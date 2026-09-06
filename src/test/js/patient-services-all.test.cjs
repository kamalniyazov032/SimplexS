const {test} = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

async function page(result, confirmed = true) {
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
  get('allI18n').dataset = {cancelSuccess: 'Cancelled', loadError: 'Error'};
  get('allSuccess').textContent = 'Created request and added 2 services';
  get('allError').textContent = 'Old error';
  const calls = [];
  vm.runInNewContext(fs.readFileSync(path.join(__dirname, '../../main/resources/static/custom/js/patient-services-all.js'), 'utf8'), {
    document: {getElementById: get, createElement: element, querySelector: () => null, querySelectorAll: () => [],
      addEventListener(event, fn) {if (event === 'DOMContentLoaded') fn();}},
    window: {confirm: () => confirmed}, Option: function() {},
    fetch: async (url, options) => {
      calls.push({url, options});
      return {ok: true, json: async () => options.method === 'POST' ? result : [{xeste_xidmet_id: 8, istek_id: 3, aktiv: true}]};
    }
  });
  await new Promise(resolve => setImmediate(resolve));
  const remove = get('allBody').children[0].children.at(-1).children[2];
  await remove.onclick();
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
