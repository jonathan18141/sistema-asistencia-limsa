package pe.uch.intro_spring_mvc;

import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;

@SpringBootApplication
@EntityScan("pe.uch.intro_spring_mvc.model")
public class IntroSpringMvcApplication {

	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(IntroSpringMvcApplication.class);

		// Valores de respaldo apuntando a TiDB Cloud (B&B LIMSA S.A.C.).
		app.setDefaultProperties(Map.of(
			"server.port", "8082",
			"spring.datasource.url", "jdbc:mysql://gateway01.us-east-1.prod.aws.tidbcloud.com:4000/asistencias"
					+ "?useSSL=true&serverTimezone=UTC&enabledTLSProtocols=TLSv1.2,TLSv1.3",
			"spring.datasource.username", "QRJ26bqPUNtc1xg.root",
			"spring.datasource.password", "qzSV2cOuX2lfS7oO",
			"spring.jpa.hibernate.ddl-auto", "update",
			"spring.jpa.open-in-view", "false",
			"spring.servlet.multipart.max-file-size", "8MB",
			"spring.servlet.multipart.max-request-size", "9MB"
		));

		app.run(args);
	}
}