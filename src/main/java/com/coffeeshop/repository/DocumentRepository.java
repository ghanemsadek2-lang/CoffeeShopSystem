package com.coffeeshop.repository;import com.coffeeshop.model.DocumentModels;public interface DocumentRepository{DocumentModels.Catalog load();long issue(long orderId,String type,long userId);}
