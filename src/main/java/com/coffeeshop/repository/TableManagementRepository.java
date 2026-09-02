package com.coffeeshop.repository;
import com.coffeeshop.dto.TableManagementCommands;import com.coffeeshop.model.TableManagementModels;import java.util.List;
public interface TableManagementRepository {List<TableManagementModels.CafeTable> tables();List<TableManagementModels.Reservation> reservations();long saveTable(TableManagementCommands.SaveTable value);void setTableActive(long id,boolean active,byte[] version);long saveReservation(TableManagementCommands.SaveReservation value);}
