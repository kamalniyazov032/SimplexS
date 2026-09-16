(() => {
    'use strict';
    const root = document.getElementById('qaimePage');
    if (!root) return;
    const t = window.QaimeI18n;
    const $ = id => document.getElementById(id);
    const warehouse = $('qaimeWarehouse'), year = $('qaimeYear'), filter = $('qaimeFilterForm');
    const header = $('qaimeHeaderForm'), material = $('qaimeMaterialForm');
    const state = {epoch: 0, listRequest: 0, detailRequest: 0, catalogRequest: 0, page: 1, total: 0, rows: [], selected: null, details: [], options: {}, draft: [], edit: null, materialMode: null, materialItem: null, draftIndex: -1, filters: {}, editorScope: null, dirty: false, busy: false};
    const editor = {
        show() { root.classList.add('qaime-editing');$('qaimeEditor').classList.remove('d-none');$('qaimeBack').focus(); },
        hide() { root.classList.remove('qaime-editing');$('qaimeEditor').classList.add('d-none');$('qaimeNew').focus(); }
    };
    function backToList() {
        if(state.busy || (state.dirty && !window.confirm(t.confirmDiscard)))return;
        state.catalogRequest++;state.dirty=false;editor.hide();
    }
    const materialHome = material.parentElement;
    const materialEditor = bootstrap.Modal.getOrCreateInstance($('qaimeMaterialEditor'));
    const filterPanel = bootstrap.Offcanvas.getOrCreateInstance($('qaimeFilter'));
    const number = value => value == null ? '—' : new Intl.NumberFormat(document.documentElement.lang || 'az', {maximumFractionDigits: 4}).format(Number(value));
    const money = value => value == null ? '—' : new Intl.NumberFormat(document.documentElement.lang || 'az', {minimumFractionDigits: 2, maximumFractionDigits: 2}).format(Number(value));
    const date = value => value ? String(value).slice(0,10).split('-').reverse().join('.') : '—';
    const today = () => { const d = new Date(); return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`; };
    const field = (form,name) => form.elements.namedItem(name);
    function formData(form) { return Object.fromEntries([...new FormData(form)].map(([k,v]) => [k, String(v).trim() || null])); }
    function message(id, value, success = false) {
        const box = $(id); box.textContent = value || ''; box.classList.toggle('d-none', !value);
        box.classList.toggle('alert-success', success); box.classList.toggle('alert-danger', !success);
    }
    function choices(select, rows, label = t.choose, idKey = 'id', nameKey = 'ad') {
        select.replaceChildren(new Option(label, ''));
        rows.forEach(row => select.add(new Option(String(row[nameKey] ?? ''), String(row[idKey]))));
    }
    function scope() { return {anbarId: warehouse.value, il: year.value}; }
    function validScope() { return Boolean(warehouse.value && year.value); }
    async function api(path, params = {}, body, fixedScope) {
        const url = new URL(root.dataset.base + path, window.location.origin);
        Object.entries({...(fixedScope || scope()), ...params}).forEach(([k,v]) => { if (v != null && v !== '') url.searchParams.set(k,v); });
        const options = {headers: {Accept: 'application/json'}, credentials: 'same-origin'};
        if (body !== undefined) {
            const csrf = $('qaimeCsrf'); options.method = 'POST'; options.headers['Content-Type'] = 'application/json';
            options.headers[csrf.dataset.header] = csrf.value; options.body = JSON.stringify(body);
        }
        const response = await fetch(url, options);
        if (!response.headers.get('content-type')?.includes('application/json')) throw new Error(t.loadFailed);
        const data = await response.json();
        if (!response.ok) throw new Error(data.message || t.saveFailed);
        return data;
    }
    function empty(body, columns, label) {
        body.replaceChildren(); const row = body.insertRow(), cell = row.insertCell();
        cell.colSpan = columns; cell.className = 'text-center text-muted py-5'; cell.textContent = label;
    }
    function cell(row,value,cls) { const td = row.insertCell(); td.textContent = value == null || value === '' ? '—' : String(value); if(cls)td.className=cls; return td; }
    function action(parent, icon, title, callback, disabled = false) {
        const button = document.createElement('button'); button.type = 'button'; button.className = 'btn btn-light text-primary qaime-action me-1';
        button.title = title; button.setAttribute('aria-label',title); button.disabled = disabled;
        const i = document.createElement('i'); i.className = 'ti ti-' + icon; i.setAttribute('aria-hidden','true'); button.append(i);
        button.addEventListener('click', callback); parent.append(button); return button;
    }
    function clearDetails() {
        state.selected = null; state.details = []; state.detailRequest++;
        $('qaimeSelected').textContent = ''; $('qaimeAddMaterial').disabled = true;
        $('qaimeDetailSummary').replaceChildren(); $('qaimeDetailSummary').classList.add('d-none');
        empty($('qaimeMaterialRows'),20,t.chooseDetail); $('qaimePurchaseTotal').textContent = '—'; $('qaimeSaleTotal').textContent = '—';
    }
    function clearList() { state.rows=[]; state.total=0; state.page=1; $('qaimeCount').textContent='0'; empty($('qaimeRows'),13,t.noContext); clearDetails(); pagination(); }
    function pagination() {
        const size=Number($('qaimePageSize').value), pages=Math.max(1,Math.ceil(state.total/size));
        $('qaimePageLabel').textContent = `${state.total} ${t.results} · ${t.page} ${state.page} / ${pages}`;
        $('qaimePrevious').disabled = state.page<=1; $('qaimeNext').disabled = state.page>=pages;
    }
    function renderList() {
        const body=$('qaimeRows'); body.replaceChildren();
        if(!state.rows.length)empty(body,13,t.empty);
        state.rows.forEach((item,index)=>{
            const row=body.insertRow(); row.dataset.id=String(item.qaime_id);
            row.classList.toggle('qaime-selected',state.selected?.qaime_id===item.qaime_id);
            cell(row,(state.page-1)*Number($('qaimePageSize').value)+index+1);cell(row,item.sira_no ?? item.qaime_id);
            cell(row,date(item.qaime_tarixi));cell(row,item.qaime_saati?.slice(0,5));cell(row,item.emeliyyat_novu_adi);
            const badge=document.createElement('span');badge.className=item.istiqamet==='G'?'badge bg-success-subtle text-success':'badge bg-danger-subtle text-danger';
            badge.textContent=item.istiqamet==='G'?t.incoming:t.outgoing;row.insertCell().append(badge);
            ['geldiyi_yer','sened_novu','sened_no','teslim_alan','teslim_eden','aciqlama'].forEach(key=>{const td=cell(row,item[key],key==='aciqlama'?'qaime-note':'');if(item[key])td.title=String(item[key]);});
            const operations=row.insertCell();action(operations,'eye',t.view,()=>loadDetails(item.qaime_id,true));action(operations,'edit',t.edit,()=>openInvoice(item));
        });
        $('qaimeCount').textContent=String(state.total);pagination();
    }
    async function loadList() {
        if(!validScope())return;
        const epoch=state.epoch,request=++state.listRequest;
        $('qaimeLoading').textContent=t.loading;message('qaimeNotice','');
        try {
            const data=await api('/siyahi',{...state.filters,page:state.page,size:$('qaimePageSize').value});
            if(epoch!==state.epoch||request!==state.listRequest)return;
            state.rows=data.rows;state.total=data.total;renderList();
        }catch(e){if(epoch===state.epoch&&request===state.listRequest){message('qaimeNotice',e.message);state.rows=[];state.total=0;renderList();}}
        finally{if(epoch===state.epoch&&request===state.listRequest)$('qaimeLoading').textContent='';}
    }
    async function loadDetails(id,scroll=false) {
        const epoch=state.epoch,request=++state.detailRequest;
        $('qaimeAddMaterial').disabled=true;empty($('qaimeMaterialRows'),20,t.loading);
        try {
            const data=await api(`/${id}/detal`);
            if(epoch!==state.epoch||request!==state.detailRequest)return;
            state.selected=data.invoice;state.details=data.rows;
            $('qaimeSelected').textContent=`#${data.invoice.sira_no ?? id}`;
            const summary=$('qaimeDetailSummary');summary.replaceChildren();summary.classList.remove('d-none');
            [[t.date,date(data.invoice.qaime_tarixi)],[t.operation,data.invoice.emeliyyat_novu_adi],[t.company,data.invoice.geldiyi_yer],[t.document,data.invoice.sened_no]].forEach(([label,value])=>{const span=document.createElement('span');span.textContent=`${label}: ${value || '—'}`;summary.append(span);});
            const body=$('qaimeMaterialRows');body.replaceChildren();if(!data.rows.length)empty(body,20,t.noMaterials);
            data.rows.forEach((item,index)=>{
                const row=body.insertRow();cell(row,item.sira_no ?? index+1);cell(row,item.material_adi);cell(row,item.material_barkod_no);cell(row,item.vahid_adi);
                ['qutu_sayi','qutu_ici_miqdar','miqdar','alis_qiymeti','edv_faizi','edvsiz_mebleg','edv_meblegi','alis_meblegi','satis_baza_qiymeti','satis_faizi','satis_qiymeti','satis_meblegi'].forEach(key=>cell(row,number(item[key])));
                cell(row,date(item.son_istifade_tarixi));cell(row,item.seriya_no);cell(row,item.aciqlama,'qaime-note');
                const operations=row.insertCell();action(operations,'edit',item.deyisdirile_biler?t.edit:t.locked,()=>openMaterial('edit',item),!item.deyisdirile_biler);
                action(operations,'trash',item.siline_biler?t.delete:t.locked,()=>removeMaterial(item),!item.siline_biler);
            });
            $('qaimePurchaseTotal').textContent=money(data.purchaseTotal);$('qaimeSaleTotal').textContent=money(data.saleTotal);$('qaimeAddMaterial').disabled=false;renderList();
            if(scroll)$('qaimeDetails').querySelector('.qaime-table-scroll').scrollTop=0;
        }catch(e){if(epoch===state.epoch&&request===state.detailRequest){clearDetails();message('qaimeNotice',e.message);}}
    }
    async function changeWarehouse() {
        const epoch=++state.epoch;clearList();year.disabled=true;choices(year,[],t.chooseYear);$('qaimeSearch').disabled=true;$('qaimeNew').disabled=true;message('qaimeNotice','');
        state.options={};state.filters={};resetForm(filter);
        if(!warehouse.value)return;
        try {
            const [years,options]=await Promise.all([api('/iller'),api('/secimler')]);
            if(epoch!==state.epoch)return;
            state.options=options;state.years=years;choices(year,years.map(y=>({id:y.il,ad:String(y.il)+(y.cariIldir?` (${t.currentYear})`:'')})),t.chooseYear);
            year.value=String(years.find(y=>y.cariIldir)?.il ?? years[0]?.il ?? '');year.disabled=!years.length;
            choices(field(filter,'operation'),options.operations,t.all);$('qaimeSearch').disabled=!validScope();$('qaimeNew').disabled=!validScope();
            if(!years.length)message('qaimeNotice',t.noYears);else await loadList();
        }catch(e){if(epoch===state.epoch)message('qaimeNotice',e.message);}
    }
    function resetForm(form) { form.reset(); form.querySelectorAll('input').forEach(el=>el._flatpickr?.clear()); }
    function fill(form,data) { [...form.elements].forEach(el=>{if(el.name){const value=data[el.name] ?? '';if(el._flatpickr)el._flatpickr.setDate(value,false);else el.value=value;}}); }
    function populateHeader() {
        choices(field(header,'emeliyyat_novu_id'),state.options.operations || []);
        choices(field(header,'firma_id'),state.options.companies || []);
        choices(field(header,'teslim_alan_personal_id'),state.options.receivers || []);
    }
    function renderDraft() {
        const body=$('qaimeDraftRows');body.replaceChildren();if(!state.draft.length)empty(body,12,t.noMaterials);
        let quantity=0,total=0;
        state.draft.forEach((item,index)=>{
            const row=body.insertRow(), amount=Number(item.qutu_sayi)*Number(item.qutu_ici_miqdar);
            quantity+=amount;total+=amount*Number(item.alis_qiymeti);
            cell(row,item.material_adi);cell(row,item.qrup_adi);cell(row,item.vahid_adi);
            [item.qutu_sayi,item.qutu_ici_miqdar,amount,item.alis_qiymeti,item.satis_baza_qiymeti,item.satis_faizi].forEach(v=>cell(row,number(v)));
            cell(row,date(item.son_istifade_tarixi));cell(row,item.seriya_no);
            const ops=row.insertCell();action(ops,'edit',t.edit,()=>openMaterial('draft',item,index));
            action(ops,'trash',t.delete,()=>{if(state.busy)return;state.draft.splice(index,1);state.dirty=true;renderDraft();if(state.draftIndex>=index)state.draftIndex--;});
        });
        $('qaimeDraftQuantity').textContent=number(quantity);$('qaimeDraftTotal').textContent=money(total);
    }
    function openInvoice(item=null) {
        if(!validScope()||state.busy)return;
        state.edit=item;state.draft=[];state.editorScope=scope();state.dirty=false;resetForm(header);populateHeader();
        $('qaimeEditorTitle').textContent=item?t.editInvoice:t.new;message('qaimeEditorError','');
        const start=state.years?.find(y=>String(y.il)===year.value)?.baslamaTarixi;
        const min=start&&start.startsWith(year.value)?start:`${year.value}-01-01`;
        const dateInput=field(header,'qaime_tarixi');dateInput.min=min;dateInput.max=`${year.value}-12-31`;
        if(dateInput._flatpickr){dateInput._flatpickr.set('minDate',min);dateInput._flatpickr.set('maxDate',dateInput.max);}
        fill(header,item || {qaime_tarixi:today().startsWith(year.value)?today():min,qaime_saati:new Date().toTimeString().slice(0,5),sened_tarixi:today()});
        ['emeliyyat_novu_id','qaime_tarixi','qaime_saati'].forEach(k=>{const el=field(header,k);el.disabled=Boolean(item);if(el._flatpickr?.altInput)el._flatpickr.altInput.disabled=Boolean(item);});
        $('qaimeDraftPanel').classList.toggle('d-none',Boolean(item));renderDraft();editor.show();if(!item)openMaterial("draft");
    }
    async function loadCatalog(selected='') {
        const request=++state.catalogRequest,group=field(material,'mehsul_qrupu_id').value;
        state.catalog=[];$('qaimeCatalogSearch').value='';
        const select=field(material,'material_id');select.disabled=true;choices(select,[],t.loading);
        if(!group){choices(select,[]);return;}
        try {
            const rows=await api('/materiallar',{qrupId:group},undefined,state.editorScope || scope());
            if(request!==state.catalogRequest)return;
            state.catalog=rows;choices(select,rows,t.choose,'material_id','material_adi');select.disabled=false;select.value=String(selected);
        }catch(e){if(request===state.catalogRequest){choices(select,[]);message('qaimeMaterialError',e.message);}}
    }
    function updateCalculation() {
        const value=name=>Number(field(material,name).value)||0;
        const quantity=value('qutu_sayi')*value('qutu_ici_miqdar');
        $('qaimeCalcQuantity').textContent=number(quantity);
        $('qaimeCalcPurchase').textContent=money(quantity*value('alis_qiymeti'));
        $('qaimeCalcSale').textContent=number(value('satis_baza_qiymeti')*(1+value('satis_faizi')/100));
    }
    async function openMaterial(mode,item=null,index=-1) {
        if(state.busy)return;
        if(mode==='draft')$('qaimeInlineMaterial').append(material);else materialHome.append(material);
        state.materialMode=mode;state.materialItem=item;state.draftIndex=index;state.catalogRequest++;
        $('qaimeMaterialSave').textContent=mode==='draft'?(item?t.save:t.stage):t.save;
        resetForm(material);$('qaimeCatalogSearch').disabled=mode==='edit';message('qaimeMaterialError','');$('qaimeMaterialTitle').textContent=item?t.editMaterial:t.addMaterial;
        choices(field(material,'mehsul_qrupu_id'),state.options.groups || []);choices(field(material,'material_id'),[]);
        fill(material,item || {qutu_sayi:'1',qutu_ici_miqdar:'1',alis_qiymeti:'0',satis_baza_qiymeti:'0',satis_faizi:'0'});
        $('qaimeMaterialUnit').value=item?.vahid_adi || '';
        ['mehsul_qrupu_id','material_id'].forEach(k=>field(material,k).disabled=mode==='edit');
        if(mode==='edit') {
            choices(field(material,'material_id'),[{id:item.material_id,ad:item.material_adi}]);field(material,'material_id').value=String(item.material_id);
        }
        updateCalculation();if(mode!=='draft')materialEditor.show();if(mode!=='edit'&&item)await loadCatalog(item.material_id);
    }
    async function saveMaterial(event) {
        event.preventDefault();if(state.busy||!material.reportValidity())return;
        const data=formData(material),mode=state.materialMode;
        if(Number(data.qutu_sayi)<=0||Number(data.qutu_ici_miqdar)<=0){message('qaimeMaterialError',t.invalidInput);return;}
        state.busy=true;$('qaimeMaterialSave').disabled=true;
        try {
            if(mode==='draft') {
                const dateValue=field(header,'qaime_tarixi').value;
                await api('/hazirla',{}, {...data,qaime_tarixi:dateValue},state.editorScope);
                data.material_adi=field(material,'material_id').selectedOptions[0]?.textContent || '';
                data.qrup_adi=field(material,'mehsul_qrupu_id').selectedOptions[0]?.textContent || '';
                data.vahid_adi=$('qaimeMaterialUnit').value;
                if(state.draftIndex<0)state.draft.push(data);else state.draft[state.draftIndex]=data;
                state.dirty=true;renderDraft();
            }else {
                const id=state.selected.qaime_id;
                const path=mode==='edit'?`/${id}/material/${state.materialItem.qaime_material_id}/yenile`:`/${id}/material/elave`;
                await api(path,{},data);await loadDetails(id);message('qaimeNotice',t.saved,true);
            }
            state.busy=false;if(mode==='draft')openMaterial('draft');else materialEditor.hide();
        }catch(e){message('qaimeMaterialError',e.message);}finally{state.busy=false;$('qaimeMaterialSave').disabled=false;}
    }
    async function saveInvoice(event) {
        event.preventDefault();if(state.busy||!header.reportValidity())return;
        if(!state.edit&&!state.draft.length){message('qaimeEditorError',t.materialsRequired);return;}
        state.busy=true;$('qaimeSave').disabled=true;
        try {
            const data=formData(header);if(!state.edit)data.materiallar=state.draft;
            const result=await api(state.edit?`/${state.edit.qaime_id}/yenile`:'/yeni',{},data,state.editorScope);
            state.dirty=false;state.busy=false;editor.hide();await loadList();await loadDetails(result.qaime_id);message('qaimeNotice',t.saved,true);
        }catch(e){message('qaimeEditorError',e.message);}finally{state.busy=false;$('qaimeSave').disabled=false;}
    }
    async function removeMaterial(item) {
        if(state.busy||!window.confirm(t.confirmDelete))return;
        state.busy=true;
        try{await api(`/${state.selected.qaime_id}/material/${item.qaime_material_id}/sil`,{},{});await loadDetails(state.selected.qaime_id);message('qaimeNotice',t.saved,true);}
        catch(e){message('qaimeNotice',e.message);}finally{state.busy=false;}
    }
    warehouse.addEventListener('change',changeWarehouse);
    year.addEventListener('change',()=>{state.epoch++;clearList();$('qaimeSearch').disabled=!validScope();$('qaimeNew').disabled=!validScope();});
    $('qaimeSearch').addEventListener('click',()=>{state.page=1;state.filters=formData(filter);clearDetails();loadList();});
    $('qaimeNew').addEventListener('click',()=>openInvoice());
    filter.addEventListener('submit',event=>{event.preventDefault();if(!filter.reportValidity())return;state.filters=formData(filter);state.page=1;clearDetails();filterPanel.hide();loadList();});
    $('qaimeFilterClear').addEventListener('click',()=>{resetForm(filter);state.filters={};state.page=1;filterPanel.hide();clearDetails();loadList();});
    $('qaimePrevious').addEventListener('click',()=>{state.page--;loadList();});$('qaimeNext').addEventListener('click',()=>{state.page++;loadList();});
    $('qaimePageSize').addEventListener('change',()=>{state.page=1;loadList();});
    $('qaimeAddMaterial').addEventListener('click',()=>{state.editorScope=scope();openMaterial('add');});
    $('qaimeCatalogSearch').addEventListener('input',()=>{
        const query=$('qaimeCatalogSearch').value.trim().toLocaleLowerCase();
        const select=field(material,'material_id'),current=select.value;
        choices(select,(state.catalog||[]).filter(row=>[row.material_adi,row.barkod_nomresi,row.material_id].some(v=>String(v??'').toLocaleLowerCase().includes(query))),t.choose,'material_id','material_adi');
        select.value=current;
    });
    field(material,'mehsul_qrupu_id').addEventListener('change',()=>loadCatalog());
    field(material,'material_id').addEventListener('change',()=>{const chosen=state.catalog?.find(m=>String(m.material_id)===field(material,'material_id').value);if(chosen){field(material,'qutu_ici_miqdar').value=chosen.stok_boleni || '1';$('qaimeMaterialUnit').value=chosen.vahid_adi || '';}else $('qaimeMaterialUnit').value='';updateCalculation();});
    material.addEventListener('input',()=>{updateCalculation();if(state.materialMode==='draft')state.dirty=true;});
    material.addEventListener('submit',saveMaterial);header.addEventListener('submit',saveInvoice);header.addEventListener('input',()=>state.dirty=true);
    $('qaimeBack').addEventListener('click',backToList);
    $('qaimeCancel').addEventListener('click',backToList);
    $('qaimeMaterialReset').addEventListener('click',()=>{if(!state.busy)openMaterial(state.materialMode,state.materialMode==='edit'?state.materialItem:null);});
    $('qaimeMaterialEditor').addEventListener('hide.bs.modal',event=>{if(state.busy)event.preventDefault();});
    clearList();if(warehouse.options.length===1)message('qaimeNotice',t.noWarehouses);
    else {warehouse.selectedIndex=1;changeWarehouse();}
})();
