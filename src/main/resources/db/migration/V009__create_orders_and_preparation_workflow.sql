SET NOCOUNT ON;
SET XACT_ABORT ON;

CREATE TABLE dbo.orders
(
    order_id           BIGINT IDENTITY (1, 1) NOT NULL,
    order_number       VARCHAR(30) NOT NULL,
    order_source_id    BIGINT NOT NULL,
    customer_id        BIGINT NULL,
    created_by_user_id BIGINT NOT NULL,
    order_type         VARCHAR(20) NOT NULL,
    status             VARCHAR(20) NOT NULL
        CONSTRAINT DF_orders_status DEFAULT ('OPEN'),
    opened_at          DATETIME2(3) NOT NULL
        CONSTRAINT DF_orders_opened_at DEFAULT (SYSUTCDATETIME()),
    completed_at       DATETIME2(3) NULL,
    cancelled_at       DATETIME2(3) NULL,
    subtotal_amount    DECIMAL(19, 4) NOT NULL
        CONSTRAINT DF_orders_subtotal_amount DEFAULT (0),
    discount_amount    DECIMAL(19, 4) NOT NULL
        CONSTRAINT DF_orders_discount_amount DEFAULT (0),
    tax_amount         DECIMAL(19, 4) NOT NULL
        CONSTRAINT DF_orders_tax_amount DEFAULT (0),
    total_amount       DECIMAL(19, 4) NOT NULL
        CONSTRAINT DF_orders_total_amount DEFAULT (0),
    notes              NVARCHAR(1000) NULL,
    created_at         DATETIME2(3) NOT NULL
        CONSTRAINT DF_orders_created_at DEFAULT (SYSUTCDATETIME()),
    updated_at         DATETIME2(3) NOT NULL
        CONSTRAINT DF_orders_updated_at DEFAULT (SYSUTCDATETIME()),
    row_version        ROWVERSION NOT NULL,

    CONSTRAINT PK_orders
        PRIMARY KEY CLUSTERED (order_id),
    CONSTRAINT UQ_orders_order_number
        UNIQUE (order_number),
    CONSTRAINT FK_orders_order_sources
        FOREIGN KEY (order_source_id)
        REFERENCES dbo.order_sources (order_source_id)
        ON DELETE NO ACTION
        ON UPDATE NO ACTION,
    CONSTRAINT FK_orders_customers
        FOREIGN KEY (customer_id)
        REFERENCES dbo.customers (customer_id)
        ON DELETE NO ACTION
        ON UPDATE NO ACTION,
    CONSTRAINT FK_orders_created_by_users
        FOREIGN KEY (created_by_user_id)
        REFERENCES dbo.users (user_id)
        ON DELETE NO ACTION
        ON UPDATE NO ACTION,
    CONSTRAINT CK_orders_order_number_not_blank
        CHECK (LEN(LTRIM(RTRIM(order_number))) > 0
            AND order_number = LTRIM(RTRIM(order_number))),
    CONSTRAINT CK_orders_order_type
        CHECK (order_type IN ('DINE_IN', 'TAKEAWAY', 'DELIVERY')),
    CONSTRAINT CK_orders_status
        CHECK (status IN ('OPEN', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT CK_orders_lifecycle
        CHECK (
            (status = 'OPEN'
                AND completed_at IS NULL
                AND cancelled_at IS NULL)
            OR
            (status = 'COMPLETED'
                AND completed_at IS NOT NULL
                AND completed_at >= opened_at
                AND cancelled_at IS NULL)
            OR
            (status = 'CANCELLED'
                AND cancelled_at IS NOT NULL
                AND cancelled_at >= opened_at
                AND completed_at IS NULL)
        ),
    CONSTRAINT CK_orders_monetary_amounts
        CHECK (subtotal_amount >= 0
            AND discount_amount >= 0
            AND tax_amount >= 0
            AND total_amount >= 0),
    CONSTRAINT CK_orders_discount_not_greater_than_subtotal
        CHECK (discount_amount <= subtotal_amount),
    CONSTRAINT CK_orders_total_amount
        CHECK (total_amount = subtotal_amount - discount_amount + tax_amount),
    CONSTRAINT CK_orders_notes_not_blank
        CHECK (notes IS NULL OR LEN(LTRIM(RTRIM(notes))) > 0)
);

CREATE INDEX IX_orders_status_opened
    ON dbo.orders (status, opened_at DESC);

CREATE INDEX IX_orders_source_opened
    ON dbo.orders (order_source_id, opened_at DESC);

CREATE INDEX IX_orders_customer_opened
    ON dbo.orders (customer_id, opened_at DESC)
    WHERE customer_id IS NOT NULL;

CREATE TABLE dbo.dine_in_orders
(
    order_id      BIGINT NOT NULL,
    cafe_table_id BIGINT NOT NULL,
    created_at    DATETIME2(3) NOT NULL
        CONSTRAINT DF_dine_in_orders_created_at DEFAULT (SYSUTCDATETIME()),
    updated_at    DATETIME2(3) NOT NULL
        CONSTRAINT DF_dine_in_orders_updated_at DEFAULT (SYSUTCDATETIME()),
    row_version   ROWVERSION NOT NULL,

    CONSTRAINT PK_dine_in_orders
        PRIMARY KEY CLUSTERED (order_id),
    CONSTRAINT FK_dine_in_orders_orders
        FOREIGN KEY (order_id)
        REFERENCES dbo.orders (order_id)
        ON DELETE NO ACTION
        ON UPDATE NO ACTION,
    CONSTRAINT FK_dine_in_orders_cafe_tables
        FOREIGN KEY (cafe_table_id)
        REFERENCES dbo.cafe_tables (cafe_table_id)
        ON DELETE NO ACTION
        ON UPDATE NO ACTION
);

CREATE INDEX IX_dine_in_orders_cafe_table
    ON dbo.dine_in_orders (cafe_table_id, order_id);

CREATE TABLE dbo.delivery_orders
(
    order_id                 BIGINT NOT NULL,
    customer_address_id      BIGINT NULL,
    recipient_name           NVARCHAR(200) NOT NULL,
    recipient_phone          NVARCHAR(30) NOT NULL,
    address_line1            NVARCHAR(200) NOT NULL,
    address_line2            NVARCHAR(200) NULL,
    city                     NVARCHAR(100) NOT NULL,
    region                   NVARCHAR(100) NULL,
    postal_code              NVARCHAR(20) NULL,
    delivery_instructions    NVARCHAR(500) NULL,
    notes                    NVARCHAR(1000) NULL,
    created_at               DATETIME2(3) NOT NULL
        CONSTRAINT DF_delivery_orders_created_at DEFAULT (SYSUTCDATETIME()),
    updated_at               DATETIME2(3) NOT NULL
        CONSTRAINT DF_delivery_orders_updated_at DEFAULT (SYSUTCDATETIME()),
    row_version              ROWVERSION NOT NULL,

    CONSTRAINT PK_delivery_orders
        PRIMARY KEY CLUSTERED (order_id),
    CONSTRAINT FK_delivery_orders_orders
        FOREIGN KEY (order_id)
        REFERENCES dbo.orders (order_id)
        ON DELETE NO ACTION
        ON UPDATE NO ACTION,
    CONSTRAINT FK_delivery_orders_customer_addresses
        FOREIGN KEY (customer_address_id)
        REFERENCES dbo.customer_addresses (customer_address_id)
        ON DELETE NO ACTION
        ON UPDATE NO ACTION,
    CONSTRAINT CK_delivery_orders_recipient_name_not_blank
        CHECK (LEN(LTRIM(RTRIM(recipient_name))) > 0),
    CONSTRAINT CK_delivery_orders_recipient_phone_not_blank
        CHECK (LEN(LTRIM(RTRIM(recipient_phone))) > 0),
    CONSTRAINT CK_delivery_orders_address_line1_not_blank
        CHECK (LEN(LTRIM(RTRIM(address_line1))) > 0),
    CONSTRAINT CK_delivery_orders_address_line2_not_blank
        CHECK (address_line2 IS NULL OR LEN(LTRIM(RTRIM(address_line2))) > 0),
    CONSTRAINT CK_delivery_orders_city_not_blank
        CHECK (LEN(LTRIM(RTRIM(city))) > 0),
    CONSTRAINT CK_delivery_orders_region_not_blank
        CHECK (region IS NULL OR LEN(LTRIM(RTRIM(region))) > 0),
    CONSTRAINT CK_delivery_orders_postal_code_not_blank
        CHECK (postal_code IS NULL OR LEN(LTRIM(RTRIM(postal_code))) > 0),
    CONSTRAINT CK_delivery_orders_delivery_instructions_not_blank
        CHECK (delivery_instructions IS NULL
            OR LEN(LTRIM(RTRIM(delivery_instructions))) > 0),
    CONSTRAINT CK_delivery_orders_notes_not_blank
        CHECK (notes IS NULL OR LEN(LTRIM(RTRIM(notes))) > 0)
);

CREATE INDEX IX_delivery_orders_customer_address
    ON dbo.delivery_orders (customer_address_id, order_id)
    WHERE customer_address_id IS NOT NULL;

CREATE TABLE dbo.order_items
(
    order_item_id               BIGINT IDENTITY (1, 1) NOT NULL,
    order_id                    BIGINT NOT NULL,
    product_variant_id          BIGINT NOT NULL,
    preparation_station_id      BIGINT NULL,
    product_name_snapshot       NVARCHAR(150) NOT NULL,
    variant_name_snapshot       NVARCHAR(100) NOT NULL,
    preparation_station_code    VARCHAR(30) NULL,
    preparation_station_name    NVARCHAR(100) NULL,
    quantity                    DECIMAL(19, 4) NOT NULL,
    unit_price                  DECIMAL(19, 4) NOT NULL,
    subtotal_amount             DECIMAL(19, 4) NOT NULL,
    discount_amount             DECIMAL(19, 4) NOT NULL
        CONSTRAINT DF_order_items_discount_amount DEFAULT (0),
    tax_rate_percent            DECIMAL(9, 6) NOT NULL,
    tax_amount                  DECIMAL(19, 4) NOT NULL,
    total_amount                DECIMAL(19, 4) NOT NULL,
    notes                       NVARCHAR(500) NULL,
    created_at                  DATETIME2(3) NOT NULL
        CONSTRAINT DF_order_items_created_at DEFAULT (SYSUTCDATETIME()),
    updated_at                  DATETIME2(3) NOT NULL
        CONSTRAINT DF_order_items_updated_at DEFAULT (SYSUTCDATETIME()),
    row_version                 ROWVERSION NOT NULL,

    CONSTRAINT PK_order_items
        PRIMARY KEY CLUSTERED (order_item_id),
    CONSTRAINT FK_order_items_orders
        FOREIGN KEY (order_id)
        REFERENCES dbo.orders (order_id)
        ON DELETE NO ACTION
        ON UPDATE NO ACTION,
    CONSTRAINT FK_order_items_product_variants
        FOREIGN KEY (product_variant_id)
        REFERENCES dbo.product_variants (product_variant_id)
        ON DELETE NO ACTION
        ON UPDATE NO ACTION,
    CONSTRAINT FK_order_items_preparation_stations
        FOREIGN KEY (preparation_station_id)
        REFERENCES dbo.preparation_stations (preparation_station_id)
        ON DELETE NO ACTION
        ON UPDATE NO ACTION,
    CONSTRAINT CK_order_items_product_name_snapshot_not_blank
        CHECK (LEN(LTRIM(RTRIM(product_name_snapshot))) > 0),
    CONSTRAINT CK_order_items_variant_name_snapshot_not_blank
        CHECK (LEN(LTRIM(RTRIM(variant_name_snapshot))) > 0),
    CONSTRAINT CK_order_items_preparation_station_snapshot
        CHECK ((preparation_station_id IS NULL
                AND preparation_station_code IS NULL
                AND preparation_station_name IS NULL)
            OR (preparation_station_id IS NOT NULL
                AND preparation_station_code IS NOT NULL
                AND LEN(LTRIM(RTRIM(preparation_station_code))) > 0
                AND preparation_station_code = LTRIM(RTRIM(preparation_station_code))
                AND preparation_station_name IS NOT NULL
                AND LEN(LTRIM(RTRIM(preparation_station_name))) > 0)),
    CONSTRAINT CK_order_items_quantity
        CHECK (quantity > 0),
    CONSTRAINT CK_order_items_monetary_amounts
        CHECK (unit_price >= 0
            AND subtotal_amount >= 0
            AND discount_amount >= 0
            AND tax_amount >= 0
            AND total_amount >= 0),
    CONSTRAINT CK_order_items_discount_not_greater_than_subtotal
        CHECK (discount_amount <= subtotal_amount),
    CONSTRAINT CK_order_items_tax_rate_percent
        CHECK (tax_rate_percent >= 0 AND tax_rate_percent <= 100),
    CONSTRAINT CK_order_items_tax_amount
        CHECK (tax_amount = CAST(ROUND(
            (subtotal_amount - discount_amount) * tax_rate_percent / 100, 4)
            AS DECIMAL(19, 4))),
    CONSTRAINT CK_order_items_total_amount
        CHECK (total_amount = subtotal_amount - discount_amount + tax_amount),
    CONSTRAINT CK_order_items_notes_not_blank
        CHECK (notes IS NULL OR LEN(LTRIM(RTRIM(notes))) > 0)
);

CREATE INDEX IX_order_items_order
    ON dbo.order_items (order_id, order_item_id);

CREATE INDEX IX_order_items_variant
    ON dbo.order_items (product_variant_id, order_item_id);

CREATE TABLE dbo.order_item_modifiers
(
    order_item_modifier_id BIGINT IDENTITY (1, 1) NOT NULL,
    order_item_id          BIGINT NOT NULL,
    modifier_id            BIGINT NOT NULL,
    modifier_name_snapshot NVARCHAR(150) NOT NULL,
    quantity               DECIMAL(19, 4) NOT NULL,
    unit_price_adjustment  DECIMAL(19, 4) NOT NULL,
    total_price_adjustment DECIMAL(19, 4) NOT NULL,
    created_at             DATETIME2(3) NOT NULL
        CONSTRAINT DF_order_item_modifiers_created_at DEFAULT (SYSUTCDATETIME()),
    updated_at             DATETIME2(3) NOT NULL
        CONSTRAINT DF_order_item_modifiers_updated_at DEFAULT (SYSUTCDATETIME()),
    row_version            ROWVERSION NOT NULL,

    CONSTRAINT PK_order_item_modifiers
        PRIMARY KEY CLUSTERED (order_item_modifier_id),
    CONSTRAINT UQ_order_item_modifiers_item_modifier
        UNIQUE (order_item_id, modifier_id),
    CONSTRAINT FK_order_item_modifiers_order_items
        FOREIGN KEY (order_item_id)
        REFERENCES dbo.order_items (order_item_id)
        ON DELETE NO ACTION
        ON UPDATE NO ACTION,
    CONSTRAINT FK_order_item_modifiers_modifiers
        FOREIGN KEY (modifier_id)
        REFERENCES dbo.modifiers (modifier_id)
        ON DELETE NO ACTION
        ON UPDATE NO ACTION,
    CONSTRAINT CK_order_item_modifiers_name_snapshot_not_blank
        CHECK (LEN(LTRIM(RTRIM(modifier_name_snapshot))) > 0),
    CONSTRAINT CK_order_item_modifiers_quantity
        CHECK (quantity > 0),
    CONSTRAINT CK_order_item_modifiers_monetary_amounts
        CHECK (unit_price_adjustment >= 0
            AND total_price_adjustment >= 0),
    CONSTRAINT CK_order_item_modifiers_total_price_adjustment
        CHECK (total_price_adjustment = CAST(ROUND(
            quantity * unit_price_adjustment, 4) AS DECIMAL(19, 4)))
);

CREATE INDEX IX_order_item_modifiers_modifier_item
    ON dbo.order_item_modifiers (modifier_id, order_item_id);

CREATE TABLE dbo.order_discounts
(
    order_discount_id          BIGINT IDENTITY (1, 1) NOT NULL,
    order_id                   BIGINT NOT NULL,
    discount_id                BIGINT NOT NULL,
    application_sequence       INT NOT NULL,
    discount_code_snapshot     VARCHAR(50) NOT NULL,
    discount_name_snapshot     NVARCHAR(150) NOT NULL,
    discount_type_snapshot     VARCHAR(20) NOT NULL,
    discount_value_snapshot    DECIMAL(19, 4) NOT NULL,
    applied_discount_amount    DECIMAL(19, 4) NOT NULL,
    created_at                 DATETIME2(3) NOT NULL
        CONSTRAINT DF_order_discounts_created_at DEFAULT (SYSUTCDATETIME()),
    updated_at                 DATETIME2(3) NOT NULL
        CONSTRAINT DF_order_discounts_updated_at DEFAULT (SYSUTCDATETIME()),
    row_version                ROWVERSION NOT NULL,

    CONSTRAINT PK_order_discounts
        PRIMARY KEY CLUSTERED (order_discount_id),
    CONSTRAINT UQ_order_discounts_order_sequence
        UNIQUE (order_id, application_sequence),
    CONSTRAINT UQ_order_discounts_order_discount
        UNIQUE (order_id, discount_id),
    CONSTRAINT FK_order_discounts_orders
        FOREIGN KEY (order_id)
        REFERENCES dbo.orders (order_id)
        ON DELETE NO ACTION
        ON UPDATE NO ACTION,
    CONSTRAINT FK_order_discounts_discounts
        FOREIGN KEY (discount_id)
        REFERENCES dbo.discounts (discount_id)
        ON DELETE NO ACTION
        ON UPDATE NO ACTION,
    CONSTRAINT CK_order_discounts_application_sequence
        CHECK (application_sequence > 0),
    CONSTRAINT CK_order_discounts_code_snapshot_not_blank
        CHECK (LEN(LTRIM(RTRIM(discount_code_snapshot))) > 0
            AND discount_code_snapshot = LTRIM(RTRIM(discount_code_snapshot))),
    CONSTRAINT CK_order_discounts_name_snapshot_not_blank
        CHECK (LEN(LTRIM(RTRIM(discount_name_snapshot))) > 0),
    CONSTRAINT CK_order_discounts_type_snapshot
        CHECK (discount_type_snapshot IN ('PERCENTAGE', 'FIXED_AMOUNT')),
    CONSTRAINT CK_order_discounts_value_snapshot
        CHECK (discount_value_snapshot >= 0
            AND (discount_type_snapshot <> 'PERCENTAGE'
                OR discount_value_snapshot <= 100)),
    CONSTRAINT CK_order_discounts_applied_discount_amount
        CHECK (applied_discount_amount >= 0)
);

CREATE INDEX IX_order_discounts_discount_order
    ON dbo.order_discounts (discount_id, order_id);
