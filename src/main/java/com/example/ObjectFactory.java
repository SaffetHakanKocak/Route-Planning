package com.example;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class ObjectFactory {
    // Yolcu üretimi için anahtar-supplier eşleştirmesi
    private static final Map<String, Supplier<Yolcu>> yolcuMap = new HashMap<>();
    
    // Ödeme yöntemi üretimi için anahtar-supplier eşleştirmesi
    private static final Map<String, Supplier<OdemeYontemi>> odemeMap = new HashMap<>();
    
    static {
        // Yolcu tipi kayıtları
        yolcuMap.put("ogrenci", Ogrenci::new);
        yolcuMap.put("yasli", Yasli::new);
        yolcuMap.put("genel", Genel::new);
        
        // Ödeme tipi kayıtları
        odemeMap.put("nakit", Nakit::new);
        odemeMap.put("kredikarti", KrediKarti::new);
        odemeMap.put("kentkart", KentKart::new);
    }
    
    public static Yolcu createYolcu(String type) {
        if (type == null) {
            throw new IllegalArgumentException("Yolcu tipi belirtilmemiş.");
        }
        Supplier<Yolcu> supplier = yolcuMap.get(type.toLowerCase());
        if (supplier != null) {
            return supplier.get();
        }
        throw new IllegalArgumentException("Bilinmeyen yolcu tipi: " + type);
    }
    
    public static OdemeYontemi createOdemeYontemi(String type) {
        if (type == null) {
            throw new IllegalArgumentException("Ödeme tipi belirtilmemiş.");
        }
        Supplier<OdemeYontemi> supplier = odemeMap.get(type.toLowerCase());
        if (supplier != null) {
            return supplier.get();
        }
        throw new IllegalArgumentException("Bilinmeyen ödeme tipi: " + type);
    }
}
