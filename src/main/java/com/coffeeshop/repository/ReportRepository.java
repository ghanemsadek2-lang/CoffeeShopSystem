package com.coffeeshop.repository;import com.coffeeshop.model.ReportModels;import java.time.LocalDate;public interface ReportRepository{ReportModels.Report load(LocalDate from,LocalDate to);}
