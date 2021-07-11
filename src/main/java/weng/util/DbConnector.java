package weng.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class DbConnector {
    public static final String DRIVER_CLASS_NAME = "com.mysql.cj.jdbc.Driver";
    public static final String DB_USERNAME = "bi_admin";
    public static final String DB_PASSWORD = "61181!BILab";
    public static final String PREFIX = "jdbc:mysql://140.112.106.212/";
    public static final String LITERATURE_YEAR = "biomedical_literature_by_year";
    public static final String SEMMED = "semmed_ver26";
    public static final String BIORELATION = "biomedical_relation";
    public static final String BIOCONCEPT = "biomedical_concept_mapping";
    public static final String LABELSYS_UMLS = "label_system_umls";

    private DataSource datasource;

    public DbConnector() {
        HikariConfig config = new HikariConfig();
        config.setDriverClassName(DRIVER_CLASS_NAME);
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("rewriteBatchedStatements", "true");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("serverTimezone", "UTC");
        config.addDataSourceProperty("useSSL", "false");
        config.setMaximumPoolSize(5);
        config.setJdbcUrl(PREFIX + LITERATURE_YEAR);
        config.setUsername(DB_USERNAME);
        config.setPassword(DB_PASSWORD);
        datasource = new HikariDataSource(config);
    }

    public Connection getConnectionByType(String schema) {
        Connection conn = null;
        try {
            conn = datasource.getConnection();
            if (!schema.equals(conn.getCatalog()))
                conn.setCatalog(schema);
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
        }
        return conn;
    }

    public Connection getSemMedConnection() {
        return getConnectionByType(SEMMED);
    }

    public Connection getLabelSystemUmlsConnection() {
        return getConnectionByType(LABELSYS_UMLS);
    }

    public Connection getLiteratureNodeConnection() {
        return getConnectionByType(LITERATURE_YEAR);
    }

    public Connection getBioRelationConnection() {
        return getConnectionByType(BIORELATION);
    }

    public Connection getBioConceptConnection() {
        return getConnectionByType(BIOCONCEPT);
    }
}
