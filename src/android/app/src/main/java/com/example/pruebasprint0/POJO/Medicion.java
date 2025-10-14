package com.example.pruebasprint0.POJO;

public class Medicion {

    private String tipo;
    private int Valor;

    public Medicion(String tipo, int Valor) {
        this.tipo = tipo;
        this.Valor = Valor;
    }
    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        String Tipo = tipo;
    }

    public int getValor() {
        return Valor;
    }

    public void setValor(int valor) {
        Valor = valor;
    }
}