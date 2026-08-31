SET NOCOUNT ON;
SET XACT_ABORT ON;

CREATE TABLE dbo.preparation_stations
(
    preparation_station_id BIGINT IDENTITY (1, 1) NOT NULL,
    station_code           VARCHAR(30) NOT NULL,
    station_name           NVARCHAR(100) NOT NULL,
    display_order          INT NOT NULL
        CONSTRAINT DF_preparation_stations_display_order DEFAULT (0),
    is_active              BIT NOT NULL
        CONSTRAINT DF_preparation_stations_is_active DEFAULT (1),
    created_at             DATETIME2(3) NOT NULL
        CONSTRAINT DF_preparation_stations_created_at DEFAULT (SYSUTCDATETIME()),
    updated_at             DATETIME2(3) NOT NULL
        CONSTRAINT DF_preparation_stations_updated_at DEFAULT (SYSUTCDATETIME()),
    row_version            ROWVERSION NOT NULL,

    CONSTRAINT PK_preparation_stations
        PRIMARY KEY CLUSTERED (preparation_station_id),
    CONSTRAINT UQ_preparation_stations_station_code
        UNIQUE (station_code),
    CONSTRAINT UQ_preparation_stations_station_name
        UNIQUE (station_name),
    CONSTRAINT CK_preparation_stations_station_code_not_blank
        CHECK (LEN(LTRIM(RTRIM(station_code))) > 0
            AND station_code = LTRIM(RTRIM(station_code))),
    CONSTRAINT CK_preparation_stations_station_name_not_blank
        CHECK (LEN(LTRIM(RTRIM(station_name))) > 0),
    CONSTRAINT CK_preparation_stations_display_order
        CHECK (display_order >= 0)
);

CREATE INDEX IX_preparation_stations_active_display
    ON dbo.preparation_stations (is_active, display_order);

CREATE TABLE dbo.categories
(
    category_id   BIGINT IDENTITY (1, 1) NOT NULL,
    category_name NVARCHAR(100) NOT NULL,
    description   NVARCHAR(500) NULL,
    display_order INT NOT NULL
        CONSTRAINT DF_categories_display_order DEFAULT (0),
    is_active     BIT NOT NULL
        CONSTRAINT DF_categories_is_active DEFAULT (1),
    created_at    DATETIME2(3) NOT NULL
        CONSTRAINT DF_categories_created_at DEFAULT (SYSUTCDATETIME()),
    updated_at    DATETIME2(3) NOT NULL
        CONSTRAINT DF_categories_updated_at DEFAULT (SYSUTCDATETIME()),
    row_version   ROWVERSION NOT NULL,

    CONSTRAINT PK_categories
        PRIMARY KEY CLUSTERED (category_id),
    CONSTRAINT UQ_categories_category_name
        UNIQUE (category_name),
    CONSTRAINT CK_categories_category_name_not_blank
        CHECK (LEN(LTRIM(RTRIM(category_name))) > 0),
    CONSTRAINT CK_categories_description_not_blank
        CHECK (description IS NULL OR LEN(LTRIM(RTRIM(description))) > 0),
    CONSTRAINT CK_categories_display_order
        CHECK (display_order >= 0)
);

CREATE INDEX IX_categories_active_display
    ON dbo.categories (is_active, display_order, category_name);

CREATE TABLE dbo.products
(
    product_id              BIGINT IDENTITY (1, 1) NOT NULL,
    category_id             BIGINT NOT NULL,
    preparation_station_id  BIGINT NULL,
    product_code            VARCHAR(50) NOT NULL,
    product_name            NVARCHAR(150) NOT NULL,
    description             NVARCHAR(1000) NULL,
    image_path              NVARCHAR(500) NULL,
    display_order           INT NOT NULL
        CONSTRAINT DF_products_display_order DEFAULT (0),
    is_active               BIT NOT NULL
        CONSTRAINT DF_products_is_active DEFAULT (1),
    created_at              DATETIME2(3) NOT NULL
        CONSTRAINT DF_products_created_at DEFAULT (SYSUTCDATETIME()),
    updated_at              DATETIME2(3) NOT NULL
        CONSTRAINT DF_products_updated_at DEFAULT (SYSUTCDATETIME()),
    row_version             ROWVERSION NOT NULL,

    CONSTRAINT PK_products
        PRIMARY KEY CLUSTERED (product_id),
    CONSTRAINT UQ_products_product_code
        UNIQUE (product_code),
    CONSTRAINT FK_products_categories
        FOREIGN KEY (category_id)
        REFERENCES dbo.categories (category_id)
        ON DELETE NO ACTION
        ON UPDATE NO ACTION,
    CONSTRAINT FK_products_preparation_stations
        FOREIGN KEY (preparation_station_id)
        REFERENCES dbo.preparation_stations (preparation_station_id)
        ON DELETE NO ACTION
        ON UPDATE NO ACTION,
    CONSTRAINT CK_products_product_code_not_blank
        CHECK (LEN(LTRIM(RTRIM(product_code))) > 0
            AND product_code = LTRIM(RTRIM(product_code))),
    CONSTRAINT CK_products_product_name_not_blank
        CHECK (LEN(LTRIM(RTRIM(product_name))) > 0),
    CONSTRAINT CK_products_description_not_blank
        CHECK (description IS NULL OR LEN(LTRIM(RTRIM(description))) > 0),
    CONSTRAINT CK_products_image_path_not_blank
        CHECK (image_path IS NULL OR LEN(LTRIM(RTRIM(image_path))) > 0),
    CONSTRAINT CK_products_display_order
        CHECK (display_order >= 0)
);

