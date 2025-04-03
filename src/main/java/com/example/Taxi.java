package com.example;


public class Taxi extends Arac {

    private double openingFee = 10.0;
    private double costPerKm = 4.0;
    private double timePerKm = 1.0; 

    public double getOpeningFee() {
        return openingFee;
    }

    public double getCostPerKm() {
        return costPerKm;
    }

    public double getTimePerKm() {
        return timePerKm;
    }

    public double UcretHesapla(double km) {
        return openingFee + (km * costPerKm);
    }

    public double SureHesapla(double km) {
        return timePerKm * km;
    }

    @Override
    public String AracTipiGoster() {
        return "Taxi";
    }
}
