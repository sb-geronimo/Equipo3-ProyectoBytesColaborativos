package es.bytescolab.msalerts.repository;

import es.bytescolab.msalerts.entity.AlertRuleEntity;
import es.bytescolab.msalerts.enums.AlertType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertRuleRepository extends JpaRepository<AlertRuleEntity, AlertType> {
}