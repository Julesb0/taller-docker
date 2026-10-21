package com.ecoair.alerta;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import java.time.Instant;

// Entidad JPA: Hibernate crea la tabla "alerta" en H2 cuando arranca la app
@Entity
public class Alerta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String sensor;   // ej: ESTACION-01-PM25
    private String tipo;     // ej: PM2.5, CO2, TEMPERATURA
    private double valor;
    private String nivel;    // ej: NORMAL, ALTO, CRITICO
    private Instant registradaEn;

    @PrePersist
    void antesDeGuardar() {
        registradaEn = Instant.now();
    }

    public Long getId() { return id; }

    public String getSensor() { return sensor; }
    public void setSensor(String sensor) { this.sensor = sensor; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public double getValor() { return valor; }
    public void setValor(double valor) { this.valor = valor; }

    public String getNivel() { return nivel; }
    public void setNivel(String nivel) { this.nivel = nivel; }

    public Instant getRegistradaEn() { return registradaEn; }
}
