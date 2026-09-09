(() => {
 'use strict';
 const get=id=>document.getElementById('advance-refund-'+id), form=get('form');
 if(!form)return;
 const choices=[...form.querySelectorAll('.advance-refund-choice')], amount=get('amount'), dialog=get('dialog'), modal=get('modal'), submit=get('submit'), account=get('account'), type=get('type'), texts=window.AdvanceRefundI18n;
 let confirmed=false,submitting=false,dirty=false,visible=true;
 const selected=()=>choices.find(c=>c.checked&&!c.disabled);
 const cents=value=>{if(!/^\d+(\.\d{1,2})?$/.test(String(value)))return NaN;const [whole,part='']=String(value).split('.');const n=Number(whole)*100+Number(part.padEnd(2,'0'));return Number.isSafeInteger(n)?n:NaN;};
 const display=value=>(value/100).toLocaleString(document.documentElement.lang||'az',{minimumFractionDigits:2,maximumFractionDigits:2});
 function update(){const choice=selected();amount.max=choice?choice.dataset.balance:'0';get('balance').textContent=choice?display(cents(choice.dataset.balance)):'—';}
 choices.forEach(c=>c.addEventListener('change',update));
 form.addEventListener('input',()=>{dirty=true;get('feedback').textContent='';});
 form.addEventListener('change',()=>{dirty=true;});
 modal.addEventListener('hide.bs.modal',e=>{if(submitting||dialog.open)e.preventDefault();});
 modal.addEventListener('hidden.bs.modal',()=>{visible=false;document.querySelector('[data-patient-row].is-selected a')?.focus();});
 modal.addEventListener('show.bs.modal',()=>{visible=true;});
 if(window.bootstrap?.Modal){document.body.appendChild(modal);window.bootstrap.Modal.getOrCreateInstance(modal).show();}
 get('no').addEventListener('click',()=>dialog.close());dialog.addEventListener('close',()=>submit.focus());
 get('yes').addEventListener('click',()=>{if(submitting||!dialog.open)return;dialog.close();confirmed=true;try{form.requestSubmit(submit);}finally{confirmed=false;}});
 form.addEventListener('submit',e=>{
  if(submitting){e.preventDefault();return;}
  const choice=selected(),value=cents(amount.value);
  const error=!choice?texts.select:!Number.isFinite(value)||value<=0||value>99999999999999?texts.amount:value>cents(choice.dataset.balance)?texts.over:!account.value?texts.account:!type.value?texts.amount:'';
  if(error){e.preventDefault();get('feedback').textContent=error;return;}
  if(!confirmed){e.preventDefault();get('confirm-number').textContent=choice.dataset.number;get('confirm-account').textContent=account.selectedOptions[0]?.textContent||'';get('confirm-type').textContent=type.selectedOptions[0]?.textContent||'';get('confirm-amount').textContent=display(value)+' '+texts.currency;if(!dialog.open)dialog.showModal();return;}
  submitting=true;dirty=false;submit.disabled=true;
 });
 window.addEventListener('beforeunload',e=>{if(visible&&dirty&&!submitting){e.preventDefault();e.returnValue='';}});
 window.addEventListener('pageshow',e=>{if(e.persisted)window.location.reload();});update();
})();
