package org.ikigaidigital;

import org.ikigaidigital.application.TimeDepositRepository;
import org.ikigaidigital.application.TimeDepositService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.context.annotation.Bean;

// No /error controller or application-hosted documentation endpoints: the API has exactly two routes.
@SpringBootApplication(exclude = ErrorMvcAutoConfiguration.class)
public class TimeDepositApplication {
    public static void main(String[] args) {
        SpringApplication.run(TimeDepositApplication.class, args);
    }

    @Bean
    TimeDepositService timeDepositService(TimeDepositRepository repository) {
        return new TimeDepositService(repository, new TimeDepositCalculator());
    }
}
