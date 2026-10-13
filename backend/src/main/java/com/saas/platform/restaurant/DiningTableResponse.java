package com.saas.platform.restaurant;

public record DiningTableResponse(Long id, String name, int capacity, String status) {
    static DiningTableResponse from(DiningTable diningTable) {
        return new DiningTableResponse(
            diningTable.getId(),
            diningTable.getName(),
            diningTable.getCapacity(),
            diningTable.getStatus()
        );
    }
}
