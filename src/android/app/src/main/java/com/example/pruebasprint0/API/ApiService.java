package com.example.pruebasprint0.API;

import com.example.pruebasprint0.POJO.Medicion;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface ApiService {

    /********************************************************
     * @fn Call<Object> ping()
     * @brief Método que realiza una petición GET al servidor para verificar su disponibilidad.
     * @return Retorna un objeto de tipo Call que contiene la respuesta del servidor.
     ********************************************************/
    @GET("/ping")
    Call<Object> ping(); // Aquí puedes usar una clase específica en lugar de Object para mapear la respuesta

    /********************************************************
     * @fn Call<Void> insertarMedicion(@Body Medicion medicion)
     * @brief Método que realiza una petición POST para insertar una medición en el servidor.
     * @param[in] medicion Objeto de tipo Medicion que contiene los datos de la medición a insertar.
     * @return Retorna un objeto de tipo Call<Void> que representa el resultado de la operación.
     ********************************************************/
    @POST("/insertar")
    Call<Void> insertarMedicion(@Body Medicion medicion);
}