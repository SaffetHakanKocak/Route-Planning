package com.example;

import java.util.List;
import java.util.Locale;

public class RotaHesaplama {

    private final double startLat;
    private final double startLon;
    private final double destLat;
    private final double destLon;
    private final GraphBuilderService graphBuilderService;
    private final Yolcu yolcu;
    private final OdemeYontemi odemeYontemi;

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

    public OdemeYontemi getOdemeYontemi() {
        return this.odemeYontemi;
    }

    // 1) EN UYGUN ÜCRETLİ ROTA (cost)
    public String getUygunUcretHtml() {
        StringBuilder sb = new StringBuilder();
        sb.append("<div class='blue-bgc' style='padding:15px; border-radius:8px; border:1px solid #ddd;'>");
        sb.append("<h2 style='margin-top:0; color:#333;'>💰 En Uygun Ücretli Rota</h2>");

        try {
            ManualGraph graph = buildWeightedGraph("cost");
            SegmentResult startSegment = processSegmentBetweenPointAndNearestStop(startLat, startLon, graph);
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

            Stop nearestDestStop = findNearestStop(destLat, destLon, graph);
            List<Stop> path = DijkstraSolver.findShortestPath(graph, startSegment.stop, nearestDestStop, "cost");

            double totalDistance = startSegment.distance;
            double totalCost = startSegment.cost;
            int totalTime = startSegment.time;

            sb.append("<div style='margin-top:10px;'><b>🛣️ Rota Detayları:</b></div>");
            sb.append("<ol style='padding-left:18px; margin:0;'>");
            if (path == null || path.isEmpty()) {
                sb.append("<li style='color:red;'>Başlangıç durağı ile hedef durağı arasında bir yol bulunamadı!</li>");
            } else {
                for (int i = 0; i < path.size() - 1; i++) {
                    Stop current = path.get(i);
                    Stop next = path.get(i + 1);
                    RouteEdge re = getEdgeBetween(graph, current, next);
                    if (re != null) {
                        totalDistance += re.getMesafe();
                        totalCost     += re.getUcret();
                        totalTime     += re.getSure();

                        String transferInfo = "";
                        if (!current.getType().equalsIgnoreCase(next.getType())) {
                            transferInfo = " (🔁 Transfer)";
                        }
                        sb.append("<li style='margin-bottom:6px;'>");
                        sb.append("<b>" + current.getName() + " [" + getModeEmoji(current.getType()) + "]</b> → ");
                        sb.append("<b>" + next.getName() + " [" + getModeEmoji(next.getType()) + "]</b>" + transferInfo);
                        sb.append(String.format(
                            " <span style='color:#666;'>⏳ Süre=%d dk, 💰 Ücret=%.2f TL, 📏 Mesafe=%.2f km</span>",
                            re.getSure(), re.getUcret(), re.getMesafe()
                        ));
                        sb.append("</li>");
                    }
                }
            }
            sb.append("</ol>");

            double endSegmentDistance = distanceBetween(nearestDestStop.getLat(), nearestDestStop.getLon(), destLat, destLon);
            SegmentResult endSegment = processSegmentStopToPoint(nearestDestStop, endSegmentDistance);
            totalDistance += endSegment.distance;
            totalCost     += endSegment.cost;
            totalTime     += endSegment.time;

            sb.append("<div style='margin-top:10px;'><b>Hedefe Varış:</b></div>");
            sb.append(String.format(
                "<p>Son Durak → Hedef | ⏳ Süre=%d dk, 💰 Ücret=%.2f TL, 📏 Mesafe=%.2f km</p>",
                endSegment.time, endSegment.cost, endSegment.distance
            ));

            sb.append("<hr style='margin:10px 0;'>");
            sb.append("<div><b>🔎 Rota Özeti:</b></div>");
            sb.append("<ul style='list-style:none; padding-left:0; margin:0;'>");
            sb.append(String.format("<li>💸 Ücret: <b>%.2f TL</b></li>", totalCost));
            sb.append(String.format("<li>⏱️ Süre: <b>%d dk</b></li>", totalTime));
            sb.append(String.format("<li>📏 Mesafe: <b>%.2f km</b></li>", totalDistance));
            sb.append("</ul>");

            double finalCost = calculateAdjustedCost(totalCost, sb);

        } catch (Exception e) {
            e.printStackTrace();
            sb.append("<p style='color:red;'>Hata: " + e.getMessage() + "</p>");
        }
        sb.append("</div>");
        return sb.toString();
    }

