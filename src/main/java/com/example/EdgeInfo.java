package com.example;

public class EdgeInfo {
    private Stop from;
    private Stop to;
    private RouteEdge routeEdge;

    public EdgeInfo(Stop from, Stop to, RouteEdge routeEdge) {
        this.from = from;
        this.to = to;
        this.routeEdge = routeEdge;
    }

    public Stop getFrom() {
        return from;
    }

    public Stop getTo() {
        return to;
    }

    public RouteEdge getRouteEdge() {
        return routeEdge;
    }
}
