package com.eugene.wc.protocol.db;

import com.eugene.wc.protocol.api.crypto.SecretKey;
import com.eugene.wc.protocol.api.db.DatabaseConfig;
import com.eugene.wc.protocol.api.db.exception.DbException;
import com.eugene.wc.protocol.api.sync.MessageFactory;
import com.eugene.wc.protocol.api.util.IoUtils;
import com.eugene.wc.protocol.api.util.StringUtils;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import javax.inject.Inject;

public class H2Database extends AbstractJdbcDatabase {

    private static final String STRING_TYPE = "VARCHAR";
    private static final String HASH_TYPE = "BINARY(32)";
    private static final String BINARY_TYPE = "BINARY";
    private static final String COUNTER_TYPE = "INTEGER NOT NULL AUTO_INCREMENT";

    private static final DatabaseTypes dbTypes = new DatabaseTypes(STRING_TYPE, HASH_TYPE,
            BINARY_TYPE, COUNTER_TYPE);

    private final DatabaseConfig dbConfig;
    private final String url;
    private SecretKey secretKey;

    @Inject
    public H2Database(DatabaseConfig dbConfig, MessageFactory messageFactory) {
        super(dbTypes, messageFactory);

        this.dbConfig = dbConfig;
        File dir = dbConfig.getDatabaseDirectory();
        String path = new File(dir, "db").getAbsolutePath();
        url = "jdbc:h2:split:" + path + ";CIPHER=AES;WRITE_DELAY=0";
    }

    @Override
    protected Connection createConnection() throws SQLException {
        Properties props = new Properties();
        props.setProperty("user", "root");
        String hexKey = StringUtils.toHexString(secretKey.getBytes());
        props.setProperty("password", hexKey + " qKPp371qa9");

        return DriverManager.getConnection(url, props);
    }

    @Override
    public boolean open(SecretKey key) throws DbException {
        this.secretKey = key;

        File dir = dbConfig.getDatabaseDirectory();
        boolean alreadyExists = !IoUtils.isDirectoryEmpty(dir);
        if (!alreadyExists) {
            boolean createdDbDir = dir.mkdirs();
        }
        super.open("org.h2.Driver", alreadyExists);
        return alreadyExists;
    }

    @Override
    public void close() throws DbException {
        try {
            super.closeAllConnections();
        } catch (SQLException e) {
            logger.warning("Unable to close all connections " + e);
            throw new DbException("Unable to close all connections", e);
        }
    }
}
