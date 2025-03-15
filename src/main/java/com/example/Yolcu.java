package com.example;

public abstract class Yolcu {
    // Yolcu tipi gösterimini sağlayan abstract metot.
    public abstract String YolcuTipiGoster();
}

// Ogrenci sınıfı, Yolcu'dan türetilmiş ve Indirim arayüzünü implement etmiştir.
class Ogrenci extends Yolcu implements Indirim {

    private double indirimYuzdesi = 0.50;

    @Override
    public String YolcuTipiGoster() {
        return "Öğrenci";
    }

    // Örnek olarak toplam ücretten %50 indirim uygular.
    @Override
    public double IndirimUygula(double toplamUcret) {
        return toplamUcret * indirimYuzdesi;
    }

    public double getIndirimYuzdesi(){
        return indirimYuzdesi;
    }
}

// Yasli sınıfı, Yolcu'dan türetilmiş ve Indirim arayüzünü implement etmiştir.
class Yasli extends Yolcu implements Indirim {

    private double indirimYuzdesi = 0.30;

    @Override
    public String YolcuTipiGoster() {
        return "Yaşlı";
    }

    // Örnek olarak toplam ücretten %30 indirim uygular.
    @Override
    public double IndirimUygula(double toplamUcret) {
        return toplamUcret * indirimYuzdesi;
    }
    
    public double getIndirimYuzdesi(){
        return indirimYuzdesi;
    }
}

// Genel sınıfı, Yolcu'dan türetilmiş ancak herhangi bir indirim uygulamaz.
class Genel extends Yolcu {

    @Override
    public String YolcuTipiGoster() {
        return "Genel";
    }
}
