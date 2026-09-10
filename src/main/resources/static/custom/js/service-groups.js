(() => {
    const rows = [...document.querySelectorAll('#service-groups-page .service-tree-row')];
    const collapsed = new Set();
    const byId = new Map(rows.map(row => [row.dataset.id, row]));
    const summary = document.getElementById('groups-page-summary');
    if (!summary) return;
    const previous = document.getElementById('groups-previous');
    const next = document.getElementById('groups-next');
    const pages = document.getElementById('groups-pages');
    let page = 0;
    function render() {
        const visible = rows.filter(row => {
            let parent = row.dataset.parentId;
            const visited = new Set();
            while (parent && !visited.has(parent)) {
                if (collapsed.has(parent)) return false;
                visited.add(parent);
                parent = byId.get(parent)?.dataset.parentId;
            }
            return true;
        });
        const count = Math.max(1, Math.ceil(visible.length / 100));
        page = Math.min(page, count - 1);
        const shown = new Set(visible.slice(page * 100, (page + 1) * 100));
        rows.forEach(row => { row.hidden = !shown.has(row); });
        summary.textContent = summary.dataset.template.replace('{0}', page + 1).replace('{1}', count);
        previous.disabled = page === 0;
        next.disabled = page === count - 1;
        pages.replaceChildren();
        const indices = new Set([0, count - 1]);
        for (let i = Math.max(0, page - 2); i <= Math.min(count - 1, page + 2); i++) indices.add(i);
        [...indices].sort((a,b) => a-b).forEach(index => {
            const button = document.createElement('button');
            button.type = 'button';
            button.className = 'btn btn-sm btn-outline-primary' + (index === page ? ' active' : '');
            button.textContent = index + 1;
            if (index === page) button.setAttribute('aria-current', 'page');
            button.addEventListener('click', () => { page = index; render(); });
            pages.append(button);
        });
        document.querySelector('.service-groups-table-scroll').scrollTop = 0;
    }
    rows.forEach(row => {
        const toggle = row.querySelector('.service-tree-toggle');
        toggle?.addEventListener('click', () => {
            const id = row.dataset.id;
            collapsed.has(id) ? collapsed.delete(id) : collapsed.add(id);
            toggle.setAttribute('aria-expanded', String(!collapsed.has(id)));
            render();
        });
    });
    previous.addEventListener('click', () => { page--; render(); });
    next.addEventListener('click', () => { page++; render(); });
    render();
})();
