SET NOCOUNT ON;
SET XACT_ABORT ON;

CREATE TABLE dbo.payments
(
    payment_id          BIGINT IDENTITY (1, 1) NOT NULL,
    order_id            BIGINT NOT NULL,
    payment_method_id   BIGINT NOT NULL,
    register_shift_id   BIGINT NULL,
    recorded_by_user_id BIGINT NOT NULL,
    amount              DECIMAL(19, 4) NOT NULL,
    refunded_amount     DECIMAL(19, 4) NOT NULL
        CONSTRAINT DF_payments_refunded_amount DEFAULT (0),
    status              VARCHAR(30) NOT NULL
        CONSTRAINT DF_payments_status DEFAULT ('COMPLETED'),
    paid_at             DATETIME2(3) NOT NULL
        CONSTRAINT DF_payments_paid_at DEFAULT (SYSUTCDATETIME()),
    refunded_at         DATETIME2(3) NULL,
    external_reference  NVARCHAR(200) NULL,
    created_at          DATETIME2(3) NOT NULL
        CONSTRAINT DF_payments_created_at DEFAULT (SYSUTCDATETIME()),
    updated_at          DATETIME2(3) NOT NULL
        CONSTRAINT DF_payments_updated_at DEFAULT (SYSUTCDATETIME()),
    row_version         ROWVERSION NOT NULL,

    CONSTRAINT PK_payments
        PRIMARY KEY CLUSTERED (payment_id),
    CONSTRAINT FK_payments_orders
        FOREIGN KEY (order_id)
        REFERENCES dbo.orders (order_id)
        ON DELETE NO ACTION
        ON UPDATE NO ACTION,
    CONSTRAINT FK_payments_payment_methods
        FOREIGN KEY (payment_method_id)
        REFERENCES dbo.payment_methods (payment_method_id)
        ON DELETE NO ACTION
        ON UPDATE NO ACTION,
    CONSTRAINT FK_payments_register_shifts
        FOREIGN KEY (register_shift_id)
        REFERENCES dbo.register_shifts (register_shift_id)
        ON DELETE NO ACTION
        ON UPDATE NO ACTION,
    CONSTRAINT FK_payments_recorded_by_users
        FOREIGN KEY (recorded_by_user_id)
        REFERENCES dbo.users (user_id)
        ON DELETE NO ACTION
        ON UPDATE NO ACTION,
    CONSTRAINT CK_payments_amount
        CHECK (amount > 0),
    CONSTRAINT CK_payments_refunded_amount
        CHECK (refunded_amount >= 0 AND refunded_amount <= amount),
    CONSTRAINT CK_payments_status
        CHECK (status IN ('COMPLETED', 'VOIDED', 'REFUNDED', 'PARTIALLY_REFUNDED')),
    CONSTRAINT CK_payments_refund_state
        CHECK (
            (status IN ('COMPLETED', 'VOIDED')
                AND refunded_amount = 0
                AND refunded_at IS NULL)
            OR
            (status = 'PARTIALLY_REFUNDED'
                AND refunded_amount > 0
                AND refunded_amount < amount
                AND refunded_at IS NOT NULL
                AND refunded_at >= paid_at)
            OR
            (status = 'REFUNDED'
                AND refunded_amount = amount
                AND refunded_at IS NOT NULL
                AND refunded_at >= paid_at)
        ),
    CONSTRAINT CK_payments_external_reference_not_blank
        CHECK (external_reference IS NULL
            OR LEN(LTRIM(RTRIM(external_reference))) > 0)
);

CREATE INDEX IX_payments_order_paid
    ON dbo.payments (order_id, paid_at DESC);

CREATE INDEX IX_payments_method_paid
    ON dbo.payments (payment_method_id, paid_at DESC);

CREATE INDEX IX_payments_register_shift_paid
    ON dbo.payments (register_shift_id, paid_at DESC)
    WHERE register_shift_id IS NOT NULL;

CREATE TABLE dbo.cash_movements
(
    cash_movement_id    BIGINT IDENTITY (1, 1) NOT NULL,
    register_shift_id   BIGINT NOT NULL,
    recorded_by_user_id BIGINT NOT NULL,
    movement_type       VARCHAR(20) NOT NULL,
    amount              DECIMAL(19, 4) NOT NULL,
    reason              NVARCHAR(500) NOT NULL,
    reference_number    NVARCHAR(100) NULL,
    notes               NVARCHAR(1000) NULL,
    moved_at            DATETIME2(3) NOT NULL
        CONSTRAINT DF_cash_movements_moved_at DEFAULT (SYSUTCDATETIME()),
    created_at          DATETIME2(3) NOT NULL
        CONSTRAINT DF_cash_movements_created_at DEFAULT (SYSUTCDATETIME()),
    row_version         ROWVERSION NOT NULL,

    CONSTRAINT PK_cash_movements
        PRIMARY KEY CLUSTERED (cash_movement_id),
    CONSTRAINT FK_cash_movements_register_shifts
        FOREIGN KEY (register_shift_id)
        REFERENCES dbo.register_shifts (register_shift_id)
        ON DELETE NO ACTION
        ON UPDATE NO ACTION,
    CONSTRAINT FK_cash_movements_recorded_by_users
        FOREIGN KEY (recorded_by_user_id)
        REFERENCES dbo.users (user_id)
        ON DELETE NO ACTION
        ON UPDATE NO ACTION,
    CONSTRAINT CK_cash_movements_movement_type
        CHECK (movement_type IN ('CASH_IN', 'CASH_OUT')),
    CONSTRAINT CK_cash_movements_amount
        CHECK (amount > 0),
    CONSTRAINT CK_cash_movements_reason_not_blank
        CHECK (LEN(LTRIM(RTRIM(reason))) > 0),
    CONSTRAINT CK_cash_movements_reference_number_not_blank
        CHECK (reference_number IS NULL OR LEN(LTRIM(RTRIM(reference_number))) > 0),
    CONSTRAINT CK_cash_movements_notes_not_blank
        CHECK (notes IS NULL OR LEN(LTRIM(RTRIM(notes))) > 0)
);