    // 2) EN UYGUN ZAMANLI ROTA (time)
    public String getUygunZamanHtml() {
        StringBuilder sb = new StringBuilder();
        sb.append("<div class='orange-bgc' style='padding:15px; border-radius:8px; border:1px solid #ddd;'>");
        sb.append("<h2 style='margin-top:0; color:#333;'>⏱️ En Uygun Zamanlı Rota</h2>");

        try {
            ManualGraph graph = buildWeightedGraph("time");
            SegmentResult startSegment = processSegmentBetweenPointAndNearestStop(startLat, startLon, graph);
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

            Stop nearestDestStop = findNearestStop(destLat, destLon, graph);
            List<Stop> path = DijkstraSolver.findShortestPath(graph, startSegment.stop, nearestDestStop, "time");

            double totalDistance = startSegment.distance;
            double totalCost = startSegment.cost;
            int totalTime = startSegment.time;

            sb.append("<div style='margin-top:10px;'><b>🛣️ Rota Detayları:</b></div>");
            sb.append("<ol style='padding-left:18px; margin:0;'>");
            if (path == null || path.isEmpty()) {
                sb.append("<li style='color:red;'>Başlangıç durağı ile hedef durağı arasında bir yol bulunamadı!</li>");
            } else {
                for (int i = 0; i < path.size() - 1; i++) {
                    Stop current = path.get(i);
                    Stop next = path.get(i + 1);
                    RouteEdge re = getEdgeBetween(graph, current, next);
                    if (re != null) {
                        totalDistance += re.getMesafe();
                        totalCost     += re.getUcret();
                        totalTime     += re.getSure();

                        String transferInfo = "";
                        if (!current.getType().equalsIgnoreCase(next.getType())) {
                            transferInfo = " (🔁 Transfer)";
                        }
                        sb.append("<li style='margin-bottom:6px;'>");
                        sb.append("<b>" + current.getName() + " [" + getModeEmoji(current.getType()) + "]</b> → ");
                        sb.append("<b>" + next.getName() + " [" + getModeEmoji(next.getType()) + "]</b>" + transferInfo);
                        sb.append(String.format(
                            " <span style='color:#666;'>⏳ Süre=%d dk, 💰 Ücret=%.2f TL, 📏 Mesafe=%.2f km</span>",
                            re.getSure(), re.getUcret(), re.getMesafe()
                        ));
                        sb.append("</li>");
                    }
                }
            }
            sb.append("</ol>");

            double endSegmentDistance = distanceBetween(nearestDestStop.getLat(), nearestDestStop.getLon(), destLat, destLon);
            SegmentResult endSegment = processSegmentStopToPoint(nearestDestStop, endSegmentDistance);
            totalDistance += endSegment.distance;
            totalCost     += endSegment.cost;
            totalTime     += endSegment.time;

            sb.append("<div style='margin-top:10px;'><b>Hedefe Varış:</b></div>");
            sb.append(String.format(
                "<p>Son Durak → Hedef | ⏳ Süre=%d dk, 💰 Ücret=%.2f TL, 📏 Mesafe=%.2f km</p>",
                endSegment.time, endSegment.cost, endSegment.distance
            ));

            sb.append("<hr style='margin:10px 0;'>");
            sb.append("<div><b>🔎 Rota Özeti:</b></div>");
            sb.append("<ul style='list-style:none; padding-left:0; margin:0;'>");
            sb.append(String.format("<li>💸 Ücret: <b>%.2f TL</b></li>", totalCost));
            sb.append(String.format("<li>⏱️ Süre: <b>%d dk</b></li>", totalTime));
            sb.append(String.format("<li>📏 Mesafe: <b>%.2f km</b></li>", totalDistance));
            sb.append("</ul>");

            double finalCost = calculateAdjustedCost(totalCost, sb);
        } catch (Exception e) {
            e.printStackTrace();
            sb.append("<p style='color:red;'>Hata: " + e.getMessage() + "</p>");
        }
        sb.append("</div>");
        return sb.toString();
    }

