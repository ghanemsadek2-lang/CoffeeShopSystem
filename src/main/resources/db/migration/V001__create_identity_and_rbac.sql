SET NOCOUNT ON;
SET XACT_ABORT ON;

CREATE TABLE dbo.employees
(
    employee_id      BIGINT IDENTITY (1, 1) NOT NULL,
    employee_number  VARCHAR(30) NOT NULL,
    first_name       NVARCHAR(100) NOT NULL,
    last_name        NVARCHAR(100) NOT NULL,
    phone            NVARCHAR(30) NULL,
    email            NVARCHAR(254) NULL,
    job_title        NVARCHAR(100) NULL,
    hire_date        DATE NOT NULL,
    termination_date DATE NULL,
    employment_status VARCHAR(20) NOT NULL,
    is_active        BIT NOT NULL
        CONSTRAINT DF_employees_is_active DEFAULT (1),
    created_at       DATETIME2(3) NOT NULL
        CONSTRAINT DF_employees_created_at DEFAULT (SYSUTCDATETIME()),
    updated_at       DATETIME2(3) NOT NULL
        CONSTRAINT DF_employees_updated_at DEFAULT (SYSUTCDATETIME()),
    row_version      ROWVERSION NOT NULL,

    CONSTRAINT PK_employees
        PRIMARY KEY CLUSTERED (employee_id),
    CONSTRAINT UQ_employees_employee_number
        UNIQUE (employee_number),
    CONSTRAINT CK_employees_employee_number_not_blank
        CHECK (LEN(LTRIM(RTRIM(employee_number))) > 0
            AND employee_number = LTRIM(RTRIM(employee_number))),
    CONSTRAINT CK_employees_first_name_not_blank
        CHECK (LEN(LTRIM(RTRIM(first_name))) > 0),
    CONSTRAINT CK_employees_last_name_not_blank
        CHECK (LEN(LTRIM(RTRIM(last_name))) > 0),
    CONSTRAINT CK_employees_phone_not_blank
        CHECK (phone IS NULL OR LEN(LTRIM(RTRIM(phone))) > 0),
    CONSTRAINT CK_employees_email_not_blank
        CHECK (email IS NULL OR LEN(LTRIM(RTRIM(email))) > 0),
    CONSTRAINT CK_employees_job_title_not_blank
        CHECK (job_title IS NULL OR LEN(LTRIM(RTRIM(job_title))) > 0),
    CONSTRAINT CK_employees_employment_status
        CHECK (employment_status IN ('ACTIVE', 'ON_LEAVE', 'TERMINATED')),
    CONSTRAINT CK_employees_termination_date
        CHECK (termination_date IS NULL OR termination_date >= hire_date),
    CONSTRAINT CK_employees_status_termination_consistency
        CHECK (
            (employment_status = 'TERMINATED' AND termination_date IS NOT NULL)
            OR
            (employment_status IN ('ACTIVE', 'ON_LEAVE') AND termination_date IS NULL)
        )
);

CREATE UNIQUE INDEX UX_employees_email
    ON dbo.employees (email)
    WHERE email IS NOT NULL;

CREATE INDEX IX_employees_name
    ON dbo.employees (last_name, first_name);

CREATE INDEX IX_employees_status_active
    ON dbo.employees (employment_status, is_active);

CREATE TABLE dbo.users
(
    user_id               BIGINT IDENTITY (1, 1) NOT NULL,
    employee_id           BIGINT NULL,
    username              NVARCHAR(100) COLLATE Latin1_General_100_CI_AI_SC NOT NULL,
    password_hash         NVARCHAR(500) NOT NULL,
    password_changed_at   DATETIME2(3) NOT NULL
        CONSTRAINT DF_users_password_changed_at DEFAULT (SYSUTCDATETIME()),
    must_change_password  BIT NOT NULL
        CONSTRAINT DF_users_must_change_password DEFAULT (0),
    failed_login_count    INT NOT NULL
        CONSTRAINT DF_users_failed_login_count DEFAULT (0),
    locked_until          DATETIME2(3) NULL,
    last_login_at         DATETIME2(3) NULL,
    is_active             BIT NOT NULL
        CONSTRAINT DF_users_is_active DEFAULT (1),
    created_at            DATETIME2(3) NOT NULL
        CONSTRAINT DF_users_created_at DEFAULT (SYSUTCDATETIME()),
    updated_at            DATETIME2(3) NOT NULL
        CONSTRAINT DF_users_updated_at DEFAULT (SYSUTCDATETIME()),
    row_version           ROWVERSION NOT NULL,

    CONSTRAINT PK_users
        PRIMARY KEY CLUSTERED (user_id),
    CONSTRAINT UQ_users_username
        UNIQUE (username),
    CONSTRAINT FK_users_employees
        FOREIGN KEY (employee_id)
        REFERENCES dbo.employees (employee_id)
        ON DELETE NO ACTION
        ON UPDATE NO ACTION,
    CONSTRAINT CK_users_username_not_blank
        CHECK (LEN(LTRIM(RTRIM(username))) > 0
            AND username = LTRIM(RTRIM(username))),
    CONSTRAINT CK_users_password_hash
        CHECK (LEN(password_hash) > 20
            AND password_hash = LTRIM(RTRIM(password_hash))),
    CONSTRAINT CK_users_failed_login_count
        CHECK (failed_login_count >= 0)
);

CREATE UNIQUE INDEX UX_users_employee_id
    ON dbo.users (employee_id)
    WHERE employee_id IS NOT NULL;

CREATE INDEX IX_users_active_locked
    ON dbo.users (is_active, locked_until);

