SET NOCOUNT ON;
SET XACT_ABORT ON;

CREATE TABLE dbo.units_of_measure
(
    unit_of_measure_id BIGINT IDENTITY (1, 1) NOT NULL,
    unit_code          VARCHAR(30) NOT NULL,
    unit_name          NVARCHAR(100) NOT NULL,
    is_active          BIT NOT NULL
        CONSTRAINT DF_units_of_measure_is_active DEFAULT (1),
    created_at         DATETIME2(3) NOT NULL
        CONSTRAINT DF_units_of_measure_created_at DEFAULT (SYSUTCDATETIME()),
    updated_at         DATETIME2(3) NOT NULL
        CONSTRAINT DF_units_of_measure_updated_at DEFAULT (SYSUTCDATETIME()),
    row_version        ROWVERSION NOT NULL,

    CONSTRAINT PK_units_of_measure
        PRIMARY KEY CLUSTERED (unit_of_measure_id),
    CONSTRAINT UQ_units_of_measure_unit_code
        UNIQUE (unit_code),
    CONSTRAINT UQ_units_of_measure_unit_name
        UNIQUE (unit_name),
    CONSTRAINT CK_units_of_measure_unit_code_not_blank
        CHECK (LEN(LTRIM(RTRIM(unit_code))) > 0
            AND unit_code = LTRIM(RTRIM(unit_code))),
    CONSTRAINT CK_units_of_measure_unit_name_not_blank
        CHECK (LEN(LTRIM(RTRIM(unit_name))) > 0)
);

CREATE INDEX IX_units_of_measure_active_name
    ON dbo.units_of_measure (is_active, unit_name);

CREATE TABLE dbo.inventory_items
(
    inventory_item_id     BIGINT IDENTITY (1, 1) NOT NULL,
    unit_of_measure_id    BIGINT NOT NULL,
    item_code             VARCHAR(50) NOT NULL,
    item_name             NVARCHAR(150) NOT NULL,
    current_stock_quantity DECIMAL(19, 4) NOT NULL
        CONSTRAINT DF_inventory_items_current_stock_quantity DEFAULT (0),
    reorder_level         DECIMAL(19, 4) NOT NULL
        CONSTRAINT DF_inventory_items_reorder_level DEFAULT (0),
    allow_negative_stock  BIT NOT NULL
        CONSTRAINT DF_inventory_items_allow_negative_stock DEFAULT (0),
    is_active             BIT NOT NULL
        CONSTRAINT DF_inventory_items_is_active DEFAULT (1),
    created_at            DATETIME2(3) NOT NULL
        CONSTRAINT DF_inventory_items_created_at DEFAULT (SYSUTCDATETIME()),
    updated_at            DATETIME2(3) NOT NULL
        CONSTRAINT DF_inventory_items_updated_at DEFAULT (SYSUTCDATETIME()),
    row_version           ROWVERSION NOT NULL,

    CONSTRAINT PK_inventory_items
        PRIMARY KEY CLUSTERED (inventory_item_id),
    CONSTRAINT UQ_inventory_items_item_code
        UNIQUE (item_code),
    CONSTRAINT UQ_inventory_items_item_name
        UNIQUE (item_name),
    CONSTRAINT FK_inventory_items_units_of_measure
        FOREIGN KEY (unit_of_measure_id)
        REFERENCES dbo.units_of_measure (unit_of_measure_id)
        ON DELETE NO ACTION
        ON UPDATE NO ACTION,
    CONSTRAINT CK_inventory_items_item_code_not_blank
        CHECK (LEN(LTRIM(RTRIM(item_code))) > 0
            AND item_code = LTRIM(RTRIM(item_code))),
    CONSTRAINT CK_inventory_items_item_name_not_blank
        CHECK (LEN(LTRIM(RTRIM(item_name))) > 0),
    CONSTRAINT CK_inventory_items_current_stock_quantity
        CHECK (current_stock_quantity >= 0 OR allow_negative_stock = 1),
    CONSTRAINT CK_inventory_items_reorder_level
        CHECK (reorder_level >= 0)
);

CREATE INDEX IX_inventory_items_active_name
    ON dbo.inventory_items (is_active, item_name);

CREATE INDEX IX_inventory_items_unit_active
    ON dbo.inventory_items (unit_of_measure_id, is_active);

CREATE TABLE dbo.recipe_items
(
    recipe_item_id    BIGINT IDENTITY (1, 1) NOT NULL,
    product_variant_id BIGINT NOT NULL,
    inventory_item_id BIGINT NOT NULL,
    quantity_required DECIMAL(19, 4) NOT NULL,
    created_at        DATETIME2(3) NOT NULL
        CONSTRAINT DF_recipe_items_created_at DEFAULT (SYSUTCDATETIME()),
    updated_at        DATETIME2(3) NOT NULL
        CONSTRAINT DF_recipe_items_updated_at DEFAULT (SYSUTCDATETIME()),
    row_version       ROWVERSION NOT NULL,

    CONSTRAINT PK_recipe_items
        PRIMARY KEY CLUSTERED (recipe_item_id),
    CONSTRAINT UQ_recipe_items_variant_inventory_item
        UNIQUE (product_variant_id, inventory_item_id),
    CONSTRAINT FK_recipe_items_product_variants
        FOREIGN KEY (product_variant_id)
        REFERENCES dbo.product_variants (product_variant_id)
        ON DELETE NO ACTION
        ON UPDATE NO ACTION,
    CONSTRAINT FK_recipe_items_inventory_items
        FOREIGN KEY (inventory_item_id)
        REFERENCES dbo.inventory_items (inventory_item_id)
        ON DELETE NO ACTION
        ON UPDATE NO ACTION,
    CONSTRAINT CK_recipe_items_quantity_required
        CHECK (quantity_required > 0)
);

