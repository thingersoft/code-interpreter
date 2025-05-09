package it.aci.ai.mcp.servers.code_interpreter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import it.aci.ai.mcp.servers.code_interpreter.config.AppConfig;
import it.aci.ai.mcp.servers.code_interpreter.config.DockerConfig;

@SpringBootApplication
@EnableMongoRepositories
@EnableConfigurationProperties({ AppConfig.class, DockerConfig.class })
public class CodeInterpreterApplication {

	public static void main(String[] args) {
		SpringApplication.run(CodeInterpreterApplication.class, args);
	}

}
