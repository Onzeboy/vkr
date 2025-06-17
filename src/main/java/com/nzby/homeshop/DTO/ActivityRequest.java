package com.nzby.homeshop.DTO;

import com.nzby.homeshop.POJO.Enum.ActivityType;
import lombok.Data;

@Data
public class ActivityRequest {
    private String activityType;
    private String details;

    public String getActivityType() {
        return activityType;
    }

    public void setActivityType(String activityType) {
        this.activityType = activityType;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }
}