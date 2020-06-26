package com.ververica.troubleshooting;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;
import org.apache.flink.api.common.restartstrategy.RestartStrategies;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.core.fs.Path;
import org.apache.flink.runtime.state.StateBackend;
import org.apache.flink.runtime.state.filesystem.FsStateBackend;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.DiscardingSink;
import org.apache.flink.util.FileUtils;

public class RocksDBMemLeak {

  public static void main(String[] args) throws Exception {
    ParameterTool parameters = ParameterTool.fromArgs(args);

    final boolean local = parameters.getBoolean("local", false);

    StreamExecutionEnvironment env = createConfiguredEnvironment(parameters, local);

    // Checkpointing Configuration
    env.enableCheckpointing(5000);
    //        env.getCheckpointConfig().enableExternalizedCheckpoints(
    //                CheckpointConfig.ExternalizedCheckpointCleanup.RETAIN_ON_CANCELLATION);
    env.getCheckpointConfig().setMinPauseBetweenCheckpoints(5000);

    DataStream<Tuple2<Integer, Double>> outstream =
        env.addSource(FakeSource.createSource())
            .name("FakeSource")
            .uid("FakeSource")
            .keyBy(SimpleMeasurement::getSensorId)
            .map(new MovingCountAverage(5_000_000))
            .name("MovingCountAverage")
            .uid("MovingCountAverage");

    if (local) {
      outstream.print().name("NormalOutput").uid("NormalOutput");
    } else {
      outstream.addSink(new DiscardingSink<>()).name("NormalOutput").uid("NormalOutput");
    }

    env.execute(RocksDBMemLeak.class.getSimpleName());
  }

  public static StreamExecutionEnvironment createConfiguredEnvironment(
      final ParameterTool parameters, final boolean local) throws IOException, URISyntaxException {
    StreamExecutionEnvironment env;
    if (local) {
      env = StreamExecutionEnvironment.createLocalEnvironmentWithWebUI(new Configuration());

      String statePath = parameters.get("fsStatePath");
      Path checkpointPath;
      if (statePath != null) {
        FileUtils.deleteDirectory(new File(new URI(statePath)));
        checkpointPath = Path.fromLocalFile(new File(new URI(statePath)));
      } else {
        checkpointPath = Path.fromLocalFile(Files.createTempDirectory("checkpoints").toFile());
      }

      StateBackend stateBackend = new FsStateBackend(checkpointPath);
      env.setStateBackend(stateBackend);
    } else {
      env = StreamExecutionEnvironment.getExecutionEnvironment();
    }

    env.setRestartStrategy(
        RestartStrategies.fixedDelayRestart(
            Integer.MAX_VALUE,
            org.apache.flink.api.common.time.Time.of(100, TimeUnit.MILLISECONDS) // delay
            ));
    return env;
  }
}
