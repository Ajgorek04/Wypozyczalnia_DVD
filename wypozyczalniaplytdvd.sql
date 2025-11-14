CREATE DATABASE WypozyczalniaPlytDVD;
USE WypozyczalniaPlytDVD;



CREATE TABLE Klient (
    id INT AUTO_INCREMENT PRIMARY KEY,
    imie VARCHAR(50) NOT NULL,
    nazwisko VARCHAR(50) NOT NULL,
    email VARCHAR(100),
    telefon VARCHAR(20)
);



CREATE TABLE Film (
    id INT AUTO_INCREMENT PRIMARY KEY,
    tytul VARCHAR(100) NOT NULL,
    gatunek VARCHAR(50),
    rok INT,
    dostepny BOOLEAN DEFAULT TRUE
);



CREATE TABLE Transakcja (
    id INT AUTO_INCREMENT PRIMARY KEY,
    klient_id INT NOT NULL,
    film_id INT NOT NULL,
    dataWypozyczenia DATE NOT NULL,
    dataZwrotu DATE,
    FOREIGN KEY (klient_id) REFERENCES Klient(id),
    FOREIGN KEY (film_id) REFERENCES Film(id)
);



CREATE TABLE Rachunek (
    id INT AUTO_INCREMENT PRIMARY KEY,
    klient_id INT NOT NULL,
    dataWystawienia DATE NOT NULL,
    lacznaKwota DOUBLE,
    FOREIGN KEY (klient_id) REFERENCES Klient(id)
);



CREATE TABLE Oplata (
    id INT AUTO_INCREMENT PRIMARY KEY,
    transakcja_id INT NOT NULL,
    rachunek_id INT,
    kwota DOUBLE NOT NULL,
    powod VARCHAR(255),
    FOREIGN KEY (transakcja_id) REFERENCES Transakcja(id),
    FOREIGN KEY (rachunek_id) REFERENCES Rachunek(id)
);
