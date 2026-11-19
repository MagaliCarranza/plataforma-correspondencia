-- Marca las cuentas sembradas por el modo demo para distinguirlas del admin real.
-- Las cuentas no-demo (es_demo = false) nunca se tocan en el reset nocturno.
ALTER TABLE usuario
    ADD COLUMN es_demo BOOLEAN NOT NULL DEFAULT false;

CREATE INDEX idx_usuario_es_demo ON usuario (es_demo);