CREATE INDEX IX_products_category_active_display
    ON dbo.products (category_id, is_active, display_order, product_name);

CREATE INDEX IX_products_preparation_station_active
    ON dbo.products (preparation_station_id, is_active)
    WHERE preparation_station_id IS NOT NULL;

CREATE TABLE dbo.product_variants
(
    product_variant_id      BIGINT IDENTITY (1, 1) NOT NULL,
    product_id              BIGINT NOT NULL,
    preparation_station_id  BIGINT NULL,
    variant_code            VARCHAR(50) NOT NULL,
    variant_name            NVARCHAR(100) NOT NULL,
    sku                     VARCHAR(80) NULL,
    selling_price           DECIMAL(19, 4) NOT NULL,
    display_order           INT NOT NULL
        CONSTRAINT DF_product_variants_display_order DEFAULT (0),
    is_default              BIT NOT NULL
        CONSTRAINT DF_product_variants_is_default DEFAULT (0),
    is_active               BIT NOT NULL
        CONSTRAINT DF_product_variants_is_active DEFAULT (1),
    created_at              DATETIME2(3) NOT NULL
        CONSTRAINT DF_product_variants_created_at DEFAULT (SYSUTCDATETIME()),
    updated_at              DATETIME2(3) NOT NULL
        CONSTRAINT DF_product_variants_updated_at DEFAULT (SYSUTCDATETIME()),
    row_version             ROWVERSION NOT NULL,

    CONSTRAINT PK_product_variants
        PRIMARY KEY CLUSTERED (product_variant_id),
    CONSTRAINT UQ_product_variants_product_code
        UNIQUE (product_id, variant_code),
    CONSTRAINT FK_product_variants_products
        FOREIGN KEY (product_id)
        REFERENCES dbo.products (product_id)
        ON DELETE NO ACTION
        ON UPDATE NO ACTION,
    CONSTRAINT FK_product_variants_preparation_stations
        FOREIGN KEY (preparation_station_id)
        REFERENCES dbo.preparation_stations (preparation_station_id)
        ON DELETE NO ACTION
        ON UPDATE NO ACTION,
    CONSTRAINT CK_product_variants_variant_code_not_blank
        CHECK (LEN(LTRIM(RTRIM(variant_code))) > 0
            AND variant_code = LTRIM(RTRIM(variant_code))),
    CONSTRAINT CK_product_variants_variant_name_not_blank
        CHECK (LEN(LTRIM(RTRIM(variant_name))) > 0),
    CONSTRAINT CK_product_variants_sku_not_blank
        CHECK (sku IS NULL OR LEN(LTRIM(RTRIM(sku))) > 0),
    CONSTRAINT CK_product_variants_selling_price
        CHECK (selling_price >= 0),
    CONSTRAINT CK_product_variants_display_order
        CHECK (display_order >= 0)
);

CREATE UNIQUE INDEX UX_product_variants_sku
    ON dbo.product_variants (sku)
    WHERE sku IS NOT NULL;

CREATE UNIQUE INDEX UX_product_variants_one_active_default
    ON dbo.product_variants (product_id)
    WHERE is_default = 1 AND is_active = 1;

CREATE INDEX IX_product_variants_product_active_display
    ON dbo.product_variants (product_id, is_active, display_order);

CREATE INDEX IX_product_variants_preparation_station_active
    ON dbo.product_variants (preparation_station_id, is_active)
    WHERE preparation_station_id IS NOT NULL;

CREATE TABLE dbo.order_sources
(
    order_source_id BIGINT IDENTITY (1, 1) NOT NULL,
    source_code     VARCHAR(50) NOT NULL,
    source_name     NVARCHAR(100) NOT NULL,
    display_order   INT NOT NULL
        CONSTRAINT DF_order_sources_display_order DEFAULT (0),
    is_active       BIT NOT NULL
        CONSTRAINT DF_order_sources_is_active DEFAULT (1),
    created_at      DATETIME2(3) NOT NULL
        CONSTRAINT DF_order_sources_created_at DEFAULT (SYSUTCDATETIME()),
    updated_at      DATETIME2(3) NOT NULL
        CONSTRAINT DF_order_sources_updated_at DEFAULT (SYSUTCDATETIME()),
    row_version     ROWVERSION NOT NULL,

    CONSTRAINT PK_order_sources
        PRIMARY KEY CLUSTERED (order_source_id),
    CONSTRAINT UQ_order_sources_source_code
        UNIQUE (source_code),
    CONSTRAINT CK_order_sources_source_code_not_blank
        CHECK (LEN(LTRIM(RTRIM(source_code))) > 0
            AND source_code = LTRIM(RTRIM(source_code))),
    CONSTRAINT CK_order_sources_source_name_not_blank
        CHECK (LEN(LTRIM(RTRIM(source_name))) > 0),
    CONSTRAINT CK_order_sources_display_order
        CHECK (display_order >= 0)
);

CREATE INDEX IX_order_sources_active_display
    ON dbo.order_sources (is_active, display_order);
