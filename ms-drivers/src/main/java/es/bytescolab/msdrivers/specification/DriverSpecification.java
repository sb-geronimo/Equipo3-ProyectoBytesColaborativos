package es.bytescolab.msdrivers.specification;

import es.bytescolab.msdrivers.entity.DriverEntity;
import es.bytescolab.msdrivers.enums.DriverStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

public class DriverSpecification {

    public static Specification<DriverEntity> hasStatus(DriverStatus status) {
        return (root, query, criteriaBuilder) -> {
            if (status == null) return null;

            return criteriaBuilder.equal(root.get("status"), status);
        };
    }

    public static Specification<DriverEntity> hasLicenseExpiringInDays(Integer days) {
        return (root, query, criteriaBuilder) -> {
            if (days == null) return null;

            LocalDate now = LocalDate.now().plusDays(days);
            return criteriaBuilder.lessThanOrEqualTo(root.get("licenseExpiresAt"), now);
        };
    }
}
