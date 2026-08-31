SET NOCOUNT ON;
SET XACT_ABORT ON;

CREATE TABLE dbo.discounts
(
    discount_id     BIGINT IDENTITY (1, 1) NOT NULL,
    discount_code   VARCHAR(50) NOT NULL,
    discount_name   NVARCHAR(150) NOT NULL,
    discount_type   VARCHAR(20) NOT NULL,
    discount_value  DECIMAL(19, 4) NOT NULL,
    valid_from      DATETIME2(3) NULL,
    valid_until     DATETIME2(3) NULL,
    is_active       BIT NOT NULL
        CONSTRAINT DF_discounts_is_active DEFAULT (1),
    created_at      DATETIME2(3) NOT NULL
        CONSTRAINT DF_discounts_created_at DEFAULT (SYSUTCDATETIME()),
    updated_at      DATETIME2(3) NOT NULL
        CONSTRAINT DF_discounts_updated_at DEFAULT (SYSUTCDATETIME()),
    row_version     ROWVERSION NOT NULL,

    CONSTRAINT PK_discounts
        PRIMARY KEY CLUSTERED (discount_id),
    CONSTRAINT UQ_discounts_discount_code
        UNIQUE (discount_code),
    CONSTRAINT CK_discounts_discount_code_not_blank
        CHECK (LEN(LTRIM(RTRIM(discount_code))) > 0
            AND discount_code = LTRIM(RTRIM(discount_code))),
    CONSTRAINT CK_discounts_discount_name_not_blank
        CHECK (LEN(LTRIM(RTRIM(discount_name))) > 0),
    CONSTRAINT CK_discounts_discount_type
        CHECK (discount_type IN ('PERCENTAGE', 'FIXED_AMOUNT')),
    CONSTRAINT CK_discounts_discount_value
        CHECK (discount_value >= 0
            AND (discount_type <> 'PERCENTAGE' OR discount_value <= 100)),
    CONSTRAINT CK_discounts_validity_period
        CHECK (valid_from IS NULL OR valid_until IS NULL OR valid_until > valid_from)
);

CREATE INDEX IX_discounts_active_validity
    ON dbo.discounts (is_active, valid_from, valid_until);

CREATE TABLE dbo.payment_methods
(
    payment_method_id BIGINT IDENTITY (1, 1) NOT NULL,
    method_code       VARCHAR(50) NOT NULL,
    method_name       NVARCHAR(100) NOT NULL,
    display_order     INT NOT NULL
        CONSTRAINT DF_payment_methods_display_order DEFAULT (0),
    is_active         BIT NOT NULL
        CONSTRAINT DF_payment_methods_is_active DEFAULT (1),
    created_at        DATETIME2(3) NOT NULL
        CONSTRAINT DF_payment_methods_created_at DEFAULT (SYSUTCDATETIME()),
    updated_at        DATETIME2(3) NOT NULL
        CONSTRAINT DF_payment_methods_updated_at DEFAULT (SYSUTCDATETIME()),
    row_version       ROWVERSION NOT NULL,

    CONSTRAINT PK_payment_methods
        PRIMARY KEY CLUSTERED (payment_method_id),
    CONSTRAINT UQ_payment_methods_method_code
        UNIQUE (method_code),
    CONSTRAINT CK_payment_methods_method_code_not_blank
        CHECK (LEN(LTRIM(RTRIM(method_code))) > 0
            AND method_code = LTRIM(RTRIM(method_code))),
    CONSTRAINT CK_payment_methods_method_name_not_blank
        CHECK (LEN(LTRIM(RTRIM(method_name))) > 0),
    CONSTRAINT CK_payment_methods_display_order
        CHECK (display_order >= 0)
);

CREATE INDEX IX_payment_methods_active_display
    ON dbo.payment_methods (is_active, display_order, method_name);

CREATE TABLE dbo.registers
(
    register_id   BIGINT IDENTITY (1, 1) NOT NULL,
    register_code VARCHAR(50) NOT NULL,
    register_name NVARCHAR(100) NOT NULL,
    display_order INT NOT NULL
        CONSTRAINT DF_registers_display_order DEFAULT (0),
    is_active     BIT NOT NULL
        CONSTRAINT DF_registers_is_active DEFAULT (1),
    created_at    DATETIME2(3) NOT NULL
        CONSTRAINT DF_registers_created_at DEFAULT (SYSUTCDATETIME()),
    updated_at    DATETIME2(3) NOT NULL
        CONSTRAINT DF_registers_updated_at DEFAULT (SYSUTCDATETIME()),
    row_version   ROWVERSION NOT NULL,

    CONSTRAINT PK_registers
        PRIMARY KEY CLUSTERED (register_id),
    CONSTRAINT UQ_registers_register_code
        UNIQUE (register_code),
    CONSTRAINT CK_registers_register_code_not_blank
        CHECK (LEN(LTRIM(RTRIM(register_code))) > 0
            AND register_code = LTRIM(RTRIM(register_code))),
    CONSTRAINT CK_registers_register_name_not_blank
        CHECK (LEN(LTRIM(RTRIM(register_name))) > 0),
    CONSTRAINT CK_registers_display_order
        CHECK (display_order >= 0)
);

