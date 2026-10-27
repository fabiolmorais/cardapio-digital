-- Equipe (Guia 4): garçom e cozinha não têm e-mail nem senha; entram por PIN num aparelho vinculado.
-- E-mail e senha passam a ser opcionais, mas andam juntos (os dois ou nenhum).
ALTER TABLE app_user ALTER COLUMN email DROP NOT NULL;
ALTER TABLE app_user ALTER COLUMN password_hash DROP NOT NULL;
ALTER TABLE app_user ADD CONSTRAINT app_user_login_ck CHECK ((email IS NULL) = (password_hash IS NULL));

-- Todo mundo da equipe tem PIN; só o dono (que entra com e-mail e senha) pode ficar sem.
ALTER TABLE membership ADD CONSTRAINT membership_pin_ck CHECK (role = 'OWNER' OR pin_hash IS NOT NULL);
