document.addEventListener('DOMContentLoaded', () => {
    const root = document.getElementById('patientServiceWorkspace');
    if (!root) return;

    const gelisId = root.dataset.gelisId;
    const istekId = root.dataset.istekId || '';

    const serviceDate = document.getElementById('serviceDate');
    const validDate = value => {
        if (!/^\d{4}-\d{2}-\d{2}$/.test(value) || value < '0001-01-01') return false;
        const parsed = new Date(`${value}T00:00:00Z`);
        return !Number.isNaN(parsed.getTime()) && parsed.toISOString().slice(0, 10) === value;
    };
    let lastServiceDate = serviceDate.value;
    const openDatePicker = () => {
        if (typeof serviceDate.showPicker === 'function') {
            try { serviceDate.showPicker(); } catch (_) { /* Native picker remains available. */ }
        }
    };
    serviceDate.addEventListener('click', openDatePicker);
    serviceDate.addEventListener('keydown', event => {
        if (event.key === 'Tab' || event.key === 'Escape') return;
        event.preventDefault();
        if (event.key === 'Enter' || event.key === ' ' || event.key === 'ArrowDown') openDatePicker();
    });
    ['beforeinput', 'paste', 'drop'].forEach(eventName => {
        serviceDate.addEventListener(eventName, event => event.preventDefault());
    });
    serviceDate.addEventListener('focus', () => {
        if (validDate(serviceDate.value)) lastServiceDate = serviceDate.value;
    });
    serviceDate.addEventListener('input', () => {
        if (validDate(serviceDate.value)) lastServiceDate = serviceDate.value;
        else serviceDate.value = lastServiceDate;
    });
    const checkServiceDate = () => {
        const valid = validDate(serviceDate.value) && serviceDate.checkValidity();
        serviceDate.classList.toggle('is-invalid', !valid);
        if (!valid) {
            serviceDate.reportValidity();
            serviceDate.focus();
            showError(tr.invalidDate);
        }
        return valid;
    };

    const payload = x => ({
        xeste_xidmet_id: x.xesteXidmetId ?? null,
        xidmet_id: x.id,
        rutin_id: x.rutinId ?? null,
        sobe_id: x.sobeId,
        xidmet_tarixi: x.tarix,
        gonderen_hekim_id: x.gonderenHekimId,
        isteyen_hekim_id: x.isteyenHekimId,
        icra_eden_hekim_id: x.icraEdenHekimId,
        miqdar: x.miqdar,
        tecili: x.tecili,
        aciqlama: x.aciqlama
    });

    async function validate(row) {
        const form = document.getElementById('selectedForm');
        const token = form.querySelector('input[name="_csrf"]');
        const response = await fetch(`/xeste-xidmetleri/${gelisId}/hazirla?istekId=${istekId}`, {
            method: 'POST',
            headers: {'Content-Type': 'application/json', ...(token ? {'X-CSRF-TOKEN': token.value} : {})},
            body: JSON.stringify(payload(row))
        });
        const result = await response.json();
        if (!response.ok || result.ugurlu !== true) throw new Error(result.mesaj || tr.selectionRequired);
    }

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
    const alertClose = document.getElementById('catalogAlertClose');
    const referringDoctor = document.getElementById('referringDoctor');
    const doctorDefaults = {
        gonderenHekimId: Number(referringDoctor.options[1]?.value) || null,
        gonderenHekimAdi: referringDoctor.options[1]?.textContent || '',
        isteyenHekimId: Number(document.getElementById('requestingDoctor').value) || null,
        isteyenHekimAdi: document.getElementById('requestingDoctor').selectedOptions[0]?.textContent || ''
    };
    referringDoctor.value = doctorDefaults.gonderenHekimId ?? '';
    const selected = new Map();
    const preparing = new Set();
    let activeGroup = '', activeCollection = '', page = 0, hasMore = false, request, timer, editingId = null,
        draft = null;

    const text = value => value == null ? '' : String(value);
    const money = value => `${Number(value || 0).toFixed(2)} ${tr.currency}`;
    const doctorName = h => [h.hekim_kodu, h.hekim_ad, h.hekim_soyad, h.hekim_ata_adi].filter(Boolean).join(' ');
    const option = (value, label) => {
        const o = document.createElement('option');
        o.value = value ?? '';
        o.textContent = label;
        return o;
    };
    const newService = row => ({
        id: Number(row.id),
        kod: text(row.kod),
        ad: text(row.ad),
        standartQiymet: Number(row.qiymet || 0),
        qiymet: Number(row.qiymet || 0),
        miqdar: Number(row.miqdar || 1),
        tarix: serviceDate.value,

        rutinId: type.value === 'RUTIN'
            ? (Number(row.rutin_id ?? activeCollection) || null)
            : null,

        ...doctorDefaults,

        sobeId: null,
        sobeAdi: '',
        hekimSecimQaydasiKodu: 'SECIMLI',
        icraEdenHekimId: null,
        icraEdenHekimAdi: '',
        tecili: false,
        aciqlama: null,

        nov: type.value
    });
    const containsService = id => [...selected.values()].some(x => String(x.id) === String(id));
    const currentService = () => selected.get(editingId) || (draft && String(draft.id) === editingId ? draft : null);

    function showError(message) {
        alertMessage.textContent = message || tr.loadError;
        alert.classList.remove('d-none');
    }

    function clearError() {
        alert.classList.add('d-none');
        alertMessage.textContent = '';
    }

    alertClose.addEventListener('click', clearError);

    async function json(url, signal = request?.signal) {
        const response = await fetch(url, {headers: {Accept: 'application/json'}, signal});
        let payload = {};
        try {
            payload = await response.json();
        } catch (_) { /* Cavab JSON deyilsə standart mətn göstərilir. */
        }
        if (!response.ok) {
            const message = payload.message || tr.loadError;
            showError(message);
            throw new Error(message);
        }
        return payload;
    }

    function infoCell(row) {
        const td = document.createElement('td');
        const name = document.createElement('strong');
        name.textContent = text(row.ad);
        td.append(name);
        return td;
    }

    function renderCatalog(items) {
        body.replaceChildren();
        count.textContent = items.length;
        empty.classList.toggle('d-none', items.length !== 0);
        items.forEach(row => {
            const line = document.createElement('tr');
            const active = String(row.id) === editingId;
            line.dataset.serviceId = row.id;
            line.classList.toggle('psw-service-active', active);
            line.classList.toggle('psw-service-added', containsService(row.id));
            line.setAttribute('aria-selected', String(active));
            const code = document.createElement('td');
            code.innerHTML = `<span class="badge bg-light text-dark"></span>`;
            code.firstChild.textContent = text(row.kod);
            const group = document.createElement('td');
            group.textContent = text(row.qrup_adi) || '—';
            const price = document.createElement('td');
            price.className = 'fw-semibold text-nowrap';
            price.textContent = money(row.qiymet);
            const actionCell = document.createElement('td');
            const add = document.createElement('button');
            add.type = 'button';
            add.className = 'btn btn-sm btn-outline-success';
            add.title = tr.add;
            add.innerHTML = `<i class="ti ${containsService(row.id) ? 'ti-check' : 'ti-plus'}"></i>`;
            add.disabled = containsService(row.id);
            add.addEventListener('click', event => {
                event.stopPropagation();
                addService(row, add);
            });
            actionCell.append(add);
            line.append(code, infoCell(row), group, price, actionCell);
            body.append(line);
        });
    }

    function renderCollections(items) {
        collections.replaceChildren();
        if (!items.length) {
            const d = document.createElement('div');
            d.className = 'psw-empty psw-empty-small';
            d.textContent = tr.notFound;
            collections.append(d);
            return;
        }
        items.forEach(row => {
            const button = document.createElement('button');
            button.type = 'button';
            button.className = 'psw-collection-item';
            button.dataset.id = row.id;
            button.innerHTML = '<i class="ti ti-box"></i><span></span><small></small>';
            button.querySelector('span').textContent = row.ad;
            button.querySelector('small').textContent = [row.kod, row.xidmet_sayi].filter(v => v != null).join(' · ');
            button.addEventListener('click', () => {
                closeDetails();
                activeCollection = String(row.id);
                collections.querySelectorAll('button').forEach(x => x.classList.toggle('active', x === button));
                loadContents();
            });
            collections.append(button);
        });
    }

    async function loadCatalog() {
        request?.abort();
        request = new AbortController();
        clearError();
        loading.classList.remove('d-none');
        empty.classList.add('d-none');
        body.replaceChildren();
        const params = new URLSearchParams({nov: type.value, page: String(page), istekId});
        const query = search.value.trim();
        if (query) params.set('q', query);
        if (type.value === 'XIDMET' && activeGroup) params.set('qrupId', activeGroup);
        try {
            const data = await json(`/xeste-xidmetleri/${gelisId}/kataloq?${params}`);
            hasMore = data.hasMore;
            previous.disabled = page === 0;
            next.disabled = !hasMore;
            pageLabel.textContent = tr.page.replace('{0}', page + 1);
            if (type.value === 'XIDMET' || type.value === 'PAKET') renderCatalog(data.items); else {
                renderCollections(data.items);
                renderCatalog([]);
            }
        } catch (e) {
            if (e.name !== 'AbortError') {
                empty.classList.remove('d-none');
                empty.querySelector('span').textContent = tr.loadError;
            }
        } finally {
            loading.classList.add('d-none');
        }
    }

    async function loadContents() {
        if (!activeCollection || !checkServiceDate()) return;
        request?.abort();
        request = new AbortController();
        clearError();
        loading.classList.remove('d-none');
        body.replaceChildren();
        const params = new URLSearchParams({
            nov: type.value,
            secimId: activeCollection,
            tarix: serviceDate.value
        });
        try {
            const data = await json(`/xeste-xidmetleri/${gelisId}/kataloq?${params}`);
            renderCatalog(data.items);
        } catch (e) {
            if (e.name !== 'AbortError') {
                empty.classList.remove('d-none');
                empty.querySelector('span').textContent = tr.loadError;
            }
        } finally {
            loading.classList.add('d-none');
        }
    }

    async function addService(row, button) {
        const id = String(row.id);

        if (containsService(id) || preparing.has(id) || !checkServiceDate()) return;

        const newlyOpened = editingId !== id || !draft;
        if (newlyOpened) openCandidate(row);
        const pending = draft;
        button.disabled = true;
        if (pending?.detailsReady) await pending.detailsReady;
        button.disabled = containsService(id);
        if (!pending || draft !== pending || editingId !== id || pending.detailsLoaded === false) return;

        syncDetails();

        const department = document.getElementById('serviceDepartment');
        const performing = document.getElementById('performingDoctor');

        const isPackage = draft.nov === 'PAKET';

        const doctorRequired =
            !isPackage &&
            department.selectedOptions[0]?.dataset.doctorRule === 'MECBURI';

        const requiredFields = [
            document.getElementById('serviceDate')
        ];

        if (!isPackage) {
            requiredFields.push(department);

            if (doctorRequired) {
                requiredFields.push(performing);
            }
        }

        const invalid = requiredFields.find(field => !field.value || !field.checkValidity());

        [department, performing].forEach(field =>
            field.classList.toggle('is-invalid', field === invalid)
        );

        if (invalid) {
            invalid.focus();
            showError(tr.selectionRequired);
            return;
        }

        if (newlyOpened && !pending.canAddDirectly) {
            showError(tr.selectionRequired);
            return;
        }

        const candidate = {...draft};

        try {
            button.disabled = true;
            preparing.add(id);
            await validate(candidate);
        } catch (e) {
            showError(e.message);
            button.disabled = false;
            return;
        } finally {
            preparing.delete(id);
        }

        selected.set(id, candidate);
        if (draft === pending) draft = null;

        clearError();

        button.disabled = true;
        button.innerHTML = '<i class="ti ti-check"></i>';

        button.closest('tr')?.classList.add('psw-service-added');

        markActiveCatalogRow();
        renderSelected();
    }

    function openCandidate(row) {
        if (!checkServiceDate()) return;
        const id = String(row.id);

        if (containsService(id)) {
            const entry = [...selected].find(([, x]) => String(x.id) === id);
            if (!entry[1].locked) openDetails(entry[0]);
            return;
        }

        draft = newService(row);
        editingId = id;

        markActiveCatalogRow();
        renderSelected();

        if (type.value === 'PAKET') {
            populatePackageDetails(draft);
        } else {
            draft.detailsReady = populateDetails(draft);
        }
    }

    function populatePackageDetails(row) {

        document.getElementById('detailsName').textContent = row.kod;
        document.getElementById('serviceDate').value = row.tarix;

        referringDoctor.value = row.gonderenHekimId ?? '';

        document.getElementById('requestingDoctor').value =
            row.isteyenHekimId ?? '';

        document.getElementById('serviceQuantity').value = row.miqdar;
        document.getElementById('serviceUrgent').checked = row.tecili;
        document.getElementById('serviceNote').value = row.aciqlama ?? '';

        const department = document.getElementById('serviceDepartment');
        const performing = document.getElementById('performingDoctor');

        department.replaceChildren(option('', tr.select));
        performing.replaceChildren(option('', tr.select));

        department.value = '';
        performing.value = '';

        row.sobeId = null;
        row.sobeAdi = '';
        row.icraEdenHekimId = null;
        row.icraEdenHekimAdi = '';
    }

    function renderSelected() {
        const selectedBody = document.getElementById('selectedBody');
        selectedBody.replaceChildren();
        document.getElementById('selectedCount').textContent = selected.size;
        document.getElementById('selectedEmpty').classList.toggle('d-none', selected.size > 0);
        document.getElementById('selectedTable').classList.toggle('d-none', selected.size === 0);
        document.getElementById('saveServices').disabled = selected.size === 0;
        let total = 0, index = 0;
        selected.forEach((row, id) => {
            index++;
            total += row.qiymet * row.miqdar;
            const line = document.createElement('tr');
            if (id === editingId) line.classList.add('table-primary');
            [index, row.kod, row.ad, row.tarix, row.rutinId ?? '—', row.miqdar, money(row.qiymet), money(row.qiymet * row.miqdar), row.sobeAdi || '—', row.icraEdenHekimAdi || '—', row.gonderenHekimAdi || '—', row.isteyenHekimAdi || '—'].forEach((value, i) => {
                const td = document.createElement('td');
                td.textContent = value;
                if (i === 2) td.className = 'fw-semibold';
                line.append(td);
            });
            const actions = document.createElement('td');
            actions.className = 'text-nowrap';
            const edit = document.createElement('button');
            edit.type = 'button';
            edit.className = 'btn btn-sm btn-outline-primary me-1';
            edit.title = tr.edit;
            edit.innerHTML = '<i class="ti ti-edit"></i>';
            edit.disabled = row.locked === true;
            edit.addEventListener('click', () => openDetails(id));
            const remove = document.createElement('button');
            remove.type = 'button';
            remove.className = 'btn btn-sm btn-outline-danger';
            remove.title = tr.remove;
            remove.innerHTML = '<i class="ti ti-trash"></i>';
            remove.disabled = row.removable === false;
            remove.addEventListener('click', () => {
                selected.delete(id);
                if (editingId === id) closeDetails();
                renderSelected();
                loadCatalog();
            });
            actions.append(edit, remove);
            line.append(actions);
            selectedBody.append(line);
        });
        document.getElementById('grandTotal').textContent = money(total);
    }

    async function populateDetails(row) {
        document.getElementById('detailsName').textContent = row.kod;
        document.getElementById('serviceDate').value = row.tarix;
        referringDoctor.value = row.gonderenHekimId ?? '';
        document.getElementById('requestingDoctor').value = row.isteyenHekimId ?? '';
        document.getElementById('serviceQuantity').value = row.miqdar;
        document.getElementById('serviceUrgent').checked = row.tecili;
        document.getElementById('serviceNote').value = row.aciqlama ?? '';
        const department = document.getElementById('serviceDepartment');
        department.replaceChildren(option('', tr.select));
        document.getElementById('performingDoctor').replaceChildren(option('', tr.select));
        document.getElementById('performingDoctor').disabled = true;
        row.detailsLoaded = false;
        row.canAddDirectly = false;
        try {
            const rows = await json(`/xeste-xidmetleri/${gelisId}/xidmet/${row.id}/sobeler`, null);
            if (currentService() !== row) return;

            rows.forEach(x => {
                const departmentOption = option(x.sobe_id, x.sobe_adi);
                departmentOption.dataset.doctorRule = text(x.hekim_secim_qaydasi_kodu) || 'SECIMLI';
                department.append(departmentOption);
            });
            if (row.sobeId == null && rows.length === 1) {
                row.sobeId = Number(rows[0].sobe_id);
                row.sobeAdi = text(rows[0].sobe_adi);
                if (selected.has(editingId)) renderSelected();
            }
            department.value = row.sobeId ?? '';
            if (!rows.length) department.append(option('', tr.noDepartment));
            const doctorCount = await loadDoctors();
            if (currentService() !== row) return;
            row.detailsLoaded = doctorCount !== null;
            row.canAddDirectly = rows.length === 1 && (
                (doctorCount === 0 && row.hekimSecimQaydasiKodu !== 'MECBURI') ||
                (doctorCount === 1 && row.hekimSecimQaydasiKodu === 'MECBURI')
            );
        } catch (e) {
            if (currentService() === row && e.name !== 'AbortError') department.append(option('', tr.loadError));
        }
    }

    function openDetails(id) {
        editingId = id;
        draft = null;
        const row = selected.get(id);
        if (!row || row.locked) return;
        markActiveCatalogRow();
        renderSelected();
        if (row.nov === 'PAKET') populatePackageDetails(row);
        else populateDetails(row);
    }

    async function loadDoctors() {
        const row = currentService(), department = document.getElementById('serviceDepartment'),
            doctors = document.getElementById('performingDoctor');
        const doctorRule = department.selectedOptions[0]?.dataset.doctorRule || 'SECIMLI';
        doctors.replaceChildren(option('', tr.select));
        doctors.disabled = !department.value || doctorRule === 'SECILMIR';
        if (row) {
            row.hekimSecimQaydasiKodu = doctorRule;
            if (doctorRule === 'SECILMIR') {
                row.icraEdenHekimId = null;
                row.icraEdenHekimAdi = '';
                row.qiymet = row.standartQiymet;
            }
        }
        if (!department.value || !row || doctorRule === 'SECILMIR') return 0;
        const departmentId = department.value;
        try {
            const rows = await json(`/xeste-xidmetleri/${gelisId}/xidmet/${row.id}/sobe/${department.value}/hekimler?tarix=${encodeURIComponent(row.tarix)}`, null);
            if (currentService() !== row || department.value !== departmentId) return null;
            rows.forEach(x => {
                const name = doctorName(x);
                const doctorOption = option(x.hekim_id, `${name} (${money(x.yekun_qiymet)})`);
                doctorOption.dataset.doctorName = name;
                doctorOption.dataset.finalPrice = Number(x.yekun_qiymet ?? row.standartQiymet);
                doctors.append(doctorOption);
            });
            if (doctorRule === 'MECBURI' && rows.length === 1 && row.icraEdenHekimId == null) {
                row.icraEdenHekimId = Number(rows[0].hekim_id);
                row.icraEdenHekimAdi = doctorName(rows[0]);
                row.qiymet = Number(rows[0].yekun_qiymet ?? row.standartQiymet);
                if (selected.has(editingId)) renderSelected();
            }
            doctors.value = row.icraEdenHekimId ?? '';
            return rows.length;
        } catch (e) {
            if (currentService() === row && e.name !== 'AbortError') doctors.append(option('', tr.loadError));
            return null;
        }
    }

    function syncDetails() {
        const row = currentService();
        if (!row || row.locked) return;
        const department = document.getElementById('serviceDepartment'),
            doctors = document.getElementById('performingDoctor');
        if (validDate(serviceDate.value) && serviceDate.checkValidity()) row.tarix = serviceDate.value;
        row.gonderenHekimId = Number(referringDoctor.value) || null;
        row.gonderenHekimAdi = referringDoctor.value ? referringDoctor.selectedOptions[0]?.textContent || '' : '';
        row.sobeId = Number(department.value) || null;
        row.sobeAdi = department.value ? department.options[department.selectedIndex].text : '';
        const requestingDoctor = document.getElementById('requestingDoctor');
        if (!requestingDoctor.disabled) {
            row.isteyenHekimId = Number(requestingDoctor.value) || null;
            row.isteyenHekimAdi = requestingDoctor.value ? requestingDoctor.selectedOptions[0]?.textContent || '' : '';
        }
        row.icraEdenHekimId = Number(doctors.value) || null;
        row.icraEdenHekimAdi = doctors.value ? doctors.options[doctors.selectedIndex].dataset.doctorName : '';
        row.qiymet = doctors.value ? Number(doctors.options[doctors.selectedIndex].dataset.finalPrice ?? row.standartQiymet) : row.standartQiymet;
        row.miqdar = Math.max(1, Number(document.getElementById('serviceQuantity').value) || 1);
        row.tecili = document.getElementById('serviceUrgent').checked;
        row.aciqlama = document.getElementById('serviceNote').value.trim() || null;
        if (selected.has(editingId)) renderSelected();
    }

    function markActiveCatalogRow() {
        body.querySelectorAll('tr').forEach(row => {
            const active = row.dataset.serviceId === editingId;
            row.classList.toggle('psw-service-active', active);
            row.setAttribute('aria-selected', String(active));
        });
    }

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

    function closeDetails() {
        editingId = null;
        draft = null;
        markActiveCatalogRow();
        document.getElementById('serviceDepartment').replaceChildren(option('', tr.select));
        document.getElementById('performingDoctor').replaceChildren(option('', tr.select));
        document.getElementById('performingDoctor').disabled = true;
        document.getElementById('detailsName').textContent = '';
    }

    type.addEventListener('change', () => {
        clearTimeout(timer);
        search.value = '';
        page = 0;
        activeCollection = '';
        closeDetails();
        clearError();

        const isService = type.value === 'XIDMET';
        const isRoutine = type.value === 'RUTIN';

        groups.classList.toggle('d-none', !isService);
        collections.classList.toggle('d-none', !isRoutine);
        title.textContent = type.options[type.selectedIndex].text;

        body.replaceChildren();

        loadCatalog();
    });

    groups.addEventListener('click', e => {
        const button = e.target.closest('[data-group]');
        if (!button) return;
        activeGroup = button.dataset.group;
        page = 0;
        groups.querySelectorAll('button').forEach(x => x.classList.toggle('active', x === button));
        if (button.dataset.hasChildren === 'true') {
            const icon = button.querySelector('i');
            const opening = icon.classList.contains('ti-chevron-right');
            icon.classList.toggle('ti-chevron-right', !opening);
            icon.classList.toggle('ti-chevron-down', opening);
            if (opening) groups.querySelectorAll(`[data-parent="${button.dataset.group}"]`).forEach(x => x.classList.remove('d-none')); else collapseGroup(button.dataset.group);
        }
        loadCatalog();
    });
    search.addEventListener('input', () => {
        clearTimeout(timer);

        timer = setTimeout(() => {
            page = 0;
            activeCollection = '';

            closeDetails();
            loadCatalog();
        }, 300);
    });
    previous.addEventListener('click', () => {
        if (page > 0) {
            page--;
            loadCatalog();
        }
    });
    next.addEventListener('click', () => {
        if (hasMore) {
            page++;
            loadCatalog();
        }
    });
    document.getElementById('serviceDepartment').addEventListener('change', async () => {
        const row = currentService();
        if (row) {
            row.icraEdenHekimId = null;
            row.icraEdenHekimAdi = '';
        }
        document.getElementById('performingDoctor').value = '';
        syncDetails();
        await loadDoctors();
        syncDetails();
    });
    [['referringDoctor', 'gonderenHekim'], ['requestingDoctor', 'isteyenHekim']].forEach(([id, key]) => {
        const control = document.getElementById(id);
        control.addEventListener('change', () => {
            doctorDefaults[`${key}Id`] = Number(control.value) || null;
            doctorDefaults[`${key}Adi`] = control.value ? control.selectedOptions[0]?.textContent || '' : '';
            syncDetails();
        });
    });
    ['performingDoctor', 'serviceUrgent'].forEach(id => document.getElementById(id).addEventListener('change', syncDetails));
    serviceDate.addEventListener('change', async () => {
        if (!validDate(serviceDate.value)) serviceDate.value = lastServiceDate;
        if (!checkServiceDate()) return;
        lastServiceDate = serviceDate.value;
        clearError();
        syncDetails();
        await loadDoctors();
        syncDetails();
    });
    ['serviceQuantity', 'serviceNote'].forEach(id => document.getElementById(id).addEventListener('input', syncDetails));
    document.getElementById('selectedForm').addEventListener('submit', async e => {
        e.preventDefault();
        if (!checkServiceDate()) return;
        syncDetails();
        const button = document.getElementById('saveServices');
        button.disabled = true;
        try {
            for (const row of selected.values()) {
                if (row.locked) continue;
                if (!validDate(row.tarix)) throw new Error(tr.invalidDate);
                if (row.nov !== 'PAKET' && !row.sobeId) throw new Error(tr.selectionRequired);
                await validate(row);
            }
            document.getElementById('servicesJson').value = JSON.stringify([...selected.values()].map(payload));
            e.target.submit();
        } catch (error) {
            showError(error.message);
            button.disabled = false;
        }
    });
    document.body.classList.add('patient-services-open');
    const fitWorkspace = () => {
        const top = root.getBoundingClientRect().top;
        root.style.height = `${Math.max(320, window.innerHeight - top - 10)}px`;
    };
    fitWorkspace();
    window.addEventListener('resize', fitWorkspace);

    async function initialize() {
        if (istekId) {
            try {
                const rows = await json(`/xeste-xidmetleri/${gelisId}/siyahi?istekId=${istekId}`);
                rows.forEach(x => selected.set(`existing-${x.xeste_xidmet_id}`, {
                    ...newService({
                        id: x.xidmet_id,
                        kod: x.xidmet_kodu,
                        ad: x.xidmet_adi,
                        qiymet: x.vahid_qiymet
                    }),
                    xesteXidmetId: x.xeste_xidmet_id,
                    tarix: String(x.xidmet_tarixi).slice(0, 10),
                    sobeId: x.sobe_id,
                    sobeAdi: x.sobe_adi,
                    gonderenHekimId: x.gonderen_hekim_id,
                    gonderenHekimAdi: x.gonderen_hekim_adi,
                    isteyenHekimId: x.isteyen_hekim_id,
                    isteyenHekimAdi: x.isteyen_hekim_adi,
                    icraEdenHekimId: x.icra_eden_hekim_id,
                    icraEdenHekimAdi: x.icra_eden_hekim_adi,
                    miqdar: x.miqdar,
                    tecili: x.tecili,
                    aciqlama: x.aciqlama,
                    rutinId: x.rutin_id,
                    locked: x.edit_oluna_biler === false,
                    removable: x.siline_biler !== false
                }));
                renderSelected();
            } catch (e) {
                showError(e.message);
                return;
            }
        }
        loadCatalog();
    }

    initialize();
});
