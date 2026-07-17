CREATE TABLE vehicles (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(100) NOT NULL,
    lat DOUBLE PRECISION,
    lon DOUBLE PRECISION,
    status VARCHAR(100) NOT NULL,
    battery_pct INTEGER NOT NULL CHECK (battery_pct BETWEEN 0 AND 100),
    qr_code VARCHAR(255) NOT NULL UNIQUE
);

INSERT INTO vehicles (id, name, type, lat, lon, status, battery_pct, qr_code) VALUES
    ('vehicle-1', 'Fiat 500e Aurora', 'CAR', 44.505461, 11.341496, 'AVAILABLE', 96, 'ACME-QR-vehicle-1'),
    ('vehicle-2', 'Vespa Elettrica Brio', 'SCOOTER', 44.505461, 11.341496, 'AVAILABLE', 88, 'ACME-QR-vehicle-2'),
    ('vehicle-3', 'Segway Ninebot Luna', 'KICKSCOOTER', 44.496518, 11.354375, 'AVAILABLE', 92, 'ACME-QR-vehicle-3'),
    ('vehicle-4', 'Renault Zoe Verde', 'CAR', 44.493673, 11.341753, 'ON_HOLD', 91, 'ACME-QR-vehicle-4'),
    ('vehicle-5', 'Askoll eS3 Nettuno', 'SCOOTER', 44.482744, 11.355395, 'AVAILABLE', 84, 'ACME-QR-vehicle-5'),
    ('vehicle-6', 'Xiaomi Pro 4 Saetta', 'KICKSCOOTER', 44.509036, 11.363389, 'MAINTENANCE', 22, 'ACME-QR-vehicle-6'),
    ('vehicle-7', 'Peugeot e-208 Blu', 'CAR', 44.509036, 11.363389, 'AVAILABLE', 97, 'ACME-QR-vehicle-7'),
    ('vehicle-8', 'Silence S01 Arancio', 'SCOOTER', 44.505461, 11.341496, 'CHARGING', 14, 'ACME-QR-vehicle-8'),
    ('vehicle-9', 'Dacia Spring Sole', 'CAR', 44.496518, 11.354375, 'AVAILABLE', 89, 'ACME-QR-vehicle-9'),
    ('vehicle-10', 'NIU NQi GTS Vento', 'SCOOTER', 44.493673, 11.341753, 'AVAILABLE', 93, 'ACME-QR-vehicle-10'),
    ('vehicle-11', 'Segway Max G2 Freccia', 'KICKSCOOTER', 44.482744, 11.355395, 'AVAILABLE', 86, 'ACME-QR-vehicle-11'),
    ('vehicle-12', 'Citroen e-C3 Alba', 'CAR', 44.509036, 11.363389, 'AVAILABLE', 94, 'ACME-QR-vehicle-12');
