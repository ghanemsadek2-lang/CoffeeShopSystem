SET NOCOUNT ON;
SET XACT_ABORT ON;

CREATE TABLE dbo.customers
(
    customer_id              BIGINT IDENTITY (1, 1) NOT NULL,
    customer_number          VARCHAR(30) NOT NULL,
    first_name               NVARCHAR(100) NOT NULL,
    last_name                NVARCHAR(100) NULL,
    legal_name               NVARCHAR(200) NULL,
    tax_registration_number  NVARCHAR(100) NULL,
    phone                    NVARCHAR(30) NULL,
    email                    NVARCHAR(254) NULL,
    notes                    NVARCHAR(1000) NULL,
    is_active                BIT NOT NULL
        CONSTRAINT DF_customers_is_active DEFAULT (1),
    created_at               DATETIME2(3) NOT NULL
        CONSTRAINT DF_customers_created_at DEFAULT (SYSUTCDATETIME()),
    updated_at               DATETIME2(3) NOT NULL
        CONSTRAINT DF_customers_updated_at DEFAULT (SYSUTCDATETIME()),
    row_version              ROWVERSION NOT NULL,

    CONSTRAINT PK_customers
        PRIMARY KEY CLUSTERED (customer_id),
    CONSTRAINT UQ_customers_customer_number
        UNIQUE (customer_number),
    CONSTRAINT CK_customers_customer_number_not_blank
        CHECK (LEN(LTRIM(RTRIM(customer_number))) > 0
            AND customer_number = LTRIM(RTRIM(customer_number))),
    CONSTRAINT CK_customers_first_name_not_blank
        CHECK (LEN(LTRIM(RTRIM(first_name))) > 0),
    CONSTRAINT CK_customers_last_name_not_blank
        CHECK (last_name IS NULL OR LEN(LTRIM(RTRIM(last_name))) > 0),
    CONSTRAINT CK_customers_legal_name_not_blank
        CHECK (legal_name IS NULL OR LEN(LTRIM(RTRIM(legal_name))) > 0),
    CONSTRAINT CK_customers_tax_registration_number_not_blank
        CHECK (tax_registration_number IS NULL
            OR LEN(LTRIM(RTRIM(tax_registration_number))) > 0),
    CONSTRAINT CK_customers_phone_not_blank
        CHECK (phone IS NULL OR LEN(LTRIM(RTRIM(phone))) > 0),
    CONSTRAINT CK_customers_email_not_blank
        CHECK (email IS NULL OR LEN(LTRIM(RTRIM(email))) > 0),
    CONSTRAINT CK_customers_notes_not_blank
        CHECK (notes IS NULL OR LEN(LTRIM(RTRIM(notes))) > 0)
);

CREATE INDEX IX_customers_phone
    ON dbo.customers (phone)
    WHERE phone IS NOT NULL;

CREATE INDEX IX_customers_email
    ON dbo.customers (email)
    WHERE email IS NOT NULL;

CREATE INDEX IX_customers_name
    ON dbo.customers (last_name, first_name);

CREATE INDEX IX_customers_tax_registration_number
    ON dbo.customers (tax_registration_number)
    WHERE tax_registration_number IS NOT NULL;

