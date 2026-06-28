package com.eventrouting.routing.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.eventrouting.routing.entity.PaymentEvent;

public interface PaymentEventRepository extends JpaRepository<PaymentEvent, Long> {
}
