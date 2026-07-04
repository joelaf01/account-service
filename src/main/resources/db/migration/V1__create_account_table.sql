CREATE SCHEMA accounts;

CREATE TABLE accounts.account
(
    id     UUID NOT NULL,
    name   VARCHAR(255),
    status SMALLINT,
    CONSTRAINT pk_account PRIMARY KEY (id)
);