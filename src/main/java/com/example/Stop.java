package com.example;

import java.util.List;

public class Stop {
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

