-- schema

-- !Ups

DROP TABLE CREATE_REQUESTS;
DROP TABLE FUND_LEND_REQUESTS;
DROP TABLE REPAYMENT_REQUESTS;

CREATE TABLE CREATE_REQUESTS (
    ID                          BIGINT Not null PRIMARY key auto_increment,
    NAME                        VARCHAR(255) NOT NULL,
    DESCRIPTION                 VARCHAR(10000),
    GOAL                        BIGINT NOT NULL,
    DEADLINE_HEIGHT             BIGINT NOT NULL,
    repayment_height            BIGINT NOT NULL,
    INTEREST_RATE               BIGINT NOT NULL,
    TX_STATE                    INT,
    BORROWER_ADDRESS            VARCHAR(10000),
    PAYMENT_ADDRESS             VARCHAR(10000),
    CREATE_TX_ID                VARCHAR(100),
    TIME_STAMP                  VARCHAR(100),
    TTL                         BIGINT,
    DELETED                     BOOLEAN
);

CREATE TABLE FUND_LEND_REQUESTS (
    ID                          BIGINT NOT NULL primary key auto_increment,
    LEND_BOX_ID                 VARCHAR(10000),
    ERG_AMOUNT                  BIGINT NOT NULL,
    STATE                       INT,
    PAYMENT_ADDRESS             VARCHAR(10000),
    LEND_TX_ID                  VARCHAR(1000000),
    LENDER_ADDRESS              VARCHAR(10000),
    TIME_STAMP                  VARCHAR(100),
    TTL                         BIGINT,
    DELETED                     BOOLEAN
);

CREATE TABLE REPAYMENT_REQUESTS (
    ID                          BIGINT NOT NULL primary key auto_increment,
    REPAYMENT_BOX_ID            VARCHAR(10000),
    ERG_AMOUNT                  BIGINT NOT NULL,
    STATE                       INT,
    PAYMENT_ADDRESS             VARCHAR(10000),
    REPAYMENT_TX_ID             VARCHAR(1000000),
    USER_ADDRESS                VARCHAR(10000),
    TIME_STAMP                  VARCHAR(100),
    TTL                         BIGINT,
    DELETED                     BOOLEAN
);


-- !Downs
