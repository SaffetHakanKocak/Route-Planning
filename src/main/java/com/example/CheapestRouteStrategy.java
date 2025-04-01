package com.example;

public class CheapestRouteStrategy implements RouteStrategy {
    private final RotaHesaplama rotaHesaplama;

    public CheapestRouteStrategy(RotaHesaplama rotaHesaplama) {
        this.rotaHesaplama = rotaHesaplama;
    }

    @Override
    public String calculateRouteHtml() {
        return rotaHesaplama.getUygunUcretHtml();
    }
}
