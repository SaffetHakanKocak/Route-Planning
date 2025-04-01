package com.example;

public class KentKart extends OdemeYontemi implements Indirim {
    private final double indirimYuzdesi = 0.20;

    @Override
    public String OdemeYontemiGoster() {
        return "Kent Kart";
    }
    
    // KentKart ile takside ödeme yapılamaz.
    @Override
    public boolean isValidForTaxi() {
        return false;
    }
    
    @Override
    public double getIndirimYuzdesi(){
        return indirimYuzdesi;
    }
    
    @Override
    public double IndirimUygula(double toplamUcret){
        return toplamUcret * indirimYuzdesi;
    }
}
