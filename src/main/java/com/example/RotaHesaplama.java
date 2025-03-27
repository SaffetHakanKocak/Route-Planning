package com.example;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.graph.DefaultUndirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;

/**
 * RotaHesaplama, başlangıç/varış noktalarına göre farklı kriterlerde (ücret, zaman, mesafe)
 * ve farklı ulaşım modlarında (sadece otobüs, sadece tramvay, sadece taksi) rota hesaplaması yapar.
 * 
 * Hem konsola yazan hem de dinamik HTML döndüren metodları içerir.
 * 
 * Not: Bu güncellenmiş versiyonda, en kısa yol hesaplaması için JGraphT’nin hazır DijkstraShortestPath
 * sınıfı yerine, manuel Dijkstra algoritması (DijkstraSolver) kullanılmıştır.
 */
public class RotaHesaplama {

    // 3 km üzeri mesafede taksi, aksi halde yürüme (0 TL).
    private static final double TAXI_THRESHOLD = 3.0;
    private static final int WALK_MIN_PER_KM = 3;  // Yürüme hızı: km başına 3 dk

    private final double startLat;
    private final double startLon;
    private final double destLat;
    private final double destLon;

    private final GraphBuilderService graphBuilderService;
    private final Yolcu yolcu;
    private final OdemeYontemi odemeYontemi;