    // 3) EN UYGUN MESAFELİ ROTA (distance)
    public String getUygunMesafeHtml() {
        StringBuilder sb = new StringBuilder();
        sb.append("<div class='purple-bgc' style='padding:15px; border-radius:8px; border:1px solid #ddd;'>");
        sb.append("<h2 style='margin-top:0; color:#333;'>📏 En Uygun Mesafeli Rota</h2>");

        try {
            ManualGraph graph = buildWeightedGraph("distance");
            SegmentResult startSegment = processSegmentBetweenPointAndNearestStop(startLat, startLon, graph);
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

            Stop nearestDestStop = findNearestStop(destLat, destLon, graph);
            List<Stop> path = DijkstraSolver.findShortestPath(graph, startSegment.stop, nearestDestStop, "distance");

            double totalDistance = startSegment.distance;
            double totalCost = startSegment.cost;
            int totalTime = startSegment.time;

            sb.append("<div style='margin-top:10px;'><b>🛣️ Rota Detayları:</b></div>");
            sb.append("<ol style='padding-left:18px; margin:0;'>");
            if (path == null || path.isEmpty()) {
                sb.append("<li style='color:red;'>Başlangıç durağı ile hedef durağı arasında bir yol bulunamadı!</li>");
            } else {
                for (int i = 0; i < path.size() - 1; i++) {
                    Stop current = path.get(i);
                    Stop next = path.get(i + 1);
                    RouteEdge re = getEdgeBetween(graph, current, next);
                    if (re != null) {
                        totalDistance += re.getMesafe();
                        totalCost     += re.getUcret();
                        totalTime     += re.getSure();

                        String transferInfo = "";
                        if (!current.getType().equalsIgnoreCase(next.getType())) {
                            transferInfo = " (🔁 Transfer)";
                        }
                        sb.append("<li style='margin-bottom:6px;'>");
                        sb.append("<b>" + current.getName() + " [" + getModeEmoji(current.getType()) + "]</b> → ");
                        sb.append("<b>" + next.getName() + " [" + getModeEmoji(next.getType()) + "]</b>" + transferInfo);
                        sb.append(String.format(
                            " <span style='color:#666;'>⏳ Süre=%d dk, 💰 Ücret=%.2f TL, 📏 Mesafe=%.2f km</span>",
                            re.getSure(), re.getUcret(), re.getMesafe()
                        ));
                        sb.append("</li>");
                    }
                }
            }
            sb.append("</ol>");

            double endSegmentDistance = distanceBetween(nearestDestStop.getLat(), nearestDestStop.getLon(), destLat, destLon);
            SegmentResult endSegment = processSegmentStopToPoint(nearestDestStop, endSegmentDistance);
            totalDistance += endSegment.distance;
            totalCost     += endSegment.cost;
            totalTime     += endSegment.time;

            sb.append("<div style='margin-top:10px;'><b>Hedefe Varış:</b></div>");
            sb.append(String.format(
                "<p>Son Durak → Hedef | ⏳ Süre=%d dk, 💰 Ücret=%.2f TL, 📏 Mesafe=%.2f km</p>",
                endSegment.time, endSegment.cost, endSegment.distance
            ));

            sb.append("<hr style='margin:10px 0;'>");
            sb.append("<div><b>🔎 Rota Özeti:</b></div>");
            sb.append("<ul style='list-style:none; padding-left:0; margin:0;'>");
            sb.append(String.format("<li>💸 Ücret: <b>%.2f TL</b></li>", totalCost));
            sb.append(String.format("<li>⏱️ Süre: <b>%d dk</b></li>", totalTime));
            sb.append(String.format("<li>📏 Mesafe: <b>%.2f km</b></li>", totalDistance));
            sb.append("</ul>");

            double finalCost = calculateAdjustedCost(totalCost, sb);
        } catch (Exception e) {
            e.printStackTrace();
            sb.append("<p style='color:red;'>Hata: " + e.getMessage() + "</p>");
        }
        sb.append("</div>");
        return sb.toString();
    }

