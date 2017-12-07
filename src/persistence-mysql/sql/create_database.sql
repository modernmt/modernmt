CREATE DATABASE IF NOT EXISTS mmt;

USE mmt;

CREATE TABLE mmt_memories (
  id              BIGINT       NOT NULL AUTO_INCREMENT,
  name            VARCHAR(255) NOT NULL,
  PRIMARY KEY (id)
)
  ENGINE InnoDB
  DEFAULT CHARSET = utf8;

CREATE TABLE mmt_import_jobs (
  id              BIGINT      NOT NULL AUTO_INCREMENT,
  memory          BIGINT      NOT NULL,
  size            INT         NOT NULL,
  begin           BIGINT      NOT NULL,
  end             BIGINT      NOT NULL,
  data_channel    SMALLINT    NOT NULL,

  PRIMARY KEY (id)
)
  ENGINE InnoDB
  DEFAULT CHARSET = utf8;

CREATE TABLE mmt_metadata (
  id            BIGINT       NOT NULL,
  initialized   BOOLEAN      NOT NULL,
  PRIMARY KEY (id)
)
  ENGINE InnoDB
  DEFAULT CHARSET = utf8;

INSERT INTO mmt_metadata (id, initialized) VALUES (1, FALSE);
