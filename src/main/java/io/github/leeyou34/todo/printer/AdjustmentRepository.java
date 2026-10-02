package io.github.leeyou34.todo.printer;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;


/** ownerId를 조건으로만 조회합니다. */
public interface AdjustmentRepository extends JpaRepository<Adjustment, UUID> {
	List<Adjustment> findByOwnerIdOrderByCreatedOnAsc(UUID ownerId);
}
