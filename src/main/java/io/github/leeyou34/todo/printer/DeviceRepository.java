package io.github.leeyou34.todo.printer;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import io.github.leeyou34.todo.printer.Enums.DeviceStatus;

/** ownerId를 조건으로만 조회합니다. */
public interface DeviceRepository extends JpaRepository<Device, UUID> {
	List<Device> findByOwnerIdOrderBySerialAsc(UUID ownerId);

	List<Device> findByOwnerIdAndStatusOrderBySerialAsc(UUID ownerId, DeviceStatus status);

	List<Device> findByOwnerIdAndRequestId(UUID ownerId, UUID requestId);

	Optional<Device> findByOwnerIdAndSerial(UUID ownerId, String serial);

	Optional<Device> findByIdAndOwnerId(UUID id, UUID ownerId);

	boolean existsByOwnerIdAndSerial(UUID ownerId, String serial);
}