CREATE INDEX IX_registers_active_display
    ON dbo.registers (is_active, display_order, register_name);

CREATE TABLE dbo.register_shifts
(
    register_shift_id    BIGINT IDENTITY (1, 1) NOT NULL,
    register_id          BIGINT NOT NULL,
    opened_by_user_id    BIGINT NOT NULL,
    closed_by_user_id    BIGINT NULL,
    opened_at            DATETIME2(3) NOT NULL
        CONSTRAINT DF_register_shifts_opened_at DEFAULT (SYSUTCDATETIME()),
    closed_at            DATETIME2(3) NULL,
    opening_cash         DECIMAL(19, 4) NOT NULL,
    expected_closing_cash DECIMAL(19, 4) NULL,
    actual_closing_cash  DECIMAL(19, 4) NULL,
    cash_difference      DECIMAL(19, 4) NULL,
    status               VARCHAR(20) NOT NULL
        CONSTRAINT DF_register_shifts_status DEFAULT ('OPEN'),
    created_at           DATETIME2(3) NOT NULL
        CONSTRAINT DF_register_shifts_created_at DEFAULT (SYSUTCDATETIME()),
    updated_at           DATETIME2(3) NOT NULL
        CONSTRAINT DF_register_shifts_updated_at DEFAULT (SYSUTCDATETIME()),
    row_version          ROWVERSION NOT NULL,

    CONSTRAINT PK_register_shifts
        PRIMARY KEY CLUSTERED (register_shift_id),
    CONSTRAINT FK_register_shifts_registers
        FOREIGN KEY (register_id)
        REFERENCES dbo.registers (register_id)
        ON DELETE NO ACTION
        ON UPDATE NO ACTION,
    CONSTRAINT FK_register_shifts_opened_by_users
        FOREIGN KEY (opened_by_user_id)
        REFERENCES dbo.users (user_id)
        ON DELETE NO ACTION
        ON UPDATE NO ACTION,
    CONSTRAINT FK_register_shifts_closed_by_users
        FOREIGN KEY (closed_by_user_id)
        REFERENCES dbo.users (user_id)
        ON DELETE NO ACTION
        ON UPDATE NO ACTION,
    CONSTRAINT CK_register_shifts_status
        CHECK (status IN ('OPEN', 'CLOSED')),
    CONSTRAINT CK_register_shifts_opening_cash
        CHECK (opening_cash >= 0),
    CONSTRAINT CK_register_shifts_expected_closing_cash
        CHECK (expected_closing_cash IS NULL OR expected_closing_cash >= 0),
    CONSTRAINT CK_register_shifts_actual_closing_cash
        CHECK (actual_closing_cash IS NULL OR actual_closing_cash >= 0),
    CONSTRAINT CK_register_shifts_closed_at
        CHECK (closed_at IS NULL OR closed_at > opened_at),
    CONSTRAINT CK_register_shifts_lifecycle
        CHECK (
            (status = 'OPEN'
                AND closed_at IS NULL
                AND closed_by_user_id IS NULL
                AND expected_closing_cash IS NULL
                AND actual_closing_cash IS NULL
                AND cash_difference IS NULL)
            OR
            (status = 'CLOSED'
                AND closed_at IS NOT NULL
                AND closed_by_user_id IS NOT NULL
                AND expected_closing_cash IS NOT NULL
                AND actual_closing_cash IS NOT NULL
                AND cash_difference IS NOT NULL
                AND cash_difference = actual_closing_cash - expected_closing_cash)
        )
);

CREATE UNIQUE INDEX UX_register_shifts_one_open_per_register
    ON dbo.register_shifts (register_id)
    WHERE status = 'OPEN';

CREATE UNIQUE INDEX UX_register_shifts_one_open_per_user
    ON dbo.register_shifts (opened_by_user_id)
    WHERE status = 'OPEN';

CREATE INDEX IX_register_shifts_register_opened
    ON dbo.register_shifts (register_id, opened_at DESC);

CREATE INDEX IX_register_shifts_user_opened
    ON dbo.register_shifts (opened_by_user_id, opened_at DESC);
