-- As regras do rodízio valendo na mesa (Guia 26).

-- O produto "só do rodízio": false tira o produto do cardápio à la carte (ele continua na aba
-- Rodízio, se for incluído em algum). Todo produto que já existe continua à la carte.
ALTER TABLE product ADD COLUMN a_la_carte BOOLEAN NOT NULL DEFAULT TRUE;

-- O fim previsto do rodízio da comanda, calculado na ativação (início + tempo limite). Nulo:
-- o rodízio não tinha tempo limite. Mudar o cadastro depois não muda o fim de quem já começou.
ALTER TABLE tab_rodizio ADD COLUMN ends_at TIMESTAMPTZ;
