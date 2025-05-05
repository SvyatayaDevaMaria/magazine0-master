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
.notification { position: fixed; top: 20px; right: 20px; padding: 15px 20px; background: #28a745; color: white; display: none; border-radius: 5px; z-index: 1000; box-shadow: 0 2px 5px rgba(0,0,0,0.2); }
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
<tr><th>Название</th><th>Кол-во</th><th>Цена (€)</th><th>Действия</th></tr>
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
<tr><th>Дата</th><th>Адрес</th><th>Товары в заказе</th><th>Действие</th></tr>
</thead>
<tbody><!-- Данные заказов --></tbody>
</table>
</div>

<script>
let items = [];
let orders = [];
let currentOrderItems = [];

function showNotification(message, isError = false) {
    const notification = document.getElementById('notification');
    if (!notification) return;
    notification.textContent = message;
    notification.className = isError ? 'notification error' : 'notification';
    notification.style.display = 'block';
    setTimeout(() => notification.style.display = 'none', 3000);
}

async function getErrorFromBody(response) {
    try {
        const data = await response.json();
        return data.error || data.message || JSON.stringify(data);
    } catch (e) {
        try {
            const text = await response.text();
            return text;
        } catch (t) {
            return `Ошибка статуса: ${response.status}`;
        }
    }
}

async function loadAllData() {
    try {
        // Загружаем товары
        const itemsRes = await fetch('/api/items');
        const itemsText = await itemsRes.text();
        // Проверяем, что ответ не пустой и похож на JSON
        if (!itemsText || itemsText.trim().length === 0 || !itemsText.trim().startsWith('[') && !itemsText.trim().startsWith('{')) {
             throw new Error("Empty or non-JSON response for items: " + itemsText.substring(0, 50) + "...");
        }
         if (!isValidJson(itemsText)) throw new Error("Invalid JSON for items");


        // Загружаем заказы
        const ordersRes = await fetch('/api/orders');
        const ordersText = await ordersRes.text();
         // Проверяем, что ответ не пустой и похож на JSON
         if (!ordersText || ordersText.trim().length === 0 || !ordersText.trim().startsWith('[') && !ordersText.trim().startsWith('{')) {
              throw new Error("Empty or non-JSON response for orders: " + ordersText.substring(0, 50) + "...");
         }
        if (!isValidJson(ordersText)) throw new Error("Invalid JSON for orders");

        // Парсим
        items = JSON.parse(itemsText);
        orders = JSON.parse(ordersText);

        renderItems(items);
        renderOrders();
        updateItemSelect();

    } catch (error) {
        console.error("Ошибка загрузки данных:", error);
        showNotification("Ошибка загрузки данных: " + error.message, true);
    }
}

function isValidJson(str) {
    try {
        JSON.parse(str);
        return true;
    } catch (e) {
        console.error("JSON parse error:", e.message);
        return false;
    }
}

function renderItems(itemsList) {
    const tbody = document.querySelector('#itemsTable tbody');
    if (!tbody) return;
    tbody.innerHTML = itemsList.map(item => `
        <tr data-id="${item.id}">
            <td>${item.name}</td> 
            <td>${item.quantity}</td>
            <td>${item.price.toFixed(2)}</td>
            <td>
                <button class="edit-btn" onclick="editItem(${item.id})">Изменить</button> 
                <button class="delete-btn" onclick="deleteItem(${item.id})">Удалить</button>
            </td>
        </tr>
    `).join('');
}

                function renderOrders() {
                     const tbody = document.querySelector('#ordersTable tbody');
                     if (!tbody) return;
                     tbody.innerHTML = orders.length ? orders.map(order => `
                         <tr>
                             <td>${order.orderDate ? new Date(order.orderDate).toLocaleString() : 'N/A'}</td>
                             <td>${order.address || 'Не указан'}</td>
                             <td>
                                 <ul>
                                     ${(order.items || []).map(i => `<li>${i.itemName || '[Неизвестный товар]'} — ${i.quantity} шт.</li>`).join('')}
                                 </ul>
                             </td>
                             <td>
                                 <button class="delete-btn" onclick="deleteOrder(${order.id})">Удалить</button>
                             </td>
                         </tr>
                     `).join('') : '<tr><td colspan="4">Нет заказов</td></tr>';
                 }



// --- Товары ---
async function addItem() {
    const name = document.getElementById('itemName').value.trim();
    const quantity = parseInt(document.getElementById('itemQuantity').value);
    const price = parseFloat(document.getElementById('itemPrice').value);

    if (!name || isNaN(quantity) || quantity < 0 || isNaN(price) || price < 0) {
        showNotification("Проверьте введенные данные: Название, Количество (>=0), Цена (>=0)", true);
        return;
    }

    try {
        const res = await fetch('/api/items', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({name, quantity, price})
        });
        if (!res.ok) {
             const errorMsg = await getErrorFromBody(res);
             throw new Error(errorMsg);
        }
        showNotification("Товар добавлен");
        document.getElementById('itemName').value = '';
        document.getElementById('itemQuantity').value = '0';
        document.getElementById('itemPrice').value = '0.00';
        await loadAllData(); // Перезагружаем данные после добавления
    } catch (e) {
        showNotification("Ошибка добавления товара: " + e.message, true);
         console.error("Add item error:", e); // Добавляем лог в консоль для отладки
    }
}