    // WeightedEdge -> RouteEdge eşleştirmesi (manuel Dijkstra’da kullanılacak)
    private final Map<DefaultWeightedEdge, RouteEdge> edgeMap = new HashMap<>();

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
    // 1) ÜCRETE GÖRE ROTA HESAPLAMA (KONSOLA YAZAR)
    // -------------------------------------------------------------------------
    public void UygunUcretHesapla() {
        try {
            Graph<Stop, DefaultWeightedEdge> wgraph = buildWeightedGraph("cost");
            SegmentResult startSegment = processSegmentBetweenPointAndNearestStop(startLat, startLon, wgraph, "Başlangıç");
            System.out.println("Başlangıç noktasına en yakın durak: " + startSegment.stop.getName());

            Stop nearestDestStop = findNearestStop(destLat, destLon, wgraph);

            System.out.println("\n[Console] En Uygun Ücretli Rota:");
            System.out.println("Hedefe en yakın durak: " + nearestDestStop.getName());

            // Manuel Dijkstra algoritması kullanılarak en kısa yol hesaplanıyor
            List<Stop> path = DijkstraSolver.findShortestPath(wgraph, startSegment.stop, nearestDestStop);

            double totalDistance = startSegment.distance;
            double totalCost = startSegment.cost;
            int totalTime = startSegment.time;

            if (path == null || path.size() == 0) {
                System.out.println("Başlangıç durağı ile hedef durağı arasında bir yol bulunamadı!");
            } else {
                for (int i = 0; i < path.size() - 1; i++) {
                    Stop current = path.get(i);
                    Stop next = path.get(i + 1);
                    DefaultWeightedEdge dwe = wgraph.getEdge(current, next);
                    if (dwe != null) {
                        RouteEdge re = edgeMap.get(dwe);
                        if (re != null) {
                            totalDistance += re.getMesafe();
                            totalCost     += re.getUcret();
                            totalTime     += re.getSure();

                            String transferInfo = "";
                            if (!current.getType().equalsIgnoreCase(next.getType())) {
                                transferInfo = " (🔁 Transfer)";
                            }
                            System.out.printf("%s [%s] -> %s [%s]%s | ⏳ Süre=%d dk, 💰 Ücret=%.2f TL, 📏 Mesafe=%.2f km",
                                    current.getName(), getModeEmoji(current.getType()),
                                    next.getName(), getModeEmoji(next.getType()),
                                    transferInfo,
                                    re.getSure(), re.getUcret(), re.getMesafe());
                            System.out.println();
                        }
                    }
                }
            }
            // Son durak -> hedef segmenti
            double endSegmentDistance = distanceBetween(nearestDestStop.getLat(), nearestDestStop.getLon(), destLat, destLon);
            SegmentResult endSegment = processSegmentStopToPoint(nearestDestStop, endSegmentDistance, "Hedef");
            totalDistance += endSegment.distance;
            totalCost     += endSegment.cost;
            totalTime     += endSegment.time;
            System.out.printf("Son Durak -> Hedef | ⏳ Süre=%d dk, 💰 Ücret=%.2f TL, 📏 Mesafe=%.2f km%n",
                    endSegment.time, endSegment.cost, endSegment.distance);

            System.out.println("--- Rota Özeti (Ücret) ---");
            System.out.println("Toplam Mesafe: " + totalDistance + " km");
            System.out.println("Toplam Süre: " + totalTime + " dk");
            System.out.println("Toplam Ücret: " + totalCost + " TL");

            applyAdjustments(totalCost);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // -------------------------------------------------------------------------
    // 2) ZAMANA GÖRE ROTA HESAPLAMA (KONSOLA YAZAR)
    // -------------------------------------------------------------------------
    public void UygunZamanHesapla() {
        try {
            Graph<Stop, DefaultWeightedEdge> wgraph = buildWeightedGraph("time");
            SegmentResult startSegment = processSegmentBetweenPointAndNearestStop(startLat, startLon, wgraph, "Başlangıç");
            System.out.println("Başlangıç noktasına en yakın durak: " + startSegment.stop.getName());

            Stop nearestDestStop = findNearestStop(destLat, destLon, wgraph);

            System.out.println("\n[Console] En Uygun Zamanlı Rota:");
            System.out.println("Hedefe en yakın durak: " + nearestDestStop.getName());

            List<Stop> path = DijkstraSolver.findShortestPath(wgraph, startSegment.stop, nearestDestStop);

            double totalDistance = startSegment.distance;
            double totalCost = startSegment.cost;
            int totalTime = startSegment.time;

            if (path == null || path.size() == 0) {
                System.out.println("Başlangıç durağı ile hedef durağı arasında bir yol bulunamadı!");
            } else {
                for (int i = 0; i < path.size() - 1; i++) {
                    Stop current = path.get(i);
                    Stop next = path.get(i + 1);
                    DefaultWeightedEdge dwe = wgraph.getEdge(current, next);
                    if (dwe != null) {
                        RouteEdge re = edgeMap.get(dwe);
                        if (re != null) {
                            totalDistance += re.getMesafe();
                            totalCost     += re.getUcret();
                            totalTime     += re.getSure();

                            String transferInfo = "";
                            if (!current.getType().equalsIgnoreCase(next.getType())) {
                                transferInfo = " (🔁 Transfer)";
                            }
                            System.out.printf("%s [%s] -> %s [%s]%s | ⏳ Süre=%d dk, 💰 Ücret=%.2f TL, 📏 Mesafe=%.2f km%n",
                                    current.getName(), getModeEmoji(current.getType()),
                                    next.getName(), getModeEmoji(next.getType()),
                                    transferInfo,
                                    re.getSure(), re.getUcret(), re.getMesafe());
                        }
                    }
                }
            }
            double endSegmentDistance = distanceBetween(nearestDestStop.getLat(), nearestDestStop.getLon(), destLat, destLon);
            SegmentResult endSegment = processSegmentStopToPoint(nearestDestStop, endSegmentDistance, "Hedef");
            totalDistance += endSegment.distance;
            totalCost     += endSegment.cost;
            totalTime     += endSegment.time;
            System.out.printf("Son Durak -> Hedef | ⏳ Süre=%d dk, 💰 Ücret=%.2f TL, 📏 Mesafe=%.2f km%n",
                    endSegment.time, endSegment.cost, endSegment.distance);

            System.out.println("--- Rota Özeti (Zaman) ---");
            System.out.println("Toplam Mesafe: " + totalDistance + " km");
            System.out.println("Toplam Süre: " + totalTime + " dk");
            System.out.println("Toplam Ücret: " + totalCost + " TL");

            applyAdjustments(totalCost);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // -------------------------------------------------------------------------
    // 3) MESAFEYE GÖRE ROTA HESAPLAMA (KONSOLA YAZAR)
    // -------------------------------------------------------------------------
    public void UygunKmHesapla() {
        try {
            Graph<Stop, DefaultWeightedEdge> wgraph = buildWeightedGraph("distance");
            SegmentResult startSegment = processSegmentBetweenPointAndNearestStop(startLat, startLon, wgraph, "Başlangıç");
            System.out.println("Başlangıç noktasına en yakın durak: " + startSegment.stop.getName());

            Stop nearestDestStop = findNearestStop(destLat, destLon, wgraph);

            System.out.println("\n[Console] En Uygun Mesafeli Rota:");
            System.out.println("Hedefe en yakın durak: " + nearestDestStop.getName());

            List<Stop> path = DijkstraSolver.findShortestPath(wgraph, startSegment.stop, nearestDestStop);

            double totalDistance = startSegment.distance;
            double totalCost = startSegment.cost;
            int totalTime = startSegment.time;

            if (path == null || path.size() == 0) {
                System.out.println("Başlangıç durağı ile hedef durağı arasında bir yol bulunamadı!");
            } else {
                for (int i = 0; i < path.size() - 1; i++) {
                    Stop current = path.get(i);
                    Stop next = path.get(i + 1);
                    DefaultWeightedEdge dwe = wgraph.getEdge(current, next);
                    if (dwe != null) {
                        RouteEdge re = edgeMap.get(dwe);
                        if (re != null) {
                            totalDistance += re.getMesafe();
                            totalCost     += re.getUcret();
                            totalTime     += re.getSure();

                            String transferInfo = "";
                            if (!current.getType().equalsIgnoreCase(next.getType())) {
                                transferInfo = " (🔁 Transfer)";
                            }
                            System.out.printf("%s [%s] -> %s [%s]%s | ⏳ Süre=%d dk, 💰 Ücret=%.2f TL, 📏 Mesafe=%.2f km%n",
                                    current.getName(), getModeEmoji(current.getType()),
                                    next.getName(), getModeEmoji(next.getType()),
                                    transferInfo,
                                    re.getSure(), re.getUcret(), re.getMesafe());
                        }
                    }
                }
            }
            double endSegmentDistance = distanceBetween(nearestDestStop.getLat(), nearestDestStop.getLon(), destLat, destLon);
            SegmentResult endSegment = processSegmentStopToPoint(nearestDestStop, endSegmentDistance, "Hedef");
            totalDistance += endSegment.distance;
            totalCost     += endSegment.cost;
            totalTime     += endSegment.time;
            System.out.printf("Son Durak -> Hedef | ⏳ Süre=%d dk, 💰 Ücret=%.2f TL, 📏 Mesafe=%.2f km%n",
                    endSegment.time, endSegment.cost, endSegment.distance);

            System.out.println("--- Rota Özeti (Mesafe) ---");
            System.out.println("Toplam Mesafe: " + totalDistance + " km");
            System.out.println("Toplam Süre: " + totalTime + " dk");
            System.out.println("Toplam Ücret: " + totalCost + " TL");

            applyAdjustments(totalCost);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // -------------------------------------------------------------------------
    // SADECE TAKSİ (KONSOLA YAZAR)
    // -------------------------------------------------------------------------
    public void SadeceTaxiRota() {
        System.out.println("\n[Console] Sadece Taksi Rota:");
        double distance = distanceBetween(startLat, startLon, destLat, destLon);
        Taxi taxi = new Taxi();
        double cost = taxi.UcretHesapla(distance);
        double taxiTime = taxi.SureHesapla(distance);
        int time = (int) Math.ceil(taxiTime);

        System.out.println("Başlangıç noktasına en yakın durak: Taksi (doğrudan hesaplama)");
        System.out.printf("Başlangıç -> Hedef | ⏳ Süre=%d dk, 💰 Ücret=%.2f TL, 📏 Mesafe=%.2f km%n", time, cost, distance);

        applyAdjustments(cost);
    }

    // -------------------------------------------------------------------------
    // SADECE OTOBÜS ROTASI (KONSOLA YAZAR)
    // -------------------------------------------------------------------------
    public void SadeceOtobusRota() {
        try {
            System.out.println("\n[Console] Sadece Otobüs Rota:");
            Graph<Stop, RouteEdge> originalGraph = graphBuilderService.buildGraph();

            DefaultUndirectedWeightedGraph<Stop, DefaultWeightedEdge> busGraph =
                    new DefaultUndirectedWeightedGraph<>(DefaultWeightedEdge.class);
            Map<DefaultWeightedEdge, RouteEdge> busEdgeMap = new HashMap<>();

            for (Stop s : originalGraph.vertexSet()) {
                if (s.getType().equalsIgnoreCase("bus")) {
                    busGraph.addVertex(s);
                }
            }
            for (RouteEdge re : originalGraph.edgeSet()) {
                Stop source = originalGraph.getEdgeSource(re);
                Stop target = originalGraph.getEdgeTarget(re);
                if (source.getType().equalsIgnoreCase("bus") && target.getType().equalsIgnoreCase("bus")) {
                    DefaultWeightedEdge dwe = busGraph.addEdge(source, target);
                    if (dwe != null) {
                        busGraph.setEdgeWeight(dwe, re.getUcret());
                        busEdgeMap.put(dwe, re);
                    }
                }
            }

            SegmentResult startSegment = processSegmentBetweenPointAndNearestStop(startLat, startLon, busGraph, "Başlangıç");
            System.out.println("Başlangıç noktasına en yakın otobüs durağı: " + startSegment.stop.getName());
            Stop startBus = startSegment.stop;

            Stop destBus = findNearestStopByType(destLat, destLon, busGraph, "bus");

            double totalDistance = startSegment.distance;
            double totalCost = startSegment.cost;
            int totalTime = startSegment.time;

            List<Stop> path = DijkstraSolver.findShortestPath(busGraph, startBus, destBus);
            if (path == null || path.size() == 0) {
                System.out.println("Otobüs durakları arasında rota bulunamadı!");
            } else {
                for (int i = 0; i < path.size() - 1; i++) {
                    Stop current = path.get(i);
                    Stop next = path.get(i + 1);
                    DefaultWeightedEdge dwe = busGraph.getEdge(current, next);
                    if (dwe != null) {
                        RouteEdge re = busEdgeMap.get(dwe);
                        if (re != null) {
                            totalDistance += re.getMesafe();
                            totalCost     += re.getUcret();
                            totalTime     += re.getSure();

                            String transferInfo = "";
                            if (!current.getType().equalsIgnoreCase(next.getType())) {
                                transferInfo = " (🔁 Transfer)";
                            }
                            System.out.printf("%s [%s] -> %s [%s]%s | ⏳ Süre=%d dk, 💰 Ücret=%.2f TL, 📏 Mesafe=%.2f km%n",
                                    current.getName(), getModeEmoji(current.getType()),
                                    next.getName(), getModeEmoji(next.getType()),
                                    transferInfo,
                                    re.getSure(), re.getUcret(), re.getMesafe());
                        }
                    }
                }
            }
            double endSegmentDistance = distanceBetween(destBus.getLat(), destBus.getLon(), destLat, destLon);
            SegmentResult endSegment = processSegmentStopToPoint(destBus, endSegmentDistance, "Hedef");
            totalDistance += endSegment.distance;
            totalCost     += endSegment.cost;
            totalTime     += endSegment.time;
            System.out.printf("Son Durak -> Hedef | ⏳ Süre=%d dk, 💰 Ücret=%.2f TL, 📏 Mesafe=%.2f km%n",
                    endSegment.time, endSegment.cost, endSegment.distance);

            System.out.println("--- Sadece Otobüs Özeti ---");
            System.out.printf("Mesafe=%.2f km, Süre=%d dk, Ücret=%.2f TL%n", totalDistance, totalTime, totalCost);
            applyAdjustments(totalCost);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // -------------------------------------------------------------------------
    // SADECE TRAMVAY ROTASI (KONSOLA YAZAR)
    // -------------------------------------------------------------------------
    public void SadeceTramvayRota() {
        try {
            System.out.println("\n[Console] Sadece Tramvay Rota:");
            Graph<Stop, RouteEdge> originalGraph = graphBuilderService.buildGraph();

            DefaultUndirectedWeightedGraph<Stop, DefaultWeightedEdge> tramGraph =
                    new DefaultUndirectedWeightedGraph<>(DefaultWeightedEdge.class);
            Map<DefaultWeightedEdge, RouteEdge> tramEdgeMap = new HashMap<>();

            for (Stop s : originalGraph.vertexSet()) {
                if (s.getType().equalsIgnoreCase("tram")) {
                    tramGraph.addVertex(s);
                }
            }
            for (RouteEdge re : originalGraph.edgeSet()) {
                Stop source = originalGraph.getEdgeSource(re);
                Stop target = originalGraph.getEdgeTarget(re);
                if (source.getType().equalsIgnoreCase("tram") && target.getType().equalsIgnoreCase("tram")) {
                    DefaultWeightedEdge dwe = tramGraph.addEdge(source, target);
                    if (dwe != null) {
                        tramGraph.setEdgeWeight(dwe, re.getUcret());
                        tramEdgeMap.put(dwe, re);
                    }
                }
            }

            SegmentResult startSegment = processSegmentBetweenPointAndNearestStop(startLat, startLon, tramGraph, "Başlangıç");
            System.out.println("Başlangıç noktasına en yakın tramvay durağı: " + startSegment.stop.getName());
            Stop startTram = startSegment.stop;

            Stop destTram = findNearestStopByType(destLat, destLon, tramGraph, "tram");

            double totalDistance = startSegment.distance;
            double totalCost = startSegment.cost;
            int totalTime = startSegment.time;

            List<Stop> path = DijkstraSolver.findShortestPath(tramGraph, startTram, destTram);
            if (path == null || path.size() == 0) {
                System.out.println("Tramvay durakları arasında rota bulunamadı!");
            } else {
                for (int i = 0; i < path.size() - 1; i++) {
                    Stop current = path.get(i);
                    Stop next = path.get(i + 1);
                    DefaultWeightedEdge dwe = tramGraph.getEdge(current, next);
                    if (dwe != null) {
                        RouteEdge re = tramEdgeMap.get(dwe);
                        if (re != null) {
                            totalDistance += re.getMesafe();
                            totalCost     += re.getUcret();
                            totalTime     += re.getSure();

                            String transferInfo = "";
                            if (!current.getType().equalsIgnoreCase(next.getType())) {
                                transferInfo = " (🔁 Transfer)";
                            }
                            System.out.printf("%s [%s] -> %s [%s]%s | ⏳ Süre=%d dk, 💰 Ücret=%.2f TL, 📏 Mesafe=%.2f km%n",
                                    current.getName(), getModeEmoji(current.getType()),
                                    next.getName(), getModeEmoji(next.getType()),
                                    transferInfo,
                                    re.getSure(), re.getUcret(), re.getMesafe());
                        }
                    }
                }
            }
            double endSegmentDistance = distanceBetween(destTram.getLat(), destTram.getLon(), destLat, destLon);
            SegmentResult endSegment = processSegmentStopToPoint(destTram, endSegmentDistance, "Hedef");
            totalDistance += endSegment.distance;
            totalCost     += endSegment.cost;
            totalTime     += endSegment.time;
            System.out.printf("Son Durak -> Hedef | ⏳ Süre=%d dk, 💰 Ücret=%.2f TL, 📏 Mesafe=%.2f km%n",
                    endSegment.time, endSegment.cost, endSegment.distance);

            System.out.println("--- Sadece Tramvay Özeti ---");
            System.out.printf("Mesafe=%.2f km, Süre=%d dk, Ücret=%.2f TL%n", totalDistance, totalTime, totalCost);
            applyAdjustments(totalCost);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // =========================================================================
    // ================== DİNAMİK HTML DÖNDÜREN METODLAR =======================
    // =========================================================================

    public String getUygunUcretHtml()      { return buildDynamicRouteHtml("cost", "💰 En Uygun Ücretli Rota", "blue-bgc"); }
    public String getUygunZamanHtml()      { return buildDynamicRouteHtml("time", "⏱️ En Uygun Zamanlı Rota", "orange-bgc"); }
    public String getUygunMesafeHtml()     { return buildDynamicRouteHtml("distance", "📏 En Uygun Mesafeli Rota", "purple-bgc"); }
    public String getSadeceOtobusHtml()    { return buildBusRouteHtml(); }
    public String getSadeceTramvayHtml()   { return buildTramRouteHtml(); }
    public String getSadeceTaxiHtml()      { return buildTaxiRouteHtml(); }

    private String buildDynamicRouteHtml(String weightType, String title, String styleClass) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div class='" + styleClass + "' style='padding:15px; border-radius:8px; border:1px solid #ddd;'>");
        sb.append("<h2 style='margin-top:0; color:#333;'>" + title + "</h2>");

        try {
            Graph<Stop, DefaultWeightedEdge> wgraph = buildWeightedGraph(weightType);

            // Başlangıç segmenti
            SegmentResult startSegment = processSegmentBetweenPointAndNearestStop(startLat, startLon, wgraph, "Başlangıç");
            double distanceToNearest = distanceBetween(startLat, startLon, startSegment.stop.getLat(), startSegment.stop.getLon());
            sb.append("<p style='font-weight:bold; color:#555;'>");
            sb.append("📍 Başlangıç Noktasına En Yakın Durak: " + startSegment.stop.getName());
            sb.append(String.format(" (%.2f km) ", distanceToNearest));
            if (startSegment.cost > 0) {
                sb.append(" → 🚕 Taksi => " + String.format("%.2f TL", startSegment.cost));
            } else {
                sb.append(" → 🚶 Yürüme => 0 TL");
            }
            sb.append("</p>");

            Stop nearestDestStop = findNearestStop(destLat, destLon, wgraph);

            List<Stop> path = DijkstraSolver.findShortestPath(wgraph, startSegment.stop, nearestDestStop);

            double totalDistance = startSegment.distance;
            double totalCost = startSegment.cost;
            int totalTime = startSegment.time;

            sb.append("<div style='margin-top:10px;'><b>🛣️ Rota Detayları:</b></div>");
            sb.append("<ol style='padding-left:18px; margin:0;'>");
            if (path == null || path.size() == 0) {
                sb.append("<li style='color:red;'>Başlangıç durağı ile hedef durağı arasında bir yol bulunamadı!</li>");
            } else {
                for (int i = 0; i < path.size() - 1; i++) {
                    Stop current = path.get(i);
                    Stop next = path.get(i + 1);
                    DefaultWeightedEdge dwe = wgraph.getEdge(current, next);
                    if (dwe != null) {
                        RouteEdge re = edgeMap.get(dwe);
                        if (re != null) {
                            totalDistance += re.getMesafe();
                            totalCost     += re.getUcret();
                            totalTime     += re.getSure();

                            String transferInfo = "";
                            if (!current.getType().equalsIgnoreCase(next.getType())) {
                                transferInfo = " (🔁 Transfer)";
                            }
                            sb.append("<li style='margin-bottom:6px;'>");
                            sb.append("<b>" + current.getName() + " [" + getModeEmoji(current.getType()) + "]</b> -> <b>" + next.getName() + " [" + getModeEmoji(next.getType()) + "]</b>" + transferInfo);
                            sb.append(" <span style='color:#666;'>⏳ Süre=" + re.getSure() + " dk, 💰 Ücret=" + String.format("%.2f", re.getUcret()) + " TL, 📏 Mesafe=" + String.format("%.2f", re.getMesafe()) + " km</span>");
                            sb.append("</li>");
                        }
                    }
                }
            }
            sb.append("</ol>");

            double endSegmentDistance = distanceBetween(nearestDestStop.getLat(), nearestDestStop.getLon(), destLat, destLon);
            SegmentResult endSegment = processSegmentStopToPoint(nearestDestStop, endSegmentDistance, "Hedef");
            totalDistance += endSegment.distance;
            totalCost     += endSegment.cost;
            totalTime     += endSegment.time;

            sb.append("<div style='margin-top:10px;'><b>Hedef Segment Detayları:</b></div>");
            sb.append(String.format("<p>Son Durak -> Hedef | ⏳ Süre=%d dk, 💰 Ücret=%.2f TL, 📏 Mesafe=%.2f km</p>",
                    endSegment.time, endSegment.cost, endSegment.distance));

            sb.append("<hr style='margin:10px 0;'>");
            sb.append("<div><b>🔎 Toplam:</b></div>");
            sb.append("<ul style='list-style:none; padding-left:0; margin:0;'>");
            sb.append(String.format("<li>💸 Ücret: <b>%.2f TL</b></li>", totalCost));
            sb.append(String.format("<li>⏱️ Süre: <b>%d dk</b></li>", totalTime));
            sb.append(String.format("<li>📏 Mesafe: <b>%.2f km</b></li>", totalDistance));
            sb.append("</ul>");

            double finalCost = applyAdjustmentsAndReturn(totalCost, sb);
            sb.append(String.format("<p style='color:#007bff;'><b>Güncel Ücret: %.2f TL</b></p>", finalCost));

        } catch (Exception e) {
            e.printStackTrace();
            sb.append("<p style='color:red;'>Hata: " + e.getMessage() + "</p>");
        }

        sb.append("</div>");
        return sb.toString();
    }

    private String buildTaxiRouteHtml() {
        StringBuilder sb = new StringBuilder();
        sb.append("<div style='background:#fff9c4;padding:15px;border:1px solid #ccc;border-radius:5px;'>");
        sb.append("<h2 style='margin-top:0;'>🚖 Sadece Taksi Rota (Dinamik)</h2>");
    
        try {
            double distance = distanceBetween(startLat, startLon, destLat, destLon);
            Taxi taxi = new Taxi();
            double cost = taxi.UcretHesapla(distance);
            double taxiTime = taxi.SureHesapla(distance);
            int time = (int) Math.ceil(taxiTime);
    
            sb.append("<p style='font-weight:bold; color:#555;'>");
            sb.append("📍 Başlangıç -> Hedef (Doğrudan Taksi)");
            sb.append("</p>");
    
            sb.append(String.format("<p>⏳ Süre=%d dk, 💰 Ücret=%.2f TL, 📏 Mesafe=%.2f km</p>", time, cost, distance));
    
            double finalCost = applyAdjustmentsAndReturn(cost, sb);
            sb.append(String.format("<p style='color:#007bff;'><b>Güncel Ücret: %.2f TL</b></p>", finalCost));
    
        } catch (Exception e) {
            e.printStackTrace();
            sb.append("<p style='color:red;'>Hata: " + e.getMessage() + "</p>");
        }
    
        sb.append("</div>");
        return sb.toString();
    }
    
    private String buildBusRouteHtml() {
        StringBuilder sb = new StringBuilder();
        sb.append("<div style='background:#e8f5e9;padding:15px;border:1px solid #ccc;border-radius:5px;'>");
        sb.append("<h2 style='margin-top:0;'>🚌 Sadece Otobüs Rota (Dinamik)</h2>");

        try {
            Graph<Stop, RouteEdge> originalGraph = graphBuilderService.buildGraph();
            DefaultUndirectedWeightedGraph<Stop, DefaultWeightedEdge> busGraph =
                new DefaultUndirectedWeightedGraph<>(DefaultWeightedEdge.class);
            Map<DefaultWeightedEdge, RouteEdge> busEdgeMap = new HashMap<>();

            for (Stop s : originalGraph.vertexSet()) {
                if (s.getType().equalsIgnoreCase("bus")) {
                    busGraph.addVertex(s);
                }
            }
            for (RouteEdge re : originalGraph.edgeSet()) {
                Stop source = originalGraph.getEdgeSource(re);
                Stop target = originalGraph.getEdgeTarget(re);
                if (source.getType().equalsIgnoreCase("bus") && target.getType().equalsIgnoreCase("bus")) {
                    DefaultWeightedEdge dwe = busGraph.addEdge(source, target);
                    if (dwe != null) {
                        busGraph.setEdgeWeight(dwe, re.getUcret());
                        busEdgeMap.put(dwe, re);
                    }
                }
            }

            SegmentResult startSegment = processSegmentBetweenPointAndNearestStop(startLat, startLon, busGraph, "Başlangıç");
            sb.append("<p style='font-weight:bold; color:#555;'>");
            sb.append("📍 Başlangıç Noktasına En Yakın Otobüs Durağı: " + startSegment.stop.getName());
            sb.append(String.format(" (%.2f km) ", startSegment.distance));
            if (startSegment.cost > 0) {
                sb.append(" → 🚕 Taksi => " + String.format("%.2f TL", startSegment.cost));
            } else {
                sb.append(" → 🚶 Yürüme => 0 TL");
            }
            sb.append("</p>");

            Stop startBus = startSegment.stop;
            Stop destBus = findNearestStopByType(destLat, destLon, busGraph, "bus");

            double totalDistance = startSegment.distance;
            double totalCost = startSegment.cost;
            int totalTime = startSegment.time;

            List<Stop> path = DijkstraSolver.findShortestPath(busGraph, startBus, destBus);
            if (path == null || path.size() == 0) {
                sb.append("<p style='color:red;'>Otobüs durakları arasında rota bulunamadı!</p>");
            } else {
                sb.append("<ol style='padding-left:18px; margin:0;'>");
                for (int i = 0; i < path.size() - 1; i++) {
                    Stop current = path.get(i);
                    Stop next = path.get(i + 1);
                    DefaultWeightedEdge dwe = busGraph.getEdge(current, next);
                    if (dwe != null) {
                        RouteEdge re = busEdgeMap.get(dwe);
                        if (re != null) {
                            totalDistance += re.getMesafe();
                            totalCost     += re.getUcret();
                            totalTime     += re.getSure();

                            String transferInfo = "";
                            if (!current.getType().equalsIgnoreCase(next.getType())) {
                                transferInfo = " (🔁 Transfer)";
                            }
                            sb.append("<li style='margin-bottom:6px;'>");
                            sb.append("<b>" + current.getName() + " [" + getModeEmoji(current.getType()) + "]</b> -> <b>" + next.getName() + " [" + getModeEmoji(next.getType()) + "]</b>" + transferInfo);
                            sb.append(" <span style='color:#666;'>⏳ Süre=" + re.getSure() + " dk, 💰 Ücret=" + String.format("%.2f", re.getUcret()) + " TL, 📏 Mesafe=" + String.format("%.2f", re.getMesafe()) + " km</span>");
                            sb.append("</li>");
                        }
                    }
                }
                sb.append("</ol>");
            }

            double endSegmentDistance = distanceBetween(destBus.getLat(), destBus.getLon(), destLat, destLon);
            SegmentResult endSegment = processSegmentStopToPoint(destBus, endSegmentDistance, "Hedef");
            totalDistance += endSegment.distance;
            totalCost     += endSegment.cost;
            totalTime     += endSegment.time;

            sb.append("<div style='margin-top:10px;'><b>Hedef Segment Detayları:</b></div>");
            sb.append(String.format("<p>Son Durak -> Hedef | ⏳ Süre=%d dk, 💰 Ücret=%.2f TL, 📏 Mesafe=%.2f km</p>",
                    endSegment.time, endSegment.cost, endSegment.distance));

            sb.append("<hr>");
            sb.append("<div><b>Özet Bilgiler (Otobüs):</b></div>");
            sb.append(String.format("<p>Mesafe=%.2f km<br>Süre=%d dk<br>Ücret=%.2f TL</p>",
                    totalDistance, totalTime, totalCost));

            double finalCost = applyAdjustmentsAndReturn(totalCost, sb);
            sb.append(String.format("<p style='color:#007bff;'><b>Güncel Ücret: %.2f TL</b></p>", finalCost));

        } catch (Exception e) {
            e.printStackTrace();
            sb.append("<p style='color:red;'>Hata: " + e.getMessage() + "</p>");
        }

        sb.append("</div>");
        return sb.toString();
    }

    private String buildTramRouteHtml() {
        StringBuilder sb = new StringBuilder();
        sb.append("<div style='background:#fce4ec;padding:15px;border:1px solid #ccc;border-radius:5px;'>");
        sb.append("<h2 style='margin-top:0;'>🚋 Sadece Tramvay Rota (Dinamik)</h2>");

        try {
            Graph<Stop, RouteEdge> originalGraph = graphBuilderService.buildGraph();
            DefaultUndirectedWeightedGraph<Stop, DefaultWeightedEdge> tramGraph =
                    new DefaultUndirectedWeightedGraph<>(DefaultWeightedEdge.class);
            Map<DefaultWeightedEdge, RouteEdge> tramEdgeMap = new HashMap<>();

            for (Stop s : originalGraph.vertexSet()) {
                if (s.getType().equalsIgnoreCase("tram")) {
                    tramGraph.addVertex(s);
                }
            }
            for (RouteEdge re : originalGraph.edgeSet()) {
                Stop source = originalGraph.getEdgeSource(re);
                Stop target = originalGraph.getEdgeTarget(re);
                if (source.getType().equalsIgnoreCase("tram") && target.getType().equalsIgnoreCase("tram")) {
                    DefaultWeightedEdge dwe = tramGraph.addEdge(source, target);
                    if (dwe != null) {
                        tramGraph.setEdgeWeight(dwe, re.getUcret());
                        tramEdgeMap.put(dwe, re);
                    }
                }
            }

            SegmentResult startSegment = processSegmentBetweenPointAndNearestStop(startLat, startLon, tramGraph, "Başlangıç");
            sb.append("<p style='font-weight:bold; color:#555;'>");
            sb.append("📍 Başlangıç Noktasına En Yakın Tramvay Durağı: " + startSegment.stop.getName());
            sb.append(String.format(" (%.2f km) ", startSegment.distance));
            if (startSegment.cost > 0) {
                sb.append(" → 🚕 Taksi => " + String.format("%.2f TL", startSegment.cost));
            } else {
                sb.append(" → 🚶 Yürüme => 0 TL");
            }
            sb.append("</p>");

            Stop startTram = startSegment.stop;
            Stop destTram = findNearestStopByType(destLat, destLon, tramGraph, "tram");

            double totalDistance = startSegment.distance;
            double totalCost = startSegment.cost;
            int totalTime = startSegment.time;

            List<Stop> path = DijkstraSolver.findShortestPath(tramGraph, startTram, destTram);
            sb.append("<div><b>🛣️ Tramvay Rota Detayları:</b></div>");
            if (path == null || path.size() == 0) {
                sb.append("<p style='color:red;'>Tramvay durakları arasında rota bulunamadı!</p>");
            } else {
                sb.append("<ol style='padding-left:18px; margin:0;'>");
                for (int i = 0; i < path.size() - 1; i++) {
                    Stop current = path.get(i);
                    Stop next = path.get(i + 1);
                    DefaultWeightedEdge dwe = tramGraph.getEdge(current, next);
                    if (dwe != null) {
                        RouteEdge re = tramEdgeMap.get(dwe);
                        if (re != null) {
                            totalDistance += re.getMesafe();
                            totalCost     += re.getUcret();
                            totalTime     += re.getSure();

                            String transferInfo = "";
                            if (!current.getType().equalsIgnoreCase(next.getType())) {
                                transferInfo = " (🔁 Transfer)";
                            }
                            sb.append("<li style='margin-bottom:6px;'>");
                            sb.append("<b>" + current.getName() + " [" + getModeEmoji(current.getType()) + "]</b> -> <b>" + next.getName() + " [" + getModeEmoji(next.getType()) + "]</b>" + transferInfo);
                            sb.append(" <span style='color:#666;'>⏳ Süre=" + re.getSure() + " dk, 💰 Ücret=" + String.format("%.2f", re.getUcret()) + " TL, 📏 Mesafe=" + String.format("%.2f", re.getMesafe()) + " km</span>");
                            sb.append("</li>");
                        }
                    }
                }
                sb.append("</ol>");
            }

            double endSegmentDistance = distanceBetween(destTram.getLat(), destTram.getLon(), destLat, destLon);
            SegmentResult endSegment = processSegmentStopToPoint(destTram, endSegmentDistance, "Hedef");
            totalDistance += endSegment.distance;
            totalCost     += endSegment.cost;
            totalTime     += endSegment.time;

            sb.append("<div style='margin-top:10px;'><b>Hedef Segment Detayları:</b></div>");
            sb.append(String.format("<p>Son Durak -> Hedef | ⏳ Süre=%d dk, 💰 Ücret=%.2f TL, 📏 Mesafe=%.2f km</p>",
                    endSegment.time, endSegment.cost, endSegment.distance));

            sb.append("<hr>");
            sb.append("<div><b>Özet Bilgiler (Tramvay):</b></div>");
            sb.append(String.format("<p>Mesafe=%.2f km<br>Süre=%d dk<br>Ücret=%.2f TL</p>",
                    totalDistance, totalTime, totalCost));

            double finalCost = applyAdjustmentsAndReturn(totalCost, sb);
            sb.append(String.format("<p style='color:#007bff;'><b>Güncel Ücret: %.2f TL</b></p>", finalCost));

        } catch (Exception e) {
            e.printStackTrace();
            sb.append("<p style='color:red;'>Hata: " + e.getMessage() + "</p>");
        }

        sb.append("</div>");
        return sb.toString();
    }

    // -------------------------------------------------------------------------
    // İndirim/Zam uygularken final ücreti döndüren yardımcı metot
    // -------------------------------------------------------------------------
    private double applyAdjustmentsAndReturn(double baseCost, StringBuilder sb) {
        double adjustedCost = baseCost;
        if (yolcu instanceof Indirim) {
            double discount = ((Indirim) yolcu).IndirimUygula(baseCost);
            adjustedCost -= discount;
            sb.append(String.format("<p style='color:green;'>🚶 Yolcu %s indirimi: -%.2f TL</p>",
                    yolcu.YolcuTipiGoster(), discount));
        }
        if (odemeYontemi instanceof KentKart) {
            double discount = ((Indirim) odemeYontemi).IndirimUygula(adjustedCost);
            adjustedCost -= discount;
            sb.append(String.format("<p style='color:green;'>💳 KentKart indirimi: -%.2f TL</p>", discount));
        } else if (odemeYontemi instanceof KrediKarti) {
            double zam = ((KrediKarti) odemeYontemi).ZamUygula(adjustedCost);
            adjustedCost += zam;
            sb.append(String.format("<p style='color:red;'>💳 KrediKartı zammı: +%.2f TL</p>", zam));
        }
        return adjustedCost;
    }

    // -------------------------------------------------------------------------
    // buildWeightedGraph, segment işleyiciler ve diğer yardımcı metotlar
    // -------------------------------------------------------------------------
    private Graph<Stop, DefaultWeightedEdge> buildWeightedGraph(String weightType) throws Exception {
        Graph<Stop, RouteEdge> originalGraph = graphBuilderService.buildGraph();
        DefaultUndirectedWeightedGraph<Stop, DefaultWeightedEdge> wgraph =
                new DefaultUndirectedWeightedGraph<>(DefaultWeightedEdge.class);

        edgeMap.clear();

        for (Stop s : originalGraph.vertexSet()) {
            wgraph.addVertex(s);
        }

        for (RouteEdge re : originalGraph.edgeSet()) {
            Stop source = originalGraph.getEdgeSource(re);
            Stop target = originalGraph.getEdgeTarget(re);
            DefaultWeightedEdge dwe = wgraph.addEdge(source, target);
            if (dwe != null) {
                double weight;
                switch (weightType) {
                    case "time":
                        weight = re.getSure();
                        break;
                    case "distance":
                        weight = re.getMesafe();
                        break;
                    case "cost":
                    default:
                        weight = re.getUcret();
                        break;
                }
                wgraph.setEdgeWeight(dwe, weight);
                edgeMap.put(dwe, re);
            }
        }
        return wgraph;
    }

    private SegmentResult processSegmentBetweenPointAndNearestStop(
            double lat, double lon,
            Graph<Stop, DefaultWeightedEdge> wgraph,
            String segmentName) {

        Stop nearestStop = findNearestStop(lat, lon, wgraph);
        double distance  = distanceBetween(lat, lon, nearestStop.getLat(), nearestStop.getLon());
        double cost = 0.0;
        int time = 0;

        if (distance > TAXI_THRESHOLD) {
            Taxi taxi = new Taxi();
            cost = taxi.UcretHesapla(distance);
            double taxiTime = taxi.SureHesapla(distance);
            time = (int) Math.ceil(taxiTime);
        } else {
            time = (int) Math.ceil(distance * WALK_MIN_PER_KM);
        }
        return new SegmentResult(distance, cost, time, nearestStop);
    }

    private SegmentResult processSegmentStopToPoint(
            Stop stop, double distance, String segmentName) {

        double cost = 0.0;
        int time = 0;
        if (distance > TAXI_THRESHOLD) {
            Taxi taxi = new Taxi();
            cost = taxi.UcretHesapla(distance);
            double taxiTime = taxi.SureHesapla(distance);
            time = (int) Math.ceil(taxiTime);
        } else {
            time = (int) Math.ceil(distance * WALK_MIN_PER_KM);
        }
        return new SegmentResult(distance, cost, time, stop);
    }

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

    private Stop findNearestStopByType(double lat, double lon, Graph<Stop, DefaultWeightedEdge> graph, String type) {
        Stop nearest = null;
        double minDist = Double.MAX_VALUE;
        for (Stop s : graph.vertexSet()) {
            if (s.getType().equalsIgnoreCase(type)) {
                double d = distanceBetween(lat, lon, s.getLat(), s.getLon());
                if (d < minDist) {
                    minDist = d;
                    nearest = s;
                }
            }
        }
        return nearest;
    }

    private double distanceBetween(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                 * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.asin(Math.sqrt(a));
        return R * c;
    }

    private void applyAdjustments(double baseCost) {
        double adjustedCost = baseCost;
        if (yolcu instanceof Indirim) {
            double discount = ((Indirim) yolcu).IndirimUygula(baseCost);
            adjustedCost -= discount;
            System.out.printf("Yolcu %s indirimi: -%.2f TL%n", yolcu.YolcuTipiGoster(), discount);
        }
        if (odemeYontemi instanceof KentKart) {
            double discount = ((Indirim) odemeYontemi).IndirimUygula(adjustedCost);
            adjustedCost -= discount;
            System.out.printf("KentKart indirimi: -%.2f TL%n", discount);
        } else if (odemeYontemi instanceof KrediKarti) {
            double zam = ((KrediKarti) odemeYontemi).ZamUygula(adjustedCost);
            adjustedCost += zam;
            System.out.printf("KrediKartı zammı: +%.2f TL%n", zam);
        }
        System.out.printf("Ayarlanmış Toplam Ücret: %.2f TL%n", adjustedCost);
    }

    /**
     * Segment Sonuçları için iç sınıf.
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
    
    // Yardımcı metot: Durak tipi için uygun emoji döndürür.
    private String getModeEmoji(String type) {
        if (type.equalsIgnoreCase("bus")) {
            return "🚌";
        } else if (type.equalsIgnoreCase("tram")) {
            return "🚋";
        } else {
            return "➡️";
        }
    }
}

/**
 * DijkstraSolver – Bu sınıf, JGraphT yapısındaki graf üzerinde
 * manuel Dijkstra algoritması uygulayarak en kısa yolu hesaplar.
 * Bu sayede, algoritma sorumluluğu RotaHesaplama’dan ayrılarak SOLID prensiplerine uyum sağlanmış olur.
 */
class DijkstraSolver {
    public static <V, E> List<V> findShortestPath(Graph<V, E> graph, V start, V end) {
        Map<V, Double> distances = new HashMap<>();
        Map<V, V> previous = new HashMap<>();
        PriorityQueue<V> queue = new PriorityQueue<>(Comparator.comparingDouble(distances::get));

        // Tüm düğümlerin başlangıçta uzaklığı sonsuz olarak ayarlanır.
        for (V vertex : graph.vertexSet()) {
            distances.put(vertex, Double.POSITIVE_INFINITY);
            previous.put(vertex, null);
        }
        distances.put(start, 0.0);
        queue.add(start);

        while (!queue.isEmpty()) {
            V current = queue.poll();
            if (current.equals(end)) {
                break;
            }
            // Grafın, current düğümündeki tüm kenarlarını döner (undirected olduğu için edgesOf kullanıyoruz)
            for (E edge : graph.edgesOf(current)) {
                V neighbor = Graphs.getOppositeVertex(graph, edge, current);
                double weight = graph.getEdgeWeight(edge);
                double alt = distances.get(current) + weight;
                if (alt < distances.get(neighbor)) {
                    distances.put(neighbor, alt);
                    previous.put(neighbor, current);
                    // PriorityQueue'da güncellemek için yeniden ekleme yapıyoruz.
                    queue.remove(neighbor);
                    queue.add(neighbor);
                }
            }
        }
        
        // Eğer hedefe ulaşılamadıysa boş liste döneriz.
        if (distances.get(end) == Double.POSITIVE_INFINITY) {
            return Collections.emptyList();
        }
        // Önceki düğümleri takip ederek yolu ters sırada yeniden oluştururuz.
        LinkedList<V> path = new LinkedList<>();
        for (V at = end; at != null; at = previous.get(at)) {
            path.addFirst(at);
        }
        return path;
    }
}
