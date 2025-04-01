package com.example;

public class KrediKarti extends OdemeYontemi implements Zam {
    private final double ZamYuzdesi = 0.30;

    @Override
    public String OdemeYontemiGoster() {
        return "Kredi Kartı";
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
}
