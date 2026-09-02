package com.coffeeshop.model;

import java.time.LocalDateTime;

public final class TableManagementModels {
    private TableManagementModels() { }
    public record CafeTable(long id,String code,String name,int capacity,String status,int displayOrder,boolean active,byte[] rowVersion) {
        public CafeTable { rowVersion=rowVersion.clone(); }
        @Override public byte[] rowVersion(){return rowVersion.clone();}
        @Override public String toString(){return name+" ("+code+")";}
    }
    public record Reservation(long id,String number,long tableId,String tableName,Long customerId,String guestName,
                              String phone,String email,LocalDateTime startsAt,int partySize,String status,String notes,byte[] rowVersion) {
        public Reservation { rowVersion=rowVersion.clone(); }
        @Override public byte[] rowVersion(){return rowVersion.clone();}
    }
}