CREATE INDEX IX_cash_movements_register_shift_moved
    ON dbo.cash_movements (register_shift_id, moved_at DESC);

CREATE TABLE dbo.receipts
(
    receipt_id                       BIGINT IDENTITY (1, 1) NOT NULL,
    order_id                         BIGINT NOT NULL,
    document_number                  VARCHAR(50) NOT NULL,
    document_type                    VARCHAR(20) NOT NULL,
    issued_at                        DATETIME2(3) NOT NULL
        CONSTRAINT DF_receipts_issued_at DEFAULT (SYSUTCDATETIME()),
    issued_by_user_id                BIGINT NOT NULL,
    subtotal_amount                  DECIMAL(19, 4) NOT NULL,
    discount_amount                  DECIMAL(19, 4) NOT NULL,
    tax_amount                       DECIMAL(19, 4) NOT NULL,
    total_amount                     DECIMAL(19, 4) NOT NULL,
    customer_name_snapshot           NVARCHAR(200) NULL,
    business_name_snapshot           NVARCHAR(200) NULL,
    tax_registration_number_snapshot NVARCHAR(100) NULL,
    billing_address_snapshot         NVARCHAR(500) NULL,
    created_at                       DATETIME2(3) NOT NULL
        CONSTRAINT DF_receipts_created_at DEFAULT (SYSUTCDATETIME()),
    row_version                      ROWVERSION NOT NULL,

    CONSTRAINT PK_receipts
        PRIMARY KEY CLUSTERED (receipt_id),
    CONSTRAINT UQ_receipts_document_number
        UNIQUE (document_number),
    CONSTRAINT FK_receipts_orders
        FOREIGN KEY (order_id)
        REFERENCES dbo.orders (order_id)
        ON DELETE NO ACTION
        ON UPDATE NO ACTION,
    CONSTRAINT FK_receipts_issued_by_users
        FOREIGN KEY (issued_by_user_id)
        REFERENCES dbo.users (user_id)
        ON DELETE NO ACTION
        ON UPDATE NO ACTION,
    CONSTRAINT CK_receipts_document_number_not_blank
        CHECK (LEN(LTRIM(RTRIM(document_number))) > 0
            AND document_number = LTRIM(RTRIM(document_number))),
    CONSTRAINT CK_receipts_document_type
        CHECK (document_type IN ('RECEIPT', 'INVOICE', 'CREDIT_NOTE')),
    CONSTRAINT CK_receipts_monetary_amounts
        CHECK (subtotal_amount >= 0
            AND discount_amount >= 0
            AND tax_amount >= 0
            AND total_amount >= 0),
    CONSTRAINT CK_receipts_discount_not_greater_than_subtotal
        CHECK (discount_amount <= subtotal_amount),
    CONSTRAINT CK_receipts_total_amount
        CHECK (total_amount = subtotal_amount - discount_amount + tax_amount),
    CONSTRAINT CK_receipts_customer_name_snapshot_not_blank
        CHECK (customer_name_snapshot IS NULL
            OR LEN(LTRIM(RTRIM(customer_name_snapshot))) > 0),
    CONSTRAINT CK_receipts_business_name_snapshot_not_blank
        CHECK (business_name_snapshot IS NULL
            OR LEN(LTRIM(RTRIM(business_name_snapshot))) > 0),
    CONSTRAINT CK_receipts_tax_registration_number_snapshot_not_blank
        CHECK (tax_registration_number_snapshot IS NULL
            OR LEN(LTRIM(RTRIM(tax_registration_number_snapshot))) > 0),
    CONSTRAINT CK_receipts_billing_address_snapshot_not_blank
        CHECK (billing_address_snapshot IS NULL
            OR LEN(LTRIM(RTRIM(billing_address_snapshot))) > 0)
);

CREATE INDEX IX_receipts_order_issued
    ON dbo.receipts (order_id, issued_at DESC);

CREATE INDEX IX_receipts_type_issued
    ON dbo.receipts (document_type, issued_at DESC);
