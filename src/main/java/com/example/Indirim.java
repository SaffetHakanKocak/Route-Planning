package com.example;

public interface Indirim {
    // İndirim uygulayan metot. Uygulama sınıfı kendi indirim oranını belirleyerek toplam ücretten indirimi uygular.
    double IndirimUygula(double toplamUcret);
    
    // Her implementasyonun kendi indirim oranını döndüren abstract metot.
    double getIndirimYuzdesi();
}
