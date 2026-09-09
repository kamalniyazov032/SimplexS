const {test}=require('node:test');
const assert=require('node:assert/strict');
const vm=require('node:vm');
const fs=require('node:fs');
const path=require('node:path');
test('receipt cancellation requires a reason and explicit confirmation and submits once',()=>{
 const node=()=>({listeners:{},value:'',addEventListener(n,f){this.listeners[n]=f;},focus(){}});
 const ids=Object.fromEntries(['receipt-cancel-form','receipt-cancel-dialog','receipt-cancel-submit','receipt-reason','receipt-cancel-no','receipt-cancel-yes','receipt-confirm-reason'].map(id=>[id,node()]));
 const form=ids['receipt-cancel-form'],dialog=ids['receipt-cancel-dialog'];
 dialog.open=false;dialog.showModal=()=>{dialog.open=true;};dialog.close=()=>{dialog.open=false;dialog.listeners.close();};
 let submissions=0;
 form.requestSubmit=()=>{let blocked=false;form.listeners.submit({preventDefault(){blocked=true;}});if(!blocked)submissions++;};
 vm.runInNewContext(fs.readFileSync(path.join(__dirname,'../../main/resources/static/custom/js/kassa-receipts.js'),'utf8'),{document:{getElementById:id=>ids[id]},window:{addEventListener(){}}});
 form.requestSubmit();assert.equal(dialog.open,false);assert.equal(submissions,0);
 ids['receipt-reason'].value='Incorrect receipt';form.requestSubmit();assert.equal(dialog.open,true);assert.equal(submissions,0);
 ids['receipt-cancel-no'].listeners.click();assert.equal(submissions,0);
 form.requestSubmit();ids['receipt-cancel-yes'].listeners.click();ids['receipt-cancel-yes'].listeners.click();form.requestSubmit();assert.equal(submissions,1);
});
