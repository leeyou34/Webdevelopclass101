package io.github.leeyou34.todo.printer;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;


/** ownerId를 조건으로만 조회합니다. */
public interface CaseRepository extends JpaRepository<ServiceCase, UUID> {
	List<ServiceCase> findByOwnerIdOrderByOpenedOnDesc(UUID ownerId);

	Optional<ServiceCase> findByIdAndOwnerId(UUID id, UUID ownerId);
}
