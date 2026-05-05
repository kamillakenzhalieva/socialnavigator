document.addEventListener('DOMContentLoaded', function () {
    const chatbotContainer = document.getElementById('chatbot-container');
    const chatbotToggle = document.querySelector('.chatbot-toggle');
    const chatbotClose = document.querySelector('.chatbot-close');
    const chatbotWindow = document.querySelector('.chatbot-window');
    const chatbotSend = document.getElementById('chatbot-send');
    const chatbotText = document.getElementById('chatbot-text');
    const chatbotMessages = document.getELEMENTBYID('chatbot-messages');

    if (chatbotToggle && chatbotClose && chatbotWindow) {
        chatbotToggle.addEventListener('click', () => {
            chatbotWindow.classList.toggle('active');
            if (chatbotWindow.classList.contains('active')) {
                loadChatHistory();
            }
        });

        chatbotClose.addEventListener('click', () => {
            chatbotWindow.classList.remove('active');
        });

        chatbotSend.addEventListener('click', sendMessage);
        chatbotText.addEventListener('keypress', (event) => {
            if (event.key === 'Enter' && !event.shiftKey) {
                event.preventDefault();
                sendMessage();
            }
        });

        function sendMessage() {
            const messageText = chatbotText.value.trim();
            if (!messageText) {
                showNotification('Пожалуйста, введите сообщение', 'error');
                return;
            }
            if (messageText.length > 500) {
                showNotification('Сообщение не должно превышать 500 символов', 'error');
                return;
            }

            fetch('/check-auth')
                .then(response => {
                    if (!response.ok) throw new Error('Auth check failed');
                    return response.json();
                })
                .then(data => {
                    if (data.authenticated) {
                        sendChatMessage(messageText, data.userId);
                    } else {
                        showNotification('Для отправки сообщения необходимо войти в систему', 'error');
                        window.location.href = '/login?redirect=' + encodeURIComponent(window.location.pathname);
                    }
                })
                .catch(error => {
                    console.error('Auth check error:', error);
                    showNotification('Ошибка проверки авторизации', 'error');
                });
        }

        function sendChatMessage(text, userId) {
            const messageData = { text: text, userId: userId, timestamp: new Date().toISOString() };
            console.log('Sending message:', messageData);
            fetch('/api/chat/messages', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(messageData),
                credentials: 'include'
            })
            .then(response => {
                console.log('Response status:', response.status);
                if (!response.ok) return response.json().then(err => { throw new Error(err.message || 'Failed to send message'); });
                return response.json();
            })
            .then(message => {
                console.log('Message sent successfully:', message);
                addChatMessageToUI('user', message.text, message.timestamp);
                chatbotText.value = '';
                showNotification('Сообщение отправлено', 'success');
                pollAdminResponses(message.id);
            })
            .catch(error => {
                console.error('Error sending message:', error);
                showNotification(error.message || 'Ошибка при отправке сообщения', 'error');
            });
        }

        function loadChatHistory() {
            chatbotMessages.innerHTML = '';
            fetch('/api/chat/history', {
                method: 'GET',
                credentials: 'include'
            })
                .then(response => {
                    if (!response.ok) throw new Error('Failed to fetch chat history');
                    return response.json();
                })
                .then(messages => {
                    console.log('Chat history:', messages);
                    messages.forEach(message => {
                        addChatMessageToUI(message.isAdminResponse ? 'admin' : 'user', message.text, message.timestamp);
                    });
                })
                .catch(error => {
                    console.error('Error loading chat history:', error);
                    showNotification('Ошибка при загрузке истории чата', 'error');
                });
        }

        function addChatMessageToUI(sender, text, timestamp) {
            const messageDiv = document.createElement('div');
            messageDiv.className = `chatbot-message ${sender}`;
            messageDiv.innerHTML = `
                <div class="message-content">
                    <strong>${sender === 'user' ? 'Вы' : 'Администратор'}</strong>
                    <p>${text}</p>
                    <span class="timestamp">${new Date(timestamp).toLocaleString('ru-RU', {
                        day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit'
                    })}</span>
                </div>
            `;
            chatbotMessages.appendChild(messageDiv);
            chatbotMessages.scrollTop = chatbotMessages.scrollHeight;
        }

        function pollAdminResponses(messageId) {
            const interval = setInterval(() => {
                fetch(`/api/chat/messages/${messageId}/responses`, {
                    method: 'GET',
                    credentials: 'include'
                })
                    .then(response => {
                        if (!response.ok) throw new Error('Failed to fetch responses');
                        return response.json();
                    })
                    .then(responses => {
                        console.log('Responses for message', messageId, ':', responses);
                        responses.forEach(response => {
                            if (response.isAdminResponse && !chatbotMessages.querySelector(`[data-message-id="${response.id}"]`)) {
                                addChatMessageToUI('admin', response.text, response.timestamp);
                            }
                        });
                        if (responses.length > 0) clearInterval(interval);
                    })
                    .catch(error => {
                        console.error('Error polling responses:', error);
                        showNotification('Ошибка при получении ответа администратора', 'error');
                    });
            }, 5000);
        }

        const charCountDisplay = document.getElementById('char-count');
        if (charCountDisplay) {
            chatbotText.addEventListener('input', function () {
                const charCount = this.value.length;
                charCountDisplay.textContent = `${charCount} / 500`;
                if (charCount > 500) {
                    this.value = this.value.substring(0, 500);
                    charCountDisplay.textContent = '500 / 500';
                    showNotification('Максимальная длина сообщения — 500 символов', 'error');
                }
            });
        }
    }
});