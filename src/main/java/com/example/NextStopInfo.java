package com.example;

public class NextStopInfo {
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
