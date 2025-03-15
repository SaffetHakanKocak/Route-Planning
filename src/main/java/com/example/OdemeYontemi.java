package com.example;

public abstract class OdemeYontemi {
    // Ödeme yöntemi gösterimini sağlayan abstract metot.
    public abstract String OdemeYontemiGoster();
}

// Nakit ödeme yöntemi.
class Nakit extends OdemeYontemi {
    @Override
    public String OdemeYontemiGoster() {
        return "Nakit";
    }
}

// Kredi Kartı ödeme yöntemi.
class KrediKarti extends OdemeYontemi implements Zam {
    private double ZamYuzdesi = 0.30;

    @Override
    public String OdemeYontemiGoster() {
        return "Kredi Kartı";
    }

    public double getZamYuzdesi()
    {
        return ZamYuzdesi;
    }

    public double ZamUygula(double toplamUcret)
    {
        return toplamUcret * ZamYuzdesi;
    }
}

// Kent Kart ödeme yöntemi.
class KentKart extends OdemeYontemi implements Indirim{

    private double indirimYuzdesi = 0.20;

    @Override
    public String OdemeYontemiGoster() {
        return "Kent Kart";
    }

    public double getIndirimYuzdesi(){
        return indirimYuzdesi;
    }
    
    public double IndirimUygula(double toplamUcret){
        return toplamUcret * indirimYuzdesi;
    }

}
