-- Очищаем таблицы в правильном порядке
DELETE FROM order_items;
DELETE FROM orders;
DELETE FROM items;

INSERT INTO items (name, quantity, price) VALUES
('Ноутбук', 10, 999.99),
('Телефон', 15, 499.50);