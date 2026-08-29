package br.com.jobsearch.JobClient;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public record AdzunaResult(String title,
                           String description,
                           @JsonProperty("redirect_url")String redirectUrl,
                           LocalDateTime created,
                           AdzunaCompany company,
                           AdzunaLocation location) {
}
