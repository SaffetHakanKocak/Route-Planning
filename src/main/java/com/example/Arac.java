package com.example;

public abstract class Arac {
    // Araç tipini gösteren abstract metot.
    public abstract String AracTipiGoster();
}

// Otobüs sınıfı, Araç sınıfından türetilmiştir.
class Otobus extends Arac {
    @Override
    public String AracTipiGoster() {
        return "Otobüs";
    }
}

// Tramvay sınıfı, Araç sınıfından türetilmiştir.
class Tramvay extends Arac {
    @Override
    public String AracTipiGoster() {
        return "Tramvay";
    }
}

// Taxi sınıfı, Araç sınıfından türetilmiştir.
class Taxi extends Arac {
    @Override
    public String AracTipiGoster() {
        return "Taxi";
    }
}
