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
    // Bu alanlarda başlangıç ve hedef koordinatları saklanabilir. (Örneğin session veya servis aracılığıyla)
    private double startLat;
    private double startLon;
    private double destLat;
    private double destLon;

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
        System.out.println("Seçilen yolcu tipi: " + passengerType);

        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Yolcu tipi başarıyla seçildi: " + passengerType);
        return response;
    }

    @PostMapping("/selectPaymentType")
    public Map<String, String> selectPaymentType(@RequestBody Map<String, String> request) {
        String paymentType = request.get("paymentType");
        System.out.println("Seçilen ödeme türü: " + paymentType);

        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Ödeme türü başarıyla seçildi: " + paymentType);
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
    
    // Rota hesaplamayı tetikleyecek endpoint (ilk rota hesaplama methodu: UygunUcretHesapla)
    @PostMapping("/calculateRoute")
    public Map<String, String> calculateRoute() {
        RotaHesaplama rotaHesaplama = new RotaHesaplama(startLat, startLon, destLat, destLon, graphBuilderService);
        rotaHesaplama.UygunUcretHesapla();
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Rota hesaplama işlemi tamamlandı. Terminalden adımlar incelenebilir.");
        return response;
    }
}

