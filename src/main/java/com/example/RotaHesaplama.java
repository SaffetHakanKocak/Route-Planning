package com.example;

public class RotaHesaplama {
    private double startLat;
    private double startLon;
    private double destLat;
    private double destLon;
    private GraphBuilderService graphBuilderService;
    private Yolcu yolcu;                // Oluşturulan yolcu nesnesi (polimorfik)
    private OdemeYontemi odemeYontemi;   // Oluşturulan ödeme yöntemi nesnesi (polimorfik)

    // Constructor: Başlangıç, hedef koordinatları, graf servisi, yolcu ve ödeme nesneleri alınıyor.
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

    // UygunUcretHesapla metodunu daha sonra tanımlayacağız.
    public void UygunUcretHesapla() {
        // Bu metodun içeriğini ilerleyen adımlarda belirleyeceğiz.
    }
    
    // Diğer metotlar (örneğin distanceBetween) mevcut haliyle kalabilir.
    private double distanceBetween(double lat1, double lon1, double lat2, double lon2) {
        double dLat = lat1 - lat2;
        double dLon = lon1 - lon2;
        return Math.sqrt(dLat * dLat + dLon * dLon);
    }
}