    // 4) Sadece Otobüs Rota (Dinamik)
    public String getSadeceOtobusHtml() {
        StringBuilder sb = new StringBuilder();
        sb.append("<div style='background:#e8f5e9; padding:15px; border:1px solid #ccc; border-radius:5px;'>");
        sb.append("<h2 style='margin-top:0;'>🚌 Sadece Otobüs Rota </h2>");
        try {
            ManualGraph originalGraph = graphBuilderService.buildGraph();
            ManualGraph busGraph = new ManualGraph();
            for (Stop s : originalGraph.getVertices()) {
                if ("bus".equalsIgnoreCase(s.getType())) {
                    busGraph.addVertex(s);
                }
            }

            for (Stop s : originalGraph.getVertices()) {
                if ("bus".equalsIgnoreCase(s.getType())) {
                    for (EdgeInfo edgeInfo : originalGraph.getEdges(s)) {
                        Stop neighbor = edgeInfo.getTo();
                        if ("bus".equalsIgnoreCase(neighbor.getType())) {
                            if (s.getId().compareTo(neighbor.getId()) < 0) {
                                busGraph.addEdge(s, neighbor, edgeInfo.getRouteEdge());
                            }
                        }
                    }
                }
            }
    
            SegmentResult startSegment = processSegmentBetweenPointAndNearestStop(startLat, startLon, busGraph);
            double distanceToNearest = distanceBetween(startLat, startLon, startSegment.stop.getLat(), startSegment.stop.getLon());

            sb.append("<p style='font-weight:bold; color:#555;'>");
            sb.append("📍 Başlangıç Noktasına En Yakın Otobüs Durağı: " + startSegment.stop.getName());
            sb.append(String.format(" (%.2f km) ", distanceToNearest));
            if (startSegment.cost > 0) {
            sb.append(" → 🚕 Taksi => " + String.format("%.2f TL", startSegment.cost));
            } else {
            sb.append(" → 🚶 Yürüme => 0 TL");
            }
            sb.append("</p>");
            Stop nearestDestStop = findNearestStop(destLat, destLon, busGraph);
            List<Stop> path = DijkstraSolver.findShortestPath(busGraph, startSegment.stop, nearestDestStop, "cost");

            double totalDistance = startSegment.distance;
            double totalCost = startSegment.cost;
            int totalTime = startSegment.time;

            sb.append("<div style='margin-top:10px;'><b>🛣️ Otobüs Rota Detayları:</b></div>");
            sb.append("<ol style='padding-left:18px; margin:0;'>");
            if (path == null || path.isEmpty()) {
                sb.append("<li style='color:red;'>Otobüs durakları arasında bir rota bulunamadı!</li>");
            } else {
                for (int i = 0; i < path.size() - 1; i++) {
                    Stop current = path.get(i);
                    Stop next = path.get(i + 1);
                    RouteEdge re = getEdgeBetween(busGraph, current, next);
                    if (re != null) {
                        totalDistance += re.getMesafe();
                        totalCost += re.getUcret();
                        totalTime += re.getSure();
                        String transferInfo = "";
                        if (!current.getType().equalsIgnoreCase(next.getType())) {
                            transferInfo = " (🔁 Transfer)";
                        }
                        sb.append("<li style='margin-bottom:6px;'>");
                        sb.append("<b>" + current.getName() + " [" + getModeEmoji(current.getType()) + "]</b> → ");
                        sb.append("<b>" + next.getName() + " [" + getModeEmoji(next.getType()) + "]</b>" + transferInfo);
                        sb.append(" <span style='color:#666;'>⏳ Süre=" + re.getSure() + " dk, 💰 Ücret=" + String.format("%.2f", re.getUcret()) +
                                  " TL, 📏 Mesafe=" + String.format("%.2f", re.getMesafe()) + " km</span>");
                        sb.append("</li>");
                    }
                }
            }
            double endSegmentDistance = distanceBetween(nearestDestStop.getLat(), nearestDestStop.getLon(), destLat, destLon);
            SegmentResult endSegment = processSegmentStopToPoint(nearestDestStop, endSegmentDistance);
            totalDistance += endSegment.distance;
            totalCost += endSegment.cost;
            totalTime += endSegment.time;
            sb.append("<div style='margin-top:10px;'><b>Hedefe Varış:</b></div>");
            sb.append(String.format("<p>Son Durak → Hedef | ⏳ Süre=%d dk, 💰 Ücret=%.2f TL, 📏 Mesafe=%.2f km</p>",
                    endSegment.time, endSegment.cost, endSegment.distance));
            sb.append("<hr style='margin:10px 0;'>");
            sb.append("<div><b>🔎 Rota Özeti:</b></div>");
            sb.append("<ul style='list-style:none; padding-left:0; margin:0;'>");
            sb.append(String.format("<li>💸 Ücret: <b>%.2f TL</b></li>", totalCost));
            sb.append(String.format("<li>⏱️ Süre: <b>%d dk</b></li>", totalTime));
            sb.append(String.format("<li>📏 Mesafe: <b>%.2f km</b></li>", totalDistance));
            sb.append("</ul>");

            double finalCost = calculateAdjustedCost(totalCost, sb);
        } catch(Exception e) {
            e.printStackTrace();
            sb.append("<p style='color:red;'>Hata: " + e.getMessage() + "</p>");
        }
        sb.append("</div>");
        return sb.toString();
    }

