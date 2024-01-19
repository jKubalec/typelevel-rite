CREATE TABLE users (
  email text NOT NULL,
  hashedPassword text NOT NULL,
  firstName text,
  lastName text,
  company text,
  role text NOT NULL
);

ALTER TABLE users
ADD CONSTRAINT pk_users PRIMARY KEY (email);

INSERT INTO users(
  email,
  hashedPassword,
  firstName,
  lastName,
  company,
  role
) VALUES (
  'mehere@email.cz',
  '$2a$10$aZDWYgMMAR3wgI69j8jrquvyF553lxjSxydkApLs2N7oYy3ZH5ziG',
  'Honza',
  'Kub',
  'Rock the JVM',
  'ADMIN'
);

INSERT INTO users(
  email,
  hashedPassword,
  firstName,
  lastName,
  company,
  role
) VALUES (
  'pavel@email.cz',
  '$2a$10$mbxwO/.Ojkml0S9pgDeNDui2On7bC9.GUHScjVQK3GOxIuIGNbf.e',
  'Pavel',
  'Petr',
  'Rock the JVM',
  'RECRUITER'
);
