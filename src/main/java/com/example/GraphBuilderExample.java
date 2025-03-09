package com.example;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.google.gson.Gson;

// ==================== Uygulama Başlatıcı ====================
@SpringBootApplication
public class GraphBuilderExample implements CommandLineRunner {
    private final GraphBuilderService graphBuilderService;

    @Autowired
    public GraphBuilderExample(GraphBuilderService graphBuilderService) {
        this.graphBuilderService = graphBuilderService;
    }

    public static void main(String[] args) {
        SpringApplication.run(GraphBuilderExample.class, args);
    }

    @Override
    public void run(String... args) {
        try {
            Graph<Stop, RouteEdge> graph = graphBuilderService.buildGraph();
            System.out.println("Graf Kenarları:");
            graph.edgeSet().forEach(edge -> {
                Stop source = graph.getEdgeSource(edge);
                Stop target = graph.getEdgeTarget(edge);
                System.out.println(source.getId() + " -> " + target.getId() + " | " + edge);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

// ==================== Graf Oluşturma Servisi ====================
@Service
class GraphBuilderService {
    private final CityDataRepository cityDataRepository;

    @Autowired
    public GraphBuilderService(CityDataRepository cityDataRepository) {
        this.cityDataRepository = cityDataRepository;
    }

    public Graph<Stop, RouteEdge> buildGraph() throws Exception {
        CityData cityData = cityDataRepository.loadCityData();

        // Durakları id'lerine göre haritalıyoruz
        Map<String, Stop> stopMap = new HashMap<>();
        for (Stop stop : cityData.getDuraklar()) {
            stopMap.put(stop.getId(), stop);
        }

        // Grafı oluşturuyoruz: Düğümler durak, kenarlar yollar
        Graph<Stop, RouteEdge> graph = new DefaultDirectedGraph<>(RouteEdge.class);
        for (Stop stop : cityData.getDuraklar()) {
            graph.addVertex(stop);
        }

        for (Stop stop : cityData.getDuraklar()) {
            if (!stop.isSonDurak() && stop.getNextStops() != null) {
                for (NextStopInfo ns : stop.getNextStops()) {
                    Stop target = stopMap.get(ns.getStopId());
                    if (target != null) {
                        RouteEdge edge = new RouteEdge(ns.getMesafe(), ns.getSure(), ns.getUcret());
                        graph.addEdge(stop, target, edge);
                    }
                }
            }
            if (stop.getTransfer() != null) {
                Transfer transfer = stop.getTransfer();
                Stop transferTarget = stopMap.get(transfer.getTransferStopId());
                if (transferTarget != null) {
                    RouteEdge transferEdge = new RouteEdge(0.0, transfer.getTransferSure(), transfer.getTransferUcret());
                    graph.addEdge(stop, transferTarget, transferEdge);
                }
            }
        }
        return graph;
    }
}

// ==================== Veri Erişimi (Repository) ====================
@Service
class CityDataRepository {
    public CityData loadCityData() throws Exception {
        InputStream is = getClass().getResourceAsStream("/data.json");
        if (is == null) {
            throw new Exception("data.json not found in src/main/resources!");
        }
        Gson gson = new Gson();
        return gson.fromJson(new InputStreamReader(is, StandardCharsets.UTF_8), CityData.class);
    }
}

// ==================== Web Controller'lar ====================

// Ana sayfa için Controller
@Controller
class HomeController {
    @GetMapping("/")
    public String home() {
        return "index";
    }
}

// API Controller
@RestController
@RequestMapping("/api")
class StopController {

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

// ==================== Model Sınıfları ====================
class CityData {
    private String city;
    private Taxi taxi;
    private List<Stop> duraklar;

    public String getCity() { return city; }
    public Taxi getTaxi() { return taxi; }
    public List<Stop> getDuraklar() { return duraklar; }
}

class Taxi {
    private double openingFee;
    private double costPerKm;

    public double getOpeningFee() { return openingFee; }
    public double getCostPerKm() { return costPerKm; }
}

class Stop {
    private String id;
    private String name;
    private String type;
    private double lat;
    private double lon;
    private boolean sonDurak;
    private List<NextStopInfo> nextStops;
    private Transfer transfer;

    public String getId() { return id; }
    public String getName() { return name; }
    public String getType() { return type; }
    public double getLat() { return lat; }
    public double getLon() { return lon; }
    public boolean isSonDurak() { return sonDurak; }
    public List<NextStopInfo> getNextStops() { return nextStops; }
    public Transfer getTransfer() { return transfer; }

    @Override
    public String toString() { return id + " (" + name + ")"; }
}

class NextStopInfo {
    private String stopId;
    private double mesafe;
    private int sure;
    private double ucret;

    public String getStopId() { return stopId; }
    public double getMesafe() { return mesafe; }
    public int getSure() { return sure; }
    public double getUcret() { return ucret; }

    @Override
    public String toString() {
        return "[mesafe=" + mesafe + ", sure=" + sure + ", ucret=" + ucret + "]";
    }
}

class Transfer {
    private String transferStopId;
    private int transferSure;
    private double transferUcret;

    public String getTransferStopId() { return transferStopId; }
    public int getTransferSure() { return transferSure; }
    public double getTransferUcret() { return transferUcret; }
}

class RouteEdge {
    private double mesafe;
    private int sure;
    private double ucret;

    public RouteEdge(double mesafe, int sure, double ucret) {
        this.mesafe = mesafe;
        this.sure = sure;
        this.ucret = ucret;
    }

    public double getMesafe() { return mesafe; }
    public int getSure() { return sure; }
    public double getUcret() { return ucret; }

    @Override
    public String toString() {
        return "[mesafe=" + mesafe + ", sure=" + sure + ", ucret=" + ucret + "]";
    }
}
