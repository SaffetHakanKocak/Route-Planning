package com.example;

public class ShortestRouteStrategy implements RouteStrategy {
    private final RotaHesaplama rotaHesaplama;

    public ShortestRouteStrategy(RotaHesaplama rotaHesaplama) {
        this.rotaHesaplama = rotaHesaplama;
    }

    @Override
    public String calculateRouteHtml() {
        return rotaHesaplama.getUygunMesafeHtml();
    }
}
