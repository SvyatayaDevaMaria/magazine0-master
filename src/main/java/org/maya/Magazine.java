package org.maya;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/")
public class Magazine {

    @GET
    @Produces(MediaType.TEXT_HTML)
    public String getInventory() {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>Магазин</title>
                <style>
                    body { font-family: Arial, sans-serif; margin: 20px; background-color: #f9f9f9; }
                    .section { margin-bottom: 30px; padding: 20px; border: 1px solid #ddd; background-color: #fff; border-radius: 5px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
                    h2 { color: #333; border-bottom: 2px solid #eee; padding-bottom: 10px; }
                    h3 { color: #555; margin-top: 20px; }
                    table { width: 100%; border-collapse: collapse; margin-bottom: 15px; }
                    th, td { border: 1px solid #ddd; padding: 10px; text-align: left; }
                    th { background-color: #f2f2f2; font-weight: bold; }
                    tr:nth-child(even) { background-color: #fdfdfd; }
                    button, input, select { padding: 9px 12px; margin: 5px; border: 1px solid #ccc; border-radius: 4px; font-size: 14px; }
                    button { background-color: #007bff; color: white; border-color: #007bff; cursor: pointer; transition: background-color 0.2s ease; }
                    button:hover { background-color: #0056b3; }
                    button:disabled { background-color: #cccccc; cursor: not-allowed; }
                    button.delete-btn { background-color: #dc3545; border-color: #dc3545; }
                    button.delete-btn:hover { background-color: #c82333; }
                    button.edit-btn { background-color: #ffc107; border-color: #ffc107; color: #333; }
                    button.edit-btn:hover { background-color: #e0a800; }
                    input[type="number"] { width: 80px; }
                    #orderAddress { width: 300px; }
                    .notification {
                        position: fixed;
                        top: 20px;
                        right: 20px;
                        padding: 15px 20px;
                        background: #28a745;
                        color: white;
                        display: none;
                        border-radius: 5px;
                        z-index: 1000;
                        box-shadow: 0 2px 5px rgba(0,0,0,0.2);
                    }
                    .notification.error { background: #dc3545; }
                    #error-message { display: none; color: red; padding: 10px; background: #ffecec; border: 1px solid red; border-radius: 4px; margin-top: 15px; }
                    ul { padding-left: 20px; margin: 5px 0; }
                </style>
            </head>
            <body>
                <div id="notification" class="notification"></div>
                <div id="error-message"></div>

                <!-- Секция Склад -->
                <div class="section">
                    <h2>Склад</h2>
                    <div>
                        <input type="text" id="itemName" placeholder="Название товара">
                        <input type="number" id="itemQuantity" placeholder="Кол-во" min="0" value="0">
                        <input type="number" id="itemPrice" placeholder="Цена" step="0.01" min="0" value="0.00">
                        <button onclick="addItem()">Добавить товар</button>
                    </div>
                    <table id="itemsTable">
                        <thead>
                            <tr><!-- Убрали ID --><th>Название</th><th>Кол-во</th><th>Цена (€)</th><th>Действия</th></tr>
                        </thead>
                        <tbody><!-- Данные товаров --></tbody>
                    </table>
                </div>

                <!-- Секция Заказы -->
                <div class="section">
                    <h2>Заказы</h2>
                    <div>
                        <input type="text" id="orderAddress" placeholder="Адрес доставки">
                    </div>
                    <div>
                        <select id="itemSelect"><option value="">Загрузка товаров...</option></select>
                        <input type="number" id="orderQuantity" placeholder="Количество" min="1" value="1">
                        <button onclick="addToOrder()" disabled>Добавить в заказ</button>
                        <button onclick="createOrder()" disabled>Создать заказ</button>
                    </div>
                    <h3>Корзина нового заказа</h3>
                    <table id="orderItemsTable">
                        <thead>
                            <tr><th>Товар</th><th>Кол-во</th><th>Действие</th></tr>
                        </thead>
                        <tbody><!-- Товары для нового заказа --></tbody>
                    </table>

                    <h3>Список созданных заказов</h3>
                    <table id="ordersTable">
                        <thead>
                            <tr><!-- Убрали ID --><th>Дата</th><th>Адрес</th><th>Товары в заказе</th><th>Действие</th></tr>
                        </thead>
                        <tbody><!-- Данные заказов --></tbody>
                    </table>
                </div>

                <script>
                                                 let items = [];
                                                 let orders = [];
                                                 let currentOrderItems = []; // Корзина для нового заказа
                
                                                 function showNotification(message, isError = false) {
                                                     const notification = document.getElementById('notification');
                                                     if (!notification) return;
                                                     notification.textContent = message;
                                                     notification.className = isError ? 'notification error' : 'notification';
                                                     notification.style.display = 'block';
                                                     setTimeout(() => {
                                                         notification.style.display = 'none';
                                                     }, 3000); // Уведомление исчезает через 3 секунды
                                                 }
                
                                                 // Глобальный обработчик ошибок (для общих ошибок скрипта)
                                                 window.onerror = function(message, source, lineno, colno, error) {
                                                     console.error("Global error:", {message, source, lineno, colno, error});
                                                     const errorDiv = document.getElementById('error-message');
                                                      if(errorDiv) {
                                                         errorDiv.textContent = `Ошибка в скрипте: ${message} в ${source} (${lineno}:${colno})`;
                                                         errorDiv.style.display = 'block';
                                                     }
                                                     return true; 
                                                 };
                
                                                 // Попытка извлечь сообщение об ошибке из тела ответа сервера
                                                 async function getErrorFromBody(response) {
                                                      try {
                                                          const errorData = await response.json();
                                                          // Ищем поле 'error' или 'message' в JSON ответе
                                                          return errorData.error || errorData.message || JSON.stringify(errorData);
                                                      } catch (e) {
                                                          // Если ответ не JSON, попробуем прочитать как текст
                                                          try {
                                                              const textError = await response.text();
                                                              if (textError) return textError;
                                                          } catch (textEx) { /* Игнорируем ошибку чтения текста */ }
                                                          // Если ничего не получилось, возвращаем статус и текст статуса
                                                          return `Статус: ${response.status} ${response.statusText}`;
                                                      }
                                                  }
                
                                                 // --- Загрузка данных ---
                                                 async function loadAllData() {
                                                     console.log("Загрузка данных...");
                                                     try {
                                                         // Одновременно загружаем товары и заказы
                                                         const [itemsRes, ordersRes] = await Promise.all([
                                                             fetch('/api/items'),
                                                             fetch('/api/orders')
                                                         ]);
                
                                                         if (!itemsRes.ok) throw new Error(`Ошибка загрузки товаров: ${await getErrorFromBody(itemsRes)}`);
                                                         if (!ordersRes.ok) throw new Error(`Ошибка загрузки заказов: ${await getErrorFromBody(ordersRes)}`);
                
                                                         items = await itemsRes.json();
                                                         orders = await ordersRes.json();
                
                                                         console.log("Данные получены:", { items, orders });
                
                                                         renderItems();
                                                         renderOrders();
                                                         updateItemSelect();
                
                                                     } catch (error) {
                                                         console.error("Ошибка загрузки данных:", error);
                                                         showNotification("Ошибка загрузки данных: " + error.message, true);
                                                         // Если загрузка не удалась, очищаем таблицы, чтобы не показывать старые данные
                                                         items = [];
                                                         orders = [];
                                                         renderItems();
                                                         renderOrders();
                                                         updateItemSelect(); 
                                                     }
                                                 }
                
                                                 // --- Рендеринг таблиц ---
                                                 function renderItems() {
                                                     const tbody = document.querySelector('#itemsTable tbody');
                                                     if (!tbody) return; // Защита от null
                                                     tbody.innerHTML = items.map(item => `
                                                         <tr>
                                                             <!-- ID не отображаем в UI -->
                                                             <td>${item.name || 'Без названия'}</td>
                                                             <td>${item.quantity ?? 'N/A'}</td>
                                                             <td>${(item.price ?? 0).toFixed(2)}</td>
                                                             <td>
                                                                 <button class="edit-btn" onclick="editItem(${item.id})">Изменить</button>
                                                                 <button class="delete-btn" onclick="deleteItem(${item.id})">Удалить</button>
                                                             </td>
                                                         </tr>
                                                     `).join('');
                                                      if (items.length === 0) {
                                                          tbody.innerHTML = '<tr><td colspan="4">Нет товаров на складе</td></tr>'; 
                                                      }
                                                 }
                
                                                  function renderOrders() {
                                                     const tbody = document.querySelector('#ordersTable tbody');
                                                      if (!tbody) return; 
                                                     tbody.innerHTML = orders.length ? orders.map(order => `
                                                         <tr>
                                                             <td>${order.orderDate ? new Date(order.orderDate).toLocaleString() : 'N/A'}</td>
                                                             <td>${order.address || 'Не указан'}</td>
                                                             <td>
                                                                 <ul>${(order.items || []).map(itemDto => `
                                                                     <li>${itemDto.itemName || '[Товар удален]'} - ${itemDto.quantity} шт. (€${(itemDto.price || 0).toFixed(2)})</li>
                                                                 `).join('')}</ul>
                                                                 ${!(order.items && order.items.length) ? 'Нет товаров в заказе' : ''}
                                                             </td>
                                                             <td>
                                                                 <button class="delete-btn" onclick="deleteOrder(${order.id})">Удалить</button>
                                                             </td>
                                                         </tr>
                                                     `).join('') : '<tr><td colspan="4">Нет созданных заказов</td></tr>'; 
                                                 }
                
                                                 // Обновление таблицы корзины для нового заказа
                                                 function updateOrderTable() {
                                                     const tbody = document.querySelector('#orderItemsTable tbody');
                                                     if (!tbody) return; 
                                                     tbody.innerHTML = currentOrderItems.map((item, index) => {
                                                         const fullItemInfo = items.find(i => i.id === item.itemId);
                                                         const itemName = fullItemInfo ? fullItemInfo.name : `[Товар ID ${item.itemId} не найден]`;
                
                                                          return `
                                                             <tr>
                                                                 <td>${itemName}</td>
                                                                 <td>${item.quantity}</td>
                                                                 <td><button class="delete-btn" onclick="removeFromOrder(${index})">Убрать</button></td>
                                                             </tr>`;
                                                     }).join('');
                                                     if (currentOrderItems.length === 0) {
                                                          tbody.innerHTML = '<tr><td colspan="3">Корзина пуста</td></tr>';
                                                          document.querySelector('button[onclick="createOrder()"]').disabled = true;
                                                     } else {
                                                          document.querySelector('button[onclick="createOrder()"]').disabled = false;
                                                     }
                                                 }
                
                
                                                 // --- Функции для Товаров ---
                                                 async function addItem() {
                                                      const nameInput = document.getElementById('itemName');
                                                      const quantityInput = document.getElementById('itemQuantity');
                                                      const priceInput = document.getElementById('itemPrice');
                                                      const name = nameInput.value.trim();
                                                      const quantity = parseInt(quantityInput.value);
                                                      const price = parseFloat(priceInput.value);
                
                                                      if (!name) { showNotification("Название товара обязательно", true); nameInput.focus(); return; }
                                                      if (isNaN(quantity) || quantity < 0) { showNotification("Количество должно быть неотрицательным числом", true); quantityInput.focus(); return; }
                                                      if (isNaN(price) || price < 0) { showNotification("Цена должна быть неотрицательным числом", true); priceInput.focus(); return; }
                
                                                      try {
                                                          const response = await fetch('/api/items', {
                                                              method: 'POST',
                                                              headers: { 'Content-Type': 'application/json' },
                                                              body: JSON.stringify({ name: name, quantity: quantity, price: price }) 
                                                          });
                
                                                          // Проверяем статус ответа
                                                          if (!response.ok) {
                                                              throw new Error(await getErrorFromBody(response));
                                                          }
                
                                                          showNotification("Товар успешно добавлен");
                                                          loadAllData();
                                                          nameInput.value = '';
                                                          quantityInput.value = '0';
                                                          priceInput.value = '0.00';
                
                                                      } catch (error) {
                                                          showNotification("Ошибка добавления товара: " + error.message, true);
                                                          console.error("Ошибка addItem:", error);
                                                      }
                                                  }
                
                                                  async function editItem(id) {
                                                      const item = items.find(i => i.id === id);
                                                      if (!item) { showNotification("Товар для изменения не найден в локальных данных", true); return; }
                
                                                      const newName = prompt('Введите новое название:', item.name);
                                                      if (newName === null) return;
                                                      if (newName.trim() === '') { showNotification("Название товара не может быть пустым", true); return; }
                
                                                      const newQtyStr = prompt('Введите новое количество:', item.quantity);
                                                      if (newQtyStr === null) return;
                                                      const newQty = parseInt(newQtyStr);
                                                      if (isNaN(newQty) || newQty < 0) { showNotification("Количество должно быть неотрицательным числом", true); return; }
                
                                                      const newPriceStr = prompt('Введите новую цену:', item.price);
                                                      if (newPriceStr === null) return;
                                                      const newPrice = parseFloat(newPriceStr);
                                                      if (isNaN(newPrice) || newPrice < 0) { showNotification("Цена должна быть неотрицательным числом", true); return; }
                
                                                      try {
                                                          const response = await fetch(`/api/items/${id}`, {
                                                              method: 'PUT',
                                                              headers: { 'Content-Type': 'application/json' },
                                                              body: JSON.stringify({ name: newName.trim(), quantity: newQty, price: newPrice })
                                                          });
                
                                                          if (!response.ok) {
                                                               throw new Error(await getErrorFromBody(response));
                                                          }
                
                                                          showNotification("Товар успешно обновлен");
                                                          loadAllData(); 
                
                                                      } catch (error) {
                                                          showNotification("Ошибка обновления товара: " + error.message, true);
                                                          console.error("Ошибка editItem:", error);
                                                      }
                                                  }
                
                                                  async function deleteItem(id) {
                                                      if (confirm('ВНИМАНИЕ! Удалить товар? Это также удалит его из ВСЕХ существующих заказов! Пустые заказы будут удалены. Продолжить?')) {
                                                          try {
                                                              const response = await fetch(`/api/items/${id}`, { method: 'DELETE' });
                
                                                              if (!response.ok) {
                                                                  throw new Error(await getErrorFromBody(response));
                                                              }
                
                                                              showNotification("Товар и связанные позиции удалены");
                                                              loadAllData();
                
                                                          } catch (error) {
                                                              showNotification("Ошибка удаления товара: " + error.message, true);
                                                              console.error("Ошибка deleteItem:", error);
                                                          }
                                                      }
                                                  }
                
                                                 // --- Функции для Заказов ---
                
                                                 // Обновляет выпадающий список товаров для добавления в заказ
                                                 function updateItemSelect() {
                                                     const select = document.getElementById('itemSelect');
                                                     const addToOrderBtn = document.querySelector('button[onclick="addToOrder()"]');
                                                     const createOrderBtn = document.querySelector('button[onclick="createOrder()"]');
                
                                                     if (!select || !addToOrderBtn || !createOrderBtn) return; // Защита
                
                                                     const availableItems = items.filter(item => item.quantity > 0);
                
                                                     select.innerHTML = availableItems.map(item =>
                                                         `<option value="${item.id}" data-available="${item.quantity}">${item.name} (Доступно: ${item.quantity})</option>`
                                                     ).join('');
                
                                                     // Управляем состоянием кнопок и селекта
                                                     if (availableItems.length === 0) {
                                                          select.innerHTML = '<option value="">Нет товаров в наличии</option>';
                                                          select.disabled = true;
                                                          addToOrderBtn.disabled = true;
                                                     } else {
                                                          select.disabled = false;
                                                          addToOrderBtn.disabled = false;
                                                     }
                                                 }
                
                
                                                 // Добавляет выбранный товар из селекта в корзину (currentOrderItems)
                                                 function addToOrder() {
                                                     const select = document.getElementById('itemSelect');
                                                     const quantityInput = document.getElementById('orderQuantity');
                
                                                     if (!select || select.disabled || !select.value) {
                                                         showNotification("Нет доступных товаров для добавления", true);
                                                         return;
                                                     }
                
                                                     const itemId = parseInt(select.value);
                                                     const quantity = parseInt(quantityInput.value);
                
                                                     const selectedOption = select.options[select.selectedIndex];
                                                     const itemName = selectedOption.text.split(' (')[0]; // Извлекаем имя из текста опции
                                                     const availableQty = parseInt(selectedOption.getAttribute('data-available') || '0');
                
                                                     if (isNaN(quantity) || quantity <= 0) {
                                                          showNotification("Введите корректное количество (> 0) для добавления в заказ", true);
                                                          quantityInput.focus();
                                                          return;
                                                     }
                
                                                     // Проверяем суммарное количество этого товара уже в корзине
                                                     const alreadyAddedQty = currentOrderItems
                                                         .filter(item => item.itemId === itemId)
                                                         .reduce((sum, item) => sum + item.quantity, 0);
                
                                                     // Проверяем, не превысит ли новое количество доступное
                                                     if (quantity + alreadyAddedQty > availableQty) {
                                                         showNotification(`Недостаточно товара "${itemName}" для добавления. Доступно на складе: ${availableQty} (уже в корзине: ${alreadyAddedQty})`, true);
                                                         return;
                                                     }
                
                                                     // Добавляем товар в корзину
                                                     currentOrderItems.push({ itemId: itemId, quantity: quantity }); 
                                                     console.log("Корзина:", currentOrderItems); 
                
                                                     updateOrderTable(); 
                                                     quantityInput.value = '1';
                
                                                 }
                
                
                                                 // Удаляет позицию из корзины по индексу
                                                 function removeFromOrder(index) {
                                                     if (index >= 0 && index < currentOrderItems.length) {
                                                         currentOrderItems.splice(index, 1); // Удаляем элемент по индексу
                                                         console.log("Корзина после удаления:", currentOrderItems); 
                                                         updateOrderTable(); // Обновляем таблицу корзины
                                                     }
                                                 }
                
                
                                                 // Создает новый заказ на основе корзины
                                                 async function createOrder() {
                                                      if (currentOrderItems.length === 0) {
                                                          showNotification("Сначала добавьте товары в корзину", true);
                                                          return;
                                                      }
                                                      const addressInput = document.getElementById('orderAddress');
                                                      const address = addressInput.value.trim();
                
                                                      // Клиентская валидация адреса
                                                      if (!address) {
                                                          showNotification("Введите адрес доставки", true);
                                                          addressInput.focus();
                                                          return;
                                                      }
                
                                                      // Формируем объект запроса в формате DTO OrderRequest
                                                      const orderRequest = {
                                                          address: address,
                                                          items: currentOrderItems.map(item => ({ 
                                                              itemId: item.itemId,
                                                              quantity: item.quantity
                                                          }))
                                                      };
                
                                                      console.log("Отправка заказа:", orderRequest); 
                
                                                      try {
                                                          const response = await fetch('/api/orders', {
                                                              method: 'POST',
                                                              headers: { 'Content-Type': 'application/json' },
                                                              body: JSON.stringify(orderRequest) 
                                                          });
                
                                                          if (!response.ok) {
                                                               throw new Error(await getErrorFromBody(response));
                                                          }
                
                                                          showNotification("Заказ успешно создан");
                                                          currentOrderItems = []; 
                                                          updateOrderTable(); 
                                                          addressInput.value = ''; 
                                                          document.getElementById('orderQuantity').value = '1'; 
                
                                                          loadAllData();
                
                                                      } catch (error) {
                                                          showNotification("Ошибка создания заказа: " + error.message, true);
                                                          console.error("Ошибка createOrder:", error);
                                                      }
                                                 }
                
                                                 // Удаляет заказ
                                                 async function deleteOrder(id) {
                                                      // Подтверждение с предупреждением о возврате товаров
                                                      if (confirm('Удалить этот заказ? Товары из заказа ВЕРНУТСЯ на склад.')) {
                                                          try {
                                                              const response = await fetch(`/api/orders/${id}`, { method: 'DELETE' });
                
                                                              if (!response.ok) {
                                                                   throw new Error(await getErrorFromBody(response));
                                                              }
                
                                                              showNotification("Заказ успешно удален");
                                                              loadAllData(); 
                
                                                          } catch (error) {
                                                              showNotification("Ошибка удаления заказа: " + error.message, true);
                                                              console.error("Ошибка deleteOrder:", error);
                                                          }
                                                      }
                                                  }
                
                
                                                 // --- Инициализация ---
                                                 document.addEventListener('DOMContentLoaded', () => {
                                                     console.log("DOM загружен, вызываем loadAllData...");
                                                     loadAllData();
                                                     updateOrderTable();
                                                 });
                
                                             </script>
            </body>
            </html>
            """;
    }
}