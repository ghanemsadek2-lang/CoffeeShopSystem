SET NOCOUNT ON;
SET XACT_ABORT ON;

CREATE TABLE dbo.payment_refunds
(
    payment_refund_id   BIGINT IDENTITY (1, 1) NOT NULL,
    payment_id          BIGINT NOT NULL,
    register_shift_id   BIGINT NULL,
    refunded_by_user_id BIGINT NOT NULL,
    amount              DECIMAL(19, 4) NOT NULL,
    reason              NVARCHAR(500) NOT NULL,
    reference_number    NVARCHAR(100) NULL,
    notes               NVARCHAR(1000) NULL,
    refunded_at         DATETIME2(3) NOT NULL
        CONSTRAINT DF_payment_refunds_refunded_at DEFAULT (SYSUTCDATETIME()),

    CONSTRAINT PK_payment_refunds
        PRIMARY KEY CLUSTERED (payment_refund_id),
    CONSTRAINT FK_payment_refunds_payments
        FOREIGN KEY (payment_id)
        REFERENCES dbo.payments (payment_id)
        ON DELETE NO ACTION
        ON UPDATE NO ACTION,
    CONSTRAINT FK_payment_refunds_register_shifts
        FOREIGN KEY (register_shift_id)
        REFERENCES dbo.register_shifts (register_shift_id)
        ON DELETE NO ACTION
        ON UPDATE NO ACTION,
    CONSTRAINT FK_payment_refunds_refunded_by_users
        FOREIGN KEY (refunded_by_user_id)
        REFERENCES dbo.users (user_id)
        ON DELETE NO ACTION
        ON UPDATE NO ACTION,
    CONSTRAINT CK_payment_refunds_amount
        CHECK (amount > 0),
    CONSTRAINT CK_payment_refunds_reason_not_blank
        CHECK (LEN(LTRIM(RTRIM(reason))) > 0),
    CONSTRAINT CK_payment_refunds_reference_number_not_blank
        CHECK (reference_number IS NULL
            OR LEN(LTRIM(RTRIM(reference_number))) > 0),
    CONSTRAINT CK_payment_refunds_notes_not_blank
        CHECK (notes IS NULL OR LEN(LTRIM(RTRIM(notes))) > 0)
);

CREATE INDEX IX_payment_refunds_payment_refunded
    ON dbo.payment_refunds (payment_id, refunded_at DESC);

CREATE INDEX IX_payment_refunds_register_shift_refunded
    ON dbo.payment_refunds (register_shift_id, refunded_at DESC)
    WHERE register_shift_id IS NOT NULL;

CREATE INDEX IX_payment_refunds_user_refunded
    ON dbo.payment_refunds (refunded_by_user_id, refunded_at DESC);
