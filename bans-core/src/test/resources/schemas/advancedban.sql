CREATE TABLE Punishments (
  id INTEGER IDENTITY PRIMARY KEY,
  name VARCHAR(16),
  uuid VARCHAR(35),
  reason VARCHAR(100),
  operator VARCHAR(16),
  punishmentType VARCHAR(16),
  start BIGINT,
  end BIGINT,
  calculation VARCHAR(50)
);

CREATE TABLE PunishmentHistory (
  id INTEGER IDENTITY PRIMARY KEY,
  name VARCHAR(16),
  uuid VARCHAR(35),
  reason VARCHAR(100),
  operator VARCHAR(16),
  punishmentType VARCHAR(16),
  start BIGINT,
  end BIGINT,
  calculation VARCHAR(50)
);