package com.example;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.DefaultUndirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;

/**
 * Bu sınıf, başlangıç ve varış noktaları arasındaki en uygun rotayı
 * 3 farklı kritere göre hesaplayabilir:
 *  - UygunUcretHesapla: Ücret bazlı
 *  - UygunZamanHesapla: Süre bazlı
 *  - UygunKmHesapla: Mesafe bazlı
 *
 * Ayrıca başlangıç/hedef segmentlerinde taksi veya yürüyüş hesaplaması da yapar.
 */
public class RotaHesaplama {

    private static final double TAXI_THRESHOLD = 3.0;  // 3 km üzeri taksi
    private static final int WALK_MIN_PER_KM = 3;      // 1 km = 3 dk yürüyüş

    private final double startLat;  // Başlangıç noktasının enlemi
    private final double startLon;  // Başlangıç noktasının boylamı
    private final double destLat;   // Varış noktasının enlemi
    private final double destLon;   // Varış noktasının boylamı

    private final GraphBuilderService graphBuilderService;
    private final Yolcu yolcu;                // Polimorfik yolcu nesnesi
    private final OdemeYontemi odemeYontemi;  // Polimorfik ödeme yöntemi nesnesi

    /**
     * WeightedGraph üzerinde "DefaultWeightedEdge" --> "RouteEdge" eşleştirmesi tutmak için
     * bir map kullanıyoruz. Böylece, Dijkstra yolunda ilerlerken gerçekte hangi RouteEdge
     * ile karşılaştığımızı bulup mesafe, süre ve ücreti ekrana yazabiliriz.
     */
    private final Map<DefaultWeightedEdge, RouteEdge> edgeMap = new HashMap<>();

    /**
     * Constructor
     */
    public RotaHesaplama(double startLat, double startLon,
                         double destLat, double destLon,
                         GraphBuilderService graphBuilderService,
                         Yolcu yolcu,
                         OdemeYontemi odemeYontemi) {

        this.startLat = startLat;
        this.startLon = startLon;
        this.destLat = destLat;
        this.destLon = destLon;
        this.graphBuilderService = graphBuilderService;
        this.yolcu = yolcu;
        this.odemeYontemi = odemeYontemi;
    }

