package com.nailsalon.booking;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.nailsalon.booking.mapper")
public class NailSalonBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(NailSalonBackendApplication.class, args);
	}

}
