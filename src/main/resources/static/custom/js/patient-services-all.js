document.addEventListener('DOMContentLoaded', () => {
  const root = document.getElementById('allServices'); if (!root) return;
  const tr = document.getElementById('allI18n').dataset, base = `/xeste-xidmetleri/${root.dataset.gelisId}`;
  const body = document.getElementById('allBody'), error = document.getElementById('allError');
  let rows = [], visible = [], tab = 'services', grouped = false;
  const str = x => x == null ? '' : String(x), money = x => `${Number(x || 0).toFixed(2)} ${tr.currency}`;
  const doctor = (x, prefix) => ['ad','soyad','ata_adi'].map(k => x[`${prefix}_${k}`]).filter(Boolean).join(' ');
  const value = (x, key) => key === 'doctor' ? doctor(x,'icra_eden_hekim') : str(x[{status:'status_adi',type:'xidmet_tipi_adi',department:'sobe_adi'}[key]]);
  const fail = e => {error.textContent = e.message || tr.loadError; error.classList.remove('d-none');};
  const cells = x => [x.istek_id, x.xidmet_adi, str(x.xidmet_tarixi).slice(0,10), x.sobe_adi, x.miqdar, money(x.vahid_qiymet), money(x.yekun_mebleg), doctor(x,'gonderen_hekim'),doctor(x,'isteyen_hekim'),doctor(x,'icra_eden_hekim'),x.status_adi];
  const keys = ['request','serviceName','date','department','quantity','price','total','referringDoctor','requestingDoctor','performingDoctor','status'];
  async function load() {
    try {
      const response = await fetch(`${base}/siyahi`, {headers:{Accept:'application/json'}}); if (!response.ok) throw new Error(tr.loadError);
      rows = await response.json();
      ['status','type','department','doctor'].forEach(key => {
        const select = document.getElementById(`filter-${key}`), old = select.value; select.replaceChildren(new Option(tr.allFilter,''));
        [...new Set(rows.map(x=>value(x,key)).filter(Boolean))].sort().forEach(v=>select.add(new Option(v,v))); select.value=old;
      }); render();
    } catch(e) {fail(e);}
  }
  function details(row) {
    const dl = document.getElementById('serviceInfo'); dl.replaceChildren();
    [...keys.map((key,i)=>[tr[key],cells(row)[i]]),[tr.note,row.aciqlama],[tr.patientAmount,money(row.xeste_meblegi)],[tr.organizationAmount,money(row.qurum_meblegi)],[tr.locked,row.blok_sebebi || row.maliyye_blok_sebebi || '—']].forEach(([k,v])=>{const dt=document.createElement('dt'),dd=document.createElement('dd');dt.textContent=k;dd.textContent=str(v)||'—';dl.append(dt,dd);});
    document.getElementById('serviceDialog').showModal();
  }
  function render() {
    const query=document.getElementById('allSearch').value.toLocaleLowerCase();
    visible=rows.filter(x => (tab !== 'packages' || x.paket_daxildir || x.paket_menbe_xidmet_id || /PAKET/i.test(x.xidmet_tipi_kodu || '')) && (tab !== 'routines' || x.rutin_id) && (!query || `${x.xidmet_adi} ${x.xidmet_kodu}`.toLocaleLowerCase().includes(query)) && ['status','type','department','doctor'].every(k=>!document.getElementById(`filter-${k}`).value || document.getElementById(`filter-${k}`).value===value(x,k)));
    if(grouped) visible.sort((a,b)=>Number(a.istek_id)-Number(b.istek_id));
    if(tab==='history') visible.sort((a,b)=>str(b.yenilenme_tarixi || b.yaranma_tarixi).localeCompare(str(a.yenilenme_tarixi || a.yaranma_tarixi)));
    body.replaceChildren(); let previous=null;
    visible.forEach((x,i)=>{
      if(grouped && x.istek_id!==previous){const row=document.createElement('tr'),td=document.createElement('td');row.className='request-group';td.colSpan=13;td.textContent=`${tr.request} #${x.istek_id}`;row.append(td);body.append(row);previous=x.istek_id;}
      const row=document.createElement('tr');
      [i+1,...cells(x)].forEach((v,n)=>{const td=document.createElement('td');td.textContent=str(v)||'—';if(n===2){const code=document.createElement('small');code.textContent=x.xidmet_kodu;td.append(code);}if(n===11){const badge=document.createElement('span');badge.className=`badge ${x.aktiv===false?'bg-danger-subtle text-danger':'bg-success-subtle text-success'}`;badge.textContent=td.textContent;td.replaceChildren(badge);}row.append(td);});
      const actions=document.createElement('td');actions.className='text-nowrap';
      const button=(title,icon,fn)=>{const b=document.createElement('button');b.type='button';b.className='btn btn-sm btn-outline-primary me-1';b.title=title;b.setAttribute('aria-label',title);const i=document.createElement('i');i.className=`ti ti-${icon}`;b.append(i);b.onclick=fn;actions.append(b);return b;};
      button(tr.view,'eye',()=>details(x));
      button(tr.edit,'edit',()=>{window.location.href=`${base}?istekId=${encodeURIComponent(x.istek_id)}`;}).disabled=!!x.kilidlidir || x.aktiv===false;
      button(tr.remove,'trash',async()=>{
        if(!window.confirm(tr.cancelConfirm))return;
        try {const token=document.querySelector('#allCsrf input[name="_csrf"]');const response=await fetch(`${base}/legv/${x.xeste_xidmet_id}`,{method:'POST',headers:token?{'X-CSRF-TOKEN':token.value}:{}});const result=await response.json();if(!response.ok || !/UGUR|SUCCESS|OK/i.test(str(result.status_kodu)))throw new Error(result.mesaj || tr.loadError);await load();}catch(e){fail(e);}
      }).disabled=!!x.kilidlidir || x.aktiv===false;
      row.append(actions);body.append(row);
    });
    document.getElementById('allCount').textContent=visible.length;
    document.getElementById('allTotal').textContent=money(visible.reduce((s,x)=>s+Number(x.yekun_mebleg||0),0));
    document.getElementById('allEmpty').classList.toggle('d-none',visible.length>0);
    const finance=document.getElementById('financeSummary');finance.classList.toggle('d-none',tab!=='finance');finance.textContent=`${tr.patientAmount}: ${money(visible.reduce((s,x)=>s+Number(x.xeste_meblegi||0),0))} · ${tr.organizationAmount}: ${money(visible.reduce((s,x)=>s+Number(x.qurum_meblegi||0),0))}`;
    document.querySelectorAll('[data-tab]').forEach(b=>{b.classList.toggle('active',b.dataset.tab===tab);b.setAttribute('aria-pressed',String(b.dataset.tab===tab));});
  }
  document.getElementById('allTabs').onclick=e=>{const b=e.target.closest('[data-tab]');if(b){tab=b.dataset.tab;render();}};
  document.getElementById('allSearch').oninput=render;
  ['status','type','department','doctor'].forEach(k=>document.getElementById(`filter-${k}`).onchange=render);
  document.getElementById('clearFilters').onclick=()=>{document.querySelectorAll('.psa-filters input,.psa-filters select').forEach(x=>x.value='');render();};
  document.getElementById('groupRequests').onclick=e=>{grouped=!grouped;e.target.setAttribute('aria-pressed',String(grouped));render();};
  document.getElementById('closeDetails').onclick=()=>document.getElementById('serviceDialog').close();
  document.addEventListener('keydown',e=>{if(e.key==='Escape'&&!document.getElementById('serviceDialog').open)document.getElementById('allClose').click();});
  ['print','pdf'].forEach(k=>document.getElementById(`export-${k}`).onclick=()=>window.print());
  document.getElementById('export-excel').onclick=()=>{const csvCell=v=>'"'+str(v).replace(/^[=+@\-\t\r]/,"'$&").replaceAll('"','""')+'"';const csv='\uFEFF'+[keys.map(k=>tr[k]),...visible.map(cells)].map(r=>r.map(csvCell).join(';')).join('\r\n');const url=URL.createObjectURL(new Blob([csv],{type:'text/csv;charset=utf-8'}));const a=document.createElement('a');a.href=url;a.download=`services-${root.dataset.gelisId}.csv`;a.click();setTimeout(()=>URL.revokeObjectURL(url),1000);};
  load();
});
