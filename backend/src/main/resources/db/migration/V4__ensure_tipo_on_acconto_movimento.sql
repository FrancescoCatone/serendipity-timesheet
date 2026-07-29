ALTER TABLE acconto_movimento
ADD COLUMN IF NOT EXISTS tipo VARCHAR(50);

UPDATE acconto_movimento
SET tipo = 'ACCONTO'
WHERE tipo IS NULL;

ALTER TABLE acconto_movimento
ALTER COLUMN tipo SET DEFAULT 'ACCONTO';

ALTER TABLE acconto_movimento
ALTER COLUMN tipo SET NOT NULL;
