package de.tuberlin.dima.aim3.exercises.streaming;

import de.tuberlin.dima.aim3.exercises.model.DebsFeature;
import org.apache.flink.api.common.functions.ReduceFunction;
import org.apache.flink.api.java.tuple.Tuple;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.core.fs.FileSystem;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.timestamps.AscendingTimestampExtractor;
import org.apache.flink.streaming.api.functions.windowing.AllWindowFunction;
import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.windowing.assigners.*;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.GlobalWindow;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;

import java.math.BigInteger;

/**
 * The implementation of {@link FootballStatistics} to perform analytics on DEBS dataset.
 *
 * @author Imran, Muhammad
 */
public class FootballStatisticsImpl implements FootballStatistics {
    private final String filePath;

    /*
           The StreamExecutionEnvironment is the context in which a streaming program is executed.
          */
    final StreamExecutionEnvironment STREAM_EXECUTION_ENVIRONMENT =
            StreamExecutionEnvironment.getExecutionEnvironment();

    /**
     * stream of events to be evaluated lazily
     */
    private DataStream<DebsFeature> events;

    /**
     * @param filePath dataset file path as a {@link String}
     */
    FootballStatisticsImpl(String filePath) {
        this.filePath = filePath;
    }

    /**
     * write the events that show that ball almost came near goal (within penalty area),
     * but the ball was kicked out of the penalty area by a player of the opposite team.
     */
    @Override
    public void writeAvertedGoalEvents() {
        events.filter(t-> t.getSensorId() == 4 || t.getSensorId() == 8 || t.getSensorId() == 10 || t.getSensorId() == 12)
                .keyBy(t -> t.getSensorId()) // we need to split window calculations per ball so that the different ball sensors do not conflict with each other
                .window(TumblingEventTimeWindows.of(Time.minutes(1)))
                .process(new CalculateAvertedGoals())
                .windowAll(TumblingEventTimeWindows.of(Time.minutes(1)))
                .apply(new AllWindowFunction<Tuple2<String, Long>, Tuple2<String,Long>, TimeWindow>() {
                    @Override
                    public void apply(TimeWindow timeWindow, Iterable<Tuple2<String, Long>> iterable, Collector<Tuple2<String, Long>> collector) throws Exception {
                        long sumA = 0;
                        long sumB = 0;
                        for (Tuple2<String,Long> t : iterable){
                            String team = t.f0;
                            if(team.equals("TeamA")){
                                sumA += t.f1;
                            } else if (team.equals("TeamB")){
                                sumB += t.f1;
                            }
                        }
                        collector.collect(new Tuple2<>("TeamA",sumA));
                        collector.collect(new Tuple2<>("TeamB",sumB));
                    }
                })
                .writeAsCsv("averted.csv", FileSystem.WriteMode.OVERWRITE)
                .setParallelism(1);

        try {
            STREAM_EXECUTION_ENVIRONMENT.execute("writeAvertedGoalEvents");
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    /**
     * Highest average distance of player A1 (of Team A) ran in every 5 minutes duration. You can skip 1 minute duration between every two durations.
     */
    @Override
    public void writeHighestAvgDistanceCovered() {
        events.filter(t -> t.getSensorId() == 47 )
                .windowAll(SlidingEventTimeWindows.of(Time.minutes(5), Time.minutes(1)))
                .process(new CalculateAverageDistance())
                .writeAsCsv("highestAvg.csv", FileSystem.WriteMode.OVERWRITE)
                .setParallelism(1);

        // Implementation for taking into account both A1 player legs
//        events.filter(t -> t.getSensorId() == 47 || t.getSensorId() == 16)
//                .keyBy(t -> t.getSensorId())
//                .window(SlidingEventTimeWindows.of(Time.minutes(5), Time.minutes(1)))
//                .process(new CalculateAverageDistance())
//                .keyBy(0)
//                .reduce(new ReduceFunction<Tuple3<Long, Long, Double>>() {
//                    @Override
//                    public Tuple3<Long, Long, Double> reduce(Tuple3<Long, Long, Double> v1, Tuple3<Long, Long, Double> v2) throws Exception {
////                        System.out.println("First tuple3: \t" + v1.toString());
////                        System.out.println("Second tuple3: \t" + v2.toString());
////                        System.out.println("Result: \t" +  v1.f0 + " " +  v1.f1 + " "+ (v1.f2 + v2.f2) / 2);
//                        return new Tuple3<>(v1.f0, v1.f1, (v1.f2 + v2.f2) / 2);
//                    }
//                })
//                .reduce( (e,t) -> (e.f2 + t.f2) / 2) // calculate average for the two incoming events
//                .writeAsCsv("highestAvg.csv", FileSystem.WriteMode.OVERWRITE)
//                .setParallelism(1);

        try {
            STREAM_EXECUTION_ENVIRONMENT.execute("writeHighestAvgDistanceCovered");
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    /**
     * Creates {@link StreamExecutionEnvironment} and {@link DataStream} before each streaming task
     * to initialize a stream from the beginning.
     */
    @Override
    public void initStreamExecEnv() {

         /*
          Setting the default parallelism of our execution environment.
          Feel free to change it according to your environment/number of operators, sources, and sinks you would use.
          However, it should not have any impact on your results whatsoever.
         */
        STREAM_EXECUTION_ENVIRONMENT.setParallelism(1);

        /*
          Event time is the time that each individual event occurred on its producing device.
          This time is typically embedded within the records before they enter Flink.
         */
        STREAM_EXECUTION_ENVIRONMENT.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);

        /*
          Reads the file as a text file.
         */
        DataStream<String> dataStream = STREAM_EXECUTION_ENVIRONMENT.readTextFile(filePath);

        /*
          Creates DebsFeature for each record.
          ALTERNATIVELY You can use Tuple type (For that you would need to change generic type of 'event').
         */
        events = dataStream.map(DebsFeature::fromString);

        events = events.assignTimestampsAndWatermarks(new AscendingTimestampExtractor<DebsFeature>() {

            @Override
            public long extractAscendingTimestamp(DebsFeature element) {
                return  element.getTimeStamp().divide(new BigInteger(String.valueOf(1000000000))).longValue(); //dividing by 1B in order to convert pico to milliseconds
            }
        });
    }
}
