SET NOCOUNT ON;
SET XACT_ABORT ON;

CREATE SEQUENCE dbo.seq_customer_number
    AS BIGINT
    START WITH 1
    INCREMENT BY 1
    NO CYCLE;

CREATE SEQUENCE dbo.seq_reservation_number
    AS BIGINT
    START WITH 1
    INCREMENT BY 1
    NO CYCLE;

CREATE SEQUENCE dbo.seq_order_number
    AS BIGINT
    START WITH 1
    INCREMENT BY 1
    NO CYCLE;

CREATE SEQUENCE dbo.seq_purchase_number
    AS BIGINT
    START WITH 1
    INCREMENT BY 1
    NO CYCLE;

CREATE SEQUENCE dbo.seq_expense_number
    AS BIGINT
    START WITH 1
    INCREMENT BY 1
    NO CYCLE;

CREATE SEQUENCE dbo.seq_document_number
    AS BIGINT
    START WITH 1
    INCREMENT BY 1
    NO CYCLE;
