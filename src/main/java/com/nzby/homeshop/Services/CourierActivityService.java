package com.nzby.homeshop.Services;

import com.nzby.homeshop.POJO.Courier;
import com.nzby.homeshop.POJO.CourierActivity;
import com.nzby.homeshop.POJO.Enum.ActivityType;
import com.nzby.homeshop.Repository.CourierActivityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class CourierActivityService {
    @Autowired
    private CourierActivityRepository courierActivityRepository;

    public void save(CourierActivity activity) {
        courierActivityRepository.save(activity);
    }
    @Transactional
    public void logActivity(Courier courier, ActivityType activityType, String details) {
        CourierActivity activity = new CourierActivity();
        activity.setCourier(courier);
        activity.setActivityType(activityType);
        activity.setDetails(details);
        activity.setTimestamp(LocalDateTime.now());
        courierActivityRepository.save(activity);
    }
}
