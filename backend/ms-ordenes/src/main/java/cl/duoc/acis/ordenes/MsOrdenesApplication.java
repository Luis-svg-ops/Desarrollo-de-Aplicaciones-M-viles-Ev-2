package cl.duoc.acis.ordenes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Microservicio de órdenes de trabajo de ACIS: http://localhost:8091 */
@SpringBootApplication
public class MsOrdenesApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsOrdenesApplication.class, args);
    }
}
