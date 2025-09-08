package com.order.order.ordering.repository;

import com.order.order.ordering.domain.Ordering;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Ordering, Long> {
    List<Ordering> findAllByMemberEmail(String email);
}
