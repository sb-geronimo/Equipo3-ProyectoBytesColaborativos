package es.bytescolab.msalerts.repository;

import es.bytescolab.msalerts.entity.AlertEntity;
import es.bytescolab.msalerts.enums.AlertStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.domain.Specification;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface AlertRepository extends JpaRepository<AlertEntity, UUID>, JpaSpecificationExecutor<AlertEntity> {

    default Page<AlertEntity> searchAlerts(Specification<AlertEntity> specification, Pageable pageable) {
        return this.findAll(specification, pageable);
    }

    default List<AlertEntity> findActiveByDedupKeys(Collection<String> dedupKeys) {
        if (dedupKeys == null || dedupKeys.isEmpty()) {
            return List.of();
        }
        Specification<AlertEntity> specification = AlertSpecifications.statusIn(
                List.of(AlertStatus.OPEN, AlertStatus.ACKNOWLEDGED));
        return this.findAll(specification.and((root, query, cb) -> root.get("dedupKey").in(dedupKeys)));
    }
}