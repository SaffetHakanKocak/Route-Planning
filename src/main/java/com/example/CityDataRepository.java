package com.example;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.springframework.stereotype.Service;

import com.google.gson.Gson;

@Service
public class CityDataRepository {
    public CityData loadCityData() throws Exception {
        InputStream is = getClass().getResourceAsStream("/data.json");
        if (is == null) {
            throw new Exception("data.json not found in src/main/resources!");
        }
        Gson gson = new Gson();
        return gson.fromJson(new InputStreamReader(is, StandardCharsets.UTF_8), CityData.class);
    }
}
