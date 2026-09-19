(() => {
    'use strict';
    const root=document.getElementById('telebPage');if(!root)return;
    const $=id=>document.getElementById(id),t=window.TelebI18n;
    const editor=root.dataset.editor==='true',editId=root.dataset.editId || null;
    const warehouse=$(editor?'telebMyWarehouse':'telebWarehouse'),year=$('telebYear'),target=$('telebTarget');
    const state={side:root.dataset.side,epoch:0,listRequest:0,detailRequest:0,catalogRequest:0,unitRequest:0,page:1,total:0,rows:[],selected:null,details:[],draft:[],counterparts:[],material:null,units:[],filters:{},busy:false,dirty:false,warehouse:'',target:'',date:null};
    const picker=bootstrap.Modal.getOrCreateInstance($('telebPicker'));
    const filterPanel=bootstrap.Offcanvas.getOrCreateInstance($('telebFilter'));
    const number=v=>v==null?'—':new Intl.NumberFormat(document.documentElement.lang||'az',{maximumFractionDigits:3}).format(Number(v));
    const money=v=>v==null?'—':new Intl.NumberFormat(document.documentElement.lang||'az',{minimumFractionDigits:2,maximumFractionDigits:3}).format(Number(v));
    const date=v=>v?String(v).slice(0,10).split('-').reverse().join('.'):'—';
    const today=()=>{const d=new Date();return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`;};
    function notice(text='',success=false,id='telebNotice') {const el=$(id);el.textContent=text;el.classList.toggle('d-none',!text);el.classList.toggle('alert-success',success);el.classList.toggle('alert-danger',!success);}
    function choices(el,rows,id='id',name='ad',label=t.choose) {el.replaceChildren(new Option(label,''));rows.forEach(r=>el.add(new Option(String(r[name]??''),String(r[id]))));}
    function empty(body,count,label) {body.replaceChildren();const td=body.insertRow().insertCell();td.colSpan=count;td.className='text-center text-muted py-4';td.textContent=label;}
    function cell(row,value,cls='') {const td=row.insertCell();td.textContent=value==null||value===''?'—':String(value);td.className=cls;if(cls==='teleb-note')td.title=String(value??'');return td;}
    function badge(row,status) {const el=document.createElement('span');el.className='badge '+({GOZLEYIR:'bg-primary-subtle text-primary',ANBARDA_YOXDUR:'bg-secondary-subtle text-secondary',REDD_EDILDI:'bg-danger-subtle text-danger',QISMEN_VERILDI:'bg-warning-subtle text-warning',QISMEN_QARSILANDI:'bg-warning-subtle text-warning',TAM_QARSILANDI:'bg-success-subtle text-success',QARSILANDI:'bg-success-subtle text-success'}[status]||'bg-light text-dark');el.textContent=t[status]||status||'—';row.insertCell().append(el);}
    function action(td,icon,label,callback) {const b=document.createElement('button');b.type='button';b.className='btn btn-light teleb-icon-button me-1 '+(icon==='trash'?'text-danger':'text-primary');b.title=label;b.setAttribute('aria-label',label);const i=document.createElement('i');i.className='ti ti-'+icon;i.setAttribute('aria-hidden','true');b.append(i);b.addEventListener('click',callback);td.append(b);}
    async function api(path,params={},body) {
        const url=new URL(root.dataset.base+path,location.origin);Object.entries({anbarId:warehouse.value,...params}).forEach(([k,v])=>{if(v!=null&&v!=='')url.searchParams.set(k,v);});
        const options={credentials:'same-origin',headers:{Accept:'application/json'}};
        if(body!==undefined){options.method='POST';options.headers['Content-Type']='application/json';options.headers[$('telebCsrf').dataset.header]=$('telebCsrf').value;options.body=JSON.stringify(body);}
        const response=await fetch(url,options);if(!response.headers.get('content-type')?.includes('application/json'))throw new Error(t.loadFailed);
        const data=await response.json();if(!response.ok || (data.status_kodu&&data.status_kodu!=='UGURLU'))throw new Error(data.mesaj||data.message||t.saveFailed);return data;
    }
    function clearDetails() {state.selected=null;state.details=[];state.detailRequest++;$('telebSelected').textContent='';empty($('telebDetailRows'),9,t.chooseDetail);$('telebConfirm').disabled=true;}
    function pagination() {const pages=Math.max(1,Math.ceil(state.total/Number($('telebPageSize').value)));$('telebPagination').textContent=`${state.total} ${t.results} · ${t.page} ${state.page} / ${pages}`;$('telebPrevious').disabled=state.page<=1;$('telebNext').disabled=state.page>=pages;$('telebCount').textContent=String(state.total);}
    function renderList() {
        const body=$('telebRows');body.replaceChildren();if(!state.rows.length)empty(body,state.side==='GELEN'?10:11,t.empty);
        state.rows.forEach((item,index)=>{
            const row=body.insertRow();row.classList.toggle('teleb-selected',item.teleb_id===state.selected?.teleb_id);
            cell(row,(state.page-1)*Number($('telebPageSize').value)+index+1);cell(row,'T-'+String(item.teleb_id).padStart(5,'0'));cell(row,date(item.teleb_tarixi));cell(row,item.teleb_saati?.slice(0,5));
            cell(row,item[state.side==='GELEN'?'isteyen_anbar_adi':'qarsilayan_anbar_adi']);cell(row,item.material_sayi);cell(row,number(item.teleb_yekun_miqdar));badge(row,item.status);cell(row,item.aciqlama,'teleb-note');
            if(state.side!=='GELEN')cell(row,item.yaranma_tarixi?date(item.yaranma_tarixi)+' '+item.yaranma_tarixi.slice(11,16):null);
            const td=row.insertCell();action(td,'eye',t.view,()=>loadDetails(item.teleb_id));
            if(state.side==='GONDERILEN'&&item.redakte_edile_biler)action(td,'edit',t.edit,()=>{if(!state.busy)location.href=root.dataset.base+`/${item.teleb_id}/redakte?anbarId=${encodeURIComponent(warehouse.value)}`;});
        });pagination();
    }
    async function loadList() {
        if(!warehouse.value || (state.side==='GONDERILEN'&&!year.value))return;
        const epoch=state.epoch,request=++state.listRequest;
        empty($('telebRows'),state.side==='GELEN'?10:11,t.loading);notice();
        try {
            const data=await api('/siyahi',{teref:state.side,il:state.side==='GONDERILEN'?year.value:null,...state.filters,page:state.page,size:$('telebPageSize').value});
            if(epoch!==state.epoch||request!==state.listRequest)return;state.rows=data.rows;state.total=data.total;renderList();
        }catch(e){if(epoch===state.epoch&&request===state.listRequest){state.rows=[];state.total=0;renderList();notice(e.message);}}
    }
    async function loadDetails(id) {
        if(state.busy)return;
        const epoch=state.epoch,request=++state.detailRequest;state.selected=null;state.details=[];$('telebConfirm').disabled=true;empty($('telebDetailRows'),9,t.loading);notice();
        try {
            let data;
            if(state.side==='GELEN'){
                state.busy=true;
                await api(`/${id}/baxilir`,{},{});
                if(epoch!==state.epoch||request!==state.detailRequest)return;
                state.date=today();data=await api(`/${id}/qarsilama-materiallari`,{tarix:state.date});
            }else data=await api(`/${id}/detal`);
            if(epoch!==state.epoch||request!==state.detailRequest)return;
            state.selected=data.request;state.details=data.rows;$('telebSelected').textContent='— T-'+String(id).padStart(5,'0');
            const body=$('telebDetailRows');body.replaceChildren();if(!data.rows.length)empty(body,9,t.noMaterials);
            data.rows.forEach((item,index)=>{
                const row=body.insertRow();cell(row,item.sira_no??index+1);cell(row,item.material_adi);cell(row,item.barkod_nomresi);cell(row,item.secilen_vahid_adi);cell(row,number(item.secilen_vahid_miqdari));
                let input;
                if(state.side==='GELEN'&&item.qerar_verile_biler){
                    input=document.createElement('input');input.type='number';input.className='form-control form-control-sm teleb-fulfill-quantity';input.min='0';input.step='0.001';input.required=true;
                    const max=item.tam_qarsilamaq_olar?Number(item.qalan_secilen_miqdar):item.qismen_qarsilamaq_olar?Math.min(Number(item.qalan_secilen_miqdar),Number(item.movcud_stok_secilen_vahid)):0;
                    input.max=String(max);input.value=String(max);input.dataset.id=String(item.teleb_material_id);input.setAttribute('aria-label',`${t.fulfilled}: ${item.material_adi}`);input.title=`${t.stockAvailable}: ${number(item.movcud_stok_secilen_vahid)} ${item.secilen_vahid_adi}`;
                    row.insertCell().append(input);
                }else cell(row,number(item.qarsilanan_secilen_miqdar ?? Number(item.qarsilanan_miqdar)/Number(item.ana_vahide_emsal)));
                const remaining=cell(row,number(input?Math.max(0,Number(item.qalan_secilen_miqdar)-Number(input.value)):item.qalan_secilen_miqdar));
                if(input)input.addEventListener('input',()=>{remaining.textContent=number(Math.max(0,Number(item.qalan_secilen_miqdar)-Number(input.value)));});
                badge(row,item.status);cell(row,item.aciqlama,'teleb-note');
            });
            $('telebConfirm').disabled=!data.rows.some(r=>r.qerar_verile_biler);renderList();
        }catch(e){if(epoch===state.epoch&&request===state.detailRequest){clearDetails();notice(e.message);}}
        finally{state.busy=false;}
    }
    async function changeWarehouse() {
        const epoch=++state.epoch;state.page=1;state.rows=[];state.total=0;state.filters={};state.warehouse=warehouse.value;clearDetails();renderList();choices(year,[],'id','ad',t.chooseYear);year.disabled=true;if($('telebNew'))$('telebNew').disabled=true;
        $('telebFilterForm').reset();['telebFilterFrom','telebFilterTo'].forEach(id=>{$(id).value=today();$(id)._flatpickr?.setDate(today(),false);});choices($('telebFilterWarehouse'),[],'anbar_id','anbar_adi',t.all);$('telebStatus').value='';$('telebFrom').value=today().slice(0,7)+'-01';$('telebTo').value=today();$('telebRange')._flatpickr?.setDate([$('telebFrom').value,$('telebTo').value],false);notice();
        if(!warehouse.value)return;
        try {
            const years=await api('/iller',{teref:state.side});if(epoch!==state.epoch)return;
            choices(year,years.map(y=>({id:y.il,ad:y.il})), 'id','ad',t.chooseYear);year.value=String(years.find(y=>y.cariIldir)?.il??years[0]?.il??'');year.disabled=!years.length;
            if($('telebNew'))$('telebNew').disabled=!year.value;
            if(state.side==='GELEN')state.filters={from:$('telebFrom').value,to:$('telebTo').value};
            else {
                state.filters={from:$('telebFilterFrom').value,to:$('telebFilterTo').value};
                const counterparts=await api('/qarsi-anbarlar');if(epoch!==state.epoch)return;
                choices($('telebFilterWarehouse'),counterparts,'anbar_id','anbar_adi',t.all);
            }
            await loadList();
        }catch(e){if(epoch===state.epoch)notice(e.message);}
    }
    function changeSide(side) {
        if(state.busy)return;
        state.side=side;const incoming=side==='GELEN';root.classList.toggle('teleb-incoming',incoming);
        $('telebSentTab')?.classList.toggle('active',!incoming);$('telebIncomingTab')?.classList.toggle('active',incoming);
        $('telebSentTab')?.setAttribute('aria-selected',String(!incoming));$('telebIncomingTab')?.setAttribute('aria-selected',String(incoming));
        $('telebIncomingFilters').classList.toggle('d-none',!incoming);$('telebFulfillActions').classList.toggle('d-none',!incoming);
        $('telebYear').parentElement.classList.toggle('d-none',incoming);$('telebSearch').classList.toggle('d-none',incoming);$('telebFilterButton').classList.toggle('d-none',incoming);$('telebNew')?.classList.toggle('d-none',incoming);
        if(incoming)$('telebIncomingWarehouse').append($('telebWarehouseField'));else $('telebToolbar').prepend($('telebWarehouseField'));
        $('telebListTitle').textContent=incoming?t.incomingList:t.list;$('telebCounterpartTitle').textContent=incoming?t.requester:t.target;changeWarehouse();
    }
    function stock(item,unit) {
        const visible=state.counterparts.find(w=>String(w.anbar_id)===target.value)?.telebde_stok_gorunsun===true;
        return visible&&item?.stok_gosterilsin===true&&item.qaliq_miqdar!=null&&unit?Number(item.qaliq_miqdar)/Number(unit.ana_vahide_emsal):null;
    }
    function resetMaterial() {state.material=null;state.units=[];state.unitRequest++;$('telebMaterialName').value='';choices($('telebUnit'),[],'vahid_id','vahid_adi');$('telebUnit').disabled=true;$('telebStock').value='—';$('telebQuantity').value='';$('telebMaterialNote').value='';}
    function renderDraft() {
        const body=$('telebDraftRows');body.replaceChildren();if(!state.draft.length)empty(body,8,t.noMaterials);
        state.draft.forEach((item,index)=>{
            const row=body.insertRow();cell(row,index+1);cell(row,item.material_adi);cell(row,item.barkod_nomresi);cell(row,item.vahid_adi);cell(row,number(item.stock));cell(row,number(item.miqdar));cell(row,item.aciqlama,'teleb-note');
            action(row.insertCell(),'trash',t.delete,()=>{if(state.busy)return;state.draft.splice(index,1);state.dirty=true;renderDraft();});
        });
        $('telebDraftCount').textContent=String(state.draft.length);$('telebTotalCount').textContent=t.totalMaterials.replace('{0}',state.draft.length);
        $('telebQuantityTotal').textContent=money(state.draft.reduce((sum,r)=>sum+Number(r.miqdar),0));
        $('telebStockTotal').textContent=money(state.draft.length&&state.draft.every(r=>r.stock!=null)?state.draft.reduce((sum,r)=>sum+Number(r.stock),0):null);
        $('telebClearDraft').disabled=!state.draft.length;
    }
    async function changeMyWarehouse() {
        const epoch=++state.epoch;state.warehouse=warehouse.value;state.target='';state.counterparts=[];state.draft=[];state.catalogRequest++;choices(target,[],'anbar_id','anbar_adi');choices($('telebGroup'),[]);resetMaterial();renderDraft();notice();
        if(!warehouse.value)return;
        try{const rows=await api('/qarsi-anbarlar');if(epoch!==state.epoch)return;state.counterparts=rows;choices(target,rows,'anbar_id','anbar_adi');}
        catch(e){if(epoch===state.epoch)notice(e.message);}
    }
    async function changeTarget() {
        const epoch=++state.epoch;state.target=target.value;state.catalogRequest++;state.draft=[];choices($('telebGroup'),[]);resetMaterial();renderDraft();notice();
        if(!target.value)return;
        try{const rows=await api('/qruplar',{qarsiAnbarId:target.value});if(epoch!==state.epoch)return;choices($('telebGroup'),rows);}
        catch(e){if(epoch===state.epoch)notice(e.message);}
    }
    async function searchMaterials() {
        const epoch=state.epoch,request=++state.catalogRequest,query=$('telebPickerQuery').value.trim().toLocaleLowerCase();
        const body=$('telebPickerRows');empty(body,6,t.loading);notice('',false,'telebPickerError');
        try {
            const rows=await api('/materiallar',{qarsiAnbarId:target.value,qrupId:$('telebGroup').value});
            if(epoch!==state.epoch||request!==state.catalogRequest)return;
            body.replaceChildren();rows.filter(r=>[r.material_adi,r.barkod_nomresi,r.material_id].some(v=>String(v??'').toLocaleLowerCase().includes(query))).forEach(item=>{
                const row=body.insertRow();cell(row,item.material_id);cell(row,item.material_adi);cell(row,item.barkod_nomresi);cell(row,item.ana_vahid_adi);cell(row,number(stock(item,{ana_vahide_emsal:1})));
                const button=document.createElement('button');button.type='button';button.className='btn btn-sm btn-primary';button.textContent=t.select;
                button.addEventListener('click',async()=>{
                    state.material=item;state.units=[];const unitRequest=++state.unitRequest;$('telebMaterialName').value=item.material_adi;$('telebStock').value='—';choices($('telebUnit'),[],'vahid_id','vahid_adi');$('telebUnit').disabled=true;picker.hide();notice();
                    try{const units=await api('/vahidler',{qarsiAnbarId:target.value,materialId:item.material_id});if(epoch!==state.epoch||unitRequest!==state.unitRequest)return;state.units=units;choices($('telebUnit'),units,'vahid_id','vahid_adi');$('telebUnit').disabled=false;$('telebUnit').value=String(units.find(u=>u.ana_vahiddir)?.vahid_id??units[0]?.vahid_id??'');$('telebUnit').dispatchEvent(new Event('change'));$('telebQuantity').focus();}
                    catch(e){if(epoch===state.epoch&&unitRequest===state.unitRequest)notice(e.message);}
                });row.insertCell().append(button);
            });if(!body.rows.length)empty(body,6,t.empty);
        }catch(e){if(epoch===state.epoch&&request===state.catalogRequest){empty(body,6,t.empty);notice(e.message,false,'telebPickerError');}}
    }
    async function openEditor() {
        state.busy=true;$('telebSave').disabled=true;warehouse.disabled=true;
        $('telebDate').max=today();$('telebDate').value=today();$('telebDate')._flatpickr?.set('maxDate',today());$('telebDate')._flatpickr?.setDate(today(),false);
        $('telebEditorTitle').textContent=editId?t.edit:t.newTitle;$('telebSaveLabel').textContent=editId?t.save:t.create;
        try {
            await changeMyWarehouse();
            if(editId){
                const data=await api(`/${editId}/detal`);if(!data.edit.redakte_edile_biler){notice(t.editLocked);return;}
                target.value=String(data.request.qarsilayan_anbar_id);await changeTarget();$('telebDate').value=data.request.teleb_tarixi;$('telebDate')._flatpickr?.setDate(data.request.teleb_tarixi,false);$('telebNote').value=data.request.aciqlama||'';
                const catalog=await api('/materiallar',{qarsiAnbarId:target.value});
                state.draft=data.rows.map(item=>({material_id:item.material_id,vahid_id:item.secilen_vahid_id,miqdar:item.secilen_vahid_miqdari,aciqlama:item.aciqlama,material_adi:item.material_adi,barkod_nomresi:item.barkod_nomresi,vahid_adi:item.secilen_vahid_adi,stock:stock(catalog.find(m=>m.material_id===item.material_id),{ana_vahide_emsal:item.ana_vahide_emsal})}));renderDraft();
            }
            $('telebSave').disabled=false;state.dirty=false;
        }catch(e){notice(e.message);}
        finally{state.busy=false;warehouse.disabled=Boolean(editId);}
    }
    function back() {if(state.busy || (state.dirty&&!window.confirm(t.discard)))return;state.dirty=false;location.href=root.dataset.base+'?anbarId='+encodeURIComponent(warehouse.value);}
    $('telebSentTab')?.addEventListener('click',()=>changeSide('GONDERILEN'));$('telebIncomingTab')?.addEventListener('click',()=>changeSide('GELEN'));
    $('telebWarehouse').addEventListener('change',()=>{if(!state.busy)changeWarehouse();else $('telebWarehouse').value=state.warehouse;});
    year.addEventListener('change',()=>{state.epoch++;state.page=1;clearDetails();loadList();});
    $('telebSearch').addEventListener('click',()=>{state.page=1;clearDetails();loadList();});
    $('telebNew')?.addEventListener('click',()=>{if(!state.busy&&warehouse.value)location.href=root.dataset.base+'/yeni?anbarId='+encodeURIComponent(warehouse.value);});
    $('telebIncomingFilterForm').addEventListener('submit',e=>{e.preventDefault();if(state.busy)return;$('telebIncomingSearch').click();bootstrap.Offcanvas.getOrCreateInstance($('telebIncomingFilter')).hide();});
    $('telebIncomingSearch').addEventListener('click',()=>{if(state.busy)return;state.filters={from:$('telebFrom').value,to:$('telebTo').value,status:$('telebStatus').value};state.page=1;clearDetails();loadList();});
    $('telebIncomingClear').addEventListener('click',()=>{if(state.busy)return;$('telebFrom').value='';$('telebTo').value='';$('telebRange')._flatpickr?.clear();$('telebStatus').value='';state.filters={};state.page=1;clearDetails();loadList();});
    $('telebFilterForm').addEventListener('submit',e=>{e.preventDefault();if(state.busy||!e.target.reportValidity())return;state.filters=Object.fromEntries(new FormData(e.target));state.page=1;filterPanel.hide();clearDetails();loadList();});
    $('telebFilterClear').addEventListener('click',()=>{if(state.busy)return;$('telebFilterForm').reset();['telebFilterFrom','telebFilterTo'].forEach(id=>{$(id).value=today();$(id)._flatpickr?.setDate(today(),false);});state.filters={from:$('telebFilterFrom').value,to:$('telebFilterTo').value};state.page=1;filterPanel.hide();clearDetails();loadList();});
    $('telebPrevious').addEventListener('click',()=>{if(!state.busy){state.page--;loadList();}});$('telebNext').addEventListener('click',()=>{if(!state.busy){state.page++;loadList();}});$('telebPageSize').addEventListener('change',()=>{state.page=1;loadList();});
    $('telebFulfillCancel').addEventListener('click',()=>{if(!state.busy){clearDetails();renderList();}});
    $('telebFulfillForm').addEventListener('submit',async e=>{
        e.preventDefault();if(state.busy||!state.selected||!e.target.reportValidity())return;
        const id=state.selected.teleb_id;
        const decisions=[...$('telebDetailRows').querySelectorAll('input[data-id]')].map(input=>{
            const item=state.details.find(r=>String(r.teleb_material_id)===input.dataset.id),quantity=Number(input.value);
            return {teleb_material_id:item.teleb_material_id,qerar:quantity===0?(Number(item.movcud_stok)===0?'STOKDA_YOXDUR':'VERILMIR'):quantity===Number(item.secilen_vahid_miqdari)?'TAM':'QISMEN',miqdar:quantity,aciqlama:item.aciqlama||null};
        });
        if(!decisions.length){notice(t.decisionsRequired);return;}
        state.busy=true;$('telebConfirm').disabled=true;
        try{const result=await api(`/${id}/qarsila`,{},{tarix:state.date,saat:new Date().toTimeString().slice(0,8),qerarlar:decisions,aciqlama:null});state.busy=false;await loadList();await loadDetails(id);notice(result.mesaj||t.saved,true);}
        catch(error){notice(error.message);$('telebConfirm').disabled=false;}finally{state.busy=false;}
    });
    $('telebMyWarehouse').addEventListener('change',()=>{if(state.busy || (state.draft.length&&!window.confirm(t.changeTarget))){warehouse.value=state.warehouse;return;}state.dirty=true;changeMyWarehouse();});
    target.addEventListener('change',()=>{if(state.busy || (state.draft.length&&!window.confirm(t.changeTarget))){target.value=state.target;return;}state.dirty=true;changeTarget();});
    $('telebGroup').addEventListener('change',()=>{state.catalogRequest++;resetMaterial();});
    $('telebMaterialName').addEventListener('input',()=>{state.material=null;state.units=[];state.unitRequest++;choices($('telebUnit'),[],'vahid_id','vahid_adi');$('telebUnit').disabled=true;$('telebStock').value='—';});
    $('telebMaterialSearch').addEventListener('click',()=>{if(state.busy||!warehouse.reportValidity()||!target.reportValidity()||!$('telebGroup').reportValidity())return;$('telebPickerQuery').value=$('telebMaterialName').value;picker.show();searchMaterials();});
    $('telebMaterialName').addEventListener('keydown',e=>{if(e.key==='Enter'){e.preventDefault();$('telebMaterialSearch').click();}});
    $('telebPickerForm').addEventListener('submit',e=>{e.preventDefault();searchMaterials();});$('telebPicker').addEventListener('shown.bs.modal',()=>$('telebPickerQuery').focus());$('telebPicker').addEventListener('hidden.bs.modal',()=>state.catalogRequest++);
    $('telebUnit').addEventListener('change',()=>{$('telebStock').value=number(stock(state.material,state.units.find(u=>String(u.vahid_id)===$('telebUnit').value)));});
    $('telebMaterialForm').addEventListener('submit',e=>{
        e.preventDefault();if(state.busy||!e.target.reportValidity())return;
        if(!state.material){notice(t.selectRequired);return;}
        const unit=state.units.find(u=>String(u.vahid_id)===$('telebUnit').value);if(!unit){notice(t.invalidInput);return;}
        if(state.draft.some(r=>r.material_id===state.material.material_id)){notice(t.duplicate);return;}
        state.draft.push({material_id:state.material.material_id,vahid_id:unit.vahid_id,miqdar:Number($('telebQuantity').value),aciqlama:$('telebMaterialNote').value.trim()||null,material_adi:state.material.material_adi,barkod_nomresi:state.material.barkod_nomresi,vahid_adi:unit.vahid_adi,stock:stock(state.material,unit)});
        state.dirty=true;renderDraft();resetMaterial();notice();
    });
    $('telebClearDraft').addEventListener('click',()=>{if(state.busy||!window.confirm(t.confirmClear))return;state.draft=[];state.dirty=true;renderDraft();});
    $('telebHeader').addEventListener('input',()=>state.dirty=true);
    $('telebHeader').addEventListener('submit',async e=>{
        e.preventDefault();if(state.busy||!e.target.reportValidity())return;if(!state.draft.length){notice(t.materialsRequired);return;}
        state.busy=true;$('telebSave').disabled=true;
        const data=Object.fromEntries(new FormData(e.target));data.materiallar=state.draft.map(r=>({material_id:r.material_id,vahid_id:r.vahid_id,miqdar:r.miqdar,aciqlama:r.aciqlama}));
        try{await api(editId?`/${editId}/yenile`:'/yeni',{},data);state.dirty=false;location.href=root.dataset.base+'?anbarId='+encodeURIComponent(warehouse.value);}
        catch(error){notice(error.message);state.busy=false;$('telebSave').disabled=false;}
    });
    $('telebBack').addEventListener('click',back);$('telebCancel').addEventListener('click',back);
    window.addEventListener('beforeunload',e=>{if(editor&&state.dirty){e.preventDefault();e.returnValue='';}});
    const range=$('telebRange')._flatpickr || flatpickr($('telebRange'),{mode:'range',locale:'az',dateFormat:'Y-m-d',altInput:true,altFormat:'d.m.Y',disableMobile:true});
    range.config.onChange.push((dates,text,instance)=>{$('telebFrom').value=dates[0]?instance.formatDate(dates[0],'Y-m-d'):'';$('telebTo').value=dates[1]?instance.formatDate(dates[1],'Y-m-d'):$('telebFrom').value;});
    warehouse.value=root.dataset.warehouse||warehouse.options[1]?.value||'';
    if(!warehouse.value){notice(t.noWarehouses);if(editor){$('telebSave').disabled=true;renderDraft();}else{clearDetails();renderList();}}
    else if(editor)openEditor();else changeSide(state.side);
})();
