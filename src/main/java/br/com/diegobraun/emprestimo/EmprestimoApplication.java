package br.com.diegobraun.emprestimo;

import org.camunda.bpm.spring.boot.starter.annotation.EnableProcessApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableProcessApplication
public class EmprestimoApplication {

    public static void main(String[] args) {
        SpringApplication.run(EmprestimoApplication.class, args);
    }
}
