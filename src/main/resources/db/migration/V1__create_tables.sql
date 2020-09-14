CREATE TABLE states (
  id INT NOT NULL AUTO_INCREMENT,
  name VARCHAR(256) NOT NULL UNIQUE,
  PRIMARY KEY (id));

CREATE TABLE transitions (
  id INT NOT NULL AUTO_INCREMENT,
  fromState INT NOT NULL,
  toState INT NOT NULL,
  PRIMARY KEY (id),
    FOREIGN KEY (fromState) REFERENCES states (id) ON DELETE CASCADE ON UPDATE CASCADE,
    FOREIGN KEY (toState) REFERENCES states (id) ON DELETE CASCADE ON UPDATE CASCADE);

CREATE TABLE entities (
  id Int NOT NULL AUTO_INCREMENT,
  name VARCHAR(256) NOT NULL UNIQUE,
  stateId INT,
  PRIMARY KEY (id),
  FOREIGN KEY (stateId) REFERENCES states (id) ON DELETE RESTRICT ON UPDATE CASCADE);

CREATE TABLE operations (
  id Int NOT NULL AUTO_INCREMENT,
  entityId INT NOT NULL,
  fromState VARCHAR(256) NOT NULL,
  toState VARCHAR(256) NOT NULL,
  PRIMARY KEY (id),
  FOREIGN KEY (entityId) REFERENCES entities (id) ON DELETE CASCADE ON UPDATE CASCADE);

INSERT INTO states values(1, 'init');
INSERT INTO states values(2, 'pending');
INSERT INTO states values(3, 'finished');
INSERT INTO states values(4, 'closed');

INSERT INTO transitions values(1, 1, 2);
INSERT INTO transitions values(2, 2, 3);
INSERT INTO transitions values(3, 3, 4);
INSERT INTO transitions values(4, 4, 1);

INSERT INTO entities values(1, 'amigo', 1);

