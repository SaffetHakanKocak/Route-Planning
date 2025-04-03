package com.example;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class ObjectFactory {
    private static final Map<String, Supplier<Yolcu>> yolcuMap = new HashMap<>();
    
    private static final Map<String, Supplier<OdemeYontemi>> odemeMap = new HashMap<>();
    
    static {
        yolcuMap.put("ogrenci", Ogrenci::new);
        yolcuMap.put("yasli", Yasli::new);
        yolcuMap.put("genel", Genel::new);
        
        odemeMap.put("nakit", () -> new Nakit(200.0));          
        odemeMap.put("kredikarti", () -> new KrediKarti(500.0));  
        odemeMap.put("kentkart", () -> new KentKart(100.0));      
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
