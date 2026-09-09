(() => {
    'use strict';
    const modal = document.getElementById('kassa-balance-modal');
    if (!modal || !window.bootstrap?.Modal) return;
    document.body.appendChild(modal);
    window.bootstrap.Modal.getOrCreateInstance(modal).show();
})();
