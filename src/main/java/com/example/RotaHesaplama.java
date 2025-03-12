package com.example;

import java.util.List;

import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;

public class RotaHesaplama {
    private static final double TAXI_THRESHOLD = 3.0;  // 3 km üzeri taksi
    private static final int WALK_MIN_PER_KM = 3;      // 1 km = 3 dk yürüyüş
    private final double startLat;
    private final double startLon;
    private final double destLat;
    private final double destLon;
    private final GraphBuilderService graphBuilderService;
    private final Yolcu yolcu;                // Polimorfik yolcu nesnesi
    private final OdemeYontemi odemeYontemi;  // Polimorfik ödeme yöntemi nesnesi

    // Constructor
    public RotaHesaplama(double startLat, double startLon, double destLat, double destLon,
                         GraphBuilderService graphBuilderService, Yolcu yolcu, OdemeYontemi odemeYontemi) {
        this.startLat = startLat;
        this.startLon = startLon;
        this.destLat = destLat;
        this.destLon = destLon;
        this.graphBuilderService = graphBuilderService;
        this.yolcu = yolcu;
        this.odemeYontemi = odemeYontemi;
    }

    /**
     * Ana rota hesaplama metodu.
     * 1) Başlangıç noktası -> En yakın durak
     * 2) Hedef noktası -> En yakın durak
     * 3) Dijkstra ile bu iki durak arasındaki en uygun ücretli rota
     * 4) Hedef durağından hedef noktasına son segment
     * 5) Toplam değerleri yazdırma
     */
    public void UygunUcretHesapla() {
        try {
            // 0) Grafı oluştur
            Graph<Stop, RouteEdge> graph = graphBuilderService.buildGraph();

            // Toplam değerleri saklayacak değişkenler
            double totalDistance = 0.0;
            double totalCost = 0.0;
            int totalTime = 0; // dakika

            // 1) Başlangıç segmenti: Başlangıç Noktası -> En Yakın Durak
            SegmentResult startSegment = processSegmentBetweenPointAndNearestStop(
                startLat, startLon, graph, "Başlangıç"
            );
            totalDistance += startSegment.distance;
            totalCost     += startSegment.cost;
            totalTime     += startSegment.time;

            // 2) Hedef segmenti (yalnızca durak tespiti için): Hedef Noktası -> En Yakın Durak
            // Bu aşamada, taksi/yürüyüş hesabını SON segmentte yapacağız (adım 4).
            // Burada sadece "hedefe en yakın durak" bilgisini bulmak istiyoruz.
            Stop nearestDestStop = findNearestStop(destLat, destLon, graph);
            System.out.println("\nHedef noktasına en yakın durak: " + nearestDestStop.getName()
                + " (lat: " + nearestDestStop.getLat() + ", lon: " + nearestDestStop.getLon() + ")");

            // 3) Dijkstra: Başlangıç durağından hedef durağına en uygun ücretli rota
            GraphPath<Stop, RouteEdge> path = DijkstraShortestPath.findPathBetween(
                graph,
                startSegment.stop,     // başlarken bulunduğumuz durak
                nearestDestStop        // hedefe en yakın durak
            );

            if (path == null) {
                System.out.println("Başlangıç durağı ile hedef durağı arasında bir yol bulunamadı!");
            } else {
                System.out.println("\n--- Duraklar Arası En Uygun Ücretli Rota (Doğru Sıralama) ---");

                // Düğümleri gerçek seyahat sırasıyla alıyoruz:
                List<Stop> vertexList = path.getVertexList();

                // Sıradaki durak çiftleri üzerinden kenar bilgilerini toplayalım
                for (int i = 0; i < vertexList.size() - 1; i++) {
                    Stop current = vertexList.get(i);
                    Stop next = vertexList.get(i + 1);

                    // Kenarı alalım. Yönsüz graf kullanıyorsanız ters de olabilir; kontrol edebilirsiniz.
                    RouteEdge edge = graph.getEdge(current, next);
                    if (edge == null) {
                        // Eğer yönsüz graf kullanıyorsanız, getEdge(next, current) olabilir.
                        edge = graph.getEdge(next, current);
                    }

                    if (edge != null) {
                        double mesafe = edge.getMesafe();
                        int sure = edge.getSure();
                        double ucret = edge.getUcret();

                        totalDistance += mesafe;
                        totalTime     += sure;
                        totalCost     += ucret;

                        System.out.println(
                            current.getName() + " --> " + next.getName() +
                            " | Mesafe: " + mesafe + " km, Süre: " + sure +
                            " dk, Ücret: " + ucret + " TL"
                        );
                    }
                }
            }

            // 4) Hedef segmenti: Hedef durağından Hedef Noktası
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

            // 5) Özet Bilgileri Yazdır
            System.out.println("\n--- Rota Özeti ---");
            System.out.println("Toplam gidilen mesafe: " + totalDistance + " km");
            System.out.println("Toplam süre: " + totalTime + " dk");
            System.out.println("Toplam ücret: " + totalCost + " TL");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Başlangıç veya herhangi bir (lat, lon) noktasından en yakın durağa
     * 3 km kontrolü yaparak taksi veya yürüyüş hesabı yapan metod.
     */
    private SegmentResult processSegmentBetweenPointAndNearestStop(
            double lat, double lon, Graph<Stop, RouteEdge> graph, String segmentName) {

        // En yakın durağı bul
        Stop nearestStop = findNearestStop(lat, lon, graph);
        double distance  = distanceBetween(lat, lon, nearestStop.getLat(), nearestStop.getLon());

        System.out.println(segmentName + " noktasına en yakın durak: " + nearestStop.getName()
            + " (Mesafe: " + distance + " km)");

        double cost = 0.0;
        int time = 0;

        if (distance > TAXI_THRESHOLD) {
            // Taksi kullan
            Taxi taxi = new Taxi();
            cost = taxi.UcretHesapla(distance);
            double taxiTime = taxi.SureHesapla(distance);
            time = (int) Math.ceil(taxiTime);

            System.out.println(segmentName + " noktasından " + nearestStop.getName()
                + " durağına taksi ile gidiliyor. Mesafe: " + distance
                + " km, Ücret: " + cost + " TL, Süre: " + time + " dk");

        } else {
            // Yürüyerek gidilir
            time = (int) Math.ceil(distance * WALK_MIN_PER_KM);
            System.out.println(segmentName + " noktasından " + nearestStop.getName()
                + " durağına yürüyerek gidiliyor. Mesafe: " + distance
                + " km, Süre: " + time + " dk");
        }

        return new SegmentResult(distance, cost, time, nearestStop);
    }

    /**
     * Hedef durağından hedef (lat, lon) konumuna yine 3 km kontrolüyle
     * taksi veya yürüyüş hesabı yapan metod.
     */
    private SegmentResult processSegmentStopToPoint(Stop stop, double distance, String segmentName) {
        double cost = 0.0;
        int time = 0;

        if (distance > TAXI_THRESHOLD) {
            Taxi taxi = new Taxi();
            cost = taxi.UcretHesapla(distance);
            double taxiTime = taxi.SureHesapla(distance);
            time = (int) Math.ceil(taxiTime);

            System.out.println(
                stop.getName() + " durağından " + segmentName + " noktasına taksi ile gidiliyor. " +
                "Mesafe: " + distance + " km, Ücret: " + cost + " TL, Süre: " + time + " dk"
            );
        } else {
            time = (int) Math.ceil(distance * WALK_MIN_PER_KM);
            System.out.println(
                stop.getName() + " durağından " + segmentName + " noktasına yürüyerek gidiliyor. " +
                "Mesafe: " + distance + " km, Süre: " + time + " dk"
            );
        }

        return new SegmentResult(distance, cost, time, stop);
    }

    /**
     * Graf üzerindeki tüm duraklar arasından, belirtilen (lat, lon) noktasına en yakın olanı döndürür.
     */
    private Stop findNearestStop(double lat, double lon, Graph<Stop, RouteEdge> graph) {
        Stop nearest = null;
        double minDist = Double.MAX_VALUE;

        for (Stop s : graph.vertexSet()) {
            double d = distanceBetween(lat, lon, s.getLat(), s.getLon());
            if (d < minDist) {
                minDist = d;
                nearest = s;
            }
        }
        return nearest;
    }

    /**
     * Haversine formülü ile iki koordinat arasındaki mesafeyi (km) hesaplar.
     */
    private double distanceBetween(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0; // Dünya yarıçapı (km)
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double lat1Rad = Math.toRadians(lat1);
        double lat2Rad = Math.toRadians(lat2);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.cos(lat1Rad) * Math.cos(lat2Rad)
                 * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.asin(Math.sqrt(a));

        return R * c;
    }

    /**
     * Segment sonuçlarını tutmak için özel iç sınıf.
     * distance: km cinsinden,
     * cost: TL cinsinden,
     * time: dakika cinsinden,
     * stop: varılan durak
     */
    private static class SegmentResult {
        public double distance;
        public double cost;
        public int time;
        public Stop stop;

        public SegmentResult(double distance, double cost, int time, Stop stop) {
            this.distance = distance;
            this.cost = cost;
            this.time = time;
            this.stop = stop;
        }
    }
}
