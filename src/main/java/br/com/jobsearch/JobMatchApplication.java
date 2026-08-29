package br.com.jobsearch;

import br.com.jobsearch.Domain.Job;
import br.com.jobsearch.JobClient.JobSourceClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.List;

@SpringBootApplication
public class JobMatchApplication {

	public static void main(String[] args) {
		SpringApplication.run(JobMatchApplication.class, args);
	}

}
