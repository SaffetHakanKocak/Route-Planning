package com.example;

public class Taxi {
    // Açılış ücreti ve km başına ücret sabitleri
    private double openingFee = 10.0;
    private double costPerKm = 4.0;
    private double timePerkm = 1.0;

    public double getOpeningFee() {
        return openingFee;
    }

    public double getCostPerKm() {
        return costPerKm;
    }

    public double getTimePerKm()
    {
        return timePerkm;
    }

    public double UcretHesapla(double km) {
        return openingFee + (km * costPerKm);
    }

    public double SureHesapla(double km){
        return timePerkm * km;
    }
}
