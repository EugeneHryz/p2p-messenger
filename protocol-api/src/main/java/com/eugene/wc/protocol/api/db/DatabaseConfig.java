package com.eugene.wc.protocol.api.db;

import java.io.File;

public class DatabaseConfig {

    private final File dbKeyDir;

    private final File dbDir;

    public DatabaseConfig(File dbKeyDir, File dbDir) {
        this.dbKeyDir = dbKeyDir;
        this.dbDir = dbDir;
    }

    public File getDatabaseKeyDirectory() {
        return dbKeyDir;
    }

    public File getDatabaseDirectory() {
        return dbDir;
    }
}
