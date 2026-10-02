package io.github.leeyou34.todo.printer;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;


/** ownerId를 조건으로만 조회합니다. */
public interface CycleRepository extends JpaRepository<OrderCycle, UUID> {
	List<OrderCycle> findByOwnerIdOrderByMonthDesc(UUID ownerId);

	Optional<OrderCycle> findByIdAndOwnerId(UUID id, UUID ownerId);

	Optional<OrderCycle> findByOwnerIdAndMonth(UUID ownerId, String month);
}
