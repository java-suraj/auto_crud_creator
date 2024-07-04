package com.nit.service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ModelClassGeneratorService {

	public List<String> list = new ArrayList<>();
	
	public String callMethods(String tableName, String sequenceGenerator, String datasourceUrl,
			String datasourceUsername, String datasourcePassword) throws IOException {
		String entityClass = generateEntityClass(tableName, sequenceGenerator, datasourceUrl, datasourceUsername,
				datasourcePassword);
		String repositoryInterface = generateRepositoryInterface(tableName);
		String serviceClass = generateServiceClass(tableName);
		
		return  entityClass+"\n\n" + repositoryInterface + "\n\n" + serviceClass;
	}
	
	public ResponseEntity<byte[]>  downlaodAllClass(String tableName, String sequenceGenerator, String datasourceUrl,
			String datasourceUsername, String datasourcePassword) throws IOException {
		generateEntityClass(tableName, sequenceGenerator, datasourceUrl, datasourceUsername,datasourcePassword);
		generateRepositoryInterface(tableName);
		generateServiceClass(tableName);
		return compressFiles(list);
	}
	
	public String generateEntityClass(String tableName, String sequenceGenerator, String datasourceUrl,
			String datasourceUsername, String datasourcePassword) throws IOException {
		log.info("datasourceUrl :: {}",datasourceUrl);
		log.info("datasourceUsername :: {}",datasourceUsername);
		log.info("datasourcePassword :: {}",datasourcePassword);
		StringBuilder classContent = new StringBuilder();
		try (Connection connection = DriverManager.getConnection(datasourceUrl, datasourceUsername,
				datasourcePassword)) {
			DatabaseMetaData metaData = connection.getMetaData();
			ResultSet columns = metaData.getColumns(null, null, tableName, null);

			classContent.append("import javax.persistence.*;\n");
			classContent.append("import java.sql.*;\n"); // import for Blob, Clob
			classContent.append("import lombok.Data;\n");
			classContent.append("@Data\n");
			classContent.append("@Entity\n");
			classContent.append("@Table(name = \"").append(tableName.toUpperCase()).append("\")\n");
			classContent.append("public class ").append(toCamelCase(tableName.toLowerCase(), true)).append(" {\n");
			classContent.append("    @Id\n");
			if (sequenceGenerator != null) {
				String str = "    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = \"${SEQUENCE}\")\n    @SequenceGenerator(sequenceName = \"${SEQUENCE}\", allocationSize = 1, name = \"${SEQUENCE}\")\n";
				str = str.replace("${SEQUENCE}", sequenceGenerator);
				classContent.append(str);
			}

			while (columns.next()) {
				String columnName = columns.getString("COLUMN_NAME");
				String dataType = columns.getString("TYPE_NAME");
				String javaType = mapToJavaType(dataType);

				classContent.append("    private ").append(javaType).append(" ")
						.append(toCamelCase(columnName.toLowerCase(), false)).append(";\n");
			}

			classContent.append("}\n");
		} catch (SQLException e) {
			e.printStackTrace();
		}
		String className = toCamelCase(tableName.toLowerCase(), true);
		writeDataInFile(classContent.toString(),className );
		list.add(className+".java");
		return classContent.toString();
	}

	public String generateRepositoryInterface(String tableName) throws IOException {
		String className = toCamelCase(tableName.toLowerCase(), true);
		StringBuilder interfaceContent = new StringBuilder();
		interfaceContent.append("\nimport org.springframework.data.jpa.repository.JpaRepository;\n");
		interfaceContent.append("import org.springframework.stereotype.Repository;\n");
		interfaceContent.append("\n");
//        interfaceContent.append("@Repository\n");
		interfaceContent.append("public interface ").append(className).append("Repository extends JpaRepository<")
				.append(className).append(", Long> {\n\n\n");
		interfaceContent.append("}\n");
		writeDataInFile(interfaceContent.toString(), className+"Repository");
		list.add(className+"Repository.java");
		return interfaceContent.toString();
	}

	public String generateServiceClass(String tableName) throws IOException {
		String className = toCamelCase(tableName.toLowerCase(), true);
		StringBuilder classContent = new StringBuilder();
		String repoObjectName = toCamelCase(tableName.toLowerCase(), false) + "repository";
		classContent.append("import org.springframework.beans.factory.annotation.Autowired;\n");
		classContent.append("import org.springframework.stereotype.Service;\n");
		classContent.append("import java.util.List;\n");
		classContent.append("import java.util.Optional;\n");
		classContent.append("import org.modelmapper.ModelMapper;\n");
		classContent.append("import lombok.extern.slf4j.Slf4j;\n");
		classContent.append("\n");
		classContent.append("@Service\n");
		classContent.append("@Slf4j\n");
		classContent.append("public class ").append(className).append("Service {\n");
		classContent.append("\n");
		classContent.append("    @Autowired\n");
		classContent.append("    private ").append(className).append("Repository ").append(repoObjectName).append(";\n");
		classContent.append("\n");
		classContent.append("    @Autowired\n");
		classContent.append("    private ModelMapper modelMapper;\n");
		classContent.append("\n");
		// findAll method
		classContent.append("    public List<").append(className).append("> findAll() {\n");
		classContent.append("        try {\n");
		classContent.append("            return ").append(repoObjectName).append(".findAll();\n");
		classContent.append("        } catch (Exception e) {\n");
		classContent.append("            log.error(\"Error in findAll method: {}\", e.getMessage());\n");
		classContent.append("        }\n");
		classContent.append("    }\n");
		classContent.append("\n");
		// findById method
		classContent.append("    public Optional<").append(className).append("> findById(Long id) {\n");
		classContent.append("        try {\n");
		classContent.append("            return ").append(repoObjectName).append(".findById(id);\n");
		classContent.append("        } catch (Exception e) {\n");
		classContent.append("            log.error(\"Error in findById method: {}\", e.getMessage());\n");
		classContent.append("        }\n");
		classContent.append("    }\n");
		classContent.append("\n");
		// save method
		classContent.append("    public ").append(className).append(" save(").append(className).append(" entity) {\n");
		classContent.append("        try {\n");
		classContent.append("            return ").append(repoObjectName).append(".save(entity);\n");
		classContent.append("        } catch (Exception e) {\n");
		classContent.append("            log.error(\"Error in save method: {}\", e.getMessage());\n");
		classContent.append("        }\n");
		classContent.append("    }\n");
		classContent.append("\n");
		// update method
		classContent.append("    public ").append(className).append(" update(").append(className).append(" entity) {\n");
		classContent.append("        try {\n");
		classContent.append("            ").append(className).append(" existingEntity = ").append(repoObjectName).append(".findById(entity.getId()).orElse(null);\n");
		classContent.append("            if (existingEntity == null) {\n");
		classContent.append("                throw new RuntimeException(\"").append(className).append(" not found with id: \" + entity.getId());\n");
		classContent.append("            }\n");
		classContent.append("            modelMapper.map(entity, existingEntity);\n");
		classContent.append("            return ").append(repoObjectName).append(".save(existingEntity);\n");
		classContent.append("        } catch (Exception e) {\n");
		classContent.append("            log.error(\"Error in update method: {}\", e.getMessage());\n");
		classContent.append("        }\n");
		classContent.append("    }\n");
		classContent.append("\n");
		// deleteById method
		classContent.append("    public void deleteById(Long id) {\n");
		classContent.append("        try {\n");
		classContent.append("            ").append(repoObjectName).append(".deleteById(id);\n");
		classContent.append("        } catch (Exception e) {\n");
		classContent.append("            log.error(\"Error in deleteById method: {}\", e.getMessage());\n");
		classContent.append("        }\n");
		classContent.append("    }\n");
		classContent.append("}\n");
		writeDataInFile(classContent.toString(), className+"Service");
		list.add(className+"Service.java");
		return classContent.toString();
	}

	public String generatePayloadClass(String tableName, String datasourceUrl, String datasourceUsername,
			String datasourcePassword) throws IOException {
		StringBuilder classContent = new StringBuilder();
		try (Connection connection = DriverManager.getConnection(datasourceUrl, datasourceUsername,
				datasourcePassword)) {
			DatabaseMetaData metaData = connection.getMetaData();
			ResultSet columns = metaData.getColumns(null, null, tableName, null);

			classContent.append("public class ").append(toCamelCase(tableName.toLowerCase(), true))
					.append("Payload {\n");

			while (columns.next()) {
				String columnName = columns.getString("COLUMN_NAME");
				String dataType = columns.getString("TYPE_NAME");
				String javaType = mapToJavaType(dataType);

				classContent.append("    private ").append(javaType).append(" ")
						.append(toCamelCase(columnName.toLowerCase(), false)).append(";\n");
			}

			classContent.append("\n    // Getters and Setters\n");
			columns.beforeFirst();
			while (columns.next()) {
				String columnName = columns.getString("COLUMN_NAME");
				String javaType = mapToJavaType(columns.getString("TYPE_NAME"));
				String camelCaseName = toCamelCase(columnName.toLowerCase(), false);
				String capitalizedCamelCaseName = toCamelCase(columnName.toLowerCase(), true);

				classContent.append("    public ").append(javaType).append(" get").append(capitalizedCamelCaseName)
						.append("() {\n");
				classContent.append("        return ").append(camelCaseName).append(";\n");
				classContent.append("    }\n");
				classContent.append("\n");
				classContent.append("    public void set").append(capitalizedCamelCaseName).append("(").append(javaType)
						.append(" ").append(camelCaseName).append(") {\n");
				classContent.append("        this.").append(camelCaseName).append(" = ").append(camelCaseName)
						.append(";\n");
				classContent.append("    }\n");
				classContent.append("\n");
			}

			classContent.append("}\n");
		} catch (SQLException e) {
			e.printStackTrace();
		}
		writeDataInFile(classContent.toString(), toCamelCase(tableName.toLowerCase(), true));
		return classContent.toString();
	}

	private static String mapToJavaType(String sqlType) {
		switch (sqlType.toUpperCase()) {
		case "INT":
		case "INTEGER":
		case "NUMBER":
			return "Long";
		case "VARCHAR":
		case "VARCHAR2":
		case "CHAR":
		case "TEXT":
			return "String";
		case "DATE":
			return "Date";
		case "BOOLEAN":
			return "Boolean";
		case "BLOB":
			return "Blob";
		case "CLOB":
			return "Clob";
		default:
			return "Object";
		}
	}

	private static String toCamelCase(String input, boolean capitalizeFirst) {
		StringBuilder result = new StringBuilder();
		boolean capitalizeNext = capitalizeFirst;

		for (char c : input.toCharArray()) {
			if (c == '_') {
				capitalizeNext = true;
			} else {
				if (capitalizeNext) {
					result.append(Character.toUpperCase(c));
					capitalizeNext = false;
				} else {
					result.append(c);
				}
			}
		}
		return result.toString();
	}
	
	
	public static void writeDataInFile(String str, String fileName) throws IOException {
	    String directoryPath = "/SpringBoot/";
	    File directory = new File(directoryPath);
	    if (!directory.exists()) {
	        directory.mkdirs(); 
	    }

	    File file = new File(directoryPath + fileName + ".java");
	    try (FileOutputStream fos = new FileOutputStream(file)) {
	        fos.write(str.getBytes());
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
	}
	
	
	public ResponseEntity<byte[]> compressFiles(@RequestBody List<String> filePaths) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
    	    String directoryPath = "/SpringBoot/";
            try (ZipOutputStream zos = new ZipOutputStream(baos)) {
                for (String filePath : filePaths) {
                    File file = new File(directoryPath+filePath);
                    try (FileInputStream fis = new FileInputStream(file)) {
                        ZipEntry zipEntry = new ZipEntry(file.getName());
                        zos.putNextEntry(zipEntry);

                        byte[] buffer = new byte[1024];
                        int length;
                        while ((length = fis.read(buffer)) >= 0) {
                            zos.write(buffer, 0, length);
                        }
                        zos.closeEntry();
                    }
                }
            }

            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Disposition", "attachment; filename=files.zip");

            return new ResponseEntity<>(baos.toByteArray(), headers, HttpStatus.OK);
        } catch (IOException e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
