// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.install;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

@Slf4j
public abstract class SqlAbstractDatabaseSchemaService implements DatabaseSchemaService {

    protected static final String SQL_DIR = "sql";

    @Value("${spring.datasource.url}")
    protected String dbUrl;

    @Value("${spring.datasource.username}")
    protected String dbUserName;

    @Value("${spring.datasource.password}")
    protected String dbPassword;

    @Autowired
    protected InstallScripts installScripts;

    private final String schemaSql;
    private final String schemaIdxSql;

    protected SqlAbstractDatabaseSchemaService(String schemaSql, String schemaIdxSql) {
        this.schemaSql = schemaSql;
        this.schemaIdxSql = schemaIdxSql;
    }

    @Override
    public void createDatabaseSchema() throws Exception {
        this.createDatabaseSchema(true);
    }

    @Override
    public void createDatabaseSchema(boolean createIndexes) throws Exception {
        log.info("Installing SQL DataBase schema part: " + schemaSql);
        executeQueryFromFile(schemaSql);

        if (createIndexes) {
            this.createDatabaseIndexes();
        }
    }

    @Override
    public void createDatabaseIndexes() throws Exception {
        if (schemaIdxSql != null) {
            log.info("Installing SQL DataBase schema indexes part: " + schemaIdxSql);
            executeQueryFromFile(schemaIdxSql);
        }
    }

    void executeQueryFromFile(String schemaIdxSql) throws SQLException, IOException {
        Path schemaIdxFile = Paths.get(installScripts.getDataDir(), SQL_DIR, schemaIdxSql);
        String sql;
        if (Files.exists(schemaIdxFile)) {
            sql = Files.readString(schemaIdxFile);
        } else {
            try {
                sql = loadSqlFromClasspath(SQL_DIR + "/" + schemaIdxSql);
            } catch (Exception e) {
                Path fallback = findFallbackPath(SQL_DIR, schemaIdxSql);
                if (fallback != null && Files.exists(fallback)) {
                    sql = Files.readString(fallback);
                } else {
                    throw new IOException("Schema file not found on disk at " + schemaIdxFile + " or on classpath", e);
                }
            }
        }
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUserName, dbPassword)) {
            conn.createStatement().execute(sql); //NOSONAR, ignoring because method used to load initial thingsboard database schema
        }
    }

    private String loadSqlFromClasspath(String resourcePath) throws IOException {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IOException("Resource not found on classpath: " + resourcePath);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private Path findFallbackPath(String subDir, String fileName) {
        String workDir = System.getProperty("user.dir");
        if (workDir == null) {
            return null;
        }
        Path path1 = Paths.get(workDir, "dao", "src", "main", "resources", subDir, fileName);
        if (Files.exists(path1)) {
            return path1;
        }
        Path path2 = Paths.get(workDir, "..", "dao", "src", "main", "resources", subDir, fileName);
        if (Files.exists(path2)) {
            return path2;
        }
        return null;
    }

    protected void executeQuery(String query) {
        executeQuery(query, null);
    }

    protected void executeQuery(String query, String logQuery) {
        logQuery = logQuery != null ? logQuery : query;
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUserName, dbPassword)) {
            conn.createStatement().execute(query); //NOSONAR, ignoring because method used to execute thingsboard database upgrade script
            log.info("Successfully executed query: {}", logQuery);
            Thread.sleep(5000);
        } catch (InterruptedException | SQLException e) {
            throw new RuntimeException("Failed to execute query: " + logQuery, e);
        }
    }

}