    // 5) Sadece Tramvay Rota (Dinamik)
    public String getSadeceTramvayHtml() {
        StringBuilder sb = new StringBuilder();
        sb.append("<div style='background:#fce4ec; padding:15px; border:1px solid #ccc; border-radius:5px;'>");
        sb.append("<h2 style='margin-top:0;'>🚋 Sadece Tramvay Rota</h2>");
        try {
            ManualGraph originalGraph = graphBuilderService.buildGraph();
            ManualGraph tramGraph = new ManualGraph();
            for (Stop s : originalGraph.getVertices()) {
                if ("tram".equalsIgnoreCase(s.getType())) {
                    tramGraph.addVertex(s);
                }
            }
            for (Stop s : originalGraph.getVertices()) {
                if ("tram".equalsIgnoreCase(s.getType())) {
                    for (EdgeInfo edgeInfo : originalGraph.getEdges(s)) {
                        Stop neighbor = edgeInfo.getTo();
                        if ("tram".equalsIgnoreCase(neighbor.getType())) {
                            if (s.getId().compareTo(neighbor.getId()) < 0) {
                                tramGraph.addEdge(s, neighbor, edgeInfo.getRouteEdge());
                            }
                        }
                    }
                }
            }
            
SegmentResult startSegment = processSegmentBetweenPointAndNearestStop(startLat, startLon, tramGraph);
double distanceToNearest = distanceBetween(startLat, startLon, startSegment.stop.getLat(), startSegment.stop.getLon());

sb.append("<p style='font-weight:bold; color:#555;'>");
sb.append("📍 Başlangıç Noktasına En Yakın Tramvay Durağı: " + startSegment.stop.getName());
sb.append(String.format(" (%.2f km) ", distanceToNearest));
if (startSegment.cost > 0) {
    sb.append(" → 🚕 Taksi => " + String.format("%.2f TL", startSegment.cost));
} else {
    sb.append(" → 🚶 Yürüme => 0 TL");
}
sb.append("</p>");
            Stop nearestDestStop = findNearestStop(destLat, destLon, tramGraph);
            List<Stop> path = DijkstraSolver.findShortestPath(tramGraph, startSegment.stop, nearestDestStop, "cost");

            double totalDistance = startSegment.distance;
            double totalCost = startSegment.cost;
            int totalTime = startSegment.time;

            sb.append("<div style='margin-top:10px;'><b>🛣️ Tramvay Rota Detayları:</b></div>");
            sb.append("<ol style='padding-left:18px; margin:0;'>");
            if (path == null || path.isEmpty()) {
                sb.append("<li style='color:red;'>Tramvay durakları arasında bir rota bulunamadı!</li>");
            } else {
                for (int i = 0; i < path.size() - 1; i++) {
                    Stop current = path.get(i);
                    Stop next = path.get(i + 1);
                    RouteEdge re = getEdgeBetween(tramGraph, current, next);
                    if (re != null) {
                        totalDistance += re.getMesafe();
                        totalCost += re.getUcret();
                        totalTime += re.getSure();
                        String transferInfo = "";
                        if (!current.getType().equalsIgnoreCase(next.getType())) {
                            transferInfo = " (🔁 Transfer)";
                        }
                        sb.append("<li style='margin-bottom:6px;'>");
                        sb.append("<b>" + current.getName() + " [" + getModeEmoji(current.getType()) + "]</b> → ");
                        sb.append("<b>" + next.getName() + " [" + getModeEmoji(next.getType()) + "]</b>" + transferInfo);
                        sb.append(" <span style='color:#666;'>⏳ Süre=" + re.getSure() + " dk, 💰 Ücret=" + String.format("%.2f", re.getUcret()) +
                                  " TL, 📏 Mesafe=" + String.format("%.2f", re.getMesafe()) + " km</span>");
                        sb.append("</li>");
                    }
                }
            }
            sb.append("</ol>");

            double endSegmentDistance = distanceBetween(nearestDestStop.getLat(), nearestDestStop.getLon(), destLat, destLon);
            SegmentResult endSegment = processSegmentStopToPoint(nearestDestStop, endSegmentDistance);
            totalDistance += endSegment.distance;
            totalCost += endSegment.cost;
            totalTime += endSegment.time;
            sb.append("<div style='margin-top:10px;'><b>Hedefe Varış:</b></div>");
            sb.append(String.format("<p>Son Durak → Hedef | ⏳ Süre=%d dk, 💰 Ücret=%.2f TL, 📏 Mesafe=%.2f km</p>",
                    endSegment.time, endSegment.cost, endSegment.distance));
            sb.append("<hr style='margin:10px 0;'>");
            sb.append("<div><b>🔎 Rota Özeti:</b></div>");
            sb.append("<ul style='list-style:none; padding-left:0; margin:0;'>");
            sb.append(String.format("<li>💸 Ücret: <b>%.2f TL</b></li>", totalCost));
            sb.append(String.format("<li>⏱️ Süre: <b>%d dk</b></li>", totalTime));
            sb.append(String.format("<li>📏 Mesafe: <b>%.2f km</b></li>", totalDistance));
            sb.append("</ul>");

            double finalCost = calculateAdjustedCost(totalCost, sb);
        } catch(Exception e) {
            e.printStackTrace();
            sb.append("<p style='color:red;'>Hata: " + e.getMessage() + "</p>");
        }
        sb.append("</div>");
        return sb.toString();
    }

