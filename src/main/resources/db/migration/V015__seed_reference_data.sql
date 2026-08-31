SET NOCOUNT ON;
SET XACT_ABORT ON;

INSERT INTO dbo.preparation_stations
(
    station_code,
    station_name,
    display_order,
    is_active
)
VALUES
    ('BAR', N'Bar', 1, 1),
    ('KITCHEN', N'Kitchen', 2, 1);

INSERT INTO dbo.order_sources
(
    source_code,
    source_name,
    display_order,
    is_active
)
VALUES
    ('POS', N'Point of Sale', 1, 1),
    ('PHONE', N'Phone', 2, 1),
    ('DELIVERY', N'Delivery', 3, 1);

INSERT INTO dbo.payment_methods
(
    method_code,
    method_name,
    display_order,
    is_active
)
VALUES
    ('CASH', N'Cash', 1, 1),
    ('CARD', N'Card', 2, 1);

INSERT INTO dbo.units_of_measure
(
    unit_code,
    unit_name,
    is_active
)
VALUES
    ('UNIT', N'Unit', 1),
    ('GRAM', N'Gram', 1),
    ('KILOGRAM', N'Kilogram', 1),
    ('MILLILITER', N'Milliliter', 1),
    ('LITER', N'Liter', 1);

INSERT INTO dbo.settings
(
    setting_key,
    setting_value,
    value_type,
    description
)
VALUES
    ('numbering.customer.prefix', N'CUS', 'STRING', N'Prefix for generated customer numbers.'),
    ('numbering.reservation.prefix', N'RSV', 'STRING', N'Prefix for generated reservation numbers.'),
    ('numbering.order.prefix', N'ORD', 'STRING', N'Prefix for generated order numbers.'),
    ('numbering.purchase.prefix', N'PUR', 'STRING', N'Prefix for generated purchase numbers.'),
    ('numbering.expense.prefix', N'EXP', 'STRING', N'Prefix for generated expense numbers.'),
    ('numbering.document.prefix', N'DOC', 'STRING', N'Prefix for generated receipt and invoice document numbers.'),
    ('loyalty.enabled', N'true', 'BOOLEAN', N'Controls whether customer loyalty is enabled.'),
    ('loyalty.points_per_currency_unit', N'1', 'INTEGER', N'Points earned per eligible currency unit.'),
    ('loyalty.currency_value_per_point', N'0.01', 'DECIMAL', N'Currency value represented by one redeemed loyalty point.');

INSERT INTO dbo.roles
(
    role_code,
    role_name,
    description,
    is_system,
    is_active
)
VALUES
    ('ADMIN', N'Administrator', N'Full system administration role.', 1, 1),
    ('MANAGER', N'Manager', N'Coffee shop management role.', 1, 1),
    ('CASHIER', N'Cashier', N'Point-of-sale cashier role.', 1, 1);
