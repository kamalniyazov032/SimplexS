const {test}=require('node:test');const assert=require('node:assert/strict');const vm=require('node:vm');const fs=require('node:fs');const path=require('node:path');
test('advance selection sets the refund maximum and confirmation submits only once',()=>{
 const node=()=>({value:'',listeners:{},addEventListener(n,f){this.listeners[n]=f;},focus(){}});
 const ids=Object.fromEntries(['form','amount','dialog','modal','submit','account','type','balance','feedback','no','yes','confirm-number','confirm-account','confirm-type','confirm-amount'].map(id=>['advance-refund-'+id,node()]));
 const form=ids['advance-refund-form'],dialog=ids['advance-refund-dialog'],choice={...node(),checked:true,disabled:false,dataset:{balance:'25.00',number:'AV001'}};
 form.querySelectorAll=()=>[choice];dialog.open=false;dialog.showModal=()=>{dialog.open=true;};dialog.close=()=>{dialog.open=false;dialog.listeners.close();};
 for(const id of ['account','type']){ids['advance-refund-'+id].value='1';ids['advance-refund-'+id].selectedOptions=[{textContent:id}];}
 let submissions=0;form.requestSubmit=()=>{let blocked=false;form.listeners.submit({preventDefault(){blocked=true;}});if(!blocked)submissions++;};
 vm.runInNewContext(fs.readFileSync(path.join(__dirname,'../../main/resources/static/custom/js/kassa-advance-refund.js'),'utf8'),{document:{getElementById:id=>ids[id],documentElement:{lang:'en'}},window:{AdvanceRefundI18n:{select:'select',amount:'amount',over:'over',account:'account',currency:'AZN'},addEventListener(){}}});
 assert.equal(ids['advance-refund-amount'].max,'25.00');
 ids['advance-refund-amount'].value='25.01';form.requestSubmit();assert.equal(dialog.open,false);assert.equal(ids['advance-refund-feedback'].textContent,'over');
 ids['advance-refund-amount'].value='20.00';form.requestSubmit();assert.equal(dialog.open,true);assert.equal(submissions,0);
 ids['advance-refund-yes'].listeners.click();ids['advance-refund-yes'].listeners.click();form.requestSubmit();assert.equal(submissions,1);
});