CREATE INDEX IX_recipe_items_inventory_variant
    ON dbo.recipe_items (inventory_item_id, product_variant_id);

CREATE TABLE dbo.modifier_recipe_items
(
    modifier_recipe_item_id BIGINT IDENTITY (1, 1) NOT NULL,
    modifier_id             BIGINT NOT NULL,
    inventory_item_id       BIGINT NOT NULL,
    quantity_required       DECIMAL(19, 4) NOT NULL,
    created_at              DATETIME2(3) NOT NULL
        CONSTRAINT DF_modifier_recipe_items_created_at DEFAULT (SYSUTCDATETIME()),
    updated_at              DATETIME2(3) NOT NULL
        CONSTRAINT DF_modifier_recipe_items_updated_at DEFAULT (SYSUTCDATETIME()),
    row_version             ROWVERSION NOT NULL,

    CONSTRAINT PK_modifier_recipe_items
        PRIMARY KEY CLUSTERED (modifier_recipe_item_id),
    CONSTRAINT UQ_modifier_recipe_items_modifier_inventory_item
        UNIQUE (modifier_id, inventory_item_id),
    CONSTRAINT FK_modifier_recipe_items_modifiers
        FOREIGN KEY (modifier_id)
        REFERENCES dbo.modifiers (modifier_id)
        ON DELETE NO ACTION
        ON UPDATE NO ACTION,
    CONSTRAINT FK_modifier_recipe_items_inventory_items
        FOREIGN KEY (inventory_item_id)
        REFERENCES dbo.inventory_items (inventory_item_id)
        ON DELETE NO ACTION
        ON UPDATE NO ACTION,
    CONSTRAINT CK_modifier_recipe_items_quantity_required
        CHECK (quantity_required > 0)
);

CREATE INDEX IX_modifier_recipe_items_inventory_modifier
    ON dbo.modifier_recipe_items (inventory_item_id, modifier_id);

CREATE TABLE dbo.suppliers
(
    supplier_id   BIGINT IDENTITY (1, 1) NOT NULL,
    supplier_code VARCHAR(50) NOT NULL,
    supplier_name NVARCHAR(200) NOT NULL,
    contact_person NVARCHAR(150) NULL,
    phone         NVARCHAR(30) NULL,
    email         NVARCHAR(254) NULL,
    address       NVARCHAR(500) NULL,
    notes         NVARCHAR(1000) NULL,
    is_active     BIT NOT NULL
        CONSTRAINT DF_suppliers_is_active DEFAULT (1),
    created_at    DATETIME2(3) NOT NULL
        CONSTRAINT DF_suppliers_created_at DEFAULT (SYSUTCDATETIME()),
    updated_at    DATETIME2(3) NOT NULL
        CONSTRAINT DF_suppliers_updated_at DEFAULT (SYSUTCDATETIME()),
    row_version   ROWVERSION NOT NULL,

    CONSTRAINT PK_suppliers
        PRIMARY KEY CLUSTERED (supplier_id),
    CONSTRAINT UQ_suppliers_supplier_code
        UNIQUE (supplier_code),
    CONSTRAINT CK_suppliers_supplier_code_not_blank
        CHECK (LEN(LTRIM(RTRIM(supplier_code))) > 0
            AND supplier_code = LTRIM(RTRIM(supplier_code))),
    CONSTRAINT CK_suppliers_supplier_name_not_blank
        CHECK (LEN(LTRIM(RTRIM(supplier_name))) > 0),
    CONSTRAINT CK_suppliers_contact_person_not_blank
        CHECK (contact_person IS NULL OR LEN(LTRIM(RTRIM(contact_person))) > 0),
    CONSTRAINT CK_suppliers_phone_not_blank
        CHECK (phone IS NULL OR LEN(LTRIM(RTRIM(phone))) > 0),
    CONSTRAINT CK_suppliers_email_not_blank
        CHECK (email IS NULL OR LEN(LTRIM(RTRIM(email))) > 0),
    CONSTRAINT CK_suppliers_address_not_blank
        CHECK (address IS NULL OR LEN(LTRIM(RTRIM(address))) > 0),
    CONSTRAINT CK_suppliers_notes_not_blank
        CHECK (notes IS NULL OR LEN(LTRIM(RTRIM(notes))) > 0)
);

CREATE INDEX IX_suppliers_active_name
    ON dbo.suppliers (is_active, supplier_name);
