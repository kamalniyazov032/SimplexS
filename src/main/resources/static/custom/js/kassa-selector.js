(() => {
    const select = document.getElementById('cash-register');
    if (!select) return;
    const form = select.form;
    const initialCash = select.value;
    const links = [...document.querySelectorAll('.kassa-home a.kassa-action[href]')];
    function change() {
        if (!select.value) return;
        links.forEach(link => {
            const url = new URL(link.href, window.location.href);
            if (url.searchParams.has('kassaId')) {
                url.searchParams.set('kassaId', select.value);
                link.href = url.href;
            }
        });
        const url = new URL(form.action, window.location.href);
        url.searchParams.set('kassaId', select.value);
        window.history.replaceState(window.history.state, '', url.href);
    }
    form.addEventListener('submit', event => { event.preventDefault(); change(); });
    select.addEventListener('change', change);
    if (window.jQuery) window.jQuery(select).on('change.kassaSelector', change);
    // Previously loaded dialogs contain a token and data for the original cash desk.
    // A changed desk must follow the updated link instead of reopening that dialog.
    links.forEach(link => link.addEventListener('click', event => {
        if (select.value !== initialCash) event.stopImmediatePropagation();
    }, true));
})();
