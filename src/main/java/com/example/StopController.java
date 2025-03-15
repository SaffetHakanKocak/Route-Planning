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
    // Başlangıç ve hedef koordinatları
    private double startLat;
    private double startLon;
    private double destLat;
    private double destLon;

    // Seçilen yolcu ve ödeme nesneleri
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
        // ObjectFactory kullanarak yolcu nesnesi oluşturuluyor.
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
        // ObjectFactory kullanarak ödeme yöntemi nesnesi oluşturuluyor.
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
    
    @PostMapping("/calculateRoute")
    public Map<String, String> calculateRoute() {
        // Rota hesaplamada, ObjectFactory'den oluşturulan yolcu ve ödeme nesneleri de parametre olarak veriliyor.
        RotaHesaplama rotaHesaplama = new RotaHesaplama(
            startLat, startLon, destLat, destLon,
            graphBuilderService,
            selectedYolcu,
            selectedOdemeYontemi
        );
        rotaHesaplama.UygunUcretHesapla();
        rotaHesaplama.UygunZamanHesapla();
        rotaHesaplama.UygunKmHesapla();
        rotaHesaplama.SadeceOtobusRota();
        rotaHesaplama.SadeceTramvayRota();
        rotaHesaplama.SadeceTaxiRota();
        
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Rota hesaplama işlemi tamamlandı. Terminalden adımlar incelenebilir.");
        return response;
    }
}
