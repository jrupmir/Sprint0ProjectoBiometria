package com.example.pruebasprint0;

import com.example.pruebasprint0.POJO.Medicion;
import com.google.gson.Gson;

import org.junit.Test;

import static org.junit.Assert.*;

public class MedicionUnitTest {

    @Test
    public void medicion_serialization_contains_fields() {
        Medicion m = new Medicion("Temperatura", 25);
        Gson g = new Gson();
        String json = g.toJson(m);
        assertTrue(json.contains("Temperatura"));
        assertTrue(json.contains("25"));
        // keys should be 'Tipo' and 'Valor'
        assertTrue(json.contains("Tipo"));
        assertTrue(json.contains("Valor"));
    }
}
