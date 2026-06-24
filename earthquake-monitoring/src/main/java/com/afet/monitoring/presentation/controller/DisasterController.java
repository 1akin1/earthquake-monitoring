package com.afet.monitoring.presentation.controller;

import com.afet.monitoring.application.usecase.AssessDisasterCommand;
import com.afet.monitoring.application.usecase.AssessDisasterUseCase;
import com.afet.monitoring.domain.model.DisasterType;
import com.afet.monitoring.presentation.controller.dto.AssessDisasterRequest;
import com.afet.monitoring.presentation.controller.dto.DisasterAssessmentResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/** Thin adapter: parses the type string at the edge, delegates to the use case. */
@RestController
@RequestMapping("/api/disasters")
public class DisasterController {

    private final AssessDisasterUseCase assessDisaster;

    public DisasterController(AssessDisasterUseCase assessDisaster) {
        this.assessDisaster = assessDisaster;
    }

    @PostMapping("/assess")
    public DisasterAssessmentResponse assess(@Valid @RequestBody AssessDisasterRequest request) {
        var command = new AssessDisasterCommand(
                DisasterType.from(request.type()),   // string -> enum, fails fast if unknown
                request.intensity());
        return DisasterAssessmentResponse.from(assessDisaster.handle(command));
    }
}
