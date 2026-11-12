package br.com.cardapio.application.port.out.rodizio;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.floor.model.DiningTableId;
import br.com.cardapio.domain.rodizio.model.RodizioParticipation;
import br.com.cardapio.domain.rodizio.model.TabRodizioId;
import br.com.cardapio.domain.tablesession.model.TableSessionId;

import java.time.Instant;

/** Quais celulares da mesa estão no rodízio da comanda. */
public interface RodizioPhonePort {

    /** Na ativação: todo celular com a sessão ativa na mesa entra no rodízio. */
    void joinAllAtTable(EstablishmentId establishmentId, TabRodizioId rodizio, DiningTableId tableId, Instant now);

    /** "Vou participar": o celular pede para entrar (se já está dentro, nada muda). */
    void request(EstablishmentId establishmentId, TabRodizioId rodizio, TableSessionId session, Instant now);

    /** O garçom ajustou as pessoas: quem tinha pedido para entrar entra. Devolve quantos entraram. */
    int admitRequested(EstablishmentId establishmentId, TabRodizioId rodizio, Instant now);

    /** O garçom recusou o "Quero o rodízio": quem pediu para entrar volta a não estar (pode pedir de novo). */
    int cancelRequests(EstablishmentId establishmentId, TabRodizioId rodizio);

    RodizioParticipation find(EstablishmentId establishmentId, TabRodizioId rodizio, TableSessionId session);
}
