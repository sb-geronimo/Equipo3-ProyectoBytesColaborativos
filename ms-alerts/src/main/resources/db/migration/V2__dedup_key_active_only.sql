-- Unicidad de dedup_key solo entre alertas activas: una condición reaparecida tras RESOLVED puede crear una alerta nueva
ALTER TABLE alerts DROP CONSTRAINT IF EXISTS alerts_dedup_key_key;
CREATE UNIQUE INDEX IF NOT EXISTS uq_alerts_dedup_key_active ON alerts (dedup_key) WHERE status IN ('OPEN', 'ACKNOWLEDGED');