    // 6) Sadece Taksi Rota (Doğrudan hesaplama)
    public String getSadeceTaxiHtml() {
        StringBuilder sb = new StringBuilder();
        sb.append("<div style='background:#fff9c4; padding:15px; border:1px solid #ccc; border-radius:5px;'>");
        sb.append("<h2 style='margin-top:0;'>🚖 Sadece Taksi Rota</h2>");
        try {
            double distance = distanceBetween(startLat, startLon, destLat, destLon);
            Taxi taxi = new Taxi();
            double cost = taxi.UcretHesapla(distance);
            double taxiTime = taxi.SureHesapla(distance);
            int time = (int) Math.ceil(taxiTime);
            sb.append("<p style='font-weight:bold; color:#555;'>");
            sb.append("🚕 Direkt Taksi Kullanımı Başlangıç → Hedef: ");
            sb.append(String.format("📏 Mesafe: %.2f km, ⏳ Süre: %d dk, 💰 Ücret: %.2f TL", distance, time, cost));
            sb.append("</p>");
            
            double finalCost = calculateAdjustedCost(cost, sb);
            
        } catch(Exception e) {
            e.printStackTrace();
            sb.append("<p style='color:red;'>Hata: " + e.getMessage() + "</p>");
        }
        sb.append("</div>");
        return sb.toString();
    }
    

