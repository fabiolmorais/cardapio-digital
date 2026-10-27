package br.com.cardapio.domain.account.service;


import br.com.cardapio.domain.account.exception.TeamPermissionException;
import br.com.cardapio.domain.account.model.Role;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TeamPolicyTest {

    @ParameterizedTest(name = "{0} pode mexer em {1}")
    @CsvSource({
            "OWNER,   MANAGER",
            "OWNER,   STAFF",
            "OWNER,   KITCHEN",
            "MANAGER, STAFF",
            "MANAGER, KITCHEN",
    })
    void allowed(Role actor, Role target) {
        assertThatCode(() -> TeamPolicy.checkCanManage(actor, target)).doesNotThrowAnyException();
    }

    @ParameterizedTest(name = "{0} não pode mexer em {1}")
    @CsvSource({
            "OWNER,   OWNER",
            "MANAGER, OWNER",
            "MANAGER, MANAGER",
            "STAFF,   STAFF",
            "STAFF,   KITCHEN",
            "KITCHEN, KITCHEN",
    })
    void denied(Role actor, Role target) {
        assertThatThrownBy(() -> TeamPolicy.checkCanManage(actor, target))
                .isInstanceOf(TeamPermissionException.class);
    }
}