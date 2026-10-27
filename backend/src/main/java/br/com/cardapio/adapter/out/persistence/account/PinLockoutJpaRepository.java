package br.com.cardapio.adapter.out.persistence.account;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface PinLockoutJpaRepository extends JpaRepository<PinLockoutJpaEntity, UUID> {
}
