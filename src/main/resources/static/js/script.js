// Утилитные функции
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

// Основная логика
document.addEventListener('DOMContentLoaded', function () {
    // --- Логика для кнопки авторизации/кабинета/админки ---
    const authActionButton = document.getElementById('auth-action-button');
    if (authActionButton) {
        fetch('/check-auth')
            .then(response => {
                if (!response.ok) {
                    console.error('Ошибка сети при запросе /check-auth:', response.statusText);
                    authActionButton.onclick = function () { window.location.href = '/login'; };
                    throw new Error('Net err');
                }
                return response.json();
            })
            .then(data => {
                if (data.authenticated) {
                    fetch('/check-admin')
                        .then(response => {
                            if (!response.ok) {
                                console.error('Ошибка сети при запросе /check-admin:', response.statusText);
                                authActionButton.onclick = function () { window.location.href = '/cabinet'; };
                                throw new Error('Net err');
                            }
                            return response.json();
                        })
                        .then(adminData => {
                            authActionButton.onclick = function () {
                                window.location.href = adminData.isAdmin ? '/admin/panel' : '/cabinet';
                            };
                        })
                        .catch(error => {
                            console.error('Ошибка /check-admin:', error.message);
                            authActionButton.onclick = function () { window.location.href = '/cabinet'; };
                        });
                } else {
                    authActionButton.onclick = function () { window.location.href = '/login'; };
                }
            })
            .catch(error => {
                console.error('Ошибка /check-auth:', error.message);
                authActionButton.onclick = function () { window.location.href = '/login'; };
            });
    }

    // --- Элементы новой информационной панели ---
    const infoPanel = document.getElementById('info-panel-organization');
    const infoPanelCloseBtn = document.getElementById('info-panel-close-btn');
    const infoPanelImagePlaceholder = document.getElementById('info-panel-image-placeholder');
    const infoPanelName = document.getElementById('info-panel-name');
    const infoPanelEmail = document.getElementById('info-panel-email');
    const infoPanelAddress = document.getElementById('info-panel-address');
    const infoPanelPhone = document.getElementById('info-panel-phone');
    const infoPanelDiagnosis = document.getElementById('info-panel-diagnosis');
    const infoPanelAge = document.getElementById('info-panel-age');
    const currentMarkerData = { id: null };
    const categoryMap = {
        educational: 'Образовательные услуги',
        social: 'Социальные услуги',
        medical: 'Медицинские услуги',
        legal: 'Юридические услуги',
        psychological: 'Психологические услуги',
        commercial: 'Услуги на коммерческой основе'
    };

    // --- Логика Яндекс.Карт ---
    let map; // Объявляем map в более широкой области видимости
    ymaps.ready(function () {
        map = new ymaps.Map("map", {
            center: [46.3494, 48.0492],
            zoom: 12,
            controls: []
        });

        function addMarkerToMapWithData(coords, markerData) {
            var placemark = new ymaps.Placemark(coords, {
                hintContent: markerData.name || 'Детали организации',
            }, {});

            placemark.properties.set('markerData', markerData);

            placemark.events.add('click', function (e) {
                const clickedMarkerData = e.get('target').properties.get('markerData');
                showInfoPanel(clickedMarkerData);
            });

            map.geoObjects.add(placemark);
        }

        fetch('/api/map-points')
            .then(response => {
                if (!response.ok) throw new Error('Network response for /api/map-points was not ok');
                return response.json();
            })
            .then(data => {
                map.geoObjects.removeAll();
                data.forEach(markerItem => {
                    if (markerItem.latitude != null && markerItem.longitude != null) {
                        addMarkerToMapWithData([markerItem.latitude, markerItem.longitude], markerItem);
                    } else if (markerItem.address) {
                        ymaps.geocode(markerItem.address)
                            .then(function (res) {
                                var firstGeoObject = res.geoObjects.get(0);
                                if (firstGeoObject) {
                                    addMarkerToMapWithData(firstGeoObject.geometry.getCoordinates(), markerItem);
                                } else {
                                    console.error(`Адрес не найден (геокод): ${markerItem.address}`);
                                }
                            })
                            .catch(function (err) {
                                console.error('Ошибка геокодирования:', markerItem.address, err);
                            });
                    } else {
                        console.warn('Маркер без координат и адреса:', markerItem);
                    }
                });
            })
            .catch(error => console.error('Ошибка загрузки маркеров:', error));

        // Логика фильтров и сайдбара
        function applyFilters() {
            var activeDiagnosisFilters = Array.from(document.querySelectorAll('.filter-item.service-category-item.active'))
                .map(item => item.dataset.value);
            var activeAgeFilters = Array.from(document.querySelectorAll('.filter-subitem.active'))
                .map(item => item.dataset.value);
            var searchQuery = document.querySelector('.search-input').value.toLowerCase();

            map.geoObjects.each(function (geoObject) {
                const placemarkData = geoObject.properties.get('markerData');
                if (!placemarkData) {
                    geoObject.options.set('visible', false);
                    return;
                }

                var markerDiagnosis = placemarkData.diagnosis;
                var markerAge = placemarkData.age;
                var markerName = placemarkData.name;

                var searchMatch = true;
                if (searchQuery.length > 0) {
                    searchMatch = markerName && markerName.toLowerCase().includes(searchQuery);
                }

                var diagnosisMatch = true;
                if (activeDiagnosisFilters.length > 0) {
                    diagnosisMatch = activeDiagnosisFilters.includes(markerDiagnosis);
                }

                var ageMatch = true;
                if (activeAgeFilters.length > 0) {
                    ageMatch = activeAgeFilters.includes(markerAge);
                }

                let isVisible = searchMatch;
                if (activeDiagnosisFilters.length > 0) isVisible = isVisible && diagnosisMatch;
                if (activeAgeFilters.length > 0) {
                    if (activeDiagnosisFilters.length > 0 && diagnosisMatch) {
                        isVisible = isVisible && ageMatch;
                    } else if (activeDiagnosisFilters.length === 0) {
                        isVisible = isVisible && ageMatch;
                    }
                }
                geoObject.options.set('visible', isVisible);
            });
        }

        document.querySelectorAll('.filter-item.service-category-item').forEach(function (item) {
            item.addEventListener('click', function () {
                const subcategoryList = this.nextElementSibling;
                this.classList.toggle('active');
                if (subcategoryList && subcategoryList.classList.contains('filter-subcategory')) {
                    subcategoryList.classList.toggle('active', this.classList.contains('active'));
                    if (!this.classList.contains('active')) {
                        subcategoryList.querySelectorAll('.filter-subitem.active').forEach(sub => sub.classList.remove('active'));
                    }
                }
                applyFilters();
            });
        });

        document.querySelectorAll('.filter-subitem').forEach(function (subitem) {
            subitem.addEventListener('click', function () {
                this.classList.toggle('active');
                const parentCategoryDiv = this.closest('.filter-subcategory');
                if (parentCategoryDiv) {
                    const parentFilterItem = parentCategoryDiv.previousElementSibling;
                    if (this.classList.contains('active') && parentFilterItem && !parentFilterItem.classList.contains('active')) {
                        parentFilterItem.classList.add('active');
                        parentCategoryDiv.classList.add('active');
                    }
                }
                applyFilters();
            });
        });

        document.getElementById('locate-user-button').addEventListener('click', function () {
            if (!navigator.geolocation) {
                showNotification("Геолокация не поддерживается вашим браузером", "error");
                return;
            }

            showNotification("Получаем ваше местоположение...", "info");

            navigator.geolocation.getCurrentPosition(
                function (position) {
                    const latitude = position.coords.latitude;
                    const longitude = position.coords.longitude;

                    // Центрируем карту
                    map.setCenter([latitude, longitude], 15);

                    // Убираем старый маркер, если он есть
                    if (window.userLocationPlacemark) {
                        map.geoObjects.remove(window.userLocationPlacemark);
                    }

                    // Создаём новый маркер
                    window.userLocationPlacemark = new ymaps.Placemark(
                        [latitude, longitude],
                        {
                            hintContent: 'Вы здесь',
                            balloonContent: 'Ваше текущее местоположение'
                        },
                        {
                            preset: 'islands#blueCircleIcon'
                        }
                    );

                    map.geoObjects.add(window.userLocationPlacemark);
                    showNotification("Найдено! Карту перемещено к вам", "success");
                },
                function (error) {
                    let errorMessage;
                    switch (error.code) {
                        case error.PERMISSION_DENIED:
                            errorMessage = "Пользователь запретил определение геолокации";
                            break;
                        case error.POSITION_UNAVAILABLE:
                            errorMessage = "Информация о местоположении недоступна";
                            break;
                        case error.TIMEOUT:
                            errorMessage = "Превышено время ожидания ответа от сервиса геолокации";
                            break;
                        default:
                            errorMessage = "Неизвестная ошибка";
                            break;
                    }
                    showNotification("Ошибка геолокации: " + errorMessage, "error");
                }
            );
        });

        document.querySelector('.search-input').addEventListener('input', applyFilters);

        const toggleSidebarButton = document.getElementById('toggle-sidebar');
        if (toggleSidebarButton) {
            toggleSidebarButton.addEventListener('click', function () {
                const sidebar = document.getElementById('sidebar');
                const mainContentEl = document.getElementById('main-content');

                if (infoPanel && infoPanel.classList.contains('active')) {
                    hideInfoPanel();
                }

                sidebar.classList.toggle('active');
                if (mainContentEl) mainContentEl.classList.toggle('sidebar-active');

                setTimeout(function () {
                    if (map && map.container) map.container.fitToViewport();
                }, 350);
            });
        }
    });

    // Функция для отображения инфо-панели
    function showInfoPanel(markerData) {
        console.log("markerData:", markerData);
        console.log("typeof imageUrl:", typeof markerData.imageUrl);
        if (!infoPanel || !markerData) return;

        currentMarkerData.id = markerData.id;
        infoPanelName.textContent = markerData.name || 'Название не указано';
        infoPanelAddress.textContent = markerData.address || 'Адрес не указан';
        infoPanelPhone.textContent = markerData.phone || '(нет данных)';
        infoPanelEmail.textContent = markerData.email || '(нет данных)';
        infoPanelDiagnosis.textContent = categoryMap[markerData.diagnosis] || '(нет данных)';
        infoPanelAge.textContent = markerData.age || '(нет данных)';

        // Обновляем счётчик просмотров на клиенте
        const viewCount = markerData.views !== undefined ? markerData.views : 0;
        document.getElementById('info-panel-views').textContent = viewCount;

        // Отправляем запрос на сервер для увеличения счётчика
        if (markerData.id) {
            fetch(`/api/map-points/${markerData.id}/increment-view`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    //'X-CSRF-TOKEN': getCsrfToken()
                }
            })
            .then(response => response.json())
            .then(updatedData => {
                document.getElementById('info-panel-views').textContent = updatedData.views;
            })
            .catch(err => console.error('Ошибка увеличения просмотров:', err));
        }

        const imagePlaceholder = document.getElementById('info-panel-image-placeholder');

        let imageUrl = null;
        if (typeof markerData.imageUrl === 'string') {
            imageUrl = markerData.imageUrl;
        } else if (markerData.imageUrl && typeof markerData.imageUrl.url === 'string') {
            imageUrl = markerData.imageUrl.url;
        }

        if (imageUrl) {
            imagePlaceholder.innerHTML = `
                <img src="${imageUrl}" alt="Фото ${markerData.name}" class="info-img" onclick="openImageModal('${imageUrl}')">
            `;
        } else {
            imagePlaceholder.innerHTML = '<span>Фото</span>';
        }

        const sidebar = document.getElementById('sidebar');
        const mainContentElement = document.getElementById('main-content');

        if (sidebar && sidebar.classList.contains('active')) {
            sidebar.classList.remove('active');
            if (mainContentElement) mainContentElement.classList.remove('sidebar-active');
            if (map && map.container) setTimeout(() => map.container.fitToViewport(), 310);
        }

        infoPanel.classList.add('active');
        loadReviews(markerData.id);
    }

    function hideInfoPanel() {
        if (!infoPanel) return;
        infoPanel.classList.remove('active');
        if (map && map.container) setTimeout(() => map.container.fitToViewport(), 50);
    }

    if (infoPanelCloseBtn) {
        infoPanelCloseBtn.addEventListener('click', hideInfoPanel);
    }

    // --- Логика вкладок ---
    document.querySelectorAll('.info-tab').forEach(tab => {
        tab.addEventListener('click', function () {
            document.querySelectorAll('.info-tab').forEach(t => t.classList.remove('active'));
            this.classList.add('active');

            document.querySelectorAll('.info-tab-content').forEach(content => {
                content.classList.remove('active');
            });

            const tabId = this.getAttribute('data-tab');
            document.getElementById(`${tabId}-tab-content`).classList.add('active');
        });
    });

    // --- Логика отзывов ---
    document.querySelectorAll('.rating-stars i').forEach(star => {
        star.addEventListener('click', function () {
            const rating = parseInt(this.getAttribute('data-rating'));
            const stars = this.parentElement.querySelectorAll('i');
            stars.forEach((s, index) => {
                s.classList.toggle('active', index < rating);
            });
        });
    });

    document.getElementById('review-text')?.addEventListener('input', function () {
        const count = this.value.length;
        const charCountElement = document.getElementById('char-count');
        if (charCountElement) {
            charCountElement.textContent = `${count} / 500`;
            if (count > 500) {
                charCountElement.style.color = 'red';
            } else {
                charCountElement.style.color = '#666';
            }
        }
    });

    document.querySelectorAll('.review-item').forEach(reviewItem => {
        const reviewText = reviewItem.querySelector('.review-text');
        const showMoreButton = reviewItem.querySelector('.show-more');

        if (!showMoreButton) return;

        showMoreButton.addEventListener('click', () => {
            if (reviewText.style.display === 'none') {
                reviewText.style.display = '';
                showMoreButton.textContent = 'Скрыть';
            } else {
                reviewText.style.display = 'none';
                showMoreButton.textContent = 'Показать больше';
            }
        });
    });

    // Обработчик отправки отзыва
    document.getElementById('submit-review')?.addEventListener('click', function () {
        const reviewText = document.getElementById('review-text').value.trim();
        const stars = document.querySelectorAll('.rating-stars i.active').length;

        if (!reviewText) {
            showNotification('Пожалуйста, напишите текст отзыва', 'error');
            return;
        }

        if (reviewText.length > 500) {
            showNotification('Текст отзыва не должен превышать 500 символов', 'error');
            return;
        }

        if (stars === 0) {
            showNotification('Пожалуйста, поставьте оценку', 'error');
            return;
        }

        fetch('/check-auth')
            .then(response => {
                if (!response.ok) throw new Error('Auth check failed');
                return response.json();
            })
            .then(data => {
                if (data.authenticated) {
                    submitReview(currentMarkerData.id, reviewText, stars);
                } else {
                    showNotification('Для отправки отзыва необходимо войти в систему', 'error');
                    window.location.href = '/login?redirect=' + encodeURIComponent(window.location.pathname);
                }
            })
            .catch(error => {
                console.error('Auth check error:', error);
                showNotification('Ошибка проверки авторизации', 'error');
            });
    });

    // Функция для загрузки отзывов
    function loadReviews(markerId) {
        fetch(`/api/reviews/marker/${markerId}`)
            .then(response => {
                if (!response.ok) {
                    throw new Error(`Failed to load reviews: ${response.status} ${response.statusText}`);
                }

                const contentType = response.headers.get("content-type");
                if (!contentType || !contentType.includes("application/json")) {
                    console.warn("Ответ не JSON — возможно, перенаправление на /login");
                    throw new Error("Неавторизованный доступ");
                }

                return response.json();
            })
            .then(reviews => {
                const reviewsList = document.querySelector('.reviews-list');

                if (reviews.length === 0) {
                    reviewsList.innerHTML = '<p class="no-reviews-message">Пока нет отзывов. Будьте первым!</p>';
                    return;
                }

                reviewsList.innerHTML = '';
                reviews.forEach(review => {
                    addReviewToUI(review.userName, review.text, review.rating, review.createdAt);
                });
            })
            .catch(error => {
                console.error('Error loading reviews:', error);

                const reviewsList = document.querySelector('.reviews-list');
                reviewsList.innerHTML = '';

                if (error.message.includes('Неавторизованный доступ')) {
                    showNotification('Для просмотра отзывов необходимо войти в систему', 'error');
                    window.location.href = '/login?redirect=' + encodeURIComponent(window.location.pathname);
                } else {
                    const message = document.createElement('p');
                    message.className = 'no-reviews-message';
                    message.textContent = 'Ошибка загрузки отзывов';
                    reviewsList.appendChild(message);
                }
            });
    }

    // Функция для отправки отзыва
    function submitReview(markerId, text, rating) {
        const reviewData = {
            text: text,
            rating: rating,
            marker: { id: markerId }
        };

        fetch('/api/reviews', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'X-CSRF-TOKEN': getCsrfToken()
            },
            body: JSON.stringify(reviewData)
        })
            .then(response => {
                if (!response.ok) {
                    return response.json().then(err => {
                        throw new Error(err.message || 'Failed to submit review');
                    });
                }
                return response.json();
            })
            .then(review => {
                addReviewToUI('Вы', review.text, review.rating, review.createdAt);
                document.getElementById('review-text').value = '';
                document.querySelectorAll('.rating-stars i').forEach(star => {
                    star.classList.remove('active');
                });
                showNotification('Ваш отзыв добавлен!', 'success');
            })
            .catch(error => {
                console.error('Error submitting review:', error);
                showNotification(error.message || 'Ошибка при отправке отзыва', 'error');
            });
    }

    // Функция для добавления отзыва в интерфейс
    function addReviewToUI(author, text, rating, date) {
        const reviewsList = document.querySelector('.reviews-list');
        const noReviewsMsg = document.querySelector('.no-reviews-message');
        if (noReviewsMsg) {
            noReviewsMsg.remove();
        }

        const reviewItem = document.createElement('div');
        reviewItem.className = 'review-item';

        const formattedDate = new Date(date).toLocaleDateString('ru-RU', {
            year: 'numeric',
            month: 'long',
            day: 'numeric'
        });

        reviewItem.innerHTML = `
            <div class="review-author">${author}
                <span class="review-rating">${'★'.repeat(rating)}${'☆'.repeat(5 - rating)}</span>
            </div>
            <div class="review-text">${text}</div>
            <button class="show-more-btn">Показать больше</button>
            <div class="review-date">${formattedDate}</div>
        `;

        const reviewTextEl = reviewItem.querySelector('.review-text');
        const showMoreBtn = reviewItem.querySelector('.show-more-btn');

        if (showMoreBtn) {
            showMoreBtn.style.display = 'none';
        }

        if (text.length > 50) {
            if (showMoreBtn) {
                showMoreBtn.style.display = 'inline-block';
            }

            showMoreBtn.addEventListener('click', function () {
                const isTruncated = !reviewTextEl.classList.contains('full');

                if (isTruncated) {
                    reviewTextEl.classList.add('full');
                    this.textContent = 'Скрыть';
                } else {
                    reviewTextEl.classList.remove('full');
                    this.textContent = 'Показать больше';
                }
            });
        } else {
            if (showMoreBtn) {
                showMoreBtn.remove();
            }
        }

        reviewsList.prepend(reviewItem);
    }

    // --- Логика чат-бота ---
    const chatbotContainer = document.getElementById('chatbot-container');
    const chatbotToggle = document.querySelector('.chatbot-toggle');
    const chatbotClose = document.querySelector('.chatbot-close');
    const chatbotWindow = document.querySelector('.chatbot-window');
    const chatbotSend = document.getElementById('chatbot-send');
    const chatbotText = document.getElementById('chatbot-text');
    const chatbotMessages = document.getElementById('chatbot-messages');

    if (chatbotToggle && chatbotClose && chatbotWindow) {
        chatbotToggle.addEventListener('click', () => {
            chatbotWindow.classList.toggle('active');
        });

        chatbotClose.addEventListener('click', () => {
            chatbotWindow.classList.remove('active');
        });

        chatbotSend.addEventListener('click', () => {
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
        });
        
        function sendChatMessage(text, userId) {
            const messageData = {
                text: text,
                userId: userId,
                timestamp: new Date().toISOString()
            };

            fetch('/api/chat/messages', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                    // Убираем 'X-CSRF-TOKEN': getCsrfToken(), так как CSRF-защита отключена для этого эндпоинта
                },
                body: JSON.stringify(messageData),
                credentials: 'include' // Убедимся, что куки (например, JSESSIONID) отправляются для аутентификации
            })
                .then(response => {
                    if (!response.ok) {
                        return response.json().then(err => {
                            throw new Error(err.message || `Failed to send message: ${response.statusText}`);
                        });
                    }
                    return response.json();
                })
                .then(message => {
                    addChatMessageToUI('user', message.text, message.timestamp);
                    chatbotText.value = '';
                    showNotification('Сообщение отправлено!', 'success');
                    pollAdminResponses(message.id);
                })
                .catch(error => {
                    console.error('Error sending message:', error);
                    showNotification(error.message || 'Ошибка при отправке сообщения', 'error');
                });
        }

        function addChatMessageToUI(sender, text, timestamp) {
            const messageDiv = document.createElement('div');
            messageDiv.className = `chatbot-message ${sender}`;
            messageDiv.textContent = text;

            const formattedDate = new Date(timestamp).toLocaleString('ru-RU', {
                day: 'numeric',
                month: 'short',
                hour: '2-digit',
                minute: '2-digit'
            });
            const timeSpan = document.createElement('span');
            timeSpan.className = 'chatbot-message-time';
            timeSpan.textContent = formattedDate;
            timeSpan.style.fontSize = '12px';
            timeSpan.style.color = '#868e96';
            timeSpan.style.display = 'block';
            timeSpan.style.marginTop = '5px';

            messageDiv.appendChild(timeSpan);
            chatbotMessages.appendChild(messageDiv);
            chatbotMessages.scrollTop = chatbotMessages.scrollHeight;
        }

        function pollAdminResponses(messageId) {
            const interval = setInterval(() => {
                fetch(`/api/chat/messages/${messageId}/responses`)
                    .then(response => {
                        if (!response.ok) throw new Error('Failed to fetch responses');
                        return response.json();
                    })
                    .then(responses => {
                        responses.forEach(response => {
                            addChatMessageToUI('admin', response.text, response.timestamp);
                        });
                        if (responses.length > 0) {
                            clearInterval(interval);
                        }
                    })
                    .catch(error => {
                        console.error('Error polling responses:', error);
                    });
            }, 5000);
        }
    }
});