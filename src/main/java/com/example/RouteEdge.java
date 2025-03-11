package com.example;

public class RouteEdge {
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
