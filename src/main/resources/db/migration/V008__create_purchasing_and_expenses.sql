SET NOCOUNT ON;
SET XACT_ABORT ON;

CREATE TABLE dbo.purchases
(
    purchase_id              BIGINT IDENTITY (1, 1) NOT NULL,
    supplier_id              BIGINT NOT NULL,
    purchase_number          VARCHAR(30) NOT NULL,
    purchased_at             DATETIME2(3) NOT NULL,
    status                   VARCHAR(20) NOT NULL
        CONSTRAINT DF_purchases_status DEFAULT ('DRAFT'),
    subtotal_amount          DECIMAL(19, 4) NOT NULL
        CONSTRAINT DF_purchases_subtotal_amount DEFAULT (0),
    discount_amount          DECIMAL(19, 4) NOT NULL
        CONSTRAINT DF_purchases_discount_amount DEFAULT (0),
    tax_amount               DECIMAL(19, 4) NOT NULL
        CONSTRAINT DF_purchases_tax_amount DEFAULT (0),
    total_amount             DECIMAL(19, 4) NOT NULL
        CONSTRAINT DF_purchases_total_amount DEFAULT (0),
    supplier_invoice_number  NVARCHAR(100) NULL,
    notes                    NVARCHAR(1000) NULL,
    created_by_user_id       BIGINT NOT NULL,
    created_at               DATETIME2(3) NOT NULL
        CONSTRAINT DF_purchases_created_at DEFAULT (SYSUTCDATETIME()),
    updated_at               DATETIME2(3) NOT NULL
        CONSTRAINT DF_purchases_updated_at DEFAULT (SYSUTCDATETIME()),
    row_version              ROWVERSION NOT NULL,

    CONSTRAINT PK_purchases
        PRIMARY KEY CLUSTERED (purchase_id),
    CONSTRAINT UQ_purchases_purchase_number
        UNIQUE (purchase_number),
    CONSTRAINT FK_purchases_suppliers
        FOREIGN KEY (supplier_id)
        REFERENCES dbo.suppliers (supplier_id)
        ON DELETE NO ACTION
        ON UPDATE NO ACTION,
    CONSTRAINT FK_purchases_created_by_users
        FOREIGN KEY (created_by_user_id)
        REFERENCES dbo.users (user_id)
        ON DELETE NO ACTION
        ON UPDATE NO ACTION,
    CONSTRAINT CK_purchases_purchase_number_not_blank
        CHECK (LEN(LTRIM(RTRIM(purchase_number))) > 0
            AND purchase_number = LTRIM(RTRIM(purchase_number))),
    CONSTRAINT CK_purchases_status
        CHECK (status IN ('DRAFT', 'RECEIVED', 'CANCELLED')),
    CONSTRAINT CK_purchases_monetary_amounts
        CHECK (subtotal_amount >= 0
            AND discount_amount >= 0
            AND tax_amount >= 0
            AND total_amount >= 0),
    CONSTRAINT CK_purchases_discount_not_greater_than_subtotal
        CHECK (discount_amount <= subtotal_amount),
    CONSTRAINT CK_purchases_total_amount
        CHECK (total_amount = subtotal_amount - discount_amount + tax_amount),
    CONSTRAINT CK_purchases_supplier_invoice_number_not_blank
        CHECK (supplier_invoice_number IS NULL
            OR LEN(LTRIM(RTRIM(supplier_invoice_number))) > 0),
    CONSTRAINT CK_purchases_notes_not_blank
        CHECK (notes IS NULL OR LEN(LTRIM(RTRIM(notes))) > 0)
);

CREATE INDEX IX_purchases_supplier_purchased
    ON dbo.purchases (supplier_id, purchased_at DESC);

CREATE INDEX IX_purchases_status_purchased
    ON dbo.purchases (status, purchased_at DESC);

CREATE TABLE dbo.purchase_items
(
    purchase_item_id BIGINT IDENTITY (1, 1) NOT NULL,
    purchase_id      BIGINT NOT NULL,
    inventory_item_id BIGINT NOT NULL,
    quantity         DECIMAL(19, 4) NOT NULL,
    unit_cost        DECIMAL(19, 4) NOT NULL,
    line_total       DECIMAL(19, 4) NOT NULL,
    created_at       DATETIME2(3) NOT NULL
        CONSTRAINT DF_purchase_items_created_at DEFAULT (SYSUTCDATETIME()),
    updated_at       DATETIME2(3) NOT NULL
        CONSTRAINT DF_purchase_items_updated_at DEFAULT (SYSUTCDATETIME()),
    row_version      ROWVERSION NOT NULL,

    CONSTRAINT PK_purchase_items
        PRIMARY KEY CLUSTERED (purchase_item_id),
    CONSTRAINT UQ_purchase_items_purchase_inventory_item
        UNIQUE (purchase_id, inventory_item_id),
    CONSTRAINT FK_purchase_items_purchases
        FOREIGN KEY (purchase_id)
        REFERENCES dbo.purchases (purchase_id)
        ON DELETE NO ACTION
        ON UPDATE NO ACTION,
    CONSTRAINT FK_purchase_items_inventory_items
        FOREIGN KEY (inventory_item_id)
        REFERENCES dbo.inventory_items (inventory_item_id)
        ON DELETE NO ACTION
        ON UPDATE NO ACTION,
    CONSTRAINT CK_purchase_items_quantity
        CHECK (quantity > 0),
    CONSTRAINT CK_purchase_items_unit_cost
        CHECK (unit_cost >= 0),
    CONSTRAINT CK_purchase_items_line_total
        CHECK (line_total >= 0
            AND line_total = CAST(ROUND(quantity * unit_cost, 4) AS DECIMAL(19, 4)))
);

