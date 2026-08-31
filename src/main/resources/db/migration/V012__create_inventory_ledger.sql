SET NOCOUNT ON;
SET XACT_ABORT ON;

CREATE TABLE dbo.inventory_movements
(
    inventory_movement_id         BIGINT IDENTITY (1, 1) NOT NULL,
    inventory_item_id             BIGINT NOT NULL,
    order_id                      BIGINT NULL,
    purchase_id                   BIGINT NULL,
    recorded_by_user_id           BIGINT NULL,
    related_inventory_movement_id BIGINT NULL,
    movement_type                 VARCHAR(20) NOT NULL,
    quantity_delta                DECIMAL(19, 4) NOT NULL,
    reason                        NVARCHAR(500) NULL,
    notes                         NVARCHAR(1000) NULL,
    occurred_at                   DATETIME2(3) NOT NULL
        CONSTRAINT DF_inventory_movements_occurred_at DEFAULT (SYSUTCDATETIME()),
    created_at                    DATETIME2(3) NOT NULL
        CONSTRAINT DF_inventory_movements_created_at DEFAULT (SYSUTCDATETIME()),
    row_version                   ROWVERSION NOT NULL,

    CONSTRAINT PK_inventory_movements
        PRIMARY KEY CLUSTERED (inventory_movement_id),
    CONSTRAINT FK_inventory_movements_inventory_items
        FOREIGN KEY (inventory_item_id)
        REFERENCES dbo.inventory_items (inventory_item_id)
        ON DELETE NO ACTION
        ON UPDATE NO ACTION,
    CONSTRAINT FK_inventory_movements_orders
        FOREIGN KEY (order_id)
        REFERENCES dbo.orders (order_id)
        ON DELETE NO ACTION
        ON UPDATE NO ACTION,
    CONSTRAINT FK_inventory_movements_purchases
        FOREIGN KEY (purchase_id)
        REFERENCES dbo.purchases (purchase_id)
        ON DELETE NO ACTION
        ON UPDATE NO ACTION,
    CONSTRAINT FK_inventory_movements_recorded_by_users
        FOREIGN KEY (recorded_by_user_id)
        REFERENCES dbo.users (user_id)
        ON DELETE NO ACTION
        ON UPDATE NO ACTION,
    CONSTRAINT FK_inventory_movements_related_movement
        FOREIGN KEY (related_inventory_movement_id)
        REFERENCES dbo.inventory_movements (inventory_movement_id)
        ON DELETE NO ACTION
        ON UPDATE NO ACTION,
    CONSTRAINT CK_inventory_movements_movement_type
        CHECK (movement_type IN
            ('PURCHASE', 'SALE', 'ADJUSTMENT', 'WASTE', 'RETURN', 'REVERSAL')),
    CONSTRAINT CK_inventory_movements_quantity_delta
        CHECK (quantity_delta <> 0
            AND (movement_type <> 'PURCHASE' OR quantity_delta > 0)
            AND (movement_type <> 'SALE' OR quantity_delta < 0)
            AND (movement_type <> 'WASTE' OR quantity_delta < 0)),
    CONSTRAINT CK_inventory_movements_related_movement
        CHECK (related_inventory_movement_id IS NULL
            OR related_inventory_movement_id <> inventory_movement_id),
    CONSTRAINT CK_inventory_movements_reversal_reference
        CHECK (movement_type <> 'REVERSAL'
            OR related_inventory_movement_id IS NOT NULL),
    CONSTRAINT CK_inventory_movements_manual_action_audit
        CHECK (movement_type NOT IN ('ADJUSTMENT', 'WASTE')
            OR (recorded_by_user_id IS NOT NULL
                AND reason IS NOT NULL
                AND LEN(LTRIM(RTRIM(reason))) > 0)),
    CONSTRAINT CK_inventory_movements_reason_not_blank
        CHECK (reason IS NULL OR LEN(LTRIM(RTRIM(reason))) > 0),
    CONSTRAINT CK_inventory_movements_notes_not_blank
        CHECK (notes IS NULL OR LEN(LTRIM(RTRIM(notes))) > 0)
);

CREATE INDEX IX_inventory_movements_item_occurred
    ON dbo.inventory_movements (inventory_item_id, occurred_at DESC);

CREATE INDEX IX_inventory_movements_order_occurred
    ON dbo.inventory_movements (order_id, occurred_at DESC)
    WHERE order_id IS NOT NULL;

CREATE INDEX IX_inventory_movements_purchase_occurred
    ON dbo.inventory_movements (purchase_id, occurred_at DESC)
    WHERE purchase_id IS NOT NULL;

CREATE INDEX IX_inventory_movements_related_movement
    ON dbo.inventory_movements (related_inventory_movement_id)
    WHERE related_inventory_movement_id IS NOT NULL;

CREATE INDEX IX_inventory_movements_recorded_by_user
    ON dbo.inventory_movements (recorded_by_user_id, occurred_at DESC)
    WHERE recorded_by_user_id IS NOT NULL;
