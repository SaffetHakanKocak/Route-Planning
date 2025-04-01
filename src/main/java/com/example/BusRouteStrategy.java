package com.example;

public class BusRouteStrategy implements RouteStrategy {
    private final RotaHesaplama rotaHesaplama;

    public BusRouteStrategy(RotaHesaplama rotaHesaplama) {
        this.rotaHesaplama = rotaHesaplama;
    }

    @Override
    public String calculateRouteHtml() {
        return rotaHesaplama.getSadeceOtobusHtml();
    }
}
