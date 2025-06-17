package com.nzby.homeshop.DTO;

import lombok.Data;

@Data
public class ProcessedOrdersDTO {
    private long todayCount;
    private long totalCount;

    public long getTodayCount() {
        return todayCount;
    }

    public void setTodayCount(long todayCount) {
        this.todayCount = todayCount;
    }

    public long getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(long totalCount) {
        this.totalCount = totalCount;
    }
}