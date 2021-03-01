package com.example.order.utils;

public class Helper {

    /**
     * Converts the amount to the decimal amount that System can handle
     * (1BTC = 1,0000,0000 satoshi so, for XYZ and other coins, I decided to keep amount in 10*pow(8) decimals
     * @param amount
     * @return
     */
    public static long lengthen(double amount) {
        return (long) (amount * 100000000);
    }

}
