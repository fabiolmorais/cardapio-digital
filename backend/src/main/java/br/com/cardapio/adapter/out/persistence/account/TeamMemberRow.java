package br.com.cardapio.adapter.out.persistence.account;

import java.util.UUID;

/** Projeção da consulta da equipe: o vínculo e o nome da pessoa. */
public interface TeamMemberRow {

    UUID getId();

    UUID getUserId();

    UUID getEstablishmentId();

    String getRole();

    String getPinHash();

    boolean getActive();

    String getName();
}
