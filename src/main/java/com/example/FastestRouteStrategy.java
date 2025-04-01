package com.example;

public class FastestRouteStrategy implements RouteStrategy {
    private final RotaHesaplama rotaHesaplama;

    public FastestRouteStrategy(RotaHesaplama rotaHesaplama) {
        this.rotaHesaplama = rotaHesaplama;
    }

    @Override
    public String calculateRouteHtml() {
        return rotaHesaplama.getUygunZamanHtml();
    }
}
