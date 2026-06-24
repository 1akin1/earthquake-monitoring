package com.afet.monitoring.application.annotation;

import org.springframework.stereotype.Service;
import java.lang.annotation.*;

/**
 * Marks an application use case. Meta-annotated with @Service so Spring picks it
 * up, while keeping the application layer's framework coupling to this one word.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Service
public @interface UseCase {}
