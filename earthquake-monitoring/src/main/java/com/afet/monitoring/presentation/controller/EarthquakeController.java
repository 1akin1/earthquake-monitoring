package com.afet.monitoring.presentation.controller;

import com.afet.monitoring.application.usecase.*;
import com.afet.monitoring.presentation.controller.dto.CreateEarthquakeRequest;
import com.afet.monitoring.presentation.controller.dto.EarthquakeResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.List;

/** Thin adapter over the use cases. No business logic here — just HTTP <-> use case. */
@RestController
@RequestMapping("/api/earthquakes")
public class EarthquakeController {

    private final RegisterEarthquakeUseCase registerEarthquake;
    private final GetEarthquakeUseCase getEarthquake;
    private final ListEarthquakesUseCase listEarthquakes;
    private final DeleteEarthquakeUseCase deleteEarthquake;

    public EarthquakeController(RegisterEarthquakeUseCase registerEarthquake,
                                GetEarthquakeUseCase getEarthquake,
                                ListEarthquakesUseCase listEarthquakes,
                                DeleteEarthquakeUseCase deleteEarthquake) {
        this.registerEarthquake = registerEarthquake;
        this.getEarthquake = getEarthquake;
        this.listEarthquakes = listEarthquakes;
        this.deleteEarthquake = deleteEarthquake;
    }

    @PostMapping
    public ResponseEntity<EarthquakeResponse> create(@Valid @RequestBody CreateEarthquakeRequest request) {
        var command = new RegisterEarthquakeCommand(
                request.magnitude(), request.depthKm(),
                request.latitude(), request.longitude(),
                request.source(), request.occurredAt());
        var saved = registerEarthquake.handle(command);
        return ResponseEntity
                .created(URI.create("/api/earthquakes/" + saved.id()))
                .body(EarthquakeResponse.from(saved));
    }

    @GetMapping("/{id}")
    public EarthquakeResponse getById(@PathVariable Long id) {
        return EarthquakeResponse.from(getEarthquake.handle(id));
    }

    @GetMapping
    public List<EarthquakeResponse> getAll() {
        return listEarthquakes.handle().stream().map(EarthquakeResponse::from).toList();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        deleteEarthquake.handle(id);
    }
}
