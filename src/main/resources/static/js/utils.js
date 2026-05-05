function getCsrfToken() {
    const cookieValue = document.cookie
        .split('; ')
        .find(row => row.startsWith('XSRF-TOKEN='))
        ?.split('=')[1];
    return cookieValue ? decodeURIComponent(cookieValue) : null;
}

function showNotification(message, type = 'success') {
    const container = document.getElementById('notifications-container');
    if (!container) return;
    const notification = document.createElement('div');
    notification.className = `notification ${type}-notification`;
    notification.innerHTML = `
        <span>${message}</span>
        <span class="notification-close" onclick="this.parentElement.remove()">×</span>
    `;
    container.appendChild(notification);

    setTimeout(() => {
        notification.remove();
    }, 5000);
}