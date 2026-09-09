(() => {
    'use strict';
    const form = document.getElementById('receipt-cash-form');
    const cash = document.getElementById('receipt-cash');
    if (!form || !cash) return;
    const initialCash = cash.value;
    let changing = false;
    const change = () => { if (!changing && cash.value !== initialCash) { changing = true; form.requestSubmit(); } };
    cash.addEventListener('change', change);
    if (window.jQuery) window.jQuery(cash).on('change.kassaReceiptList', change);
})();
