package com.example;

public class Nakit extends OdemeYontemi {
    private double nakitMiktari;

    // Constructor ile başlangıç nakit miktarı set edilebilir.
    public Nakit(double nakitMiktari) {
        this.nakitMiktari = nakitMiktari;
    }

    @Override
    public String OdemeYontemiGoster() {
        return "Nakit (Kalan: " + String.format("%.2f", nakitMiktari) + " TL)";
    }
    
    @Override
    public boolean isValidForTaxi() {
        return true;
    }
    
    public double getNakitMiktari() {
        return nakitMiktari;
    }

    public void setNakitMiktari(double nakitMiktari) {
        this.nakitMiktari = nakitMiktari;
    }
}
