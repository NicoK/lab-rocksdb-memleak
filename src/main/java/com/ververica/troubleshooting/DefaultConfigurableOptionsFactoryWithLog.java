package com.ververica.troubleshooting;

import org.apache.flink.configuration.ConfigOption;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.MemorySize;
import org.apache.flink.contrib.streaming.state.DefaultConfigurableOptionsFactory;

import org.rocksdb.DBOptions;
import org.rocksdb.InfoLogLevel;

import java.util.Collection;
import java.util.Optional;

import static org.apache.flink.configuration.ConfigOptions.key;

public class DefaultConfigurableOptionsFactoryWithLog extends DefaultConfigurableOptionsFactory {

 private static final long serialVersionUID = 1L;
    private String dbLogDir = "/flink/log/";

    @Override
    public DBOptions createDBOptions(DBOptions currentOptions,
                                     Collection<AutoCloseable> handlesToClose) {
        currentOptions = super.createDBOptions(currentOptions, handlesToClose);

        currentOptions.setInfoLogLevel(InfoLogLevel.INFO_LEVEL);
        currentOptions.setStatsDumpPeriodSec(60);
        currentOptions.setDbLogDir(dbLogDir);

        return currentOptions;
    }

    @Override
    public String toString() {
        return "DefaultConfigurableOptionsFactoryWithLog{" +
                super.toString() +
                '}';
    }

    public void setDbLogDir(String dbLogDir) {
        this.dbLogDir = dbLogDir;
    }
}
