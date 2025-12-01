DROP TABLE IF EXISTS payments CASCADE;
DROP TABLE IF EXISTS bookings CASCADE;
DROP TABLE IF EXISTS services CASCADE;
DROP TABLE IF EXISTS users CASCADE;
DROP TABLE IF EXISTS roles CASCADE;
DROP TYPE IF EXISTS bookings_states CASCADE;
DROP VIEW IF EXISTS v_bookings_detail CASCADE;
DROP VIEW IF EXISTS v_client_stats CASCADE;
DROP FUNCTION IF EXISTS update_updated_at_column CASCADE;
DROP FUNCTION IF EXISTS get_available_slots CASCADE;
CREATE TABLE roles (
    id SERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
INSERT INTO roles (name) VALUES ('admin'), ('client');
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    role_id INTEGER NOT NULL REFERENCES roles(id) ON DELETE RESTRICT,
    stripe_id VARCHAR(255) UNIQUE,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    phone VARCHAR(10),
    password VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP
);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role_id ON users(role_id);
CREATE INDEX idx_users_stripe_id ON users(stripe_id);
CREATE TABLE services (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    price DECIMAL(10, 2) NOT NULL CHECK (price >= 0),
    duration INTEGER NOT NULL CHECK (duration > 0),
    stripe_id VARCHAR(255),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
INSERT INTO services (name, description, price, duration) VALUES 
('Masaje Deportivo', 'Ideal para atletas y personas activas. Libera la tensión muscular profunda, previene lesiones y acelera la recuperación.', 500.00, 60),
('Masaje Terapéutico', 'Enfocado en aliviar dolores específicos y mejorar lesiones. Trabajamos sobre las zonas de malestar.', 550.00, 60),
('Masaje de Revisión', 'Una evaluación completa de tu estado muscular y postural. Identificamos desequilibrios y tensiones.', 400.00, 45),
('Masaje Linfático', 'Una técnica suave que estimula la circulación linfática. Perfecto para reducir hinchazón.', 600.00, 60);
CREATE INDEX idx_services_active ON services(is_active);
CREATE TYPE bookings_states AS ENUM ('pending', 'confirmed', 'completed', 'cancelled');
CREATE TABLE bookings (
    id SERIAL PRIMARY KEY,
    service_id INTEGER NOT NULL REFERENCES services(id) ON DELETE RESTRICT,
    client_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    physiotherapeut_id INTEGER REFERENCES users(id) ON DELETE SET NULL,
    date TIMESTAMP NOT NULL,
    state bookings_states DEFAULT 'pending',
    notes VARCHAR(1000),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_bookings_client_id ON bookings(client_id);
CREATE INDEX idx_bookings_service_id ON bookings(service_id);
CREATE INDEX idx_bookings_date ON bookings(date);
CREATE INDEX idx_bookings_state ON bookings(state);
CREATE INDEX idx_bookings_physiotherapeut_id ON bookings(physiotherapeut_id);
CREATE TABLE payments (
    id SERIAL PRIMARY KEY,
    stripe_payment_id VARCHAR(255) UNIQUE NOT NULL,
    service_id INTEGER NOT NULL REFERENCES services(id) ON DELETE RESTRICT,
    client_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    amount DECIMAL(10, 2) NOT NULL CHECK (amount >= 0),
    currency VARCHAR(3) DEFAULT 'MXN',
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_payments_client_id ON payments(client_id);
CREATE INDEX idx_payments_service_id ON payments(service_id);
CREATE INDEX idx_payments_stripe_payment_id ON payments(stripe_payment_id);
CREATE INDEX idx_payments_status ON payments(status);
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
CREATE TRIGGER update_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
	CREATE TRIGGER update_services_updated_at
    BEFORE UPDATE ON services
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
	CREATE TRIGGER update_bookings_updated_at
    BEFORE UPDATE ON bookings
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
	CREATE TRIGGER update_payments_updated_at
    BEFORE UPDATE ON payments
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
	CREATE OR REPLACE VIEW v_bookings_detail AS
SELECT 
    b.id,
    b.date,
    b.state,
    b.notes,
    b.created_at,
    s.id as service_id,
    s.name as service_name,
    s.price as service_price,
    s.duration as service_duration,
    u.id as client_id,
    u.first_name || ' ' || u.last_name as client_name,
    u.email as client_email,
    u.phone as client_phone,
    p.id as physiotherapeut_id,
    p.first_name || ' ' || p.last_name as physiotherapeut_name
FROM bookings b
JOIN services s ON b.service_id = s.id
JOIN users u ON b.client_id = u.id
LEFT JOIN users p ON b.physiotherapeut_id = p.id;
CREATE OR REPLACE VIEW v_client_stats AS
SELECT 
    u.id,
    u.first_name || ' ' || u.last_name as name,
    u.email,
    u.phone,
    COUNT(b.id) as total_bookings,
    MAX(b.date) as last_visit,
    (
        SELECT s.name 
        FROM bookings b2 
        JOIN services s ON b2.service_id = s.id 
        WHERE b2.client_id = u.id 
        GROUP BY s.name 
        ORDER BY COUNT(*) DESC 
        LIMIT 1
    ) as preferred_service
FROM users u
JOIN roles r ON u.role_id = r.id
LEFT JOIN bookings b ON u.id = b.client_id
WHERE r.name = 'client'
GROUP BY u.id, u.first_name, u.last_name, u.email, u.phone;
CREATE OR REPLACE FUNCTION get_available_slots(
    p_date DATE,
    p_duration INTEGER DEFAULT 60
)
RETURNS TABLE (
    slot_time TIME,
    is_available BOOLEAN
) AS $$
DECLARE
    start_time TIME := '10:00:00';
    end_time TIME := '20:00:00';
    current_slot TIME;
BEGIN
    current_slot := start_time;
    
    WHILE current_slot < end_time LOOP
        RETURN QUERY
        SELECT 
            current_slot,
            NOT EXISTS (
                SELECT 1 FROM bookings 
                WHERE DATE(date) = p_date 
                AND EXTRACT(HOUR FROM date) = EXTRACT(HOUR FROM current_slot)
                AND state != 'cancelled'
            );
        
        current_slot := current_slot + (p_duration || ' minutes')::INTERVAL;
    END LOOP;
END;
$$ LANGUAGE plpgsql;
INSERT INTO users (role_id, first_name, last_name, email, phone, password) 
VALUES (
    1,
    'Heriberto',
    'Morales',
    'admin@gmail.com',
    '9611234567',
    '$2a$10$N9qo8uLOickgx2ZMRZoMye7PdF7YpMzLr9qZqXrP7JmJkKGdqQlgi'
);
INSERT INTO users (role_id, first_name, last_name, email, phone, password) 
VALUES 
(2, 'María', 'González', 'maria@example.com', '9611234568', '$2a$10$N9qo8uLOickgx2ZMRZoMye7PdF7YpMzLr9qZqXrP7JmJkKGdqQlgi'),
(2, 'Juan', 'Pérez', 'juan@example.com', '9611234569', '$2a$10$N9qo8uLOickgx2ZMRZoMye7PdF7YpMzLr9qZqXrP7JmJkKGdqQlgi'),
(2, 'Ana', 'Martínez', 'ana@example.com', '9611234570', '$2a$10$N9qo8uLOickgx2ZMRZoMye7PdF7YpMzLr9qZqXrP7JmJkKGdqQlgi');
INSERT INTO bookings (service_id, client_id, date, state, notes) VALUES
(1, 2, CURRENT_TIMESTAMP + INTERVAL '1 day', 'pending', 'Primera sesión'),
(2, 3, CURRENT_TIMESTAMP + INTERVAL '2 days', 'confirmed', 'Segunda visita'),
(3, 4, CURRENT_TIMESTAMP - INTERVAL '5 days', 'completed', 'Sesión completada'),
(4, 2, CURRENT_TIMESTAMP - INTERVAL '10 days', 'completed', 'Muy relajante'),
(1, 3, CURRENT_TIMESTAMP - INTERVAL '15 days', 'completed', 'Excelente servicio');
COMMENT ON TABLE roles IS 'Roles de usuario en el sistema';
COMMENT ON TABLE users IS 'Usuarios del sistema (clientes y administradores)';
COMMENT ON TABLE services IS 'Servicios de masajes ofrecidos';
COMMENT ON TABLE bookings IS 'Reservas de citas';
COMMENT ON TABLE payments IS 'Pagos realizados por servicios';

COMMENT ON COLUMN services.duration IS 'Duración del servicio en minutos';
COMMENT ON COLUMN bookings.state IS 'Estado: pending, confirmed, completed, cancelled';
COMMENT ON COLUMN payments.status IS 'Estado del pago: pending, succeeded, failed, refunded';
SELECT 'Roles creados:' as info, COUNT(*) as total FROM roles
UNION ALL
SELECT 'Usuarios creados:', COUNT(*) FROM users
UNION ALL
SELECT 'Servicios creados:', COUNT(*) FROM services
UNION ALL
SELECT 'Citas creadas:', COUNT(*) FROM bookings
UNION ALL
SELECT 'Pagos creados:', COUNT(*) FROM payments;
SELECT 'USUARIOS CREADOS:' as seccion;
SELECT id, first_name, last_name, email, 
       (SELECT name FROM roles WHERE id = role_id) as role
FROM users
ORDER BY id;

SELECT 'SERVICIOS DISPONIBLES:' as seccion;
SELECT id, name, price, duration, is_active
FROM services
ORDER BY id;

SELECT 'CITAS PROGRAMADAS:' as seccion;
SELECT id, client_name, service_name, date, state
FROM v_bookings_detail
ORDER BY date DESC;