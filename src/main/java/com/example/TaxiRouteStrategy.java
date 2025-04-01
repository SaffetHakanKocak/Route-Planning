package com.example;

public class TaxiRouteStrategy implements RouteStrategy {
    private final RotaHesaplama rotaHesaplama;

    public TaxiRouteStrategy(RotaHesaplama rotaHesaplama) {
        this.rotaHesaplama = rotaHesaplama;
    }

    @Override
    public String calculateRouteHtml() {
        // Ödeme yöntemi kontrolü: Eğer seçilen ödeme yöntemi taksi ödemesi için uygun değilse,
        // hem hata mesajı hem de daha önceden üretilen taksi rota HTML içeriğini ekleyelim.
        String taxiHtml = rotaHesaplama.getSadeceTaxiHtml();
        if (!rotaHesaplama.getOdemeYontemi().isValidForTaxi()) {
            return "<p style='color:red;'>Taksi ödemesi için geçerli ödeme yöntemi seçilmedi. Lütfen nakit veya kredi kartı seçiniz.</p>" + taxiHtml;
        }
        return taxiHtml;
    }
}
