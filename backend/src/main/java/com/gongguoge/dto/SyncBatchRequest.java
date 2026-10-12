package com.gongguoge.dto;

import jakarta.validation.Valid;
import lombok.Data;

import java.util.List;

@Data
public class SyncBatchRequest {

    @Valid
    private List<SyncItemRequest> items;
}
