package com.example.order.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * An asset can be both token, and normal currency. `XYZ` or `USD`
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
public class Asset {
    // token id
    private int id;
    // iso alpha3 codes, example `XYZ` or `USD`
    private String denom;
}
