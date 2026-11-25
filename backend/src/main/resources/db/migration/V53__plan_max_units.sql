-- Várias unidades (RF31): o limite de unidades ativas vem do plano, como mesas, pessoas e setores.
-- Nulo = sem limite (o Enterprise). A assinatura continua uma só, na empresa (organization).
ALTER TABLE plan ADD COLUMN max_units INTEGER CHECK (max_units >= 1);

UPDATE plan SET max_units = 1 WHERE code <> 'enterprise';
