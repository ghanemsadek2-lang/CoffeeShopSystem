package com.coffeeshop.dto;public final class SettingCommands{private SettingCommands(){}public record Save(Long id,String key,String value,String type,String description,byte[]version){}}
