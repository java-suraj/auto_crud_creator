package com.nit.controller;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nit.service.ModelClassGeneratorService;


@RestController
@RequestMapping("/model")
public class ClassController {

	@Value("${spring.datasource.url}")
	private String defaultDatasourceUrl;

	@Value("${spring.datasource.username}")
	private String defaultDatasourceUsername;

	@Value("${spring.datasource.password}")
	private String defaultDatasourcePassword;
	
	@Autowired
	private ModelClassGeneratorService service;

	@PostMapping("/create/{tableName}")
	public ResponseEntity<String> createModelClass(@PathVariable String tableName,
			@RequestParam(required = false) String sequenceName,
			@RequestParam(required = false) String datasourceProperties) throws IOException {
		String datasourceUrl = null;
		String datasourceUsername = null;
		String datasourcePassword = null;

		if (datasourceProperties != null) {
			String[] properties = datasourceProperties.split(" ");
			for (String property : properties) {
				if (property.startsWith("spring.datasource.url=")) {
					datasourceUrl = property.substring("spring.datasource.url=".length());
				} else if (property.startsWith("spring.datasource.username=")) {
					datasourceUsername = property.substring("spring.datasource.username=".length());
				} else if (property.startsWith("spring.datasource.password=")) {
					datasourcePassword = property.substring("spring.datasource.password=".length());
				}
			}
		}

		if (datasourceUrl == null) {
			datasourceUrl = defaultDatasourceUrl;
		}
		if (datasourceUsername == null) {
			datasourceUsername = defaultDatasourceUsername;
		}
		if (datasourcePassword == null) {
			datasourcePassword = defaultDatasourcePassword;
		}
		service.list.clear();
		return ResponseEntity.ok(service.callMethods(tableName, sequenceName, datasourceUrl, datasourceUsername, datasourcePassword));
	}
	
	@PostMapping("compress/create/{tableName}")
	public ResponseEntity<byte[]> downlaodModelClass(@PathVariable String tableName,
			@RequestParam(required = false) String sequenceName,
			@RequestParam(required = false) String datasourceProperties) throws IOException {
		String datasourceUrl = null;
		String datasourceUsername = null;
		String datasourcePassword = null;

		if (datasourceProperties != null) {
			String[] properties = datasourceProperties.split(" ");
			for (String property : properties) {
				if (property.startsWith("spring.datasource.url=")) {
					datasourceUrl = property.substring("spring.datasource.url=".length());
				} else if (property.startsWith("spring.datasource.username=")) {
					datasourceUsername = property.substring("spring.datasource.username=".length());
				} else if (property.startsWith("spring.datasource.password=")) {
					datasourcePassword = property.substring("spring.datasource.password=".length());
				}
			}
		}

		// Use default properties if user-provided properties are null
		if (datasourceUrl == null) {
			datasourceUrl = defaultDatasourceUrl;
		}
		if (datasourceUsername == null) {
			datasourceUsername = defaultDatasourceUsername;
		}
		if (datasourcePassword == null) {
			datasourcePassword = defaultDatasourcePassword;
		}
		service.list.clear();
	  return service.downlaodAllClass(tableName, sequenceName, datasourceUrl, datasourceUsername, datasourcePassword);
	}

}
