package com.nzby.homeshop.Repository;

import com.nzby.homeshop.POJO.Courier;
import com.nzby.homeshop.POJO.CourierActivity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface CourierActivityRepository extends JpaRepository<CourierActivity, Long> {

    List<CourierActivity> findByCourierAndTimestampAfterOrderByTimestampDesc(Courier courier, LocalDateTime since);
}
