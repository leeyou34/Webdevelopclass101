package io.github.leeyou34.todo.printer;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import io.github.leeyou34.todo.printer.Enums.RequestStatus;

/** ownerId를 조건으로만 조회합니다. */
public interface RequestRepository extends JpaRepository<OrderRequest, UUID> {
	List<OrderRequest> findByOwnerIdAndCycleIdOrderByCreatedAtAsc(UUID ownerId, UUID cycleId);

	List<OrderRequest> findByOwnerIdOrderByCreatedAtAsc(UUID ownerId);

	List<OrderRequest> findByOwnerIdAndPurchaseOrderId(UUID ownerId, UUID purchaseOrderId);

	List<OrderRequest> findByOwnerIdAndStatus(UUID ownerId, RequestStatus status);

	Optional<OrderRequest> findByIdAndOwnerId(UUID id, UUID ownerId);
}
