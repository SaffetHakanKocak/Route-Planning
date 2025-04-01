package com.example;

public abstract class OdemeYontemi {
    public abstract String OdemeYontemiGoster();
    
    // Taksi ödemesi için uygunluğu kontrol eden metod.
    public abstract boolean isValidForTaxi();
}
