package com.example;

public class TaxiRouteStrategy implements RouteStrategy {
    private final RotaHesaplama rotaHesaplama;

    public TaxiRouteStrategy(RotaHesaplama rotaHesaplama) {
        this.rotaHesaplama = rotaHesaplama;
    }

    @Override
    public String calculateRouteHtml() {
        return rotaHesaplama.getSadeceTaxiHtml();
    }
}
