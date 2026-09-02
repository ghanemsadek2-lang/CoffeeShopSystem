package com.coffeeshop.dto;

import java.time.LocalDateTime;

public final class TableManagementCommands {
    private TableManagementCommands() { }
    public record SaveTable(Long id,String code,String name,int capacity,String status,int displayOrder,boolean active,byte[] rowVersion) { }
    public record SaveReservation(Long id,long tableId,Long customerId,String guestName,String phone,String email,
                                  LocalDateTime startsAt,int partySize,String status,String notes,byte[] rowVersion) { }
}
