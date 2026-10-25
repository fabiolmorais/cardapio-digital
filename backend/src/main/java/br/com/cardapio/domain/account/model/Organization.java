package br.com.cardapio.domain.account.model;

import br.com.cardapio.domain.shared.model.Document;
import br.com.cardapio.domain.shared.model.Email;
import br.com.cardapio.domain.shared.model.PhoneNumber;

import java.util.Objects;

/**
 * A conta que assina o serviço (dona de um ou mais estabelecimentos).
 */
public record Organization(
        OrganizationId id,
        String name,
        Document document,
        PhoneNumber whatsapp,
        Email email
) {
    public Organization {
        Objects.requireNonNull(id);
        Objects.requireNonNull(document);
        Objects.requireNonNull(whatsapp);
        Objects.requireNonNull(email);
        name = Names.required(name, "Informe o nome do estabelecimento");
    }

    public static Organization create(String name, Document document, PhoneNumber whatsapp, Email email) {
        return new Organization(OrganizationId.newId(), name, document, whatsapp, email);
    }
}
