document.addEventListener('DOMContentLoaded', () => {
  const root = document.getElementById('patientServiceWorkspace');
  if (!root) return;

  const gelisId = root.dataset.gelisId;
  const tr = document.getElementById('patientServiceI18n').dataset;
  const type = document.getElementById('catalogType');
  const search = document.getElementById('catalogSearch');
  const groups = document.getElementById('serviceGroups');
  const collections = document.getElementById('collectionList');
  const body = document.getElementById('catalogBody');
  const title = document.getElementById('catalogTitle');
  const count = document.getElementById('catalogCount');
  const loading = document.getElementById('catalogLoading');
  const empty = document.getElementById('catalogEmpty');
  const previous = document.getElementById('catalogPrevious');
  const next = document.getElementById('catalogNext');
  const pageLabel = document.getElementById('catalogPage');
  const alert = document.getElementById('catalogAlert');
  const alertMessage = document.getElementById('catalogAlertMessage');
  const referringDoctor = document.getElementById('referringDoctor');
  const selected = new Map();
  let activeGroup = '', activeCollection = '', page = 0, hasMore = false, request, timer, editingId = null, draft = null;

  const text = value => value == null ? '' : String(value);
  const money = value => `${Number(value || 0).toFixed(2)} ${tr.currency}`;
  const doctorName = h => [h.hekim_kodu, h.hekim_ad, h.hekim_soyad, h.hekim_ata_adi].filter(Boolean).join(' ');
  const option = (value, label) => { const o = document.createElement('option'); o.value = value ?? ''; o.textContent = label; return o; };
  const newService = row => ({ id: Number(row.id), kod: text(row.kod), ad: text(row.ad), qiymet: Number(row.qiymet || 0), miqdar: Number(row.miqdar || 1), gonderenHekimId: Number(referringDoctor.dataset.defaultId) || null, sobeId: null, sobeAdi: '', isteyenHekimId: null, icraEdenHekimId: null, icraEdenHekimAdi: '', tecili: false, aciqlama: null });
  const currentService = () => selected.get(editingId) || (draft && String(draft.id) === editingId ? draft : null);

  function showError(message) { alertMessage.textContent = message || tr.loadError; alert.classList.remove('d-none'); }
  function clearError() { alert.classList.add('d-none'); alertMessage.textContent = ''; }

  async function json(url) {
    const response = await fetch(url, { headers: { Accept: 'application/json' }, signal: request?.signal });
    let payload = {};
    try { payload = await response.json(); } catch (_) { /* Cavab JSON deyilsə standart mətn göstərilir. */ }
    if (!response.ok) { const message = payload.message || tr.loadError; showError(message); throw new Error(message); }
    return payload;
  }

  function infoCell(row) {
    const td = document.createElement('td');
    const name = document.createElement('strong'); name.textContent = text(row.ad);
    td.append(name); return td;
  }

  function renderCatalog(items) {
    body.replaceChildren(); count.textContent = items.length; empty.classList.toggle('d-none', items.length !== 0);
    items.forEach(row => {
      const line = document.createElement('tr'); const active = String(row.id) === editingId; line.dataset.serviceId = row.id; line.classList.toggle('psw-service-active', active); line.classList.toggle('psw-service-added', selected.has(String(row.id))); line.setAttribute('aria-selected', String(active));
      const code = document.createElement('td'); code.innerHTML = `<span class="badge bg-light text-dark"></span>`; code.firstChild.textContent = text(row.kod);
      const group = document.createElement('td'); group.textContent = text(row.qrup_adi) || '—';
      const price = document.createElement('td'); price.className = 'fw-semibold text-nowrap'; price.textContent = money(row.qiymet);
      const actionCell = document.createElement('td');
      const add = document.createElement('button'); add.type = 'button'; add.className = 'btn btn-sm btn-outline-success'; add.title = tr.add;
      add.innerHTML = `<i class="ti ${selected.has(String(row.id)) ? 'ti-check' : 'ti-plus'}"></i>`; add.disabled = selected.has(String(row.id));
      add.addEventListener('click', event => { event.stopPropagation(); addService(row, add); }); actionCell.append(add);
      line.addEventListener('click', () => openCandidate(row));
      line.append(code, infoCell(row), group, price, actionCell); body.append(line);
    });
  }

  function renderCollections(items) {
    collections.replaceChildren();
    if (!items.length) { const d = document.createElement('div'); d.className = 'psw-empty psw-empty-small'; d.textContent = tr.loadError; collections.append(d); return; }
    items.forEach(row => {
      const button = document.createElement('button'); button.type = 'button'; button.className = 'psw-collection-item';
      button.dataset.id = row.id; button.innerHTML = '<i class="ti ti-box"></i><span></span><small></small>';
      button.querySelector('span').textContent = row.ad; button.querySelector('small').textContent = [row.kod, row.xidmet_sayi].filter(v => v != null).join(' · ');
      button.addEventListener('click', () => { activeCollection = String(row.id); collections.querySelectorAll('button').forEach(x => x.classList.toggle('active', x === button)); loadContents(); });
      collections.append(button);
    });
  }

  async function loadCatalog() {
    request?.abort(); request = new AbortController(); clearError(); loading.classList.remove('d-none'); empty.classList.add('d-none'); body.replaceChildren();
    const params = new URLSearchParams({ nov: type.value, page: String(page) });
    const query = search.value.trim();
    if (query) params.set('q', query);
    if (type.value === 'XIDMET' && activeGroup && !query) params.set('qrupId', activeGroup);
    try {
      const data = await json(`/xeste-xidmetleri/${gelisId}/kataloq?${params}`);
      hasMore = data.hasMore; previous.disabled = page === 0; next.disabled = !hasMore; pageLabel.textContent = tr.page.replace('{0}', page + 1);
      if (type.value === 'XIDMET') renderCatalog(data.items); else { renderCollections(data.items); renderCatalog([]); }
    } catch (e) { if (e.name !== 'AbortError') { empty.classList.remove('d-none'); empty.querySelector('span').textContent = tr.loadError; } }
    finally { loading.classList.add('d-none'); }
  }

  async function loadContents() {
    if (!activeCollection) return;
    request?.abort(); request = new AbortController(); clearError(); loading.classList.remove('d-none'); body.replaceChildren();
    const params = new URLSearchParams({ nov: type.value, secimId: activeCollection });
    try { const data = await json(`/xeste-xidmetleri/${gelisId}/kataloq?${params}`); renderCatalog(data.items); }
    catch (e) { if (e.name !== 'AbortError') { empty.classList.remove('d-none'); empty.querySelector('span').textContent = tr.loadError; } }
    finally { loading.classList.add('d-none'); }
  }

  function addService(row, button) {
    const id = String(row.id);
    if (selected.has(id)) return;
    if (editingId !== id || !draft) { openCandidate(row); return; }
    syncDetails();
    const department = document.getElementById('serviceDepartment');
    const performing = document.getElementById('performingDoctor');
    const invalid = [department, performing].find(field => !field.value);
    [department, performing].forEach(field => field.classList.toggle('is-invalid', field === invalid));
    if (invalid) { invalid.focus(); showError(tr.selectionRequired); return; }
    selected.set(id, { ...draft }); draft = null; clearError();
    button.disabled = true; button.innerHTML = '<i class="ti ti-check"></i>'; button.closest('tr')?.classList.add('psw-service-added');
    closeDetails(); renderSelected();
  }

  function openCandidate(row) {
    const id = String(row.id);
    if (selected.has(id)) { openDetails(id); return; }
    draft = newService(row); editingId = id; markActiveCatalogRow(); renderSelected(); populateDetails(draft);
  }

  function renderSelected() {
    const selectedBody = document.getElementById('selectedBody'); selectedBody.replaceChildren();
    document.getElementById('selectedCount').textContent = selected.size;
    document.getElementById('selectedEmpty').classList.toggle('d-none', selected.size > 0);
    document.getElementById('selectedTable').classList.toggle('d-none', selected.size === 0);
    document.getElementById('saveServices').disabled = selected.size === 0;
    let total = 0, index = 0;
    selected.forEach((row, id) => {
      index++; total += row.qiymet * row.miqdar;
      const line = document.createElement('tr'); if (id === editingId) line.classList.add('table-primary');
      [index, row.kod, row.ad, row.miqdar, money(row.qiymet), money(row.qiymet * row.miqdar), row.sobeAdi || '—', row.icraEdenHekimAdi || '—'].forEach((value, i) => { const td = document.createElement('td'); td.textContent = value; if (i === 2) td.className = 'fw-semibold'; line.append(td); });
      const actions = document.createElement('td'); actions.className = 'text-nowrap';
      const edit = document.createElement('button'); edit.type = 'button'; edit.className = 'btn btn-sm btn-outline-primary me-1'; edit.title = tr.edit; edit.innerHTML = '<i class="ti ti-edit"></i>'; edit.addEventListener('click', () => openDetails(id));
      const remove = document.createElement('button'); remove.type = 'button'; remove.className = 'btn btn-sm btn-outline-danger'; remove.title = tr.remove; remove.innerHTML = '<i class="ti ti-trash"></i>'; remove.addEventListener('click', () => { selected.delete(id); if (editingId === id) closeDetails(); renderSelected(); loadCatalog(); });
      actions.append(edit, remove); line.append(actions); selectedBody.append(line);
    });
    document.getElementById('grandTotal').textContent = money(total);
  }

  async function populateDetails(row) {
    document.getElementById('detailsEmpty').classList.add('d-none'); document.getElementById('detailsForm').classList.remove('d-none'); document.getElementById('detailsName').textContent = row.kod;
    referringDoctor.value = row.gonderenHekimId ?? '';
    document.getElementById('requestingDoctor').value = row.isteyenHekimId ?? ''; document.getElementById('serviceQuantity').value = row.miqdar; document.getElementById('serviceUrgent').checked = row.tecili; document.getElementById('serviceNote').value = row.aciqlama ?? '';
    const department = document.getElementById('serviceDepartment'); department.replaceChildren(option('', tr.select));
    try {
      request?.abort(); request = new AbortController(); const rows = await json(`/xeste-xidmetleri/${gelisId}/xidmet/${row.id}/sobeler`);
      rows.forEach(x => department.append(option(x.sobe_id, x.sobe_adi))); department.value = row.sobeId ?? '';
      if (!rows.length) department.append(option('', tr.noDepartment));
      await loadDoctors();
    } catch (e) { if (e.name !== 'AbortError') department.append(option('', tr.loadError)); }
  }

  function openDetails(id) {
    editingId = id; draft = null; const row = selected.get(id); if (!row) return;
    markActiveCatalogRow(); renderSelected(); populateDetails(row);
  }

  async function loadDoctors() {
    const row = currentService(), department = document.getElementById('serviceDepartment'), doctors = document.getElementById('performingDoctor');
    doctors.replaceChildren(option('', tr.select)); doctors.disabled = !department.value; if (!department.value || !row) return;
    try { const rows = await json(`/xeste-xidmetleri/${gelisId}/xidmet/${row.id}/sobe/${department.value}/hekimler`); rows.forEach(x => doctors.append(option(x.hekim_id, doctorName(x)))); doctors.value = row.icraEdenHekimId ?? ''; }
    catch (e) { if (e.name !== 'AbortError') doctors.append(option('', tr.loadError)); }
  }

  function syncDetails() {
    const row = currentService(); if (!row) return;
    const department = document.getElementById('serviceDepartment'), doctors = document.getElementById('performingDoctor');
    row.gonderenHekimId = Number(referringDoctor.value) || null;
    row.sobeId = Number(department.value) || null; row.sobeAdi = department.value ? department.options[department.selectedIndex].text : '';
    row.isteyenHekimId = Number(document.getElementById('requestingDoctor').value) || null;
    row.icraEdenHekimId = Number(doctors.value) || null; row.icraEdenHekimAdi = doctors.value ? doctors.options[doctors.selectedIndex].text : '';
    row.miqdar = Math.max(1, Number(document.getElementById('serviceQuantity').value) || 1); row.tecili = document.getElementById('serviceUrgent').checked; row.aciqlama = document.getElementById('serviceNote').value.trim() || null;
    if (selected.has(editingId)) renderSelected();
  }

  function markActiveCatalogRow() { body.querySelectorAll('tr').forEach(row => { const active = row.dataset.serviceId === editingId; row.classList.toggle('psw-service-active', active); row.setAttribute('aria-selected', String(active)); }); }

  function collapseGroup(groupId) {
    groups.querySelectorAll(`[data-parent="${groupId}"]`).forEach(child => {
      collapseGroup(child.dataset.group);
      child.classList.add('d-none');
      const icon = child.querySelector('i');
      if (child.dataset.hasChildren === 'true' && icon) {
        icon.classList.remove('ti-chevron-down');
        icon.classList.add('ti-chevron-right');
      }
    });
  }

  function closeDetails() { editingId = null; draft = null; markActiveCatalogRow(); document.getElementById('detailsForm').classList.add('d-none'); document.getElementById('detailsEmpty').classList.remove('d-none'); document.getElementById('detailsName').textContent = ''; }

  type.addEventListener('change', () => { page = 0; activeCollection = ''; const isService = type.value === 'XIDMET'; groups.classList.toggle('d-none', !isService); collections.classList.toggle('d-none', isService); title.textContent = type.options[type.selectedIndex].text; loadCatalog(); });
  groups.addEventListener('click', e => { const button = e.target.closest('[data-group]'); if (!button) return; activeGroup = button.dataset.group; page = 0; groups.querySelectorAll('button').forEach(x => x.classList.toggle('active', x === button)); if (button.dataset.hasChildren === 'true') { const icon = button.querySelector('i'); const opening = icon.classList.contains('ti-chevron-right'); icon.classList.toggle('ti-chevron-right', !opening); icon.classList.toggle('ti-chevron-down', opening); if (opening) groups.querySelectorAll(`[data-parent="${button.dataset.group}"]`).forEach(x => x.classList.remove('d-none')); else collapseGroup(button.dataset.group); } loadCatalog(); });
  search.addEventListener('input', () => { clearTimeout(timer); timer = setTimeout(() => { page = 0; activeCollection = ''; loadCatalog(); }, 300); });
  previous.addEventListener('click', () => { if (page > 0) { page--; loadCatalog(); } }); next.addEventListener('click', () => { if (hasMore) { page++; loadCatalog(); } });
  document.getElementById('serviceDepartment').addEventListener('change', async () => { syncDetails(); await loadDoctors(); syncDetails(); });
  ['referringDoctor','requestingDoctor','performingDoctor','serviceUrgent'].forEach(id => document.getElementById(id).addEventListener('change', syncDetails));
  ['serviceQuantity','serviceNote'].forEach(id => document.getElementById(id).addEventListener('input', syncDetails));
  document.getElementById('selectedForm').addEventListener('submit', e => { const invalid = [...selected.values()].find(x => !x.sobeId); if (invalid) { e.preventDefault(); openDetails(String(invalid.id)); return; } document.getElementById('servicesJson').value = JSON.stringify([...selected.values()].map(x => ({ xidmet_id: x.id, sobe_id: x.sobeId, hekim_menbe_novu: x.isteyenHekimId ? 'ISTEYEN' : (x.gonderenHekimId ? 'GONDEREN' : null), isteyen_hekim_id: x.isteyenHekimId, icra_eden_hekim_id: x.icraEdenHekimId, miqdar: x.miqdar, tecili: x.tecili, aciqlama: x.aciqlama }))); });
  document.body.classList.add('patient-services-open');
  const fitWorkspace = () => { const top = root.getBoundingClientRect().top; root.style.height = `${Math.max(320, window.innerHeight - top - 10)}px`; };
  fitWorkspace(); window.addEventListener('resize', fitWorkspace);
  loadCatalog();
});
