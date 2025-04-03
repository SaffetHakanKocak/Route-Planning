package com.example;

public abstract class Yolcu {
    public abstract String YolcuTipiGoster();
}

class Ogrenci extends Yolcu implements Indirim {

    private double indirimYuzdesi = 0.50;

    @Override
    public String YolcuTipiGoster() {
        return "Öğrenci";
    }

    @Override
    public double IndirimUygula(double toplamUcret) {
        return toplamUcret * indirimYuzdesi;
    }

    public double getIndirimYuzdesi(){
        return indirimYuzdesi;
    }
}

class Yasli extends Yolcu implements Indirim {

    private double indirimYuzdesi = 0.30;

    @Override
    public String YolcuTipiGoster() {
        return "Yaşlı";
    }

    @Override
    public double IndirimUygula(double toplamUcret) {
        return toplamUcret * indirimYuzdesi;
    }
    
    public double getIndirimYuzdesi(){
        return indirimYuzdesi;
    }
}

class Genel extends Yolcu {

    @Override
    public String YolcuTipiGoster() {
        return "Genel";
    }
}
