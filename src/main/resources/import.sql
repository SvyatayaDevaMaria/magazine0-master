
INSERT INTO items (id, name, quantity, price) VALUES
(nextval('items_SEQ'), 'Тестовый ноутбук', 10, 999.99),
(nextval('items_SEQ'), 'Тестовый телефон', 5, 499.50);

SELECT setval('items_SEQ', COALESCE((SELECT MAX(id) + 1 FROM items), 1), false);
SELECT setval('orders_SEQ', COALESCE((SELECT MAX(id) + 1 FROM orders), 1), false);
SELECT setval('order_items_SEQ', COALESCE((SELECT MAX(id) + 1 FROM order_items), 1), false);