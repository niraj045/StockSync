package com.stocksync.migration.dto;
import jakarta.validation.constraints.*;
public record ItemMappingRequest(Long itemId,boolean createNew,@Size(max=50)String itemCode,@Size(max=150)String itemName,
 boolean saveAlias,boolean confirmDuplicate,boolean exclude,@Size(max=500)String exclusionReason,@NotNull Long version){}
