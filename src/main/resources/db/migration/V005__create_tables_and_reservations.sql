SET NOCOUNT ON;
SET XACT_ABORT ON;

CREATE TABLE dbo.cafe_tables
(
    cafe_table_id BIGINT IDENTITY (1, 1) NOT NULL,
    table_code    VARCHAR(30) NOT NULL,
    table_name    NVARCHAR(100) NOT NULL,
    capacity      SMALLINT NOT NULL,
    status        VARCHAR(20) NOT NULL
        CONSTRAINT DF_cafe_tables_status DEFAULT ('AVAILABLE'),
    display_order INT NOT NULL
        CONSTRAINT DF_cafe_tables_display_order DEFAULT (0),
    is_active     BIT NOT NULL
        CONSTRAINT DF_cafe_tables_is_active DEFAULT (1),
    created_at    DATETIME2(3) NOT NULL
        CONSTRAINT DF_cafe_tables_created_at DEFAULT (SYSUTCDATETIME()),
    updated_at    DATETIME2(3) NOT NULL
        CONSTRAINT DF_cafe_tables_updated_at DEFAULT (SYSUTCDATETIME()),
    row_version   ROWVERSION NOT NULL,

    CONSTRAINT PK_cafe_tables
        PRIMARY KEY CLUSTERED (cafe_table_id),
    CONSTRAINT UQ_cafe_tables_table_code
        UNIQUE (table_code),
    CONSTRAINT CK_cafe_tables_table_code_not_blank
        CHECK (LEN(LTRIM(RTRIM(table_code))) > 0
            AND table_code = LTRIM(RTRIM(table_code))),
    CONSTRAINT CK_cafe_tables_table_name_not_blank
        CHECK (LEN(LTRIM(RTRIM(table_name))) > 0),
    CONSTRAINT CK_cafe_tables_capacity
        CHECK (capacity > 0),
    CONSTRAINT CK_cafe_tables_status
        CHECK (status IN ('AVAILABLE', 'OCCUPIED', 'RESERVED', 'OUT_OF_SERVICE')),
    CONSTRAINT CK_cafe_tables_display_order
        CHECK (display_order >= 0)
);

CREATE INDEX IX_cafe_tables_active_status_display
    ON dbo.cafe_tables (is_active, status, display_order, table_name);

CREATE TABLE dbo.table_reservations
(
    table_reservation_id BIGINT IDENTITY (1, 1) NOT NULL,
    reservation_number   VARCHAR(30) NOT NULL,
    cafe_table_id        BIGINT NOT NULL,
    customer_id          BIGINT NULL,
    guest_name           NVARCHAR(200) NOT NULL,
    contact_phone        NVARCHAR(30) NULL,
    contact_email        NVARCHAR(254) NULL,
    reservation_start_at DATETIME2(3) NOT NULL,
    party_size           SMALLINT NOT NULL,
    status               VARCHAR(20) NOT NULL
        CONSTRAINT DF_table_reservations_status DEFAULT ('PENDING'),
    notes                NVARCHAR(1000) NULL,
    created_at           DATETIME2(3) NOT NULL
        CONSTRAINT DF_table_reservations_created_at DEFAULT (SYSUTCDATETIME()),
    updated_at           DATETIME2(3) NOT NULL
        CONSTRAINT DF_table_reservations_updated_at DEFAULT (SYSUTCDATETIME()),
    row_version          ROWVERSION NOT NULL,

    CONSTRAINT PK_table_reservations
        PRIMARY KEY CLUSTERED (table_reservation_id),
    CONSTRAINT UQ_table_reservations_reservation_number
        UNIQUE (reservation_number),
    CONSTRAINT FK_table_reservations_cafe_tables
        FOREIGN KEY (cafe_table_id)
        REFERENCES dbo.cafe_tables (cafe_table_id)
        ON DELETE NO ACTION
        ON UPDATE NO ACTION,
    CONSTRAINT FK_table_reservations_customers
        FOREIGN KEY (customer_id)
        REFERENCES dbo.customers (customer_id)
        ON DELETE NO ACTION
        ON UPDATE NO ACTION,
    CONSTRAINT CK_table_reservations_reservation_number_not_blank
        CHECK (LEN(LTRIM(RTRIM(reservation_number))) > 0
            AND reservation_number = LTRIM(RTRIM(reservation_number))),
    CONSTRAINT CK_table_reservations_guest_name_not_blank
        CHECK (LEN(LTRIM(RTRIM(guest_name))) > 0),
    CONSTRAINT CK_table_reservations_contact_phone_not_blank
        CHECK (contact_phone IS NULL OR LEN(LTRIM(RTRIM(contact_phone))) > 0),
    CONSTRAINT CK_table_reservations_contact_email_not_blank
        CHECK (contact_email IS NULL OR LEN(LTRIM(RTRIM(contact_email))) > 0),
    CONSTRAINT CK_table_reservations_party_size
        CHECK (party_size > 0),
    CONSTRAINT CK_table_reservations_status
        CHECK (status IN
            ('PENDING', 'CONFIRMED', 'SEATED', 'COMPLETED', 'CANCELLED', 'NO_SHOW')),
    CONSTRAINT CK_table_reservations_notes_not_blank
        CHECK (notes IS NULL OR LEN(LTRIM(RTRIM(notes))) > 0)
);

CREATE INDEX IX_table_reservations_table_start_status
    ON dbo.table_reservations (cafe_table_id, reservation_start_at, status);

CREATE INDEX IX_table_reservations_customer_start_status
    ON dbo.table_reservations (customer_id, reservation_start_at, status)
    WHERE customer_id IS NOT NULL;

CREATE INDEX IX_table_reservations_status_start
    ON dbo.table_reservations (status, reservation_start_at);
