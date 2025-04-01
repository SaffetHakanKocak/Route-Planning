package com.example;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

@Component
@SessionScope
public class UserSelection {
    private double startLat;
    private double startLon;
    private double destLat;
    private double destLon;
    private Yolcu selectedYolcu;
    private OdemeYontemi selectedOdemeYontemi;
    

    public double getStartLat() {
        return startLat;
    }
    public void setStartLat(double startLat) {
        this.startLat = startLat;
    }
    public double getStartLon() {
        return startLon;
    }
    public void setStartLon(double startLon) {
        this.startLon = startLon;
    }
    public double getDestLat() {
        return destLat;
    }
    public void setDestLat(double destLat) {
        this.destLat = destLat;
    }
    public double getDestLon() {
        return destLon;
    }
    public void setDestLon(double destLon) {
        this.destLon = destLon;
    }
    public Yolcu getSelectedYolcu() {
        return selectedYolcu;
    }
    public void setSelectedYolcu(Yolcu selectedYolcu) {
        this.selectedYolcu = selectedYolcu;
    }
    public OdemeYontemi getSelectedOdemeYontemi() {
        return selectedOdemeYontemi;
    }
    public void setSelectedOdemeYontemi(OdemeYontemi selectedOdemeYontemi) {
        this.selectedOdemeYontemi = selectedOdemeYontemi;
    }
}
