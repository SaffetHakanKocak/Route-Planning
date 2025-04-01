package com.example;

public class KentKart extends OdemeYontemi implements Indirim {
    private final double indirimYuzdesi = 0.20;
    private double bakiye;

    // Constructor ile başlangıç bakiyesi belirlenir.
    public KentKart(double bakiye) {
        this.bakiye = bakiye;
    }

    @Override
    public String OdemeYontemiGoster() {
        return "Kent Kart (Bakiye: " + String.format("%.2f", bakiye) + " TL)";
    }
    
    @Override
    public boolean isValidForTaxi() {
        return false;
    }
    
    @Override
    public double getIndirimYuzdesi() {
        return indirimYuzdesi;
    }
    
    @Override
    public double IndirimUygula(double toplamUcret) {
        return toplamUcret * indirimYuzdesi;
    }
    
    public double getBakiye() {
        return bakiye;
    }

    public void setBakiye(double bakiye) {
        this.bakiye = bakiye;
    }
}