async function editItem(id) {
    const item = items.find(i => i.id === id);
    if (!item) {
        showNotification("Товар не найден", true);
        return;
    }

    const newName = prompt("Введите новое имя", item.name);
    if (newName === null) return; // Отмена

    const newQtyStr = prompt("Введите количество", item.quantity);
    if (newQtyStr === null) return; // Отмена
    const newQty = parseInt(newQtyStr);
    if (isNaN(newQty) || newQty < 0) {
         showNotification("Некорректное количество. Должно быть неотрицательным числом.", true);
         return;
    }

    const newPriceStr = prompt("Введите цену", item.price);
    if (newPriceStr === null) return; // Отмена
    const newPrice = parseFloat(newPriceStr);
     if (isNaN(newPrice) || newPrice < 0) {
         showNotification("Некорректная цена. Должна быть неотрицательным числом.", true);
         return;
     }


    try {
        const res = await fetch(`/api/items/${id}`, {
            method: 'PUT',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({name: newName.trim(), quantity: newQty, price: newPrice}) // Обрезаем пробелы у имени
        });
        if (!res.ok) {
             const errorMsg = await getErrorFromBody(res);
             throw new Error(errorMsg);
        }
        showNotification("Товар обновлен");
        await loadAllData(); // Перезагружаем данные после обновления
    } catch (e) {
        showNotification("Ошибка обновления: " + e.message, true);
         console.error("Edit item error:", e); // Добавляем лог в консоль для отладки
    }
}

async function deleteItem(id) {
    if (!confirm("Удалить товар?")) return;
    try {
        const res = await fetch(`/api/items/${id}`, {method: 'DELETE'});
        if (!res.ok) {
             const errorMsg = await getErrorFromBody(res);
             throw new Error(errorMsg);
        }
         // Проверяем статус 204 No Content, который возвращается при успешном удалении
        if (res.status !== 204) {
             // Если статус не 204, значит, возможно, есть тело ответа с ошибкой
             const errorMsg = await getErrorFromBody(res);
             if (errorMsg && errorMsg !== `Ошибка статуса: ${res.status}`) {
                  throw new Error(errorMsg);
             } else {
                   throw new Error(`Failed with status: ${res.status}`);
             }
        }

        showNotification("Товар удален");
        await loadAllData(); // Перезагружаем данные после удаления
    } catch (e) {
        showNotification("Ошибка удаления: " + e.message, true);
         console.error("Delete item error:", e); // Добавляем лог в консоль для отладки
    }
}

