package com.example;

public abstract class Arac {
    public abstract String AracTipiGoster();
}

class Otobus extends Arac {
    @Override
    public String AracTipiGoster() {
        return "Otobüs";
    }
}

class Tramvay extends Arac {
    @Override
    public String AracTipiGoster() {
        return "Tramvay";
    }
}

