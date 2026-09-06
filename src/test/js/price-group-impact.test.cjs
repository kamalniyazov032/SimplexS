const {test} = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const path = require('node:path');
function form() {
  let change, submit, group = '18', shown = 0;
  const nodes = {priceGroupApplyExisting: {value: 'false', form: {addEventListener: (_, fn) => submit = fn}}, priceGroupImpactModal: {}};
  for (const id of ['priceGroupImpactYes', 'priceGroupImpactNo']) nodes[id] = {addEventListener: (_, fn) => nodes[id].click = fn};
  const source = fs.readFileSync(path.join(__dirname, '../../main/resources/static/custom/js/customMy.js'), 'utf8');
  vm.runInNewContext(source.slice(source.indexOf('        const impactField ='), source.indexOf("        const emptyText = $qiymetQrupu")), {
    document: {getElementById: id => nodes[id]},
    $qiymetQrupu: {data: () => '18', val: () => group, on: (_, fn) => change = fn},
    bootstrap: {Modal: {getOrCreateInstance: () => ({show: () => shown++, hide() {}})}}
  });
  return {nodes, change(value) {group = value; change();}, get shown() {return shown;}, submit() {let blocked = false; submit({preventDefault() {blocked = true;}}); return blocked;}};
}
test('initial value stays silent; changed group requires yes or no before save', () => {
  const f = form(); f.change(''); f.change('18'); assert.equal(f.shown, 0);
  f.change('19'); assert.equal(f.shown, 1); assert.equal(f.submit(), true);
  f.nodes.priceGroupImpactYes.click(); assert.equal(f.nodes.priceGroupApplyExisting.value, 'true'); assert.equal(f.submit(), false);
  f.change('20'); assert.equal(f.nodes.priceGroupApplyExisting.value, 'false'); assert.equal(f.submit(), true);
  f.nodes.priceGroupImpactNo.click(); assert.equal(f.nodes.priceGroupApplyExisting.value, 'false'); assert.equal(f.submit(), false);
  f.change('19'); f.nodes.priceGroupImpactYes.click(); f.change('18');
  assert.equal(f.nodes.priceGroupApplyExisting.value, 'false'); assert.equal(f.submit(), false);
});
