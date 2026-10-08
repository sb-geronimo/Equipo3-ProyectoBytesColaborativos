package es.bytescolab.msroutes.repository;

import es.bytescolab.msroutes.entity.RouteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface RouteRepository extends JpaRepository<RouteEntity, UUID>, JpaSpecificationExecutor<RouteEntity> {
}
