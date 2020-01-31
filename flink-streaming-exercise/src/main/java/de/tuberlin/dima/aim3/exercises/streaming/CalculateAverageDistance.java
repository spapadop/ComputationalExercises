package de.tuberlin.dima.aim3.exercises.streaming;

import de.tuberlin.dima.aim3.exercises.model.DebsFeature;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.commons.math3.ml.distance.EuclideanDistance;
import org.apache.flink.streaming.api.functions.windowing.ProcessAllWindowFunction;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;

public class CalculateAverageDistance extends ProcessAllWindowFunction<DebsFeature, Tuple3<Long, Long, Double>, TimeWindow> {
    final EuclideanDistance distance = new EuclideanDistance();
    private transient ValueState<Tuple3<Long, Long, Double>> maxAvgWindow; //keeping moving average in state

    @Override
    public void process(Context context, Iterable<DebsFeature> iterable, Collector<Tuple3<Long, Long, Double>> collector) throws Exception {

        maxAvgWindow = context.globalState().getState(new ValueStateDescriptor<>("maximumAverage",TypeInformation.of(new TypeHint<Tuple3<Long,Long,Double>>(){})));

        //Take the first event of the window and set it as previousEvent for the calculations
        DebsFeature startingEvent = iterable.iterator().next();
        double[] previousEvent = new double[3];
        previousEvent[0] = startingEvent.getPositionX();
        previousEvent[1] = startingEvent.getPositionY();
        previousEvent[2] = startingEvent.getPositionZ();

        double result = 0; //the final average distance result
        long count = 0; // number of events in the window, in order to divide result to get average
        for (DebsFeature event : iterable){

            count++;
            double[] eventCoords = new double[3];
            eventCoords[0] = event.getPositionX();
            eventCoords[1] = event.getPositionY();
            eventCoords[2] = event.getPositionZ();
            result += distance.compute(previousEvent,eventCoords);
            previousEvent[0] = eventCoords[0];
            previousEvent[1] = eventCoords[1];
            previousEvent[2] = eventCoords[2];
        }

        result = result / count; //take the average distance for the events of the window
        Tuple3<Long,Long,Double> currentTuple = new Tuple3<>(context.window().getStart(), context.window().getEnd(), result);

        // update the running max average, in case our current window value surpass it
        if(maxAvgWindow.value() == null || maxAvgWindow.value().f2.compareTo(result) < 0){
            maxAvgWindow.update(currentTuple);
        }

        //output the current tuple details (start, end, averageOfTheWindow)
//        collector.collect(currentTuple);

        // last window - time to output the max average window
        if(currentTuple.f1.compareTo(15180000L) >= 0){
            collector.collect(new Tuple3<>(maxAvgWindow.value().f0, maxAvgWindow.value().f1, maxAvgWindow.value().f2));
        }

    }
}
