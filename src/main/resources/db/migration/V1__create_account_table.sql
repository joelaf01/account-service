CREATE SCHEMA accounts;

CREATE TABLE accounts.account
(
    id     UUID NOT NULL,
    name   VARCHAR(255),
    status VARCHAR(20),
    CONSTRAINT pk_account PRIMARY KEY (id)
);