// --- Заказы ---
function updateItemSelect() {
    const select = document.getElementById('itemSelect');
    const availableItems = items.filter(i => i.quantity > 0);
     const currentSelectedId = select.value;

    select.innerHTML = availableItems.length
        ? availableItems.map(i => `<option value="${i.id}" data-available="${i.quantity}">${i.name} (осталось: ${i.quantity})</option>`)
        : '<option value="">Нет доступных товаров</option>';

     // Пытаемся восстановить выбранное значение, если оно еще доступно
     if (currentSelectedId && availableItems.some(i => String(i.id) === currentSelectedId)) {
         select.value = currentSelectedId;
     } else {
         // Иначе выбираем первый доступный или остаемся на пустом
         select.value = availableItems.length > 0 ? availableItems[0].id : "";
     }


    document.querySelector('button[onclick="addToOrder()"]').disabled = !availableItems.length;
}

function addToOrder() {
    const select = document.getElementById('itemSelect');
    const qtyInput = document.getElementById('orderQuantity');
    const itemId = parseInt(select.value);
    const quantity = parseInt(qtyInput.value);

    if (!itemId) {
         showNotification("Выберите товар для добавления в корзину", true);
         return;
    }

    if (isNaN(quantity) || quantity <= 0) {
        showNotification("Введите корректное количество (больше 0)", true);
        return;
    }

    const item = items.find(i => i.id === itemId);
    if (!item) { 
        showNotification("Выбранный товар не найден в списке", true);
        return;
    }

    const currentQtyInCartForThisItem = currentOrderItems
        .filter(i => i.itemId === itemId)
        .reduce((sum, i) => sum + i.quantity, 0);
    const newTotalQtyInCart = currentQtyInCartForThisItem + quantity;

    if (item.quantity < newTotalQtyInCart) {
        showNotification(`Недостаточно товара "${item.name}" на складе. Требуется: ${newTotalQtyInCart}, Доступно: ${item.quantity}`, true);
        return;
    }

    currentOrderItems.push({itemId, quantity});

    updateOrderTable();
    qtyInput.value = '1'; // Сбрасываем количество для следующего добавления
    showNotification(`Товар "${item.name}" (${quantity} шт.) добавлен в корзину`); // Уведомление
}

function updateOrderTable() {
    const tbody = document.querySelector('#orderItemsTable tbody');
    if (!tbody) return;

     const combinedItems = currentOrderItems.reduce((acc, item) => {
        const existing = acc.find(i => i.itemId === item.itemId);
        if (existing) {
            existing.quantity += item.quantity;
        } else {
            const product = items.find(p => p.id === item.itemId);
            if (product) {
                 acc.push({...item, itemName: product.name, itemPrice: product.price});
            } else {
                 acc.push({...item, itemName: '[Товар не найден]', itemPrice: 0});
            }
        }
        return acc;
    }, []);


    tbody.innerHTML = combinedItems.map(item => {
        return `
            <tr data-item-id="${item.itemId}">
                <td>${item.itemName || '[Товар не найден]'}</td> 
                <td>${item.quantity}</td>
                <td>
                     <button class="edit-btn" onclick="editCartItem(${item.itemId})">Изменить</button> 
                    <button class="delete-btn" onclick="removeFromOrder(${item.itemId})">Убрать все</button> 
                </td>
            </tr>`;
    }).join('');

    // Включаем кнопку "Создать заказ" только если в корзине есть товары
    document.querySelector('button[onclick="createOrder()"]').disabled = currentOrderItems.length === 0;
}


