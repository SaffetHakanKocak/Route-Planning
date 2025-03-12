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
class KrediKarti extends OdemeYontemi {
    @Override
    public String OdemeYontemiGoster() {
        return "Kredi Kartı";
    }
}

// Kent Kart ödeme yöntemi.
class KentKart extends OdemeYontemi {
    @Override
    public String OdemeYontemiGoster() {
        return "Kent Kart";
    }
}
