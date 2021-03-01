package com.example.order.model.request;

import lombok.Data;

@Data
public class AddAssetRequest {
    private int assetId;
    private String denom;
}