CREATE TABLE dbo.roles
(
    role_id      BIGINT IDENTITY (1, 1) NOT NULL,
    role_code    VARCHAR(50) NOT NULL,
    role_name    NVARCHAR(100) NOT NULL,
    description  NVARCHAR(500) NULL,
    is_system    BIT NOT NULL
        CONSTRAINT DF_roles_is_system DEFAULT (0),
    is_active    BIT NOT NULL
        CONSTRAINT DF_roles_is_active DEFAULT (1),
    created_at   DATETIME2(3) NOT NULL
        CONSTRAINT DF_roles_created_at DEFAULT (SYSUTCDATETIME()),
    updated_at   DATETIME2(3) NOT NULL
        CONSTRAINT DF_roles_updated_at DEFAULT (SYSUTCDATETIME()),
    row_version  ROWVERSION NOT NULL,

    CONSTRAINT PK_roles
        PRIMARY KEY CLUSTERED (role_id),
    CONSTRAINT UQ_roles_role_code
        UNIQUE (role_code),
    CONSTRAINT UQ_roles_role_name
        UNIQUE (role_name),
    CONSTRAINT CK_roles_role_code_not_blank
        CHECK (LEN(LTRIM(RTRIM(role_code))) > 0
            AND role_code = LTRIM(RTRIM(role_code))),
    CONSTRAINT CK_roles_role_name_not_blank
        CHECK (LEN(LTRIM(RTRIM(role_name))) > 0),
    CONSTRAINT CK_roles_description_not_blank
        CHECK (description IS NULL OR LEN(LTRIM(RTRIM(description))) > 0)
);

CREATE INDEX IX_roles_active
    ON dbo.roles (is_active);

CREATE TABLE dbo.permissions
(
    permission_id    BIGINT IDENTITY (1, 1) NOT NULL,
    permission_code  VARCHAR(100) NOT NULL,
    permission_name  NVARCHAR(150) NOT NULL,
    description      NVARCHAR(500) NULL,
    is_active        BIT NOT NULL
        CONSTRAINT DF_permissions_is_active DEFAULT (1),
    created_at       DATETIME2(3) NOT NULL
        CONSTRAINT DF_permissions_created_at DEFAULT (SYSUTCDATETIME()),

    CONSTRAINT PK_permissions
        PRIMARY KEY CLUSTERED (permission_id),
    CONSTRAINT UQ_permissions_permission_code
        UNIQUE (permission_code),
    CONSTRAINT CK_permissions_permission_code_not_blank
        CHECK (LEN(LTRIM(RTRIM(permission_code))) > 0
            AND permission_code = LTRIM(RTRIM(permission_code))),
    CONSTRAINT CK_permissions_permission_name_not_blank
        CHECK (LEN(LTRIM(RTRIM(permission_name))) > 0),
    CONSTRAINT CK_permissions_description_not_blank
        CHECK (description IS NULL OR LEN(LTRIM(RTRIM(description))) > 0)
);

CREATE INDEX IX_permissions_active
    ON dbo.permissions (is_active);

CREATE TABLE dbo.role_permissions
(
    role_id             BIGINT NOT NULL,
    permission_id       BIGINT NOT NULL,
    granted_at          DATETIME2(3) NOT NULL
        CONSTRAINT DF_role_permissions_granted_at DEFAULT (SYSUTCDATETIME()),
    granted_by_user_id  BIGINT NULL,

    CONSTRAINT PK_role_permissions
        PRIMARY KEY CLUSTERED (role_id, permission_id),
    CONSTRAINT FK_role_permissions_roles
        FOREIGN KEY (role_id)
        REFERENCES dbo.roles (role_id)
        ON DELETE NO ACTION
        ON UPDATE NO ACTION,
    CONSTRAINT FK_role_permissions_permissions
        FOREIGN KEY (permission_id)
        REFERENCES dbo.permissions (permission_id)
        ON DELETE NO ACTION
        ON UPDATE NO ACTION,
    CONSTRAINT FK_role_permissions_granted_by_users
        FOREIGN KEY (granted_by_user_id)
        REFERENCES dbo.users (user_id)
        ON DELETE NO ACTION
        ON UPDATE NO ACTION
);

CREATE INDEX IX_role_permissions_permission_role
    ON dbo.role_permissions (permission_id, role_id);

CREATE INDEX IX_role_permissions_granted_by_user
    ON dbo.role_permissions (granted_by_user_id)
    WHERE granted_by_user_id IS NOT NULL;

CREATE TABLE dbo.user_roles
(
    user_id              BIGINT NOT NULL,
    role_id              BIGINT NOT NULL,
    assigned_at          DATETIME2(3) NOT NULL
        CONSTRAINT DF_user_roles_assigned_at DEFAULT (SYSUTCDATETIME()),
    assigned_by_user_id  BIGINT NULL,

    CONSTRAINT PK_user_roles
        PRIMARY KEY CLUSTERED (user_id, role_id),
    CONSTRAINT FK_user_roles_users
        FOREIGN KEY (user_id)
        REFERENCES dbo.users (user_id)
        ON DELETE NO ACTION
        ON UPDATE NO ACTION,
    CONSTRAINT FK_user_roles_roles
        FOREIGN KEY (role_id)
        REFERENCES dbo.roles (role_id)
        ON DELETE NO ACTION
        ON UPDATE NO ACTION,
    CONSTRAINT FK_user_roles_assigned_by_users
        FOREIGN KEY (assigned_by_user_id)
        REFERENCES dbo.users (user_id)
        ON DELETE NO ACTION
        ON UPDATE NO ACTION
);

CREATE INDEX IX_user_roles_role_user
    ON dbo.user_roles (role_id, user_id);

CREATE INDEX IX_user_roles_assigned_by_user
    ON dbo.user_roles (assigned_by_user_id)
    WHERE assigned_by_user_id IS NOT NULL;
