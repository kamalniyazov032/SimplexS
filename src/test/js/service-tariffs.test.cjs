const {test} = require('node:test');
const assert = require('node:assert/strict');
const vm = require('node:vm');
const fs = require('node:fs');
function page() {
    const node = () => ({value:'',checked:false,dataset:{},listeners:{},classList:{add(){},remove(){},toggle(){}},
        addEventListener(k,f){this.listeners[k]=f},checkValidity(){return true},reset(){}});
    const nodes = new Map();
    const get = id => {if(!nodes.has(id))nodes.set(id,node());return nodes.get(id)};
    get('tariffI18n').dataset={invalid:'Invalid',selectOne:'Select',discounts:'{0}/{1}'};
    const rows = [1,2].map(id => {
        const r=node(), parts={};
        r.dataset={serviceId:String(id),xestePayi:'30',sigortaPayi:'70',xesteEndirim:'10',sigortaEndirim:'5'};
        r.querySelector=s=>parts[s]??=node();
        r.querySelector('.js-price').value='50';
        r.querySelector('b').textContent='Service';
        r.querySelectorAll=()=>[r.querySelector('.js-price'),r.querySelector('.js-vat')];
        return r;
    });
    get('bulkPriceForm').querySelectorAll=()=>rows;
    for(const id of ['tariffEditForm','bulkUpdateForm']) {
        const f=get(id);f.elements={};
        for(const key of ['qiymet','edv_aktivdir','xeste_payi','sigorta_payi','xeste_endirim','sigorta_endirim']) f.elements[key]=node();
        f.error=node();f.querySelector=()=>f.error;
    }
    vm.runInNewContext(fs.readFileSync('src/main/resources/static/custom/js/service-tariffs.js','utf8'),{
        document:{getElementById:get,addEventListener:(_,f)=>f()},window:{alert(){}},bootstrap:{Modal:{getInstance:()=>({hide(){}})}}});
    return {get,rows};
}
test('row edit preserves all fields and serializes the full function payload',()=>{
    const {get,rows}=page();
    get('tariffEditModal').listeners['show.bs.modal']({relatedTarget:{closest:()=>rows[0]}});
    const f=get('tariffEditForm');
    f.elements.qiymet.value='75';f.elements.edv_aktivdir.checked=true;
    f.listeners.submit({preventDefault(){}});
    get('bulkPriceForm').listeners.submit({preventDefault(){assert.fail('valid payload rejected')}});
    assert.deepEqual(JSON.parse(get('qiymetlerJson').value),[{xidmet_id:1,qiymet:75,edv_aktivdir:true,xeste_payi:30,sigorta_payi:70,xeste_endirim:10,sigorta_endirim:5}]);
    assert.equal(rows[1].querySelector('.js-price').value,'50');
});
test('invalid share totals do not modify rows',()=>{
    const {get,rows}=page();
    get('tariffEditModal').listeners['show.bs.modal']({relatedTarget:{closest:()=>rows[0]}});
    const f=get('tariffEditForm');f.elements.xeste_payi.value='50';
    f.listeners.submit({preventDefault(){}});
    assert.equal(f.error.textContent,'Invalid');assert.equal(rows[0].dataset.xestePayi,'30');
});
test('bulk edit changes only selected rows and retains unspecified fields',()=>{
    const {get,rows}=page();rows[1].querySelector('.js-select').checked=true;
    const f=get('bulkUpdateForm');f.elements.xeste_endirim.value='0';f.elements.edv_aktivdir.value='false';
    f.listeners.submit({preventDefault(){}});
    get('bulkPriceForm').listeners.submit({preventDefault(){assert.fail()}});
    const values=JSON.parse(get('qiymetlerJson').value);
    assert.equal(values.length,1);assert.equal(values[0].xidmet_id,2);
    assert.equal(values[0].xeste_endirim,0);assert.equal(values[0].qiymet,50);assert.equal(values[0].sigorta_payi,70);
});
