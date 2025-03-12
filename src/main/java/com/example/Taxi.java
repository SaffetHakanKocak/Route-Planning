package com.example;

// Arac adlı abstract sınıfınız şu şekildeyse örnek:
// public abstract class Arac {
//     public abstract String AracTipiGoster();
// }

public class Taxi extends Arac {
    // Açılış ücreti ve km başına ücret sabitleri
    private double openingFee = 10.0;
    private double costPerKm = 4.0;
    private double timePerKm = 1.0; // 1 km için 1 dakika varsayım

    public double getOpeningFee() {
        return openingFee;
    }

    public double getCostPerKm() {
        return costPerKm;
    }

    public double getTimePerKm() {
        return timePerKm;
    }

    /**
     * Verilen km bilgisine göre toplam ücreti hesaplar.
     * @param km gidilecek mesafe (km cinsinden)
     * @return açılış ücreti + (km * km başına ücret)
     */
    public double UcretHesapla(double km) {
        return openingFee + (km * costPerKm);
    }

    /**
     * Verilen km bilgisine göre toplam süreyi hesaplar.
     * @param km gidilecek mesafe (km cinsinden)
     * @return km başına dakika üzerinden toplam süre (dk)
     */
    public double SureHesapla(double km) {
        return timePerKm * km;
    }

    @Override
    public String AracTipiGoster() {
        return "Taxi";
    }
}
