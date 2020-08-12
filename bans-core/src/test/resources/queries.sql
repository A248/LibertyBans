-- A test comment
-- More lines

CREATE TABLE myTable (
myKey INT AUTO_INCREMENT PRIMARY KEY,
value VARCHAR(20) NOT NULL);

INSERT INTO myTable (value) VALUES ('another query');

-- another comment

CREATE VIEW idkView AS SELECT * FROM myTable WHERE value = 'idk';
