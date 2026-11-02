package br.com.cardapio.application.port.in.tablesession;

public interface StartTableSessionUseCase {

    /**
     * Abre a sessão do celular nesta mesa, ou continua a que ele já tem.
     *
     * @param currentToken o token do cookie que o celular mandou; null na primeira visita
     */
    TableSessionStarted start(String tableToken, String currentToken);
}
