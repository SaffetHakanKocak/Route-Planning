package com.example;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class StopController {
    private double startLat;
    private double startLon;
    private double destLat;
    private double destLon;

    private Yolcu selectedYolcu;
    private OdemeYontemi selectedOdemeYontemi;

    private final CityDataRepository cityDataRepository;
    private final GraphBuilderService graphBuilderService;

    @Autowired
    public StopController(CityDataRepository cityDataRepository, GraphBuilderService graphBuilderService) {
        this.cityDataRepository = cityDataRepository;
        this.graphBuilderService = graphBuilderService;
    }
    
    @GetMapping("/stops")
    public List<Stop> getAllStops() throws Exception {
        CityData cityData = cityDataRepository.loadCityData();
        return cityData.getDuraklar();
    }

    @PostMapping("/selectPassengerType")
    public Map<String, String> selectPassengerType(@RequestBody Map<String, String> request) {
        String passengerType = request.get("passengerType");
        selectedYolcu = ObjectFactory.createYolcu(passengerType);
        System.out.println("Seçilen yolcu tipi: " + selectedYolcu.YolcuTipiGoster());
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Yolcu tipi başarıyla seçildi: " + selectedYolcu.YolcuTipiGoster());
        return response;
    }

    @PostMapping("/selectPaymentType")
    public Map<String, String> selectPaymentType(@RequestBody Map<String, String> request) {
        String paymentType = request.get("paymentType");
        selectedOdemeYontemi = ObjectFactory.createOdemeYontemi(paymentType);
        System.out.println("Seçilen ödeme türü: " + selectedOdemeYontemi.OdemeYontemiGoster());
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Ödeme türü başarıyla seçildi: " + selectedOdemeYontemi.OdemeYontemiGoster());
        return response;
    }
    
    @PostMapping("/setStartPoint")
    public Map<String, String> setStartPoint(@RequestBody Map<String, Double> request) {
        startLat = request.get("lat");
        startLon = request.get("lon");
        System.out.println("Başlangıç noktası ayarlandı: Enlem = " + startLat + ", Boylam = " + startLon);
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Başlangıç noktası başarıyla ayarlandı: Enlem = " + startLat + ", Boylam = " + startLon);
        return response;
    }
    
    @PostMapping("/setDestinationPoint")
    public Map<String, String> setDestinationPoint(@RequestBody Map<String, Double> request) {
        destLat = request.get("lat");
        destLon = request.get("lon");
        System.out.println("Hedef noktası ayarlandı: Enlem = " + destLat + ", Boylam = " + destLon);
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Hedef noktası başarıyla ayarlandı: Enlem = " + destLat + ", Boylam = " + destLon);
        return response;
    }
    
    // Orijinal calculateRoute: Konsola tüm rota hesaplamalarını yazdırır.
    @PostMapping("/calculateRoute")
    public Map<String, String> calculateRoute() {
        RotaHesaplama rotaHesaplama = new RotaHesaplama(
            startLat, startLon, destLat, destLon,
            graphBuilderService, selectedYolcu, selectedOdemeYontemi
        );
        rotaHesaplama.UygunUcretHesapla();
        rotaHesaplama.UygunZamanHesapla();
        rotaHesaplama.UygunKmHesapla();
        rotaHesaplama.SadeceOtobusRota();
        rotaHesaplama.SadeceTramvayRota();
        rotaHesaplama.SadeceTaxiRota();
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Tüm rota hesaplamaları konsola yazıldı.");
        return response;
    }

    // Dinamik HTML döndüren endpoint'ler:

    @PostMapping("/calculateCheapestRoute")
    public Map<String, Object> calculateCheapestRoute() {
        RotaHesaplama rh = new RotaHesaplama(
            startLat, startLon, destLat, destLon,
            graphBuilderService, selectedYolcu, selectedOdemeYontemi
        );
        String routeHtml = rh.getUygunUcretHtml();
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("routeHtml", routeHtml);
        return response;
    }

    @PostMapping("/calculateFastestRoute")
    public Map<String, Object> calculateFastestRoute() {
        RotaHesaplama rh = new RotaHesaplama(
            startLat, startLon, destLat, destLon,
            graphBuilderService, selectedYolcu, selectedOdemeYontemi
        );
        String routeHtml = rh.getUygunZamanHtml();
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("routeHtml", routeHtml);
        return response;
    }

    @PostMapping("/calculateShortestRoute")
    public Map<String, Object> calculateShortestRoute() {
        RotaHesaplama rh = new RotaHesaplama(
            startLat, startLon, destLat, destLon,
            graphBuilderService, selectedYolcu, selectedOdemeYontemi
        );
        String routeHtml = rh.getUygunMesafeHtml();
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("routeHtml", routeHtml);
        return response;
    }

    @PostMapping("/calculateBusRoute")
    public Map<String, Object> calculateBusRoute() {
        RotaHesaplama rh = new RotaHesaplama(
            startLat, startLon, destLat, destLon,
            graphBuilderService, selectedYolcu, selectedOdemeYontemi
        );
        String routeHtml = rh.getSadeceOtobusHtml();
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("routeHtml", routeHtml);
        return response;
    }

    @PostMapping("/calculateTramRoute")
    public Map<String, Object> calculateTramRoute() {
        RotaHesaplama rh = new RotaHesaplama(
            startLat, startLon, destLat, destLon,
            graphBuilderService, selectedYolcu, selectedOdemeYontemi
        );
        String routeHtml = rh.getSadeceTramvayHtml();
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("routeHtml", routeHtml);
        return response;
    }

    @PostMapping("/calculateTaxiRoute")
    public Map<String, Object> calculateTaxiRoute() {
        RotaHesaplama rh = new RotaHesaplama(
            startLat, startLon, destLat, destLon,
            graphBuilderService, selectedYolcu, selectedOdemeYontemi
        );
        String routeHtml = rh.getSadeceTaxiHtml();
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("routeHtml", routeHtml);
        return response;
    }
}
