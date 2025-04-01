package com.example;

public class TramRouteStrategy implements RouteStrategy {
    private final RotaHesaplama rotaHesaplama;

    public TramRouteStrategy(RotaHesaplama rotaHesaplama) {
        this.rotaHesaplama = rotaHesaplama;
    }

    @Override
    public String calculateRouteHtml() {
        return rotaHesaplama.getSadeceTramvayHtml();
    }
}
