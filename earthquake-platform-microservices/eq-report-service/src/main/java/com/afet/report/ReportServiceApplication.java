package com.afet.report;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Independent microservice: the reporting reaction, split out of the monolith's Observer #2. */
@SpringBootApplication
public class ReportServiceApplication {
    public static void main(String[] args) { SpringApplication.run(ReportServiceApplication.class, args); }
}
