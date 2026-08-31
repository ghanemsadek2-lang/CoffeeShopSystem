SET NOCOUNT ON;
SET XACT_ABORT ON;

CREATE TABLE dbo.notifications
(
    notification_id    BIGINT IDENTITY (1, 1) NOT NULL,
    recipient_user_id  BIGINT NULL,
    notification_type  VARCHAR(20) NOT NULL,
    title              NVARCHAR(200) NOT NULL,
    message_body       NVARCHAR(2000) NOT NULL,
    related_entity_type VARCHAR(100) NULL,
    related_entity_id  BIGINT NULL,
    is_read            BIT NOT NULL
        CONSTRAINT DF_notifications_is_read DEFAULT (0),
    read_at            DATETIME2(3) NULL,
    expires_at         DATETIME2(3) NULL,
    created_at         DATETIME2(3) NOT NULL
        CONSTRAINT DF_notifications_created_at DEFAULT (SYSUTCDATETIME()),
    row_version        ROWVERSION NOT NULL,

    CONSTRAINT PK_notifications
        PRIMARY KEY CLUSTERED (notification_id),
    CONSTRAINT FK_notifications_recipient_users
        FOREIGN KEY (recipient_user_id)
        REFERENCES dbo.users (user_id)
        ON DELETE NO ACTION
        ON UPDATE NO ACTION,
    CONSTRAINT CK_notifications_notification_type
        CHECK (notification_type IN ('INFO', 'WARNING', 'LOW_STOCK', 'SYSTEM')),
    CONSTRAINT CK_notifications_title_not_blank
        CHECK (LEN(LTRIM(RTRIM(title))) > 0),
    CONSTRAINT CK_notifications_message_body_not_blank
        CHECK (LEN(LTRIM(RTRIM(message_body))) > 0),
    CONSTRAINT CK_notifications_related_entity
        CHECK ((related_entity_type IS NULL AND related_entity_id IS NULL)
            OR (related_entity_type IS NOT NULL
                AND LEN(LTRIM(RTRIM(related_entity_type))) > 0
                AND related_entity_id IS NOT NULL)),
    CONSTRAINT CK_notifications_read_state
        CHECK ((is_read = 0 AND read_at IS NULL)
            OR (is_read = 1 AND read_at IS NOT NULL AND read_at >= created_at)),
    CONSTRAINT CK_notifications_expiration
        CHECK (expires_at IS NULL OR expires_at > created_at)
);

CREATE INDEX IX_notifications_recipient_read_created
    ON dbo.notifications (recipient_user_id, is_read, created_at DESC);

CREATE INDEX IX_notifications_type_created
    ON dbo.notifications (notification_type, created_at DESC);

CREATE TABLE dbo.settings
(
    setting_id         BIGINT IDENTITY (1, 1) NOT NULL,
    setting_key        VARCHAR(150) NOT NULL,
    setting_value      NVARCHAR(2000) NOT NULL,
    value_type         VARCHAR(20) NOT NULL,
    description        NVARCHAR(500) NULL,
    updated_by_user_id BIGINT NULL,
    created_at         DATETIME2(3) NOT NULL
        CONSTRAINT DF_settings_created_at DEFAULT (SYSUTCDATETIME()),
    updated_at         DATETIME2(3) NOT NULL
        CONSTRAINT DF_settings_updated_at DEFAULT (SYSUTCDATETIME()),
    row_version        ROWVERSION NOT NULL,

    CONSTRAINT PK_settings
        PRIMARY KEY CLUSTERED (setting_id),
    CONSTRAINT UQ_settings_setting_key
        UNIQUE (setting_key),
    CONSTRAINT FK_settings_updated_by_users
        FOREIGN KEY (updated_by_user_id)
        REFERENCES dbo.users (user_id)
        ON DELETE NO ACTION
        ON UPDATE NO ACTION,
    CONSTRAINT CK_settings_setting_key_not_blank
        CHECK (LEN(LTRIM(RTRIM(setting_key))) > 0
            AND setting_key = LTRIM(RTRIM(setting_key))),
    CONSTRAINT CK_settings_setting_value_not_blank
        CHECK (LEN(LTRIM(RTRIM(setting_value))) > 0),
    CONSTRAINT CK_settings_value_type
        CHECK (value_type IN ('STRING', 'INTEGER', 'DECIMAL', 'BOOLEAN')),
    CONSTRAINT CK_settings_description_not_blank
        CHECK (description IS NULL OR LEN(LTRIM(RTRIM(description))) > 0)
);

CREATE INDEX IX_settings_updated_by_user
    ON dbo.settings (updated_by_user_id)
    WHERE updated_by_user_id IS NOT NULL;

CREATE TABLE dbo.audit_logs
(
    audit_log_id BIGINT IDENTITY (1, 1) NOT NULL,
    user_id      BIGINT NULL,
    action_code  VARCHAR(100) NOT NULL,
    entity_type  VARCHAR(100) NOT NULL,
    entity_id    BIGINT NULL,
    before_data  NVARCHAR(MAX) NULL,
    after_data   NVARCHAR(MAX) NULL,
    details      NVARCHAR(1000) NULL,
    reason       NVARCHAR(500) NULL,
    occurred_at  DATETIME2(3) NOT NULL
        CONSTRAINT DF_audit_logs_occurred_at DEFAULT (SYSUTCDATETIME()),

    CONSTRAINT PK_audit_logs
        PRIMARY KEY CLUSTERED (audit_log_id),
    CONSTRAINT FK_audit_logs_users
        FOREIGN KEY (user_id)
        REFERENCES dbo.users (user_id)
        ON DELETE NO ACTION
        ON UPDATE NO ACTION,
    CONSTRAINT CK_audit_logs_action_code_not_blank
        CHECK (LEN(LTRIM(RTRIM(action_code))) > 0
            AND action_code = LTRIM(RTRIM(action_code))),
    CONSTRAINT CK_audit_logs_entity_type_not_blank
        CHECK (LEN(LTRIM(RTRIM(entity_type))) > 0
            AND entity_type = LTRIM(RTRIM(entity_type))),
    CONSTRAINT CK_audit_logs_before_data_not_blank
        CHECK (before_data IS NULL OR LEN(LTRIM(RTRIM(before_data))) > 0),
    CONSTRAINT CK_audit_logs_after_data_not_blank
        CHECK (after_data IS NULL OR LEN(LTRIM(RTRIM(after_data))) > 0),
    CONSTRAINT CK_audit_logs_details_not_blank
        CHECK (details IS NULL OR LEN(LTRIM(RTRIM(details))) > 0),
    CONSTRAINT CK_audit_logs_reason_not_blank
        CHECK (reason IS NULL OR LEN(LTRIM(RTRIM(reason))) > 0)
);

CREATE INDEX IX_audit_logs_entity_occurred
    ON dbo.audit_logs (entity_type, entity_id, occurred_at DESC);

CREATE INDEX IX_audit_logs_user_occurred
    ON dbo.audit_logs (user_id, occurred_at DESC)
    WHERE user_id IS NOT NULL;

CREATE INDEX IX_audit_logs_action_occurred
    ON dbo.audit_logs (action_code, occurred_at DESC);

CREATE INDEX IX_audit_logs_occurred
    ON dbo.audit_logs (occurred_at DESC);
