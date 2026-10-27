package br.com.cardapio.domain.account.service;

import br.com.cardapio.domain.account.exception.TeamPermissionException;
import br.com.cardapio.domain.account.model.Role;

/**
 * Quem pode mexer em quem na equipe.
 *
 * - Dono: cadastra e altera gerente, garçom e cozinha.
 * - Gerente: só garçom e cozinha (não cria outro gerente nem mexe em gerente).
 * - Ninguém altera o dono pela tela da equipe.
 * - Garçom e cozinha não mexem na equipe.
 *
 * Como o dono não pode ser alterado e o gerente não mexe em gerente, ninguém consegue
 * desativar a si mesmo por aqui.
 */
public final class TeamPolicy {
    private TeamPolicy() {
    }

    /**
     * @param actor      papel de quem está fazendo a mudança
     * @param targetRole papel que a pessoa alterada tem (ou passará a ter)
     */
    public static void checkCanManage(Role actor, Role targetRole) {
        if (targetRole == Role.OWNER) {
            throw new TeamPermissionException("O dono não pode ser alterado pela equipe");
        }
        boolean allowed = switch (actor) {
            case OWNER -> true;
            case MANAGER -> targetRole == Role.STAFF || targetRole == Role.KITCHEN;
            case STAFF, KITCHEN -> false;
        };
        if (!allowed) {
            throw new TeamPermissionException("Seu papel não permite cadastrar ou alterar " + label(targetRole));
        }
    }

    private static String label(Role role) {
        return switch (role) {
            case OWNER -> "o dono";
            case MANAGER -> "gerentes";
            case STAFF -> "garçons";
            case KITCHEN -> "a cozinha";
        };
    }
}
