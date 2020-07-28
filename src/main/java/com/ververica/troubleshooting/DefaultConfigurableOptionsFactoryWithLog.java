package com.ververica.troubleshooting;

import org.apache.flink.configuration.ConfigOption;
import org.apache.flink.configuration.ReadableConfig;
import org.apache.flink.contrib.streaming.state.DefaultConfigurableOptionsFactory;

import org.rocksdb.DBOptions;
import org.rocksdb.InfoLogLevel;

import java.util.Collection;

import static org.apache.flink.configuration.ConfigOptions.key;

@SuppressWarnings("unused")
public class DefaultConfigurableOptionsFactoryWithLog extends DefaultConfigurableOptionsFactory {

 private static final long serialVersionUID = 1L;

    public static final ConfigOption<String> LOG_DIR =
            key("state.backend.rocksdb.log.dir")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("RocksDB log directory");

    private String dbLogDir = "";

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

    @Override
    public DefaultConfigurableOptionsFactoryWithLog configure(ReadableConfig configuration) {
        DefaultConfigurableOptionsFactoryWithLog optionsFactory =
                (DefaultConfigurableOptionsFactoryWithLog) super.configure(configuration);

        this.dbLogDir = configuration.get(LOG_DIR);

        return optionsFactory;
    }
}
