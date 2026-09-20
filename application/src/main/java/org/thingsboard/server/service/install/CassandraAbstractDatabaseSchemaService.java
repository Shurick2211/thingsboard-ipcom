// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.install;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.thingsboard.server.dao.cassandra.CassandraInstallCluster;
import org.thingsboard.server.service.install.cql.CQLStatementsParser;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Slf4j
public abstract class CassandraAbstractDatabaseSchemaService implements DatabaseSchemaService {

    private static final String CASSANDRA_DIR = "cassandra";
    private static final String CASSANDRA_STANDARD_KEYSPACE = "thingsboard";

    @Autowired
    @Qualifier("CassandraInstallCluster")
    private CassandraInstallCluster cluster;

    @Autowired
    private InstallScripts installScripts;

    @Value("${cassandra.keyspace_name}")
    private String keyspaceName;

    private final String schemaCql;

    protected CassandraAbstractDatabaseSchemaService(String schemaCql) {
        this.schemaCql = schemaCql;
    }

    @Override
    public void createDatabaseSchema() throws Exception {
        this.createDatabaseSchema(true);
    }

    @Override
    public void createDatabaseSchema(boolean createIndexes) throws Exception {
        log.info("Installing Cassandra DataBase schema part: " + schemaCql);
        Path schemaFile = getCqlFilePath(schemaCql);
        loadCql(schemaFile);
    }

    @Override
    public void createDatabaseIndexes() throws Exception {
    }

    private Path getCqlFilePath(String schemaCql) throws IOException {
        Path schemaFile = Paths.get(installScripts.getDataDir(), CASSANDRA_DIR, schemaCql);
        if (Files.exists(schemaFile)) {
            return schemaFile;
        }
        Path fallbackFile = findFallbackPath(CASSANDRA_DIR, schemaCql);
        if (fallbackFile != null && Files.exists(fallbackFile)) {
            return fallbackFile;
        }
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(CASSANDRA_DIR + "/" + schemaCql)) {
            if (is != null) {
                Path tempFile = Files.createTempFile("cassandra_schema_", "_" + schemaCql);
                tempFile.toFile().deleteOnExit();
                Files.write(tempFile, is.readAllBytes());
                return tempFile;
            }
        } catch (Exception e) {
            log.warn("Failed to load CQL from classpath fallback", e);
        }
        return schemaFile;
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

    private void loadCql(Path cql) throws Exception {
        List<String> statements = new CQLStatementsParser(cql).getStatements();
        statements.forEach(statement -> cluster.getSession().execute(getCassandraKeyspaceName(statement)));
    }

    private String getCassandraKeyspaceName(String statement) {
        return statement.replaceFirst(CASSANDRA_STANDARD_KEYSPACE, keyspaceName);
    }
}
