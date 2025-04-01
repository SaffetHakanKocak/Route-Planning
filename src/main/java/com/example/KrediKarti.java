package com.example;

public class KrediKarti extends OdemeYontemi implements Zam {
    private final double ZamYuzdesi = 0.30;
    private double krediLimiti;

    // Constructor ile kredi limiti belirlenir.
    public KrediKarti(double krediLimiti) {
        this.krediLimiti = krediLimiti;
    }

    @Override
    public String OdemeYontemiGoster() {
        return "Kredi Kartı (Limit: " + String.format("%.2f", krediLimiti) + " TL)";
    }
    
    @Override
    public boolean isValidForTaxi() {
        return true;
    }
    
    @Override
    public double getZamYuzdesi() {
        return ZamYuzdesi;
    }

    @Override
    public double ZamUygula(double toplamUcret) {
        return toplamUcret * ZamYuzdesi;
    }
    
    public double getKrediLimiti() {
        return krediLimiti;
    }

    public void setKrediLimiti(double krediLimiti) {
        this.krediLimiti = krediLimiti;
    }
}