    // Genel rota stratejisi seçimini yapan metot
    public String getRouteHtml(String strategyKey) {
        RouteStrategy strategy;
        switch (strategyKey.toLowerCase()) {
            case "cheapest":
                strategy = new CheapestRouteStrategy(this);
                break;
            case "fastest":
                strategy = new FastestRouteStrategy(this);
                break;
            case "shortest":
                strategy = new ShortestRouteStrategy(this);
                break;
            case "bus":
                strategy = new BusRouteStrategy(this);
                break;
            case "tram":
                strategy = new TramRouteStrategy(this);
                break;
            case "taxi":
                strategy = new TaxiRouteStrategy(this);
                break;
            default:
                throw new IllegalArgumentException("Bilinmeyen rota stratejisi: " + strategyKey);
        }
        return strategy.calculateRouteHtml();
    }


    private ManualGraph buildWeightedGraph(String weightType) throws Exception {
        return graphBuilderService.buildGraph();
    }

    private SegmentResult processSegmentBetweenPointAndNearestStop(double lat, double lon, ManualGraph graph) {
        Stop nearest = findNearestStop(lat, lon, graph);
        double distance = distanceBetween(lat, lon, nearest.getLat(), nearest.getLon());
        double cost = 0.0;
        int time = 0;
        if (distance > 3.0) {
            Taxi taxi = new Taxi();
            cost = taxi.UcretHesapla(distance);
            time = (int) Math.ceil(taxi.SureHesapla(distance));
        } else {
            time = (int) Math.ceil(distance * 3);
        }
        return new SegmentResult(distance, cost, time, nearest);
    }

    private SegmentResult processSegmentStopToPoint(Stop stop, double distance) {
        double cost = 0.0;
        int time = 0;
        if (distance > 3.0) {
            Taxi taxi = new Taxi();
            cost = taxi.UcretHesapla(distance);
            time = (int) Math.ceil(taxi.SureHesapla(distance));
        } else {
            time = (int) Math.ceil(distance * 3);
        }
        return new SegmentResult(distance, cost, time, stop);
    }

    private Stop findNearestStop(double lat, double lon, ManualGraph graph) {
        Stop nearest = null;
        double minDist = Double.MAX_VALUE;
        for (Stop s : graph.getVertices()) {
            double d = distanceBetween(lat, lon, s.getLat(), s.getLon());
            if (d < minDist) {
                minDist = d;
                nearest = s;
            }
        }
        return nearest;
    }

