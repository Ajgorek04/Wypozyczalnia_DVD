-- sql
-- Plik: src/test/resources/init.sql
-- Schemat potrzebny do testów (H2 w trybie MySQL)

CREATE TABLE Uzytkownik (
  id INT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(100) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL
);

CREATE TABLE Klient (
  id INT PRIMARY KEY,
  imie VARCHAR(100),
  nazwisko VARCHAR(100),
  email VARCHAR(150),
  telefon VARCHAR(50),
  CONSTRAINT fk_klient_uzytkownik FOREIGN KEY (id) REFERENCES Uzytkownik(id)
);

CREATE TABLE Film (
  id INT AUTO_INCREMENT PRIMARY KEY,
  tytul VARCHAR(255) NOT NULL,
  gatunek VARCHAR(100),
  rok INT,
  dostepny BOOLEAN DEFAULT TRUE
);

CREATE TABLE Transakcja (
  id INT AUTO_INCREMENT PRIMARY KEY,
  klient_id INT NOT NULL,
  film_id INT NOT NULL,
  dataWypozyczenia TIMESTAMP,
  dataZwrotu TIMESTAMP NULL,
  CONSTRAINT fk_trans_klient FOREIGN KEY (klient_id) REFERENCES Klient(id),
  CONSTRAINT fk_trans_film FOREIGN KEY (film_id) REFERENCES Film(id)
);

CREATE TABLE Rachunek (
  id INT AUTO_INCREMENT PRIMARY KEY,
  klient_id INT NOT NULL,
  dataWystawienia TIMESTAMP,
  lacznaKwota DOUBLE,
  CONSTRAINT fk_rachunek_klient FOREIGN KEY (klient_id) REFERENCES Klient(id)
);

CREATE TABLE Oplata (
  id INT AUTO_INCREMENT PRIMARY KEY,
  transakcja_id INT NOT NULL,
  rachunek_id INT NULL,
  kwota DOUBLE,
  powod VARCHAR(255),
  CONSTRAINT fk_oplata_trans FOREIGN KEY (transakcja_id) REFERENCES Transakcja(id),
  CONSTRAINT fk_oplata_rachunek FOREIGN KEY (rachunek_id) REFERENCES Rachunek(id)
);

-- Przykładowe dane (kilka filmów dostępnych)
INSERT INTO Film(tytul, gatunek, rok, dostepny) VALUES ('Matrix', 'Sci-Fi', 1999, TRUE);
INSERT INTO Film(tytul, gatunek, rok, dostepny) VALUES ('Incepcja', 'Sci-Fi', 2010, TRUE);
INSERT INTO Film(tytul, gatunek, rok, dostepny) VALUES ('Forrest Gump', 'Dramat', 1994, TRUE);
INSERT INTO Film(tytul, gatunek, rok, dostepny) VALUES ('Nieaktualny', 'Dramat', 2000, FALSE);
