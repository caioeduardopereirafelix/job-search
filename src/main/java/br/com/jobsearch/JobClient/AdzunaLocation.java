package br.com.jobsearch.JobClient;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AdzunaLocation(
        @JsonProperty("display_name") String location) {
}
