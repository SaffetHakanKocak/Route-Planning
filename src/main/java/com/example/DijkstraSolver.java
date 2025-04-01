package com.example;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

public class DijkstraSolver {
    public static List<Stop> findShortestPath(ManualGraph graph, Stop start, Stop end, String weightType) {
        Map<Stop, Double> distances = new HashMap<>();
        Map<Stop, Stop> previous = new HashMap<>();
        PriorityQueue<Stop> queue = new PriorityQueue<>(Comparator.comparingDouble(distances::get));

        for (Stop vertex : graph.getVertices()) {
            distances.put(vertex, Double.POSITIVE_INFINITY);
            previous.put(vertex, null);
        }
        distances.put(start, 0.0);
        queue.add(start);

        while (!queue.isEmpty()) {
            Stop current = queue.poll();
            if (current.equals(end))
                break;

            for (EdgeInfo edgeInfo : graph.getEdges(current)) {
                Stop neighbor = edgeInfo.getTo();
                double weight;
                switch (weightType.toLowerCase()) {
                    case "time":
                        weight = edgeInfo.getRouteEdge().getSure();
                        break;
                    case "distance":
                        weight = edgeInfo.getRouteEdge().getMesafe();
                        break;
                    default:
                        weight = edgeInfo.getRouteEdge().getUcret();
                        break;
                }
                double alt = distances.get(current) + weight;
                if (alt < distances.get(neighbor)) {
                    distances.put(neighbor, alt);
                    previous.put(neighbor, current);
                    // Yeniden kuyruğa eklemek için önce varsa kaldırıyoruz.
                    queue.remove(neighbor);
                    queue.add(neighbor);
                }
            }
        }

        if (distances.get(end) == Double.POSITIVE_INFINITY) {
            return Collections.emptyList();
        }

        LinkedList<Stop> path = new LinkedList<>();
        for (Stop at = end; at != null; at = previous.get(at)) {
            path.addFirst(at);
        }
        return path;
    }
}
