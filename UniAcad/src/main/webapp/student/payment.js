document.getElementById('payment-form').addEventListener('submit', async (event) => {
    event.preventDefault();
    const errorMessage = document.getElementById('error-message');
    const description = encodeURIComponent("Thanh toán học phí");

    try {
        const response = await fetch('./create-payment-link', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
            },
            body: `description=${description}`
        });

        const result = await response.json();
        if (result.error === 0 && result.data.checkoutUrl) {
            window.location.href = result.data.checkoutUrl;
        } else {
            errorMessage.textContent = result.message || 'Failed to create payment link';
            errorMessage.style.display = 'block';
        }
    } catch (error) {
        errorMessage.textContent = 'Error: ' + error.message;
        errorMessage.style.display = 'block';
    }
});
