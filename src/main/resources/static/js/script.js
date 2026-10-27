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
                            isAdmin = !!adminData.isAdmin;
                            revealAdminUI();
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

    // --- Состояние для админ-функций (редактирование карты и карточек) ---
    let isAdmin = false;
    const markersById = {}; // id -> { placemark, data }
    let editModeActive = false;
    let pendingDeleteId = null;
    let currentCardImages = [];
    let currentCardImageIndex = 0;

    function revealAdminUI() {
        const editModeButton = document.getElementById('admin-edit-mode-button');
        if (editModeButton) editModeButton.style.display = isAdmin ? 'inline-block' : 'none';
        const editBtn = document.getElementById('info-panel-edit-btn');
        if (editBtn) editBtn.style.display = (isAdmin && currentMarkerData.id) ? 'inline-flex' : 'none';
    }

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
                if (editModeActive && isAdmin) {
                    openMarkerActionModal(clickedMarkerData);
                } else {
                    showInfoPanel(clickedMarkerData);
                }
            });

            map.geoObjects.add(placemark);

            if (markerData.id != null) {
                markersById[markerData.id] = { placemark: placemark, data: markerData };
            }

            return placemark;
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

        // ==================== Режим редактирования карты (только для админов) ====================
        const editModeButton = document.getElementById('admin-edit-mode-button');
        const editModeHint = document.getElementById('edit-mode-hint');
        const mapContainerEl = document.getElementById('map-container');

        function setEditMode(active) {
            editModeActive = active;
            if (editModeButton) {
                editModeButton.classList.toggle('active', active);
                editModeButton.innerHTML = active
                    ? '<i class="fas fa-times"></i> Выйти из редактирования'
                    : '<i class="fas fa-pen"></i> Режим редактирования';
            }
            if (editModeHint) editModeHint.style.display = active ? 'block' : 'none';
            if (mapContainerEl) mapContainerEl.classList.toggle('edit-mode-active', active);
            if (active && infoPanel && infoPanel.classList.contains('active')) {
                hideInfoPanel();
            }
        }

        if (editModeButton) {
            editModeButton.addEventListener('click', function () {
                if (!isAdmin) return;
                setEditMode(!editModeActive);
            });
        }

        map.events.add('click', function (e) {
            if (!editModeActive || !isAdmin) return;
            const coords = e.get('coords');
            if (!coords) return;
            openMarkerFormModal({ mode: 'create', lat: coords[0], lng: coords[1] });

            // Обратное геокодирование: подставляем адрес по клику на карту
            ymaps.geocode(coords, { kind: 'house' })
                .then(function (res) {
                    const firstGeoObject = res.geoObjects.get(0);
                    if (firstGeoObject && markerFormAddress && markerFormMode === 'create') {
                        markerFormAddress.value = firstGeoObject.getAddressLine();
                    }
                })
                .catch(function (err) {
                    console.error('Ошибка обратного геокодирования:', err);
                });
        });

        // ---- Модальное окно формы маркера (создание/редактирование) ----
        const markerFormModal = document.getElementById('markerFormModal');
        const markerForm = document.getElementById('markerForm');
        const markerFormTitle = document.getElementById('markerFormTitle');
        const markerFormId = document.getElementById('markerFormId');
        const markerFormLat = document.getElementById('markerFormLat');
        const markerFormLng = document.getElementById('markerFormLng');
        const markerFormName = document.getElementById('markerFormName');
        const markerFormAddress = document.getElementById('markerFormAddress');
        const markerFormDiagnosis = document.getElementById('markerFormDiagnosis');
        const markerFormAge = document.getElementById('markerFormAge');
        const markerFormPhone = document.getElementById('markerFormPhone');
        const markerFormEmail = document.getElementById('markerFormEmail');
        const markerFormPhotos = document.getElementById('markerFormPhotos');
        const markerFormExistingImages = document.getElementById('markerFormExistingImages');
        const markerFormError = document.getElementById('markerFormError');
        const markerFormCloseBtn = document.getElementById('markerFormCloseBtn');
        const markerFormCancelBtn = document.getElementById('markerFormCancelBtn');

        let markerFormMode = 'create';
        let markerFormKeptImages = [];      // полные версии уже сохранённых фото
        let markerFormKeptThumbnails = [];  // обрезанные версии уже сохранённых фото (тот же индекс)
        let markerFormPendingPhotos = [];   // новые фото: [{ original: File, croppedBlob: Blob }]

        function closeMarkerFormModal() {
            if (markerFormModal) markerFormModal.style.display = 'none';
        }

        function renderExistingImagesInForm() {
            if (!markerFormExistingImages) return;
            markerFormExistingImages.innerHTML = '';

            markerFormKeptImages.forEach((url, idx) => {
                const thumbUrl = markerFormKeptThumbnails[idx] || url;
                const thumb = document.createElement('div');
                thumb.className = 'existing-image-thumb';
                thumb.innerHTML = `
                    <img src="${thumbUrl}" alt="Фото">
                    <button type="button" class="remove-existing-image" data-kind="existing" data-index="${idx}">×</button>
                `;
                markerFormExistingImages.appendChild(thumb);
            });

            markerFormPendingPhotos.forEach((pair, idx) => {
                const thumb = document.createElement('div');
                thumb.className = 'existing-image-thumb';
                const objectUrl = URL.createObjectURL(pair.croppedBlob);
                thumb.innerHTML = `
                    <img src="${objectUrl}" alt="Новое фото">
                    <button type="button" class="remove-existing-image" data-kind="pending" data-index="${idx}">×</button>
                `;
                markerFormExistingImages.appendChild(thumb);
            });

            markerFormExistingImages.querySelectorAll('.remove-existing-image').forEach(btn => {
                btn.addEventListener('click', function () {
                    const idx = parseInt(this.getAttribute('data-index'), 10);
                    if (this.getAttribute('data-kind') === 'pending') {
                        markerFormPendingPhotos.splice(idx, 1);
                    } else {
                        markerFormKeptImages.splice(idx, 1);
                        markerFormKeptThumbnails.splice(idx, 1);
                    }
                    renderExistingImagesInForm();
                });
            });
        }

        function openMarkerFormModal(options) {
            if (!markerFormModal) return;
            markerFormError.style.display = 'none';
            markerFormError.textContent = '';
            markerForm.reset();
            markerFormPhotos.value = '';
            markerFormPendingPhotos = [];

            if (options.mode === 'edit' && options.data) {
                markerFormMode = 'edit';
                markerFormTitle.textContent = 'Редактировать организацию';
                markerFormId.value = options.data.id;
                markerFormLat.value = options.data.latitude;
                markerFormLng.value = options.data.longitude;
                markerFormName.value = options.data.name || '';
                markerFormAddress.value = options.data.address || '';
                markerFormDiagnosis.value = options.data.diagnosis || '';
                markerFormAge.value = options.data.age || '';
                markerFormPhone.value = options.data.phone || '';
                markerFormEmail.value = options.data.email || '';
                markerFormKeptImages = Array.isArray(options.data.imageUrls) && options.data.imageUrls.length
                    ? options.data.imageUrls.slice()
                    : (options.data.imageUrl ? [options.data.imageUrl] : []);
                markerFormKeptThumbnails = Array.isArray(options.data.thumbnailUrls) && options.data.thumbnailUrls.length
                    ? options.data.thumbnailUrls.slice()
                    : markerFormKeptImages.slice();
            } else {
                markerFormMode = 'create';
                markerFormTitle.textContent = 'Новая организация';
                markerFormId.value = '';
                markerFormLat.value = options.lat;
                markerFormLng.value = options.lng;
                markerFormKeptImages = [];
                markerFormKeptThumbnails = [];
            }
            renderExistingImagesInForm();
            markerFormModal.style.display = 'block';
        }

        if (markerFormCloseBtn) markerFormCloseBtn.addEventListener('click', closeMarkerFormModal);
        if (markerFormCancelBtn) markerFormCancelBtn.addEventListener('click', closeMarkerFormModal);

        // ---- Обрезка фото под размер карточки (Cropper.js) ----
        const cropPhotoModal = document.getElementById('cropPhotoModal');
        const cropModalImage = document.getElementById('cropModalImage');
        const cropModalConfirmBtn = document.getElementById('cropModalConfirmBtn');
        const cropModalSkipBtn = document.getElementById('cropModalSkipBtn');

        let cropQueue = [];
        let cropperInstance = null;
        let cropCurrentObjectUrl = null;

        function getCardAspectRatio() {
            const el = document.getElementById('info-panel-image-placeholder');
            if (el && el.clientWidth && el.clientHeight) {
                return el.clientWidth / el.clientHeight;
            }
            return 310 / 200; // запасное значение, если панель ещё ни разу не рендерилась
        }

        function destroyCropper() {
            if (cropperInstance) {
                cropperInstance.destroy();
                cropperInstance = null;
            }
            if (cropCurrentObjectUrl) {
                URL.revokeObjectURL(cropCurrentObjectUrl);
                cropCurrentObjectUrl = null;
            }
        }

        function processNextInCropQueue() {
            if (!cropQueue.length) {
                if (cropPhotoModal) cropPhotoModal.style.display = 'none';
                destroyCropper();
                renderExistingImagesInForm();
                return;
            }
            const file = cropQueue.shift();
            if (cropCurrentObjectUrl) {
                URL.revokeObjectURL(cropCurrentObjectUrl);
            }
            cropCurrentObjectUrl = URL.createObjectURL(file);
            cropModalImage.src = cropCurrentObjectUrl;
            if (cropPhotoModal) cropPhotoModal.style.display = 'block';

            destroyCropperKeepModalOpen();

            cropModalImage.onload = function () {
                cropperInstance = new Cropper(cropModalImage, {
                    aspectRatio: getCardAspectRatio(),
                    viewMode: 1,
                    dragMode: 'move',
                    autoCropArea: 1,
                    background: false,
                    responsive: true,
                    zoomOnWheel: true
                });
            };

            cropModalConfirmBtn.onclick = function () {
                if (!cropperInstance) return;
                const ratio = getCardAspectRatio();
                const outputWidth = 800;
                const outputHeight = Math.round(outputWidth / ratio);
                cropperInstance.getCroppedCanvas({ width: outputWidth, height: outputHeight }).toBlob(function (blob) {
                    if (blob) {
                        markerFormPendingPhotos.push({ original: file, croppedBlob: blob });
                    }
                    processNextInCropQueue();
                }, 'image/jpeg', 0.9);
            };

            cropModalSkipBtn.onclick = function () {
                processNextInCropQueue();
            };
        }

        // Уничтожаем предыдущий инстанс cropper, не закрывая модалку (между фото в очереди)
        function destroyCropperKeepModalOpen() {
            if (cropperInstance) {
                cropperInstance.destroy();
                cropperInstance = null;
            }
        }

        if (markerFormPhotos) {
            markerFormPhotos.addEventListener('change', function () {
                if (!this.files || !this.files.length) return;
                cropQueue = Array.from(this.files);
                markerFormPhotos.value = '';
                processNextInCropQueue();
            });
        }

        if (markerForm) {
            markerForm.addEventListener('submit', function (e) {
                e.preventDefault();
                const name = markerFormName.value.trim();
                if (!name) {
                    markerFormError.textContent = 'Название обязательно';
                    markerFormError.style.display = 'block';
                    return;
                }

                const formData = new FormData();
                formData.append('name', name);
                formData.append('address', markerFormAddress.value.trim());
                formData.append('latitude', markerFormLat.value);
                formData.append('longitude', markerFormLng.value);
                formData.append('diagnosis', markerFormDiagnosis.value);
                formData.append('age', markerFormAge.value);
                formData.append('phone', markerFormPhone.value.trim());
                formData.append('email', markerFormEmail.value.trim());

                if (markerFormMode === 'edit') {
                    markerFormKeptImages.forEach(url => formData.append('existingImages', url));
                    markerFormKeptThumbnails.forEach(url => formData.append('existingThumbnails', url));
                }
                markerFormPendingPhotos.forEach((pair, i) => {
                    formData.append('photos', pair.original);
                    formData.append('thumbnails', pair.croppedBlob, `thumb_${i}.jpg`);
                });

                const requestUrl = markerFormMode === 'edit'
                    ? `/api/map-points/${markerFormId.value}`
                    : '/api/map-points';
                const method = markerFormMode === 'edit' ? 'PUT' : 'POST';

                fetch(requestUrl, { method: method, body: formData, credentials: 'include' })
                    .then(response => {
                        if (!response.ok) {
                            return response.json()
                                .catch(() => null)
                                .then(err => {
                                    throw new Error((err && err.message) || 'Не удалось сохранить организацию');
                                });
                        }
                        return response.json();
                    })
                    .then(savedMarker => {
                        if (markerFormMode === 'edit') {
                            const entry = markersById[savedMarker.id];
                            if (entry) {
                                entry.data = savedMarker;
                                entry.placemark.properties.set('markerData', savedMarker);
                                entry.placemark.properties.set('hintContent', savedMarker.name);
                            }
                            if (currentMarkerData.id === savedMarker.id) {
                                populateInfoPanelContent(savedMarker);
                            }
                            showNotification('Организация обновлена', 'success');
                        } else {
                            addMarkerToMapWithData([savedMarker.latitude, savedMarker.longitude], savedMarker);
                            showNotification('Организация добавлена на карту', 'success');
                        }
                        markerFormPendingPhotos = [];
                        closeMarkerFormModal();
                    })
                    .catch(error => {
                        markerFormError.textContent = error.message;
                        markerFormError.style.display = 'block';
                    });
            });
        }

        // ---- Модальное окно выбора действия над маркером (изменить/удалить) ----
        const markerActionModal = document.getElementById('markerActionModal');
        const markerActionTitle = document.getElementById('markerActionTitle');
        const markerActionEditBtn = document.getElementById('markerActionEditBtn');
        const markerActionDeleteBtn = document.getElementById('markerActionDeleteBtn');
        const markerActionCancelBtn = document.getElementById('markerActionCancelBtn');
        let markerActionCurrentData = null;


        function openMarkerActionModal(markerData) {
            if (!markerActionModal) return;
            markerActionCurrentData = markerData;
            markerActionTitle.textContent = markerData.name || 'Организация';
            markerActionModal.style.display = 'block';
        }

        function closeMarkerActionModal() {
            if (markerActionModal) markerActionModal.style.display = 'none';
        }

        if (markerActionCancelBtn) markerActionCancelBtn.addEventListener('click', closeMarkerActionModal);
        if (markerActionEditBtn) {
            markerActionEditBtn.addEventListener('click', function () {
                closeMarkerActionModal();
                if (markerActionCurrentData) {
                    openMarkerFormModal({ mode: 'edit', data: markerActionCurrentData });
                }
            });
        }
        if (markerActionDeleteBtn) {
            markerActionDeleteBtn.addEventListener('click', function () {
                closeMarkerActionModal();
                if (markerActionCurrentData) {
                    openConfirmDeleteModal(markerActionCurrentData.id, markerActionCurrentData.name);
                }
            });
        }

        // ---- Модальное окно подтверждения удаления ----
        const confirmDeleteModal = document.getElementById('confirmDeleteModal');
        const confirmDeleteText = document.getElementById('confirmDeleteText');
        const confirmDeleteOkBtn = document.getElementById('confirmDeleteOkBtn');
        const confirmDeleteCancelBtn = document.getElementById('confirmDeleteCancelBtn');

        function openConfirmDeleteModal(markerId, markerName) {
            if (!confirmDeleteModal) return;
            pendingDeleteId = markerId;
            confirmDeleteText.textContent = `Удалить организацию «${markerName || ''}» с карты? Это действие необратимо.`;
            confirmDeleteModal.style.display = 'block';
        }

        function closeConfirmDeleteModal() {
            if (confirmDeleteModal) confirmDeleteModal.style.display = 'none';
            pendingDeleteId = null;
        }

        if (confirmDeleteCancelBtn) confirmDeleteCancelBtn.addEventListener('click', closeConfirmDeleteModal);
        if (confirmDeleteOkBtn) {
            confirmDeleteOkBtn.addEventListener('click', function () {
                if (!pendingDeleteId) return;
                const idToDelete = pendingDeleteId;
                fetch(`/api/map-points/${idToDelete}`, { method: 'DELETE', credentials: 'include' })
                    .then(response => {
                        if (!response.ok) throw new Error('Не удалось удалить организацию');
                        return response.json().catch(() => ({}));
                    })
                    .then(() => {
                        const entry = markersById[idToDelete];
                        if (entry) {
                            map.geoObjects.remove(entry.placemark);
                            delete markersById[idToDelete];
                        }
                        if (currentMarkerData.id === idToDelete) {
                            hideInfoPanel();
                        }
                        showNotification('Организация удалена', 'success');
                        closeConfirmDeleteModal();
                    })
                    .catch(error => {
                        showNotification(error.message, 'error');
                        closeConfirmDeleteModal();
                    });
            });
        }

        // ---- Кнопка редактирования на карточке организации ----
        const infoPanelEditBtn = document.getElementById('info-panel-edit-btn');
        if (infoPanelEditBtn) {
            infoPanelEditBtn.addEventListener('click', function () {
                if (!isAdmin || !currentMarkerData.id) return;
                const entry = markersById[currentMarkerData.id];
                if (entry) {
                    openMarkerFormModal({ mode: 'edit', data: entry.data });
                }
            });
        }
    });

    // Заполняет содержимое карточки организации данными (без увеличения счётчика просмотров)
    function populateInfoPanelContent(markerData) {
        if (!infoPanel || !markerData) return;

        currentMarkerData.id = markerData.id;
        infoPanelName.textContent = markerData.name || 'Название не указано';
        infoPanelAddress.textContent = markerData.address || 'Адрес не указан';
        infoPanelPhone.textContent = markerData.phone || '(нет данных)';
        infoPanelEmail.textContent = markerData.email || '(нет данных)';
        infoPanelDiagnosis.textContent = categoryMap[markerData.diagnosis] || '(нет данных)';
        infoPanelAge.textContent = markerData.age || '(нет данных)';

        const viewCount = markerData.views !== undefined ? markerData.views : 0;
        document.getElementById('info-panel-views').textContent = viewCount;

        // Собираем список фотографий: полные версии (для полноэкранного режима)
        // и обрезанные версии под размер карточки (для карусели в сайдбаре)
        let fullImages = [];
        if (Array.isArray(markerData.imageUrls) && markerData.imageUrls.length) {
            fullImages = markerData.imageUrls.filter(Boolean);
        } else if (typeof markerData.imageUrl === 'string' && markerData.imageUrl) {
            fullImages = [markerData.imageUrl];
        } else if (markerData.imageUrl && typeof markerData.imageUrl.url === 'string') {
            fullImages = [markerData.imageUrl.url];
        }

        let thumbImages = Array.isArray(markerData.thumbnailUrls) && markerData.thumbnailUrls.length
            ? markerData.thumbnailUrls.filter(Boolean)
            : fullImages;
        // На случай рассинхронизации длины — достраиваем недостающие превью полными фото
        if (thumbImages.length < fullImages.length) {
            thumbImages = fullImages.map((url, i) => thumbImages[i] || url);
        }

        renderCardCarousel(thumbImages, fullImages, markerData.name, markerData.address);

        revealAdminUI();

        const sidebar = document.getElementById('sidebar');
        const mainContentElement = document.getElementById('main-content');

        if (sidebar && sidebar.classList.contains('active')) {
            sidebar.classList.remove('active');
            if (mainContentElement) mainContentElement.classList.remove('sidebar-active');
            if (map && map.container) setTimeout(() => map.container.fitToViewport(), 310);
        }
    }

    // Функция для отображения инфо-панели
    function showInfoPanel(markerData) {
        if (!infoPanel || !markerData) return;

        populateInfoPanelContent(markerData);

        // Отправляем запрос на сервер для увеличения счётчика просмотров
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

        infoPanel.classList.add('active');
        loadReviews(markerData.id);
    }

    // --- Карусель фотографий на карточке организации ---
    // thumbImages — обрезанные версии под размер карточки (показываются в карусели)
    // fullImages — полные версии (открываются в полноэкранном режиме)
    function renderCardCarousel(thumbImages, fullImages, name, address) {
        currentCardImages = thumbImages || [];
        const fullSizeImages = (fullImages && fullImages.length === currentCardImages.length) ? fullImages : currentCardImages;
        currentCardImageIndex = 0;

        const emptyPlaceholder = document.querySelector('.info-image-empty');
        const carouselImg = document.getElementById('info-carousel-img');
        const prevBtn = document.getElementById('info-carousel-prev');
        const nextBtn = document.getElementById('info-carousel-next');
        const dotsContainer = document.getElementById('info-carousel-dots');

        if (!carouselImg) return;

        if (!currentCardImages.length) {
            carouselImg.style.display = 'none';
            if (emptyPlaceholder) emptyPlaceholder.style.display = 'flex';
            if (prevBtn) prevBtn.style.display = 'none';
            if (nextBtn) nextBtn.style.display = 'none';
            if (dotsContainer) dotsContainer.innerHTML = '';
            return;
        }

        if (emptyPlaceholder) emptyPlaceholder.style.display = 'none';
        carouselImg.style.display = 'block';

        function showCardImage(index) {
            currentCardImageIndex = (index + currentCardImages.length) % currentCardImages.length;
            carouselImg.src = currentCardImages[currentCardImageIndex];
            carouselImg.alt = 'Фото ' + (name || '');
            if (dotsContainer) {
                dotsContainer.querySelectorAll('.carousel-dot').forEach((dot, i) => {
                    dot.classList.toggle('active', i === currentCardImageIndex);
                });
            }
        }

        showCardImage(0);

        const hasMultiple = currentCardImages.length > 1;
        if (prevBtn) prevBtn.style.display = hasMultiple ? 'flex' : 'none';
        if (nextBtn) nextBtn.style.display = hasMultiple ? 'flex' : 'none';

        if (prevBtn) prevBtn.onclick = function (e) { e.stopPropagation(); showCardImage(currentCardImageIndex - 1); };
        if (nextBtn) nextBtn.onclick = function (e) { e.stopPropagation(); showCardImage(currentCardImageIndex + 1); };

        if (dotsContainer) {
            dotsContainer.innerHTML = '';
            if (hasMultiple) {
                currentCardImages.forEach((_, i) => {
                    const dot = document.createElement('button');
                    dot.type = 'button';
                    dot.className = 'carousel-dot' + (i === 0 ? ' active' : '');
                    dot.addEventListener('click', function (e) {
                        e.stopPropagation();
                        showCardImage(i);
                    });
                    dotsContainer.appendChild(dot);
                });
            }
        }

        carouselImg.onclick = function () {
            openFullscreenViewer(fullSizeImages, currentCardImageIndex, name, address);
        };
    }

    // --- Полноэкранный просмотр фотографий с перелистыванием ---
    let fullscreenImages = [];
    let fullscreenIndex = 0;
    let fullscreenName = '';
    let fullscreenAddress = '';

    function showFullscreenImage(index) {
        if (!fullscreenImages.length) return;
        fullscreenIndex = (index + fullscreenImages.length) % fullscreenImages.length;
        const modalImg = document.getElementById('modalImage');
        if (modalImg) modalImg.src = fullscreenImages[fullscreenIndex];

        const dotsContainer = document.getElementById('image-modal-dots');
        if (dotsContainer) {
            dotsContainer.querySelectorAll('.carousel-dot').forEach((dot, i) => {
                dot.classList.toggle('active', i === fullscreenIndex);
            });
        }
    }

    function openFullscreenViewer(images, startIndex, name, address) {
        const modal = document.getElementById('imageModal');
        if (!modal || !images || !images.length) return;

        fullscreenImages = images;
        fullscreenName = name || '';
        fullscreenAddress = address || '';

        const nameEl = document.getElementById('image-modal-name');
        const addressEl = document.getElementById('image-modal-address');
        if (nameEl) nameEl.textContent = fullscreenName;
        if (addressEl) addressEl.textContent = fullscreenAddress;

        const prevBtn = document.getElementById('image-modal-prev');
        const nextBtn = document.getElementById('image-modal-next');
        const hasMultiple = fullscreenImages.length > 1;
        if (prevBtn) prevBtn.style.display = hasMultiple ? 'flex' : 'none';
        if (nextBtn) nextBtn.style.display = hasMultiple ? 'flex' : 'none';

        const dotsContainer = document.getElementById('image-modal-dots');
        if (dotsContainer) {
            dotsContainer.innerHTML = '';
            if (hasMultiple) {
                fullscreenImages.forEach((_, i) => {
                    const dot = document.createElement('button');
                    dot.type = 'button';
                    dot.className = 'carousel-dot';
                    dot.addEventListener('click', () => showFullscreenImage(i));
                    dotsContainer.appendChild(dot);
                });
            }
        }

        modal.style.display = 'block';
        showFullscreenImage(startIndex || 0);
    }

    function closeFullscreenViewer() {
        const modal = document.getElementById('imageModal');
        if (modal) modal.style.display = 'none';
    }

    const imageModalCloseBtn = document.getElementById('image-modal-close-btn');
    if (imageModalCloseBtn) imageModalCloseBtn.addEventListener('click', closeFullscreenViewer);

    const imageModalPrevBtn = document.getElementById('image-modal-prev');
    const imageModalNextBtn = document.getElementById('image-modal-next');
    if (imageModalPrevBtn) imageModalPrevBtn.addEventListener('click', () => showFullscreenImage(fullscreenIndex - 1));
    if (imageModalNextBtn) imageModalNextBtn.addEventListener('click', () => showFullscreenImage(fullscreenIndex + 1));

    window.addEventListener('click', function (event) {
        const modal = document.getElementById('imageModal');
        if (modal && event.target === modal) {
            modal.style.display = 'none';
        }
        const markerFormModalEl = document.getElementById('markerFormModal');
        if (markerFormModalEl && event.target === markerFormModalEl) {
            markerFormModalEl.style.display = 'none';
        }
        const markerActionModalEl = document.getElementById('markerActionModal');
        if (markerActionModalEl && event.target === markerActionModalEl) {
            markerActionModalEl.style.display = 'none';
        }
        const confirmDeleteModalEl = document.getElementById('confirmDeleteModal');
        if (confirmDeleteModalEl && event.target === confirmDeleteModalEl) {
            confirmDeleteModalEl.style.display = 'none';
        }
    });

    document.addEventListener('keydown', function (event) {
        const modal = document.getElementById('imageModal');
        if (!modal || modal.style.display !== 'block') return;
        if (event.key === 'Escape') closeFullscreenViewer();
        if (event.key === 'ArrowLeft') showFullscreenImage(fullscreenIndex - 1);
        if (event.key === 'ArrowRight') showFullscreenImage(fullscreenIndex + 1);
    });

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