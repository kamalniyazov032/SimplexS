(() => {
    'use strict';
    const root = document.getElementById('qaimePage');
    if (!root) return;
    const t = window.QaimeI18n;
    const $ = id => document.getElementById(id);
    const warehouse = $('qaimeWarehouse'), year = $('qaimeYear'), filter = $('qaimeFilterForm');
    const header = $('qaimeHeaderForm'), material = $('qaimeMaterialForm');
    const state = {epoch: 0, listRequest: 0, detailRequest: 0, catalogRequest: 0, unitRequest: 0, page: 1, total: 0, rows: [], selected: null, details: [], options: {}, draft: [], edit: null, materialMode: null, materialItem: null, draftIndex: -1, filters: {}, editorScope: null, dirty: false, busy: false};
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
    const materialPicker = bootstrap.Modal.getOrCreateInstance($('qaimeMaterialPicker'));
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
        if (!response.ok || (data.status_kodu && data.status_kodu !== 'UGURLU') || data.ugurlu === false) throw new Error(data.mesaj || data.message || t.saveFailed);
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
                const row=body.insertRow();cell(row,item.sira_no ?? index+1);cell(row,item.material_adi);cell(row,item.material_barkod_no);cell(row,item.secilen_vahid_adi);
                ['secilen_vahid_miqdari','ana_vahide_emsal','miqdar','alis_qiymeti','edv_faizi','edvsiz_mebleg','edv_meblegi','alis_meblegi','satis_baza_qiymeti','satis_faizi','satis_qiymeti','satis_meblegi'].forEach(key=>cell(row,number(item[key])));
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
        state.options={};state.years=[];state.filters={};state.editorScope=null;resetForm(filter);
        state.catalogRequest++;state.unitRequest++;state.catalogSelection=null;state.units=[];
        populateHeader();choices(field(filter,'operation'),[],t.all);choices(field(material,'mehsul_qrupu_id'),[]);choices(field(material,'vahid_id'),[]);
        field(material,'material_id').value='';$('qaimeCatalogSearch').value='';$('qaimeLoading').textContent='';
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
        choices(field(header,'emeliyyat_novu_id'), state.options.operations || []);
        choices(field(header,'firma_id'),state.options.companies || []);
        choices(field(header,'teslim_alan_personal_id'),state.options.receivers || []);
    }
    function renderDraft() {
        const body=$('qaimeDraftRows');body.replaceChildren();if(!state.draft.length)empty(body,15,t.noMaterials);
        const totals={edvsiz_mebleg:0,edv_meblegi:0,alis_meblegi:0,satis_meblegi:0};
        state.draft.forEach((item,index)=>{
            const row=body.insertRow();
            cell(row,item.material_adi);cell(row,item.material_barkod_no);cell(row,item.secilen_vahid_adi);
            ['miqdar','ana_miqdar','alis_qiymeti','edv_faizi','edvsiz_mebleg','edv_meblegi','alis_meblegi','satis_qiymeti','satis_meblegi'].forEach(key=>cell(row,number(item[key])));
            cell(row,date(item.son_istifade_tarixi));cell(row,item.seriya_no);
            Object.keys(totals).forEach(key=>totals[key]+=Number(item[key])||0);
            const ops=row.insertCell();action(ops,'edit',t.edit,()=>openMaterial('draft',item,index));
            action(ops,'trash',t.delete,()=>{if(state.busy)return;state.draft.splice(index,1);state.dirty=true;renderDraft();if(state.draftIndex===index)openMaterial('draft');else if(state.draftIndex>index)state.draftIndex--;});
        });
        $('qaimeDraftCount').textContent=String(state.draft.length);
        $('qaimeDraftCountLabel').textContent=t.totalMaterials.replace('{0}',state.draft.length);
        $('qaimeDraftNet').textContent=money(totals.edvsiz_mebleg);$('qaimeDraftVat').textContent=money(totals.edv_meblegi);
        $('qaimeDraftTotal').textContent=money(totals.alis_meblegi);$('qaimeDraftSale').textContent=money(totals.satis_meblegi);
        $('qaimeClearDraft').disabled=!state.draft.length;
    }
    async function openInvoice(item=null) {
        if(!validScope()||state.busy)return;
        const currentScope=scope(),epoch=state.epoch;
        state.busy=true;$('qaimeNew').disabled=true;message('qaimeNotice','');
        try {
            const options=await api('/secimler',{},undefined,currentScope);
            if(epoch!==state.epoch || currentScope.anbarId!==warehouse.value || currentScope.il!==year.value)return;
            state.options=options;
        }catch(e){if(epoch===state.epoch)message('qaimeNotice',e.message);return;}
        finally{state.busy=false;if(epoch===state.epoch)$('qaimeNew').disabled=!validScope();}
        state.edit=item;state.draft=[];state.editorScope=currentScope;state.dirty=false;resetForm(header);populateHeader();
        $('qaimeEditorTitle').textContent=item?t.editInvoice:t.new;message('qaimeEditorError','');
        const start=state.years?.find(y=>String(y.il)===year.value)?.baslamaTarixi;
        const min=start&&start.startsWith(year.value)?start:`${year.value}-01-01`;
        const dateInput=field(header,'qaime_tarixi');dateInput.min=min;dateInput.max=`${year.value}-12-31`;
        if(dateInput._flatpickr){dateInput._flatpickr.set('minDate',min);dateInput._flatpickr.set('maxDate',dateInput.max);}
        fill(header,item || {qaime_tarixi:today().startsWith(year.value)?today():min,qaime_saati:new Date().toTimeString().slice(0,5),sened_tarixi:today()});
        ['emeliyyat_novu_id','qaime_tarixi','qaime_saati'].forEach(k=>{const el=field(header,k);el.disabled=Boolean(item);if(el._flatpickr?.altInput)el._flatpickr.altInput.disabled=Boolean(item);});
        ['qaimeDraftPanel','qaimeEntryPanel','qaimeDraftSummary','qaimeDraftTotals'].forEach(id=>$(id).classList.toggle('d-none',Boolean(item)));$('qaimeSaveLabel').textContent=item?t.save:t.create;renderDraft();editor.show();if(!item)openMaterial("draft");
    }
    async function loadCatalog() {
        const request=++state.catalogRequest,group=field(material,'mehsul_qrupu_id').value;
        const query=$('qaimeMaterialSearchText').value.trim().toLocaleLowerCase();
        const body=$('qaimeMaterialSearchRows');empty(body,5,t.loading);message('qaimeMaterialSearchError','');
        try {
            const rows=await api('/materiallar',{qrupId:group},undefined,state.editorScope || scope());
            if(request!==state.catalogRequest || group!==field(material,'mehsul_qrupu_id').value)return;
            body.replaceChildren();
            rows.filter(row=>[row.material_adi,row.barkod_nomresi,row.material_id].some(v=>String(v??'').toLocaleLowerCase().includes(query))).forEach(item=>{
                const row=body.insertRow();cell(row,item.material_id);cell(row,item.material_adi);cell(row,item.barkod_nomresi);cell(row,item.ana_vahid_adi);
                const button=document.createElement('button');button.type='button';button.className='btn btn-sm btn-primary';button.textContent=t.selectMaterial;
                button.addEventListener('click',()=>{
                    if(state.busy)return;
                    state.catalogSelection=item;field(material,'material_id').value=String(item.material_id);$('qaimeCatalogSearch').value=item.material_adi;
                    if(state.materialMode==='draft')state.dirty=true;
                    message('qaimeMaterialError','');materialPicker.hide();loadUnits();
                });row.insertCell().append(button);
            });
            if(!body.rows.length)empty(body,5,t.noMaterials);
        }catch(e){if(request===state.catalogRequest){empty(body,5,t.noMaterials);message('qaimeMaterialSearchError',e.message);}}
    }
    async function loadUnits(selected='') {
        const request=++state.unitRequest,materialId=field(material,'material_id').value;
        state.units=[];choices(field(material,'vahid_id'),[]);field(material,'vahid_id').disabled=true;updateCalculation();
        if(!materialId)return;
        try {
            const rows=await api('/vahidler',{qrupId:field(material,'mehsul_qrupu_id').value,qaimeId:state.materialMode==='edit'?state.selected.qaime_id:null,materialId},undefined,state.editorScope || scope());
            if(request!==state.unitRequest || materialId!==field(material,'material_id').value)return;
            state.units=rows;choices(field(material,'vahid_id'),rows,t.choose,'vahid_id','vahid_adi');
            field(material,'vahid_id').disabled=false;
            field(material,'vahid_id').value=String(selected || rows.find(u=>u.ana_vahiddir)?.vahid_id || rows[0]?.vahid_id || '');
            updateCalculation();
        }catch(e){if(request===state.unitRequest)message('qaimeMaterialError',e.message);}
    }
    function updateCalculation() {
        const value=name=>Number(field(material,name).value)||0;
        const unit=state.units?.find(u=>String(u.vahid_id)===field(material,'vahid_id').value);
        const vat=field(material,'edv_faizi').value===''?null:value('edv_faizi');
        const round=(v,d=4)=>Math.round((v+Number.EPSILON)*10**d)/10**d;
        const quantity=unit?round(value('miqdar')*Number(unit.ana_vahide_emsal),3):null;
        const net=quantity==null?null:round(value('miqdar')*value('alis_qiymeti'));
        const tax=vat==null || net==null?null:round(net*vat/100);
        const saleInput=field(material,'satis_baza_qiymeti'),markup=value('satis_faizi');
        saleInput.required=true;saleInput.readOnly=markup>0;
        if(markup>0)saleInput.value=vat==null || field(material,'alis_qiymeti').value===''?'':round(value('alis_qiymeti')*(1+vat/100)*(1+markup/100));
        const sale=saleInput.value===''?null:round(value('satis_baza_qiymeti'));
        const result={ana_miqdar:quantity,edv_faizi:vat,edvsiz_mebleg:net,edv_meblegi:tax,alis_meblegi:tax==null?null:round(net+tax),satis_qiymeti:sale,satis_meblegi:sale==null||quantity==null?null:round(value('miqdar')*sale)};
        $('qaimeCalcUnit').textContent=state.catalogSelection?.ana_vahid_adi || '';
        $('qaimeCalcQuantity').value=number(quantity);$('qaimeCalcNet').textContent=money(net);$('qaimeCalcVat').textContent=money(tax);
        $('qaimeCalcPurchase').textContent=money(result.alis_meblegi);
        $('qaimeCalcSaleTotal').textContent=money(result.satis_meblegi);
        return result;
    }
    async function openMaterial(mode,item=null,index=-1) {
        if(state.busy)return;
        state.editorScope=scope();
        if(mode==='draft')$('qaimeInlineMaterial').append(material);else materialHome.append(material);
        state.materialMode=mode;state.materialItem=item;state.draftIndex=index;state.catalogRequest++;state.unitRequest++;state.units=[];state.catalogSelection=item?{material_id:item.material_id,material_adi:item.material_adi,barkod_nomresi:item.material_barkod_no,ana_vahid_id:item.ana_vahid_id,ana_vahid_adi:item.ana_vahid_adi}:null;
        $('qaimeMaterialSave').textContent=mode==='draft'?(item?t.save:t.stage):t.save;
        resetForm(material);$('qaimeCatalogSearch').disabled=mode==='edit';$('qaimeCatalogOpen').disabled=mode==='edit';$('qaimeCatalogSearch').value=item?.material_adi || '';message('qaimeMaterialError','');$('qaimeMaterialTitle').textContent=item?t.editMaterial:t.addMaterial;
        choices(field(material,'mehsul_qrupu_id'),state.options.groups || []);
        choices(field(material,'vahid_id'),[]);field(material,'vahid_id').disabled=false;
        fill(material,item ? {...item,satis_faizi:item.satis_faizi ?? 0,miqdar:mode==='edit'?item.secilen_vahid_miqdari:item.miqdar} : {miqdar:'1',alis_qiymeti:'0',satis_baza_qiymeti:'0',satis_faizi:'0'});
        ['mehsul_qrupu_id','material_id'].forEach(k=>field(material,k).disabled=mode==='edit');
        if(mode==='edit') {
            field(material,'material_id').value=String(item.material_id);
            state.units=[{vahid_id:item.secilen_vahid_id,vahid_adi:item.secilen_vahid_adi,ana_vahide_emsal:item.ana_vahide_emsal}];choices(field(material,'vahid_id'),state.units,t.choose,'vahid_id','vahid_adi');field(material,'vahid_id').value=String(item.secilen_vahid_id);
        }
        updateCalculation();if(mode!=='draft')materialEditor.show();if(item)await loadUnits(mode==='edit'?item.secilen_vahid_id:item.vahid_id);
    }
    async function saveMaterial(event) {
        event.preventDefault();if(state.busy||!material.reportValidity())return;
        const data=formData(material),mode=state.materialMode;
        if(mode!=='edit'&&(!data.material_id || String(state.catalogSelection?.material_id)!==data.material_id)){message('qaimeMaterialError',t.selectMaterialRequired);return;}
        data.aciqlama=state.materialItem?.aciqlama || null;
        if(!data.vahid_id||Number(data.miqdar)<=0||data.satis_baza_qiymeti==null){message('qaimeMaterialError',t.invalidInput);return;}
        data.satis_faizi=Number(data.satis_faizi || 0);
        ['material_id','vahid_id','miqdar','alis_qiymeti','edv_faizi','satis_baza_qiymeti'].forEach(key=>{if(data[key]!=null)data[key]=Number(data[key]);});
        state.busy=true;$('qaimeMaterialSave').disabled=true;
        try {
            if(mode==='draft') {
                data.material_adi=state.catalogSelection.material_adi;
                data.material_barkod_no=state.catalogSelection.barkod_nomresi;
                data.ana_vahid_id=state.catalogSelection.ana_vahid_id;data.ana_vahid_adi=state.catalogSelection.ana_vahid_adi;
                data.secilen_vahid_adi=field(material,'vahid_id').selectedOptions[0]?.textContent || '';
                Object.assign(data,updateCalculation());
                if(state.draftIndex<0)state.draft.push(data);else state.draft[state.draftIndex]=data;
                state.dirty=true;renderDraft();
            }else {
                const id=state.selected.qaime_id;
                const path=mode==='edit'?`/${id}/material/${state.materialItem.qaime_material_id}/yenile`:`/${id}/material/elave`;
                const result=await api(path,{},data);await loadDetails(id);message('qaimeNotice',result.mesaj || t.saved,true);
            }
            state.busy=false;
            if(mode==='draft'){
                await openMaterial('draft');
                field(material,'mehsul_qrupu_id').value=data.mehsul_qrupu_id;
            }else materialEditor.hide();
        }catch(e){message('qaimeMaterialError',e.message);}finally{state.busy=false;$('qaimeMaterialSave').disabled=false;}
    }
    async function saveInvoice(event) {
        event.preventDefault();if(state.busy||!header.reportValidity())return;
        if(!state.edit&&!state.draft.length){message('qaimeEditorError',t.materialsRequired);return;}
        state.busy=true;$('qaimeSave').disabled=true;
        try {
            const data=formData(header);if(!state.edit)data.materiallar=state.draft.map(item=>Object.fromEntries(['material_id','vahid_id','miqdar','alis_qiymeti','edv_faizi','satis_baza_qiymeti','satis_faizi','son_istifade_tarixi','seriya_no','aciqlama'].map(key=>[key,key==='satis_faizi'?Number(item[key] || 0):item[key] ?? null])));
            const result=await api(state.edit?`/${state.edit.qaime_id}/yenile`:'/yeni',{},data,state.editorScope);
            state.dirty=false;state.busy=false;editor.hide();await loadList();await loadDetails(result.qaime_id);message('qaimeNotice',state.edit?(result.mesaj || t.saved):t.createdSummary.replace('{0}',result.mesaj || t.saved).replace('{1}',result.elave_olunan_material_sayi).replace('{2}',money(result.alis_yekun_mebleg)).replace('{3}',money(result.satis_yekun_mebleg)),true);
        }catch(e){message('qaimeEditorError',e.message);}finally{state.busy=false;$('qaimeSave').disabled=false;}
    }
    async function removeMaterial(item) {
        if(state.busy||!window.confirm(t.confirmDelete))return;
        state.busy=true;
        try{const result=await api(`/${state.selected.qaime_id}/material/${item.qaime_material_id}/sil`,{},{});await loadDetails(state.selected.qaime_id);message('qaimeNotice',result.mesaj || t.saved,true);}
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
    $('qaimeCatalogOpen').addEventListener('click',()=>{
        if(state.busy || state.materialMode==='edit' || !field(material,'mehsul_qrupu_id').reportValidity())return;
        $('qaimeMaterialSearchText').value=$('qaimeCatalogSearch').value;
        if($('qaimeMaterialEditor').classList.contains('show')){
            $('qaimeMaterialEditor').addEventListener('hidden.bs.modal',()=>materialPicker.show(),{once:true});materialEditor.hide();
        }else materialPicker.show();
        loadCatalog();
    });
    $('qaimeCatalogSearch').addEventListener('keydown',event=>{if(event.key==='Enter'){event.preventDefault();$('qaimeCatalogOpen').click();}});
    $('qaimeMaterialSearchForm').addEventListener('submit',event=>{event.preventDefault();loadCatalog();});
    $('qaimeMaterialPicker').addEventListener('shown.bs.modal',()=>$('qaimeMaterialSearchText').focus());
    $('qaimeMaterialPicker').addEventListener('hidden.bs.modal',()=>{state.catalogRequest++;if(state.materialMode==='add')materialEditor.show();else $('qaimeCatalogSearch').focus();});
    $('qaimeCatalogSearch').addEventListener('input',()=>{
        field(material,'material_id').value='';state.catalogSelection=null;state.units=[];state.unitRequest++;
        choices(field(material,'vahid_id'),[]);field(material,'vahid_id').disabled=true;updateCalculation();
    });
    field(material,'mehsul_qrupu_id').addEventListener('change',()=>{
        state.catalogRequest++;$('qaimeCatalogSearch').value='';$('qaimeCatalogSearch').dispatchEvent(new Event('input'));
    });
    field(material,'vahid_id').addEventListener('change',updateCalculation);
    material.addEventListener('input',()=>{updateCalculation();if(state.materialMode==='draft')state.dirty=true;});
    material.addEventListener('submit',saveMaterial);header.addEventListener('submit',saveInvoice);header.addEventListener('input',()=>state.dirty=true);
    $('qaimeClearDraft').addEventListener('click',()=>{if(state.busy || !window.confirm(t.confirmClear))return;state.draft=[];state.dirty=true;renderDraft();openMaterial('draft');});
    $('qaimeBack').addEventListener('click',backToList);
    $('qaimeCancel').addEventListener('click',backToList);
    $('qaimeMaterialReset').addEventListener('click',()=>{if(!state.busy)openMaterial(state.materialMode,state.materialMode==='edit'?state.materialItem:null);});
    $('qaimeMaterialEditor').addEventListener('hide.bs.modal',event=>{if(state.busy)event.preventDefault();});
    clearList();if(warehouse.options.length===1)message('qaimeNotice',t.noWarehouses);
    else {warehouse.selectedIndex=1;changeWarehouse();}
})();
