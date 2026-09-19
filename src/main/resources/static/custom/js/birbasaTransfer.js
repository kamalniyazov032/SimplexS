(() => {
    'use strict';
    const root=document.getElementById('transferPage');if(!root)return;
    const $=id=>document.getElementById(id),t=window.TransferI18n;
    const sender=$('transferSender'),receiver=$('transferReceiver'),date=$('transferDate'),unit=$('transferUnit');
    const picker=bootstrap.Modal.getOrCreateInstance($('transferPicker'));
    const confirmation=bootstrap.Modal.getOrCreateInstance($('transferConfirmModal'));
    let catalog=[],draft=[],material=null,units=[],epoch=0,unitRequest=0,busy=false,loading=false;
    const number=value=>new Intl.NumberFormat(document.documentElement.lang||'az',{maximumFractionDigits:3}).format(Number(value));
    function notice(message='',success=false){const el=$('transferNotice');el.textContent=message;el.className=message?'alert '+(success?'alert-success':'alert-danger'):'alert d-none';}
    async function api(path,params={},body){
        const url=new URL(root.dataset.base+path,location.origin);
        Object.entries({anbarId:sender.value,tarix:date.value,...params}).forEach(([k,v])=>url.searchParams.set(k,v));
        const options={headers:{Accept:'application/json'},credentials:'same-origin'};
        if(body!==undefined){options.method='POST';options.headers['Content-Type']='application/json';options.headers[$('transferCsrf').dataset.header]=$('transferCsrf').value;options.body=JSON.stringify(body);}
        const response=await fetch(url,options);
        if(!response.headers.get('content-type')?.includes('application/json'))throw new Error(t.loadFailed);
        const data=await response.json();if(!response.ok || data.status_kodu && data.status_kodu!=='UGURLU')throw new Error(data.mesaj||data.message||t.saveFailed);return data;
    }
    function resetMaterial(){material=null;units=[];unitRequest++;$('transferMaterial').value='';unit.replaceChildren(new Option(t.unit,''));unit.disabled=true;$('transferStock').value='';$('transferQuantity').value='';$('transferMaterialNote').value='';}
    function cell(row,value){row.insertCell().textContent=value==null||value===''?'—':String(value);}
    function render(){
        const body=$('transferRows');body.replaceChildren();$('transferCount').textContent=String(draft.length);$('transferComplete').disabled=!draft.length||busy||loading;
        draft.forEach((item,index)=>{const row=body.insertRow();[index+1,item.material_adi,item.barkod_nomresi,item.vahid_adi,number(item.stock),number(item.miqdar),item.aciqlama].forEach(v=>cell(row,v));
            const b=document.createElement('button');b.type='button';b.className='btn btn-sm btn-danger';b.setAttribute('aria-label',t.delete);const i=document.createElement('i');i.className='ti ti-trash';i.setAttribute('aria-hidden','true');b.append(i);row.insertCell().append(b);
            b.addEventListener('click',()=>{if(busy)return;draft.splice(index,1);render();});});
    }
    async function loadCatalog(clearDraft){
        const request=++epoch;unitRequest++;catalog=[];resetMaterial();if(clearDraft)draft=[];loading=true;render();$('transferSearch').disabled=true;
        for(const option of receiver.options)option.disabled=option.value!==''&&option.value===sender.value;
        if(receiver.value===sender.value)receiver.value='';
        if(!sender.value||!date.value){loading=false;render();return;}
        try{const rows=await api('/materiallar');if(request!==epoch)return;catalog=rows;
            if(!clearDraft)draft.forEach(item=>{const current=rows.find(r=>r.material_id===item.material_id);item.stock=current?Number(current.movcud_qaliq)/item.emsal:0;});
        }catch(e){if(request===epoch)notice(e.message);}
        finally{if(request===epoch){loading=false;$('transferSearch').disabled=false;render();}}
    }
    function showResults(){
        const query=$('transferQuery').value.trim().toLocaleLowerCase();const body=$('transferPickerRows');body.replaceChildren();
        catalog.filter(m=>[m.material_adi,m.barkod_nomresi].some(v=>String(v??'').toLocaleLowerCase().includes(query))).forEach(item=>{
            const row=body.insertRow();[item.material_adi,item.barkod_nomresi,item.ana_vahid_adi,number(item.movcud_qaliq)].forEach(v=>cell(row,v));
            const b=document.createElement('button');b.type='button';b.className='btn btn-sm btn-primary';b.textContent=t.select;row.insertCell().append(b);
            b.addEventListener('click',async()=>{if(busy)return;resetMaterial();material=item;$('transferMaterial').value=item.material_adi;picker.hide();const request=++unitRequest,currentEpoch=epoch;
                try{const data=await api('/vahidler',{materialId:item.material_id});if(request!==unitRequest||currentEpoch!==epoch)return;units=data;unit.replaceChildren(new Option(t.unit,''));units.forEach(u=>unit.add(new Option(u.vahid_adi,String(u.vahid_id))));unit.disabled=false;unit.value=String(units.find(u=>u.ana_vahiddir)?.vahid_id??units[0]?.vahid_id??'');unit.dispatchEvent(new Event('change'));}
                catch(e){if(request===unitRequest)notice(e.message);}});
        });
        if(!body.rows.length){const td=body.insertRow().insertCell();td.colSpan=5;td.className='text-center text-muted';td.textContent=t.empty;}
    }
    $('transferSearch').addEventListener('click',()=>{if(busy||loading||!sender.value||!date.value)return;$('transferQuery').value=$('transferMaterial').value;showResults();picker.show();});
    $('transferMaterial').addEventListener('keydown',e=>{if(e.key==='Enter'){e.preventDefault();$('transferSearch').click();}});
    $('transferPickerForm').addEventListener('submit',e=>{e.preventDefault();showResults();});
    $('transferMaterial').addEventListener('input',()=>{material=null;units=[];unitRequest++;unit.replaceChildren(new Option(t.unit,''));unit.disabled=true;$('transferStock').value='';});
    unit.addEventListener('change',()=>{const selected=units.find(u=>String(u.vahid_id)===unit.value);const stock=material&&selected?Number(material.movcud_qaliq)/Number(selected.ana_vahide_emsal):null;$('transferStock').value=stock==null?'':number(stock);if(stock==null)$('transferQuantity').removeAttribute('max');else $('transferQuantity').max=String(stock);});
    sender.addEventListener('change',()=>{if(!busy){notice();loadCatalog(true);}});
    date.addEventListener('change',()=>{if(!busy){notice();loadCatalog(false);}});
    $('transferMaterialForm').addEventListener('submit',e=>{e.preventDefault();if(busy||loading)return;notice();if(!material){notice(t.selectRequired);return;}const selected=units.find(u=>String(u.vahid_id)===unit.value);if(!selected)return;
        if(draft.some(m=>m.material_id===material.material_id)){notice(t.duplicate);return;}
        const quantity=Number($('transferQuantity').value),stock=Number(material.movcud_qaliq)/Number(selected.ana_vahide_emsal);
        if(!Number.isFinite(quantity)||quantity<=0||quantity>stock){notice(t.quantityInvalid);return;}
        draft.push({material_id:material.material_id,vahid_id:selected.vahid_id,miqdar:quantity,aciqlama:$('transferMaterialNote').value.trim(),material_adi:material.material_adi,barkod_nomresi:material.barkod_nomresi,vahid_adi:selected.vahid_adi,emsal:Number(selected.ana_vahide_emsal),stock});resetMaterial();render();});
    $('transferClear').addEventListener('click',()=>{if(!busy){draft=[];render();}});
    $('transferHeader').addEventListener('submit',e=>e.preventDefault());
    $('transferComplete').addEventListener('click',()=>{
        if(busy||loading||!$('transferHeader').reportValidity())return;notice();
        if(sender.value===receiver.value){notice(t.sameWarehouse);return;}if(!draft.length){notice(t.materialsRequired);return;}
        if(draft.some(m=>m.miqdar>m.stock)){notice(t.quantityInvalid);return;}
        $('transferRoute').textContent=sender.selectedOptions[0].text+' → '+receiver.selectedOptions[0].text;
        $('transferConfirmCount').textContent=t.confirmCount.replace('{0}',String(draft.length));confirmation.show();
    });
    $('transferConfirmModal').addEventListener('hide.bs.modal',e=>{if(busy)e.preventDefault();});
    $('transferConfirm').addEventListener('click',async()=>{
        if(busy)return;busy=true;const controls=[...root.querySelectorAll('input,select,button')].map(el=>[el,el.disabled]);controls.forEach(([el])=>el.disabled=true);
        let result;
        try{result=await api('',{},{qebul_eden_anbar_id:Number(receiver.value),transfer_tarixi:date.value,aciqlama:$('transferNote').value.trim(),materiallar:draft.map(m=>({material_id:m.material_id,vahid_id:m.vahid_id,miqdar:m.miqdar,aciqlama:m.aciqlama}))});}
        catch(e){notice(e.message);}
        finally{busy=false;controls.forEach(([el,disabled])=>el.disabled=disabled);confirmation.hide();}
        if(result){$('transferHeader').reset();sender.value=sender.options[1]?.value||'';receiver.value='';date.value=root.dataset.today;date._flatpickr?.setDate(date.value,false);$('transferNote').value='';notice(result.mesaj||t.saved,true);await loadCatalog(true);}
    });
    sender.value=sender.options[1]?.value||'';render();if(sender.value)loadCatalog(true);else notice(t.noWarehouses);
})();