CREATE TABLE dbo.customer_addresses
(
    customer_address_id   BIGINT IDENTITY (1, 1) NOT NULL,
    customer_id           BIGINT NOT NULL,
    label                 NVARCHAR(50) NULL,
    recipient_name        NVARCHAR(200) NULL,
    phone                 NVARCHAR(30) NULL,
    address_line1         NVARCHAR(200) NOT NULL,
    address_line2         NVARCHAR(200) NULL,
    city                  NVARCHAR(100) NOT NULL,
    region                NVARCHAR(100) NULL,
    postal_code           NVARCHAR(20) NULL,
    delivery_instructions NVARCHAR(500) NULL,
    is_default            BIT NOT NULL
        CONSTRAINT DF_customer_addresses_is_default DEFAULT (0),
    is_active             BIT NOT NULL
        CONSTRAINT DF_customer_addresses_is_active DEFAULT (1),
    created_at            DATETIME2(3) NOT NULL
        CONSTRAINT DF_customer_addresses_created_at DEFAULT (SYSUTCDATETIME()),
    updated_at            DATETIME2(3) NOT NULL
        CONSTRAINT DF_customer_addresses_updated_at DEFAULT (SYSUTCDATETIME()),
    row_version           ROWVERSION NOT NULL,

    CONSTRAINT PK_customer_addresses
        PRIMARY KEY CLUSTERED (customer_address_id),
    CONSTRAINT FK_customer_addresses_customers
        FOREIGN KEY (customer_id)
        REFERENCES dbo.customers (customer_id)
        ON DELETE NO ACTION
        ON UPDATE NO ACTION,
    CONSTRAINT CK_customer_addresses_label_not_blank
        CHECK (label IS NULL OR LEN(LTRIM(RTRIM(label))) > 0),
    CONSTRAINT CK_customer_addresses_recipient_name_not_blank
        CHECK (recipient_name IS NULL OR LEN(LTRIM(RTRIM(recipient_name))) > 0),
    CONSTRAINT CK_customer_addresses_phone_not_blank
        CHECK (phone IS NULL OR LEN(LTRIM(RTRIM(phone))) > 0),
    CONSTRAINT CK_customer_addresses_address_line1_not_blank
        CHECK (LEN(LTRIM(RTRIM(address_line1))) > 0),
    CONSTRAINT CK_customer_addresses_address_line2_not_blank
        CHECK (address_line2 IS NULL OR LEN(LTRIM(RTRIM(address_line2))) > 0),
    CONSTRAINT CK_customer_addresses_city_not_blank
        CHECK (LEN(LTRIM(RTRIM(city))) > 0),
    CONSTRAINT CK_customer_addresses_region_not_blank
        CHECK (region IS NULL OR LEN(LTRIM(RTRIM(region))) > 0),
    CONSTRAINT CK_customer_addresses_postal_code_not_blank
        CHECK (postal_code IS NULL OR LEN(LTRIM(RTRIM(postal_code))) > 0),
    CONSTRAINT CK_customer_addresses_delivery_instructions_not_blank
        CHECK (delivery_instructions IS NULL
            OR LEN(LTRIM(RTRIM(delivery_instructions))) > 0)
);

CREATE UNIQUE INDEX UX_customer_addresses_one_active_default
    ON dbo.customer_addresses (customer_id)
    WHERE is_default = 1 AND is_active = 1;

CREATE INDEX IX_customer_addresses_customer_active
    ON dbo.customer_addresses (customer_id, is_active);

CREATE TABLE dbo.customer_loyalty_accounts
(
    loyalty_account_id     BIGINT IDENTITY (1, 1) NOT NULL,
    customer_id            BIGINT NOT NULL,
    membership_number      VARCHAR(50) NOT NULL,
    points_balance         BIGINT NOT NULL
        CONSTRAINT DF_customer_loyalty_accounts_points_balance DEFAULT (0),
    lifetime_points_earned BIGINT NOT NULL
        CONSTRAINT DF_customer_loyalty_accounts_lifetime_points_earned DEFAULT (0),
    enrolled_at            DATETIME2(3) NOT NULL
        CONSTRAINT DF_customer_loyalty_accounts_enrolled_at DEFAULT (SYSUTCDATETIME()),
    status                 VARCHAR(20) NOT NULL
        CONSTRAINT DF_customer_loyalty_accounts_status DEFAULT ('ACTIVE'),
    updated_at             DATETIME2(3) NOT NULL
        CONSTRAINT DF_customer_loyalty_accounts_updated_at DEFAULT (SYSUTCDATETIME()),
    row_version            ROWVERSION NOT NULL,

    CONSTRAINT PK_customer_loyalty_accounts
        PRIMARY KEY CLUSTERED (loyalty_account_id),
    CONSTRAINT UQ_customer_loyalty_accounts_customer
        UNIQUE (customer_id),
    CONSTRAINT UQ_customer_loyalty_accounts_membership_number
        UNIQUE (membership_number),
    CONSTRAINT FK_customer_loyalty_accounts_customers
        FOREIGN KEY (customer_id)
        REFERENCES dbo.customers (customer_id)
        ON DELETE NO ACTION
        ON UPDATE NO ACTION,
    CONSTRAINT CK_customer_loyalty_accounts_membership_number_not_blank
        CHECK (LEN(LTRIM(RTRIM(membership_number))) > 0
            AND membership_number = LTRIM(RTRIM(membership_number))),
    CONSTRAINT CK_customer_loyalty_accounts_points_balance
        CHECK (points_balance >= 0),
    CONSTRAINT CK_customer_loyalty_accounts_lifetime_points_earned
        CHECK (lifetime_points_earned >= 0),
    CONSTRAINT CK_customer_loyalty_accounts_status
        CHECK (status IN ('ACTIVE', 'SUSPENDED', 'CLOSED'))
);

CREATE INDEX IX_customer_loyalty_accounts_customer_status
    ON dbo.customer_loyalty_accounts (customer_id, status);