CREATE INDEX IX_purchase_items_inventory_purchase
    ON dbo.purchase_items (inventory_item_id, purchase_id);

CREATE TABLE dbo.expense_categories
(
    expense_category_id BIGINT IDENTITY (1, 1) NOT NULL,
    category_code       VARCHAR(50) NOT NULL,
    category_name       NVARCHAR(150) NOT NULL,
    is_active           BIT NOT NULL
        CONSTRAINT DF_expense_categories_is_active DEFAULT (1),
    created_at          DATETIME2(3) NOT NULL
        CONSTRAINT DF_expense_categories_created_at DEFAULT (SYSUTCDATETIME()),
    updated_at          DATETIME2(3) NOT NULL
        CONSTRAINT DF_expense_categories_updated_at DEFAULT (SYSUTCDATETIME()),
    row_version         ROWVERSION NOT NULL,

    CONSTRAINT PK_expense_categories
        PRIMARY KEY CLUSTERED (expense_category_id),
    CONSTRAINT UQ_expense_categories_category_code
        UNIQUE (category_code),
    CONSTRAINT UQ_expense_categories_category_name
        UNIQUE (category_name),
    CONSTRAINT CK_expense_categories_category_code_not_blank
        CHECK (LEN(LTRIM(RTRIM(category_code))) > 0
            AND category_code = LTRIM(RTRIM(category_code))),
    CONSTRAINT CK_expense_categories_category_name_not_blank
        CHECK (LEN(LTRIM(RTRIM(category_name))) > 0)
);

CREATE INDEX IX_expense_categories_active_name
    ON dbo.expense_categories (is_active, category_name);

CREATE TABLE dbo.expenses
(
    expense_id           BIGINT IDENTITY (1, 1) NOT NULL,
    expense_category_id  BIGINT NOT NULL,
    expense_number       VARCHAR(30) NOT NULL,
    expensed_at          DATETIME2(3) NOT NULL,
    amount               DECIMAL(19, 4) NOT NULL,
    description          NVARCHAR(500) NULL,
    reference_number     NVARCHAR(100) NULL,
    notes                NVARCHAR(1000) NULL,
    recorded_by_user_id  BIGINT NOT NULL,
    created_at           DATETIME2(3) NOT NULL
        CONSTRAINT DF_expenses_created_at DEFAULT (SYSUTCDATETIME()),
    updated_at           DATETIME2(3) NOT NULL
        CONSTRAINT DF_expenses_updated_at DEFAULT (SYSUTCDATETIME()),
    row_version          ROWVERSION NOT NULL,

    CONSTRAINT PK_expenses
        PRIMARY KEY CLUSTERED (expense_id),
    CONSTRAINT UQ_expenses_expense_number
        UNIQUE (expense_number),
    CONSTRAINT FK_expenses_expense_categories
        FOREIGN KEY (expense_category_id)
        REFERENCES dbo.expense_categories (expense_category_id)
        ON DELETE NO ACTION
        ON UPDATE NO ACTION,
    CONSTRAINT FK_expenses_recorded_by_users
        FOREIGN KEY (recorded_by_user_id)
        REFERENCES dbo.users (user_id)
        ON DELETE NO ACTION
        ON UPDATE NO ACTION,
    CONSTRAINT CK_expenses_expense_number_not_blank
        CHECK (LEN(LTRIM(RTRIM(expense_number))) > 0
            AND expense_number = LTRIM(RTRIM(expense_number))),
    CONSTRAINT CK_expenses_amount
        CHECK (amount > 0),
    CONSTRAINT CK_expenses_description_not_blank
        CHECK (description IS NULL OR LEN(LTRIM(RTRIM(description))) > 0),
    CONSTRAINT CK_expenses_reference_number_not_blank
        CHECK (reference_number IS NULL OR LEN(LTRIM(RTRIM(reference_number))) > 0),
    CONSTRAINT CK_expenses_notes_not_blank
        CHECK (notes IS NULL OR LEN(LTRIM(RTRIM(notes))) > 0)
);

CREATE INDEX IX_expenses_expensed
    ON dbo.expenses (expensed_at DESC);

CREATE INDEX IX_expenses_category_expensed
    ON dbo.expenses (expense_category_id, expensed_at DESC);
