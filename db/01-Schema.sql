CREATE TABLE materiais (
    id INT AUTO_INCREMENT PRIMARY KEY,
    material VARCHAR(50) NOT NULL,
    co2_kg DOUBLE,
    agua_l DOUBLE,
    energia_kwh DOUBLE,
    valor_kg DOUBLE
);