function editCartItem(itemIdToEdit) {
     const itemData = items.find(p => p.id === itemIdToEdit);
     if (!itemData) {
         showNotification("Данные товара не найдены", true);
         return;
     }

     const currentTotalInCart = currentOrderItems
         .filter(i => i.itemId === itemIdToEdit)
         .reduce((sum, i) => sum + i.quantity, 0);


     const newQtyStr = prompt(`Изменить количество "${itemData.name}" в корзине? (Текущее: ${currentTotalInCart}, Доступно: ${itemData.quantity})`, currentTotalInCart);

     if (newQtyStr === null) return; 

     const newQty = parseInt(newQtyStr);

     if (isNaN(newQty) || newQty < 0) {
         if (!isNaN(newQty) && newQty < 0) showNotification("Количество не может быть отрицательным", true);
         return; 
     }

     if (newQty === currentTotalInCart) {
         return; 
     }

     if (newQty > itemData.quantity) {
          showNotification(`Недостаточно товара "${itemData.name}" на складе. Требуется: ${newQty}, Доступно: ${itemData.quantity}`, true);
          return;
     }

     if (newQty === 0) {
         removeFromOrder(itemIdToEdit); 
         showNotification(`Товар "${itemData.name}" удален из корзины`);
         return;
     }

    // Удаляем все старые записи для этого itemId
    currentOrderItems = currentOrderItems.filter(i => i.itemId !== itemIdToEdit);
    // Добавляем новую запись с измененным количеством
    currentOrderItems.push({ itemId: itemIdToEdit, quantity: newQty });

     updateOrderTable();
     showNotification(`Количество товара "${itemData.name}" обновлено: ${newQty} шт.`);
}


// Изменяем removeFromOrder, чтобы она удаляла все записи для данного itemId
function removeFromOrder(itemIdToRemove) {
     const itemData = items.find(p => p.id === itemIdToRemove);
     if (!itemData) { // Товар мог быть удален со склада после добавления в корзину
         showNotification(`Товар с ID ${itemIdToRemove} удален из корзины`);
     } else {
          showNotification(`Товар "${itemData.name}" удален из корзины`);
     }

    currentOrderItems = currentOrderItems.filter(item => item.itemId !== itemIdToRemove);
    updateOrderTable();
}


async function createOrder() {
    const orderItemsToSend = Object.values(currentOrderItems.reduce((acc, item) => {
        if (acc[item.itemId]) {
            acc[item.itemId].quantity += item.quantity;
        } else {
            acc[item.itemId] = {...item};
        }
        return acc;
    }, {}));


    if (orderItemsToSend.length === 0) {
        showNotification("Выберите хотя бы один товар", true);
        return;
    }

    const address = document.getElementById('orderAddress').value.trim();
    if (!address) {
        showNotification("Введите адрес доставки", true);
        return;
    }

    try {
        const res = await fetch('/api/orders', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({address, items: orderItemsToSend}) 
        });

        if (!res.ok) {
             const errorMsg = await getErrorFromBody(res);
             throw new Error(errorMsg);
        }

        showNotification("Заказ создан");
        currentOrderItems = [];
        updateOrderTable();
        document.getElementById('orderAddress').value = '';
        await loadAllData(); 
    } catch (e) {
        console.error("Ошибка при создании заказа:", e);
        showNotification("Ошибка создания заказа: " + e.message, true);
    }
}

async function deleteOrder(id) {
    if (!confirm("Удалить заказ?")) return;
    try {
        const res = await fetch(`/api/orders/${id}`, {method: 'DELETE'});
        if (!res.ok) {
             const errorMsg = await getErrorFromBody(res);
             throw new Error(errorMsg);
        }
         // Проверяем статус 204 No Content, который возвращается при успешном удалении
         if (res.status !== 204) {
              const errorMsg = await getErrorFromBody(res);
              if (errorMsg && errorMsg !== `Ошибка статуса: ${res.status}`) {
                  throw new Error(errorMsg);
              } else {
                   throw new Error(`Failed with status: ${res.status}`);
              }
         }

        showNotification("Заказ удален");
        await loadAllData(); // Перезагружаем данные (склад и заказы)
    } catch (e) {
        console.error("Ошибка при удалении заказа:", e);
        showNotification("Ошибка удаления заказа: " + e.message, true);
    }
}

document.addEventListener('DOMContentLoaded', () => {
    loadAllData();
    updateOrderTable(); // Инициализируем отображение корзины
});
</script>
</body>
</html>
        """;
    }
}