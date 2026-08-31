SET NOCOUNT ON;
SET XACT_ABORT ON;

CREATE TABLE dbo.loyalty_transactions
(
    loyalty_transaction_id         BIGINT IDENTITY (1, 1) NOT NULL,
    loyalty_account_id             BIGINT NOT NULL,
    order_id                       BIGINT NULL,
    recorded_by_user_id            BIGINT NULL,
    related_loyalty_transaction_id BIGINT NULL,
    transaction_type               VARCHAR(20) NOT NULL,
    points_delta                   BIGINT NOT NULL,
    reason                         NVARCHAR(500) NULL,
    notes                          NVARCHAR(1000) NULL,
    occurred_at                    DATETIME2(3) NOT NULL
        CONSTRAINT DF_loyalty_transactions_occurred_at DEFAULT (SYSUTCDATETIME()),
    created_at                     DATETIME2(3) NOT NULL
        CONSTRAINT DF_loyalty_transactions_created_at DEFAULT (SYSUTCDATETIME()),
    row_version                    ROWVERSION NOT NULL,

    CONSTRAINT PK_loyalty_transactions
        PRIMARY KEY CLUSTERED (loyalty_transaction_id),
    CONSTRAINT FK_loyalty_transactions_loyalty_accounts
        FOREIGN KEY (loyalty_account_id)
        REFERENCES dbo.customer_loyalty_accounts (loyalty_account_id)
        ON DELETE NO ACTION
        ON UPDATE NO ACTION,
    CONSTRAINT FK_loyalty_transactions_orders
        FOREIGN KEY (order_id)
        REFERENCES dbo.orders (order_id)
        ON DELETE NO ACTION
        ON UPDATE NO ACTION,
    CONSTRAINT FK_loyalty_transactions_recorded_by_users
        FOREIGN KEY (recorded_by_user_id)
        REFERENCES dbo.users (user_id)
        ON DELETE NO ACTION
        ON UPDATE NO ACTION,
    CONSTRAINT FK_loyalty_transactions_related_transaction
        FOREIGN KEY (related_loyalty_transaction_id)
        REFERENCES dbo.loyalty_transactions (loyalty_transaction_id)
        ON DELETE NO ACTION
        ON UPDATE NO ACTION,
    CONSTRAINT CK_loyalty_transactions_transaction_type
        CHECK (transaction_type IN ('EARN', 'REDEEM', 'ADJUSTMENT', 'REVERSAL')),
    CONSTRAINT CK_loyalty_transactions_points_delta
        CHECK (points_delta <> 0
            AND (transaction_type <> 'EARN' OR points_delta > 0)
            AND (transaction_type <> 'REDEEM' OR points_delta < 0)),
    CONSTRAINT CK_loyalty_transactions_related_transaction
        CHECK (related_loyalty_transaction_id IS NULL
            OR related_loyalty_transaction_id <> loyalty_transaction_id),
    CONSTRAINT CK_loyalty_transactions_reversal_reference
        CHECK (transaction_type <> 'REVERSAL'
            OR related_loyalty_transaction_id IS NOT NULL),
    CONSTRAINT CK_loyalty_transactions_adjustment_audit
        CHECK (transaction_type <> 'ADJUSTMENT'
            OR (recorded_by_user_id IS NOT NULL
                AND reason IS NOT NULL
                AND LEN(LTRIM(RTRIM(reason))) > 0)),
    CONSTRAINT CK_loyalty_transactions_reason_not_blank
        CHECK (reason IS NULL OR LEN(LTRIM(RTRIM(reason))) > 0),
    CONSTRAINT CK_loyalty_transactions_notes_not_blank
        CHECK (notes IS NULL OR LEN(LTRIM(RTRIM(notes))) > 0)
);

CREATE INDEX IX_loyalty_transactions_account_occurred
    ON dbo.loyalty_transactions (loyalty_account_id, occurred_at DESC);

CREATE INDEX IX_loyalty_transactions_order_occurred
    ON dbo.loyalty_transactions (order_id, occurred_at DESC)
    WHERE order_id IS NOT NULL;

CREATE INDEX IX_loyalty_transactions_related_transaction
    ON dbo.loyalty_transactions (related_loyalty_transaction_id)
    WHERE related_loyalty_transaction_id IS NOT NULL;

CREATE INDEX IX_loyalty_transactions_recorded_by_user
    ON dbo.loyalty_transactions (recorded_by_user_id, occurred_at DESC)
    WHERE recorded_by_user_id IS NOT NULL;
