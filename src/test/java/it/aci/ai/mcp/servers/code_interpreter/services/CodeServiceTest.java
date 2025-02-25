package it.aci.ai.mcp.servers.code_interpreter.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import it.aci.ai.mcp.servers.code_interpreter.config.AppConfig;
import it.aci.ai.mcp.servers.code_interpreter.dto.Dependency;
import it.aci.ai.mcp.servers.code_interpreter.dto.ExecuteCodeRequest;
import it.aci.ai.mcp.servers.code_interpreter.dto.ExecuteCodeResult;
import it.aci.ai.mcp.servers.code_interpreter.dto.Language;

@SpringBootTest
class CodeServiceTest {

	@Autowired
	private AppConfig appConfig;
	@Autowired
	private CodeService codeService;

	@Test
	void whenJavaCodeWritesToStdout_thenReturnedStandardOutputShouldMatch() {

		String outputString = "Hello Java";
		String code = """
				package it.aci.ai.mcp.servers.code_interpreter;

				public class Main {

					public static void main(String[] args) {
						System.out.println("%s");
					}

				}""".formatted(outputString);

		ExecuteCodeResult executeCodeResult = codeService
				.executeCode(new ExecuteCodeRequest(Language.JAVA, code, null));

		assertEquals(outputString, executeCodeResult.stdOut().trim());

	}

	@Test
	void whenTypeScriptCodeWritesToStdout_thenReturnedStandardOutputShouldMatch() {

		Dependency expressDependency = new Dependency("express", "4.21.2");

		String outputString = "Hello Node";
		String code = """
				const express = require('express');

				console.log('%s');""".formatted(outputString);

		ExecuteCodeResult executeCodeResult = codeService
				.executeCode(new ExecuteCodeRequest(Language.TYPESCRIPT, code, null, expressDependency));

		assertTrue(executeCodeResult.stdOut().trim().endsWith(outputString));

	}

	@Test
	void whenPythonCodeWritesToStdout_thenReturnedStandardOutputShouldMatch() {

		Dependency pandasDependency = new Dependency("pandas", "==2.2.3");
		Dependency matplotlibDependency = new Dependency("matplotlib", "==3.10.0");

		String outputString = "Hello Python";
		String code = """
				import matplotlib.pyplot as plt

				fig, ax = plt.subplots()

				fruits = ['apple', 'blueberry', 'cherry', 'orange']
				counts = [40, 100, 30, 55]
				bar_labels = ['red', 'blue', '_red', 'orange']
				bar_colors = ['tab:red', 'tab:blue', 'tab:red', 'tab:orange']

				ax.bar(fruits, counts, label=bar_labels, color=bar_colors)

				ax.set_ylabel('fruit supply')
				ax.set_title('Fruit supply by kind and color')
				ax.legend(title='Fruit color')

				plt.savefig('%s/output/foo.png')

				print('%s')""".formatted(appConfig.getRemoteIoPath(), outputString);

		ExecuteCodeResult executeCodeResult = codeService
				.executeCode(
						new ExecuteCodeRequest(Language.PYTHON, code, null, pandasDependency, matplotlibDependency));

		assertTrue(executeCodeResult.stdOut().trim().endsWith(outputString));

	}

}
