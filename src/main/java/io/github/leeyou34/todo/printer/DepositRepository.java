package io.github.leeyou34.todo.printer;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DepositRepository extends JpaRepository<Deposit, UUID> {

	List<Deposit> findByOwnerIdOrderByDepositedOnDescCreatedAtDesc(UUID ownerId);

	Optional<Deposit> findByIdAndOwnerId(UUID id, UUID ownerId);
}
