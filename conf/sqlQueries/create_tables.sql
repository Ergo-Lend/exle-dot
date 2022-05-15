-- schema

-- !Ups

CREATE TABLE IF NOT EXISTS CREATE_REQUESTS (
    id                  serial PRIMARY KEY,
    name                VARCHAR(255) NOT NULL,
    description         VARCHAR(10000),
    goal                BIGINT NOT NULL,
    creation_height     BIGINT NOT NULL,
    deadline_height     BIGINT NOT NULL,
    repayment_height    BIGINT NOT NULL,
    interest_rate       BIGINT NOT NULL,
    tx_state            INT,
    borrower_address    VARCHAR(10000),
    payment_address     VARCHAR(10000),
    create_tx_id        VARCHAR(100),
    time_stamp          VARCHAR(100),
    ttl                 BIGINT,
    deleted             BOOLEAN
);

CREATE TABLE IF NOT EXISTS FUND_LEND_REQUESTS (
    id                  serial primary key,
    lend_box_id         VARCHAR(10000),
    erg_amount          BIGINT NOT NULL,
    tx_state            INT,
    payment_address     VARCHAR(10000),
    lend_tx_id          VARCHAR(1000000),
    lender_address      VARCHAR(10000),
    time_stamp          VARCHAR(100),
    ttl                 BIGINT,
    deleted             BOOLEAN
);

CREATE TABLE IF NOT EXISTS REPAYMENT_REQUESTS (
    id                  serial primary key,
    repayment_box_id    VARCHAR(10000),
    erg_amount          BIGINT NOT NULL,
    tx_state            INT,
    payment_address     VARCHAR(10000),
    repayment_tx_id     VARCHAR(1000000),
    user_address        VARCHAR(10000),
    time_stamp          VARCHAR(100),
    ttl                 BIGINT,
    deleted             BOOLEAN
);


-- !Downs