    private RouteEdge getEdgeBetween(ManualGraph graph, Stop s1, Stop s2) {
        for (EdgeInfo edgeInfo : graph.getEdges(s1)) {
            if (edgeInfo.getTo().equals(s2)) {
                return edgeInfo.getRouteEdge();
            }
        }
        return null;
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

    private String getModeEmoji(String type) {
        if ("bus".equalsIgnoreCase(type)) {
            return "🚌";
        } else if ("tram".equalsIgnoreCase(type)) {
            return "🚋";
        } else {
            return "➡️";
        }
    }

    private double calculateAdjustedCost(double baseCost, StringBuilder sb) {
        double adjustedCost = baseCost;
    
        if (yolcu instanceof Indirim) {
            double discount = ((Indirim) yolcu).IndirimUygula(baseCost);
            adjustedCost -= discount;
            sb.append(String.format("<p style='color:green;'>🚶 Yolcu %s indirimi: -%.2f TL</p>",
                    yolcu.YolcuTipiGoster(), discount));
        }
    
        if (odemeYontemi instanceof KentKart) {
            double discount = ((KentKart) odemeYontemi).IndirimUygula(adjustedCost);
            adjustedCost -= discount;
            sb.append(String.format("<p style='color:green;'>💳 KentKart indirimi: -%.2f TL</p>", discount));
        } else if (odemeYontemi instanceof KrediKarti) {
            double zam = ((KrediKarti) odemeYontemi).ZamUygula(adjustedCost);
            adjustedCost += zam;
            sb.append(String.format("<p style='color:red;'>💳 KrediKart zammı: +%.2f TL</p>", zam));
        }
    
        
    String displayCost = String.format("%.2f", adjustedCost).replace('.', ',');
    String parseCost = String.format(Locale.US, "%.2f", adjustedCost);
    
    sb.append("<p style='color:#007bff;'><b>Güncel Ücret: " + displayCost + " TL</b></p>");
    sb.append("<span id='finalCostValue' style='display:none;'>" + parseCost + "</span>");
    
        return adjustedCost;
}

    
    public String approvePayment(double cost) {
        StringBuilder sb = new StringBuilder();
        if (odemeYontemi instanceof KentKart) {
            KentKart kKart = (KentKart) odemeYontemi;
            if (kKart.getBakiye() < cost) {
                sb.append(String.format("<p style='color:red;'>⚠ KentKart bakiyesi yetersiz! (Bakiye: %.2f TL)</p>", kKart.getBakiye()));
            } else {
                kKart.setBakiye(kKart.getBakiye() - cost);
                sb.append(String.format("<p>KentKart bakiyenizden %.2f TL çekildi. Kalan bakiye: %.2f TL</p>",
                        cost, kKart.getBakiye()));
            }
        } else if (odemeYontemi instanceof KrediKarti) {
            KrediKarti kk = (KrediKarti) odemeYontemi;
            if (kk.getKrediLimiti() < cost) {
                sb.append(String.format("<p style='color:red;'>⚠ Kredi Kartı limitiniz yetersiz! (Limit: %.2f TL)</p>", kk.getKrediLimiti()));
            } else {
                kk.setKrediLimiti(kk.getKrediLimiti() - cost);
                sb.append(String.format("<p>Kredi Kartınızdan %.2f TL çekildi. Kalan limit: %.2f TL</p>",
                        cost, kk.getKrediLimiti()));
            }
        } else if (odemeYontemi instanceof Nakit) {
            Nakit nakit = (Nakit) odemeYontemi;
            if (nakit.getNakitMiktari() < cost) {
                sb.append(String.format("<p style='color:red;'>⚠ Nakit miktarınız yetersiz! (Miktar: %.2f TL)</p>", nakit.getNakitMiktari()));
            } else {
                nakit.setNakitMiktari(nakit.getNakitMiktari() - cost);
                sb.append(String.format("<p>Nakitten %.2f TL çekildi. Kalan miktar: %.2f TL</p>",
                        cost, nakit.getNakitMiktari()));
            }
        }
        return sb.toString();
    }

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