    // -------------------------------------------------------------------------
    // 1) ÜCRETE GÖRE ROTA HESAPLAMA
    // -------------------------------------------------------------------------
    public void UygunUcretHesapla() {
        try {
            // 1) WeightedGraph'i "cost" parametresiyle oluşturuyoruz
            Graph<Stop, DefaultWeightedEdge> wgraph = buildWeightedGraph("cost");

            // 2) Başlangıç segmenti (Başlangıç Noktası -> En Yakın Durak)
            SegmentResult startSegment = processSegmentBetweenPointAndNearestStop(
                startLat, startLon, wgraph, "Başlangıç"
            );

            // 3) Hedefe en yakın durağı bul
            Stop nearestDestStop = findNearestStop(destLat, destLon, wgraph);
            System.out.println("\nHedef noktasına en yakın durak: " + nearestDestStop.getName()
                + " (lat: " + nearestDestStop.getLat() + ", lon: " + nearestDestStop.getLon() + ")");

            // 4) DijkstraShortestPath ile en düşük ücretli rota
            DijkstraShortestPath<Stop, DefaultWeightedEdge> dsp =
                new DijkstraShortestPath<>(wgraph);
            GraphPath<Stop, DefaultWeightedEdge> path =
                dsp.getPath(startSegment.stop, nearestDestStop);

            // Toplam değerler
            double totalDistance = startSegment.distance;
            double totalCost = startSegment.cost;
            int totalTime = startSegment.time;

            if (path == null) {
                System.out.println("Başlangıç durağı ile hedef durağı arasında bir yol bulunamadı!");
            } else {
                System.out.println("\n--- Duraklar Arası En Uygun Ücretli Rota (Doğru Sıralama) ---");

                // Dijkstra sonucu: durak listesi
                List<Stop> stops = path.getVertexList();
                for (int i = 0; i < stops.size() - 1; i++) {
                    Stop current = stops.get(i);
                    Stop next = stops.get(i + 1);

                    // WeightedGraph üzerinde DefaultWeightedEdge
                    DefaultWeightedEdge dwe = wgraph.getEdge(current, next);
                    if (dwe != null) {
                        // edgeMap ile asıl RouteEdge nesnesini bulalım
                        RouteEdge re = edgeMap.get(dwe);
                        if (re != null) {
                            double mesafe = re.getMesafe();
                            double ucret  = re.getUcret();
                            int sure      = re.getSure();

                            totalDistance += mesafe;
                            totalCost     += ucret;
                            totalTime     += sure;

                            System.out.printf(
                                "%s --> %s | Mesafe: %.2f km, Süre: %d dk, Ücret: %.2f TL\n",
                                current.getName(), next.getName(), mesafe, sure, ucret
                            );
                        }
                    }
                }
            }

            // 5) Hedef segmenti (Hedef Durağı -> Hedef Noktası)
            double endSegmentDistance = distanceBetween(
                nearestDestStop.getLat(), nearestDestStop.getLon(),
                destLat, destLon
            );
            SegmentResult endSegment = processSegmentStopToPoint(
                nearestDestStop, endSegmentDistance, "Hedef"
            );
            totalDistance += endSegment.distance;
            totalCost     += endSegment.cost;
            totalTime     += endSegment.time;

            // 6) Özet Bilgileri Yazdır
            System.out.println("\n--- Rota Özeti ---");
            System.out.println("Toplam gidilen mesafe: " + totalDistance + " km");
            System.out.println("Toplam süre: " + totalTime + " dk");
            System.out.println("Toplam ücret: " + totalCost + " TL");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // -------------------------------------------------------------------------
    // 2) SÜREYE (ZAMAN) GÖRE ROTA HESAPLAMA
    // -------------------------------------------------------------------------
    public void UygunZamanHesapla() {
        try {
            // 1) WeightedGraph'i "time" parametresiyle oluşturuyoruz
            Graph<Stop, DefaultWeightedEdge> wgraph = buildWeightedGraph("time");

            // 2) Başlangıç segmenti
            SegmentResult startSegment = processSegmentBetweenPointAndNearestStop(
                startLat, startLon, wgraph, "Başlangıç"
            );

            // 3) Hedefe en yakın durağı bul
            Stop nearestDestStop = findNearestStop(destLat, destLon, wgraph);
            System.out.println("\nHedef noktasına en yakın durak: " + nearestDestStop.getName()
                + " (lat: " + nearestDestStop.getLat() + ", lon: " + nearestDestStop.getLon() + ")");

            // 4) DijkstraShortestPath ile en düşük süreli rota
            DijkstraShortestPath<Stop, DefaultWeightedEdge> dsp =
                new DijkstraShortestPath<>(wgraph);
            GraphPath<Stop, DefaultWeightedEdge> path =
                dsp.getPath(startSegment.stop, nearestDestStop);

            double totalDistance = startSegment.distance;
            double totalCost = startSegment.cost;
            int totalTime = startSegment.time;

            if (path == null) {
                System.out.println("Başlangıç durağı ile hedef durağı arasında bir yol bulunamadı!");
            } else {
                System.out.println("\n--- Duraklar Arası En Uygun Zamanlı Rota (Doğru Sıralama) ---");

                List<Stop> stops = path.getVertexList();
                for (int i = 0; i < stops.size() - 1; i++) {
                    Stop current = stops.get(i);
                    Stop next = stops.get(i + 1);

                    DefaultWeightedEdge dwe = wgraph.getEdge(current, next);
                    if (dwe != null) {
                        RouteEdge re = edgeMap.get(dwe);
                        if (re != null) {
                            double mesafe = re.getMesafe();
                            double ucret  = re.getUcret();
                            int sure      = re.getSure();

                            totalDistance += mesafe;
                            totalCost     += ucret;
                            totalTime     += sure;

                            System.out.printf(
                                "%s --> %s | Mesafe: %.2f km, Süre: %d dk, Ücret: %.2f TL\n",
                                current.getName(), next.getName(), mesafe, sure, ucret
                            );
                        }
                    }
                }
            }

            // 5) Hedef segmenti
            double endSegmentDistance = distanceBetween(
                nearestDestStop.getLat(), nearestDestStop.getLon(),
                destLat, destLon
            );
            SegmentResult endSegment = processSegmentStopToPoint(
                nearestDestStop, endSegmentDistance, "Hedef"
            );
            totalDistance += endSegment.distance;
            totalCost     += endSegment.cost;
            totalTime     += endSegment.time;

            System.out.println("\n--- Rota Özeti ---");
            System.out.println("Toplam gidilen mesafe: " + totalDistance + " km");
            System.out.println("Toplam süre: " + totalTime + " dk");
            System.out.println("Toplam ücret: " + totalCost + " TL");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // -------------------------------------------------------------------------
    // 3) MESAFEYE GÖRE ROTA HESAPLAMA
    // -------------------------------------------------------------------------
    public void UygunKmHesapla() {
        try {
            // 1) WeightedGraph'i "distance" parametresiyle oluşturuyoruz
            Graph<Stop, DefaultWeightedEdge> wgraph = buildWeightedGraph("distance");

            // 2) Başlangıç segmenti
            SegmentResult startSegment = processSegmentBetweenPointAndNearestStop(
                startLat, startLon, wgraph, "Başlangıç"
            );

            // 3) Hedef durağı
            Stop nearestDestStop = findNearestStop(destLat, destLon, wgraph);
            System.out.println("\nHedef noktasına en yakın durak: " + nearestDestStop.getName()
                + " (lat: " + nearestDestStop.getLat() + ", lon: " + nearestDestStop.getLon() + ")");

            // 4) DijkstraShortestPath ile en kısa mesafeli rota
            DijkstraShortestPath<Stop, DefaultWeightedEdge> dsp =
                new DijkstraShortestPath<>(wgraph);
            GraphPath<Stop, DefaultWeightedEdge> path =
                dsp.getPath(startSegment.stop, nearestDestStop);

            double totalDistance = startSegment.distance;
            double totalCost = startSegment.cost;
            int totalTime = startSegment.time;

            if (path == null) {
                System.out.println("Başlangıç durağı ile hedef durağı arasında bir yol bulunamadı!");
            } else {
                System.out.println("\n--- Duraklar Arası En Uygun Mesafeli Rota (Doğru Sıralama) ---");

                List<Stop> stops = path.getVertexList();
                for (int i = 0; i < stops.size() - 1; i++) {
                    Stop current = stops.get(i);
                    Stop next = stops.get(i + 1);

                    DefaultWeightedEdge dwe = wgraph.getEdge(current, next);
                    if (dwe != null) {
                        RouteEdge re = edgeMap.get(dwe);
                        if (re != null) {
                            double mesafe = re.getMesafe();
                            double ucret  = re.getUcret();
                            int sure      = re.getSure();

                            totalDistance += mesafe;
                            totalCost     += ucret;
                            totalTime     += sure;

                            System.out.printf(
                                "%s --> %s | Mesafe: %.2f km, Süre: %d dk, Ücret: %.2f TL\n",
                                current.getName(), next.getName(), mesafe, sure, ucret
                            );
                        }
                    }
                }
            }

            // 5) Hedef segmenti
            double endSegmentDistance = distanceBetween(
                nearestDestStop.getLat(), nearestDestStop.getLon(),
                destLat, destLon
            );
            SegmentResult endSegment = processSegmentStopToPoint(
                nearestDestStop, endSegmentDistance, "Hedef"
            );
            totalDistance += endSegment.distance;
            totalCost     += endSegment.cost;
            totalTime     += endSegment.time;

            System.out.println("\n--- Rota Özeti ---");
            System.out.println("Toplam gidilen mesafe: " + totalDistance + " km");
            System.out.println("Toplam süre: " + totalTime + " dk");
            System.out.println("Toplam ücret: " + totalCost + " TL");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // -------------------------------------------------------------------------
    // WeightedGraph Oluşturma: "cost" / "time" / "distance" parametresine göre
    // -------------------------------------------------------------------------
    private Graph<Stop, DefaultWeightedEdge> buildWeightedGraph(String weightType) throws Exception {
        // Orijinal graf (Stop, RouteEdge)
        Graph<Stop, RouteEdge> originalGraph = graphBuilderService.buildGraph();

        // WeightedGraph: Stop düğümleri, DefaultWeightedEdge kenarları
        DefaultUndirectedWeightedGraph<Stop, DefaultWeightedEdge> wgraph =
            new DefaultUndirectedWeightedGraph<>(DefaultWeightedEdge.class);

        // edgeMap'i temizliyoruz (her seferinde yeni graf)
        edgeMap.clear();

        // 1) Tüm durakları ekle
        for (Stop s : originalGraph.vertexSet()) {
            wgraph.addVertex(s);
        }

        // 2) Tüm kenarları ekle ve ağırlıklarını ayarla
        for (RouteEdge re : originalGraph.edgeSet()) {
            Stop source = originalGraph.getEdgeSource(re);
            Stop target = originalGraph.getEdgeTarget(re);

            // WeightedGraph'e kenar ekle
            DefaultWeightedEdge dwe = wgraph.addEdge(source, target);
            if (dwe != null) {
                // Kenar ağırlığı:
                double weight;
                switch (weightType) {
                    case "time":
                        weight = re.getSure();      // int -> double
                        break;
                    case "distance":
                        weight = re.getMesafe();   // double
                        break;
                    case "cost":
                    default:
                        weight = re.getUcret();    // double
                        break;
                }
                wgraph.setEdgeWeight(dwe, weight);

                // edgeMap'e, bu DefaultWeightedEdge'in asıl RouteEdge verisini koyuyoruz
                edgeMap.put(dwe, re);
            }
        }

        return wgraph;
    }

    // -------------------------------------------------------------------------
    // Başlangıç Noktası -> En Yakın Durak segmenti (taksi veya yürüyüş)
    // -------------------------------------------------------------------------
    private SegmentResult processSegmentBetweenPointAndNearestStop(
            double lat, double lon,
            Graph<Stop, DefaultWeightedEdge> wgraph,
            String segmentName) {

        // 1) En yakın durağı bul
        Stop nearestStop = findNearestStop(lat, lon, wgraph);
        double distance  = distanceBetween(lat, lon, nearestStop.getLat(), nearestStop.getLon());

        System.out.printf(
            "%s noktasına en yakın durak: %s (Mesafe: %.2f km)\n",
            segmentName, nearestStop.getName(), distance
        );

        double cost = 0.0;
        int time = 0;

        // 2) 3 km üzeriyse taksi, değilse yürüyüş
        if (distance > TAXI_THRESHOLD) {
            Taxi taxi = new Taxi();
            cost = taxi.UcretHesapla(distance);
            double taxiTime = taxi.SureHesapla(distance);
            time = (int) Math.ceil(taxiTime);

            System.out.printf(
                "%s noktasından %s durağına taksi ile gidiliyor. Mesafe: %.2f km, Ücret: %.2f TL, Süre: %d dk\n",
                segmentName, nearestStop.getName(), distance, cost, time
            );

        } else {
            time = (int) Math.ceil(distance * WALK_MIN_PER_KM);
            System.out.printf(
                "%s noktasından %s durağına yürüyerek gidiliyor. Mesafe: %.2f km, Süre: %d dk\n",
                segmentName, nearestStop.getName(), distance, time
            );
        }

        return new SegmentResult(distance, cost, time, nearestStop);
    }

    // -------------------------------------------------------------------------
    // Hedef Durağı -> Hedef Noktası segmenti (taksi veya yürüyüş)
    // -------------------------------------------------------------------------
    private SegmentResult processSegmentStopToPoint(
            Stop stop, double distance, String segmentName) {

        double cost = 0.0;
        int time = 0;

        if (distance > TAXI_THRESHOLD) {
            Taxi taxi = new Taxi();
            cost = taxi.UcretHesapla(distance);
            double taxiTime = taxi.SureHesapla(distance);
            time = (int) Math.ceil(taxiTime);

            System.out.printf(
                "%s durağından %s noktasına taksi ile gidiliyor. Mesafe: %.2f km, Ücret: %.2f TL, Süre: %d dk\n",
                stop.getName(), segmentName, distance, cost, time
            );

        } else {
            time = (int) Math.ceil(distance * WALK_MIN_PER_KM);
            System.out.printf(
                "%s durağından %s noktasına yürüyerek gidiliyor. Mesafe: %.2f km, Süre: %d dk\n",
                stop.getName(), segmentName, distance, time
            );
        }

        return new SegmentResult(distance, cost, time, stop);
    }

    // -------------------------------------------------------------------------
    // WeightedGraph içinde en yakın durağı bulmak
    // -------------------------------------------------------------------------
    private Stop findNearestStop(double lat, double lon, Graph<Stop, DefaultWeightedEdge> wgraph) {
        Stop nearest = null;
        double minDist = Double.MAX_VALUE;

        for (Stop s : wgraph.vertexSet()) {
            double d = distanceBetween(lat, lon, s.getLat(), s.getLon());
            if (d < minDist) {
                minDist = d;
                nearest = s;
            }
        }
        return nearest;
    }

    // -------------------------------------------------------------------------
    // Haversine formülü ile iki koordinat arasındaki mesafeyi (km) hesaplar
    // -------------------------------------------------------------------------
    private double distanceBetween(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0; // Dünya yarıçapı (km)
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                 * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.asin(Math.sqrt(a));

        return R * c;
    }

    // -------------------------------------------------------------------------
    // Segment Sonuçları
    // -------------------------------------------------------------------------
    private static class SegmentResult {
        public double distance; // km
        public double cost;     // TL
        public int time;        // dk
        public Stop stop;       // Varılan durak

        public SegmentResult(double distance, double cost, int time, Stop stop) {
            this.distance = distance;
            this.cost = cost;
            this.time = time;
            this.stop = stop;
        }
    }
}
