package cl.duoc.acis.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Microservicio de usuarios de ACIS: http://localhost:8090 */
@SpringBootApplication
public class MsAuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsAuthApplication.class, args);
    }
}
