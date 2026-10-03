package io.github.leeyou34.todo.printer;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;


/** ownerId를 조건으로만 조회합니다. */
public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {
	List<Invoice> findByOwnerIdOrderByCreatedAtAsc(UUID ownerId);

	List<Invoice> findByOwnerIdAndCycleId(UUID ownerId, UUID cycleId);

	List<Invoice> findByOwnerIdAndRequestId(UUID ownerId, UUID requestId);

	Optional<Invoice> findByIdAndOwnerId(UUID id, UUID ownerId);
}
