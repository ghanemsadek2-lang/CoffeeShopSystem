SET NOCOUNT ON;
SET XACT_ABORT ON;

CREATE TABLE dbo.modifier_groups
(
    modifier_group_id  BIGINT IDENTITY (1, 1) NOT NULL,
    group_code         VARCHAR(50) NOT NULL,
    group_name         NVARCHAR(150) NOT NULL,
    description        NVARCHAR(500) NULL,
    minimum_selections SMALLINT NOT NULL
        CONSTRAINT DF_modifier_groups_minimum_selections DEFAULT (0),
    maximum_selections SMALLINT NOT NULL,
    display_order      INT NOT NULL
        CONSTRAINT DF_modifier_groups_display_order DEFAULT (0),
    is_active          BIT NOT NULL
        CONSTRAINT DF_modifier_groups_is_active DEFAULT (1),
    created_at         DATETIME2(3) NOT NULL
        CONSTRAINT DF_modifier_groups_created_at DEFAULT (SYSUTCDATETIME()),
    updated_at         DATETIME2(3) NOT NULL
        CONSTRAINT DF_modifier_groups_updated_at DEFAULT (SYSUTCDATETIME()),
    row_version        ROWVERSION NOT NULL,

    CONSTRAINT PK_modifier_groups
        PRIMARY KEY CLUSTERED (modifier_group_id),
    CONSTRAINT UQ_modifier_groups_group_code
        UNIQUE (group_code),
    CONSTRAINT UQ_modifier_groups_group_name
        UNIQUE (group_name),
    CONSTRAINT CK_modifier_groups_group_code_not_blank
        CHECK (LEN(LTRIM(RTRIM(group_code))) > 0
            AND group_code = LTRIM(RTRIM(group_code))),
    CONSTRAINT CK_modifier_groups_group_name_not_blank
        CHECK (LEN(LTRIM(RTRIM(group_name))) > 0),
    CONSTRAINT CK_modifier_groups_description_not_blank
        CHECK (description IS NULL OR LEN(LTRIM(RTRIM(description))) > 0),
    CONSTRAINT CK_modifier_groups_selection_range
        CHECK (minimum_selections >= 0
            AND maximum_selections >= minimum_selections),
    CONSTRAINT CK_modifier_groups_display_order
        CHECK (display_order >= 0)
);

CREATE INDEX IX_modifier_groups_active_display
    ON dbo.modifier_groups (is_active, display_order, group_name);

CREATE TABLE dbo.modifiers
(
    modifier_id       BIGINT IDENTITY (1, 1) NOT NULL,
    modifier_group_id BIGINT NOT NULL,
    modifier_code     VARCHAR(50) NOT NULL,
    modifier_name     NVARCHAR(150) NOT NULL,
    price_adjustment  DECIMAL(19, 4) NOT NULL
        CONSTRAINT DF_modifiers_price_adjustment DEFAULT (0),
    display_order     INT NOT NULL
        CONSTRAINT DF_modifiers_display_order DEFAULT (0),
    is_active         BIT NOT NULL
        CONSTRAINT DF_modifiers_is_active DEFAULT (1),
    created_at        DATETIME2(3) NOT NULL
        CONSTRAINT DF_modifiers_created_at DEFAULT (SYSUTCDATETIME()),
    updated_at        DATETIME2(3) NOT NULL
        CONSTRAINT DF_modifiers_updated_at DEFAULT (SYSUTCDATETIME()),
    row_version       ROWVERSION NOT NULL,

    CONSTRAINT PK_modifiers
        PRIMARY KEY CLUSTERED (modifier_id),
    CONSTRAINT UQ_modifiers_group_code
        UNIQUE (modifier_group_id, modifier_code),
    CONSTRAINT UQ_modifiers_group_name
        UNIQUE (modifier_group_id, modifier_name),
    CONSTRAINT FK_modifiers_modifier_groups
        FOREIGN KEY (modifier_group_id)
        REFERENCES dbo.modifier_groups (modifier_group_id)
        ON DELETE NO ACTION
        ON UPDATE NO ACTION,
    CONSTRAINT CK_modifiers_modifier_code_not_blank
        CHECK (LEN(LTRIM(RTRIM(modifier_code))) > 0
            AND modifier_code = LTRIM(RTRIM(modifier_code))),
    CONSTRAINT CK_modifiers_modifier_name_not_blank
        CHECK (LEN(LTRIM(RTRIM(modifier_name))) > 0),
    CONSTRAINT CK_modifiers_price_adjustment
        CHECK (price_adjustment >= 0),
    CONSTRAINT CK_modifiers_display_order
        CHECK (display_order >= 0)
);

CREATE INDEX IX_modifiers_group_active_display
    ON dbo.modifiers (modifier_group_id, is_active, display_order, modifier_name);

CREATE TABLE dbo.product_modifier_group_assignments
(
    product_modifier_group_assignment_id BIGINT IDENTITY (1, 1) NOT NULL,
    product_id                           BIGINT NOT NULL,
    modifier_group_id                    BIGINT NOT NULL,
    minimum_selections_override          SMALLINT NULL,
    maximum_selections_override          SMALLINT NULL,
    display_order                        INT NOT NULL
        CONSTRAINT DF_product_modifier_group_assignments_display_order DEFAULT (0),
    is_active                            BIT NOT NULL
        CONSTRAINT DF_product_modifier_group_assignments_is_active DEFAULT (1),
    created_at                           DATETIME2(3) NOT NULL
        CONSTRAINT DF_product_modifier_group_assignments_created_at DEFAULT (SYSUTCDATETIME()),
    updated_at                           DATETIME2(3) NOT NULL
        CONSTRAINT DF_product_modifier_group_assignments_updated_at DEFAULT (SYSUTCDATETIME()),
    row_version                          ROWVERSION NOT NULL,

    CONSTRAINT PK_product_modifier_group_assignments
        PRIMARY KEY CLUSTERED (product_modifier_group_assignment_id),
    CONSTRAINT UQ_product_modifier_group_assignments_product_group
        UNIQUE (product_id, modifier_group_id),
    CONSTRAINT FK_product_modifier_group_assignments_products
        FOREIGN KEY (product_id)
        REFERENCES dbo.products (product_id)
        ON DELETE NO ACTION
        ON UPDATE NO ACTION,
    CONSTRAINT FK_product_modifier_group_assignments_modifier_groups
        FOREIGN KEY (modifier_group_id)
        REFERENCES dbo.modifier_groups (modifier_group_id)
        ON DELETE NO ACTION
        ON UPDATE NO ACTION,
    CONSTRAINT CK_product_modifier_group_assignments_override_pair
        CHECK ((minimum_selections_override IS NULL
                AND maximum_selections_override IS NULL)
            OR (minimum_selections_override IS NOT NULL
                AND maximum_selections_override IS NOT NULL)),
    CONSTRAINT CK_product_modifier_group_assignments_override_range
        CHECK (minimum_selections_override IS NULL
            OR (minimum_selections_override >= 0
                AND maximum_selections_override >= minimum_selections_override)),
    CONSTRAINT CK_product_modifier_group_assignments_display_order
        CHECK (display_order >= 0)
);

CREATE INDEX IX_product_modifier_group_assignments_product_active_display
    ON dbo.product_modifier_group_assignments
        (product_id, is_active, display_order, modifier_group_id);

CREATE INDEX IX_product_modifier_group_assignments_group
    ON dbo.product_modifier_group_assignments (modifier_group_id, product_id);
