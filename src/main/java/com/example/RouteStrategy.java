package com.example;

public interface RouteStrategy {
    /**
     * İlgili rota hesaplama stratejisine göre, dinamik HTML formatında rota çıktısı üretir.
     */
    String calculateRouteHtml();
}
