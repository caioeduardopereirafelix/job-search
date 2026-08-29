package br.com.jobsearch.JobClient;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AdzunaCompany(
        @JsonProperty("display_name")String displayName) {
}
