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
    
    private final CityDataRepository cityDataRepository;
    private final GraphBuilderService graphBuilderService;
    private final UserSelection userSelection;

    @Autowired
    public StopController(CityDataRepository cityDataRepository, 
                          GraphBuilderService graphBuilderService,
                          UserSelection userSelection) {
        this.cityDataRepository = cityDataRepository;
        this.graphBuilderService = graphBuilderService;
        this.userSelection = userSelection;
    }
    
    @GetMapping("/stops")
    public List<Stop> getAllStops() throws Exception {
        CityData cityData = cityDataRepository.loadCityData();
        return cityData.getDuraklar();
    }

    @PostMapping("/selectPassengerType")
    public Map<String, String> selectPassengerType(@RequestBody Map<String, String> request) {
        String passengerType = request.get("passengerType");
        Yolcu yolcu = ObjectFactory.createYolcu(passengerType);
        userSelection.setSelectedYolcu(yolcu);
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Yolcu tipi seçildi: " + yolcu.YolcuTipiGoster());
        return response;
    }

    @PostMapping("/selectPaymentType")
    public Map<String, String> selectPaymentType(@RequestBody Map<String, String> request) {
        String paymentType = request.get("paymentType");
        OdemeYontemi odemeYontemi = ObjectFactory.createOdemeYontemi(paymentType);
        userSelection.setSelectedOdemeYontemi(odemeYontemi);
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Ödeme türü seçildi: " + odemeYontemi.OdemeYontemiGoster());
        return response;
    }
    
    @PostMapping("/setStartPoint")
    public Map<String, String> setStartPoint(@RequestBody Map<String, Double> request) {
        double lat = request.get("lat");
        double lon = request.get("lon");
        userSelection.setStartLat(lat);
        userSelection.setStartLon(lon);
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Başlangıç noktası ayarlandı.");
        return response;
    }
    
    @PostMapping("/setDestinationPoint")
    public Map<String, String> setDestinationPoint(@RequestBody Map<String, Double> request) {
        double lat = request.get("lat");
        double lon = request.get("lon");
        userSelection.setDestLat(lat);
        userSelection.setDestLon(lon);
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Hedef noktası ayarlandı.");
        return response;
    }
    
    @PostMapping("/calculateCheapestRoute")
    public Map<String, Object> calculateCheapestRoute() {
        RotaHesaplama rh = new RotaHesaplama(
            userSelection.getStartLat(), userSelection.getStartLon(),
            userSelection.getDestLat(), userSelection.getDestLon(),
            graphBuilderService, userSelection.getSelectedYolcu(),
            userSelection.getSelectedOdemeYontemi()
        );
        String routeHtml = rh.getRouteHtml("cheapest");
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("routeHtml", routeHtml);
        return response;
    }
    
    @PostMapping("/calculateFastestRoute")
    public Map<String, Object> calculateFastestRoute() {
        RotaHesaplama rh = new RotaHesaplama(
            userSelection.getStartLat(), userSelection.getStartLon(),
            userSelection.getDestLat(), userSelection.getDestLon(),
            graphBuilderService, userSelection.getSelectedYolcu(),
            userSelection.getSelectedOdemeYontemi()
        );
        String routeHtml = rh.getRouteHtml("fastest");
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("routeHtml", routeHtml);
        return response;
    }
    
    @PostMapping("/calculateShortestRoute")
    public Map<String, Object> calculateShortestRoute() {
        RotaHesaplama rh = new RotaHesaplama(
            userSelection.getStartLat(), userSelection.getStartLon(),
            userSelection.getDestLat(), userSelection.getDestLon(),
            graphBuilderService, userSelection.getSelectedYolcu(),
            userSelection.getSelectedOdemeYontemi()
        );
        String routeHtml = rh.getRouteHtml("shortest");
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("routeHtml", routeHtml);
        return response;
    }
    
    @PostMapping("/calculateBusRoute")
    public Map<String, Object> calculateBusRoute() {
        RotaHesaplama rh = new RotaHesaplama(
            userSelection.getStartLat(), userSelection.getStartLon(),
            userSelection.getDestLat(), userSelection.getDestLon(),
            graphBuilderService, userSelection.getSelectedYolcu(),
            userSelection.getSelectedOdemeYontemi()
        );
        String routeHtml = rh.getRouteHtml("bus");
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("routeHtml", routeHtml);
        return response;
    }
    
    @PostMapping("/calculateTramRoute")
    public Map<String, Object> calculateTramRoute() {
        RotaHesaplama rh = new RotaHesaplama(
            userSelection.getStartLat(), userSelection.getStartLon(),
            userSelection.getDestLat(), userSelection.getDestLon(),
            graphBuilderService, userSelection.getSelectedYolcu(),
            userSelection.getSelectedOdemeYontemi()
        );
        String routeHtml = rh.getRouteHtml("tram");
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("routeHtml", routeHtml);
        return response;
    }
    
    @PostMapping("/calculateTaxiRoute")
    public Map<String, Object> calculateTaxiRoute() {
        RotaHesaplama rh = new RotaHesaplama(
            userSelection.getStartLat(), userSelection.getStartLon(),
            userSelection.getDestLat(), userSelection.getDestLon(),
            graphBuilderService, userSelection.getSelectedYolcu(),
            userSelection.getSelectedOdemeYontemi()
        );
        String routeHtml = rh.getRouteHtml("taxi");
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("routeHtml", routeHtml);
        return response;
    }
}