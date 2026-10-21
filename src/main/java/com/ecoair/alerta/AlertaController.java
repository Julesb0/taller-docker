package com.ecoair.alerta;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/alertas")
public class AlertaController {

    private final AlertaRepository repositorio;

    public AlertaController(AlertaRepository repositorio) {
        this.repositorio = repositorio;
    }

    // GET /api/alertas -> devuelve todas las alertas guardadas
    @GetMapping
    public List<Alerta> listar() {
        return repositorio.findAll();
    }

    // POST /api/alertas -> un sensor reporta una alerta nueva
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Alerta registrar(@RequestBody Alerta alerta) {
        return repositorio.save(alerta);
    }
}
