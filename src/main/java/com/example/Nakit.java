package com.example;

public class Nakit extends OdemeYontemi {
    private double nakitMiktari;

    public Nakit(double nakitMiktari) {
        this.nakitMiktari = nakitMiktari;
    }

    @Override
    public String OdemeYontemiGoster() {
        return "Nakit (Kalan: " + String.format("%.2f", nakitMiktari) + " TL)";
    }
    
    public double getNakitMiktari() {
        return nakitMiktari;
    }

    public void setNakitMiktari(double nakitMiktari) {
        this.nakitMiktari = nakitMiktari;
    }
}
