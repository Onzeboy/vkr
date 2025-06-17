package com.nzby.homeshop.Repository;

import com.nzby.homeshop.POJO.Courier;
import com.nzby.homeshop.POJO.Enum.AvailabilityStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourierRepository extends JpaRepository<Courier, Long> {

    List<Courier> findByStatus(AvailabilityStatus availabilityStatus);
}
