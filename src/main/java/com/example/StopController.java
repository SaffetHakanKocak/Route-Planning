package com.example;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class StopController {

    private final CityDataRepository cityDataRepository;

    @Autowired
    public StopController(CityDataRepository cityDataRepository) {
        this.cityDataRepository = cityDataRepository;
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
        double lat = request.get("lat");
        double lon = request.get("lon");
        System.out.println("Başlangıç noktası ayarlandı: Enlem = " + lat + ", Boylam = " + lon);

        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Başlangıç noktası başarıyla ayarlandı: Enlem = " + lat + ", Boylam = " + lon);
        return response;
    }
    
    @PostMapping("/setDestinationPoint")
    public Map<String, String> setDestinationPoint(@RequestBody Map<String, Double> request) {
        double lat = request.get("lat");
        double lon = request.get("lon");
        System.out.println("Hedef noktası ayarlandı: Enlem = " + lat + ", Boylam = " + lon);

        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Hedef noktası başarıyla ayarlandı: Enlem = " + lat + ", Boylam = " + lon);
        return response;
    }
}
