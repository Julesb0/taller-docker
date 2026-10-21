package com.ecoair.alerta;

import org.springframework.data.jpa.repository.JpaRepository;

// Spring Data arma solo los metodos save, findAll, etc. al arrancar
public interface AlertaRepository extends JpaRepository<Alerta, Long> {
}
