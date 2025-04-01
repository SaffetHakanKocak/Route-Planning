package com.example;

public class Nakit extends OdemeYontemi {
    @Override
    public String OdemeYontemiGoster() {
        return "Nakit";
    }
    
    @Override
    public boolean isValidForTaxi() {
        return true;
    }
}
