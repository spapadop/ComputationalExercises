package de.tuberlin.dima.aim3.exercises.streaming;

import de.tuberlin.dima.aim3.exercises.model.DebsFeature;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;

import java.io.IOException;

public class CalculateAvertedGoals extends ProcessWindowFunction<DebsFeature, Tuple2<String,Long>, Long, TimeWindow> {

    private transient ValueState<Double> runningSumA;
    private transient ValueState<Double> runningSumB;


    @Override
    public void process(Long aLong, Context context, Iterable<DebsFeature> iterable, Collector<Tuple2<String,Long>> collector) throws Exception {

        init(context);

        Long sumA = 0L;
        Long sumB = 0L;
        boolean insideA = false;
        boolean insideB = false;

        for(DebsFeature event : iterable){

            //Check if in this event the ball is inside the TeamA penalty area
            if (isInsideTeamAPenaltyArea(event)){
                insideA = true;
            } else {
                if(insideA) { //this event is not in TeamA penalty area, so if it was previously...
                    sumA++; // we increase the window's sum
                    insideA = false; // and update the flag, indicating that the ball is now out
                }
            }

            //Check if in this event the ball is inside the TeamB penalty area
            if(isInsideTeamBPenaltyArea(event)){
                insideB = true;
            } else {
                if(insideB) { //this event is not in TeamB penalty area, so if it was previously...
                    sumB++; // we increase the window's sum
                    insideB = false; // and update the flag, indicating that the ball is now out
                }
            }

        }

        //At the end of the window we increase the moving sums for each team.
        //It is preferable to update the state only once at the end, in order to avoid additional overhead.
        runningSumA.update(runningSumA.value().doubleValue() + sumA);
        runningSumB.update(runningSumB.value().doubleValue() + sumB);

        // if we have reached last window, output the moving sums for each ball sensor
        if(context.window().getEnd() >= 14940000L){ // 14940000 is last window's endTime - 11040000L for testing
            collector.collect(new Tuple2<>("TeamA", runningSumA.value().longValue()));
            collector.collect(new Tuple2<>("TeamB", runningSumB.value().longValue()));
        }


        //Alternative case: Output continuously the current sum for each team and each window, so that we aggregate later
//        collector.collect(new Tuple2<>("TeamA", sumA));
//        collector.collect(new Tuple2<>("TeamB", sumB));

        //Alternative case: Output continuously the running sum for each team
//        collector.collect(new Tuple2<>("TeamA", runningSumA.value().longValue()));
//        collector.collect(new Tuple2<>("TeamB", runningSumB.value().longValue()));


        //Testing the window calculations and output
//        System.out.println("==================================");
//        System.out.println(aLong.intValue());
//        System.out.println("winStart: " + context.window().getStart());
//        System.out.println("winEnd: " + context.window().getEnd());
//        System.out.println("RunningSumA\t" + runningSumA.value().longValue());
//        System.out.println("RunningSumB\t" + runningSumB.value().longValue());

    }


    /**
     * Checks if the ball is inside the Team A penalty area
     * @param record
     * @return
     */
    private boolean isInsideTeamAPenaltyArea(DebsFeature record){
        int x = record.getPositionX();
        int y = record.getPositionY();

        if(x > 6300 && x < 46183 && y > 15940 && y < 33940){
            return true;
        } else {
            return false;
        }
    }

    /**
     * Checks if the ball is inside the Team B penalty area
     * @param record
     * @return
     */
    private boolean isInsideTeamBPenaltyArea(DebsFeature record){
        int x = record.getPositionX();
        int y = record.getPositionY();

        if(x > 6300 && x < 46183 && y > -33968 && y < -15965){
            return true;
        } else {
            return false;
        }
    }

    /**
     * Initialize the state variables (running sums)
     * @param context
     * @throws IOException
     */
    private void init(Context context) throws IOException {
        runningSumA = context
                .globalState()
                .getState(new ValueStateDescriptor<>("TeamA",
                        TypeInformation.of(new TypeHint<Double>(){})));

        runningSumB = context
                .globalState()
                .getState(new ValueStateDescriptor<>("TeamB",
                        TypeInformation.of(new TypeHint<Double>(){})));

        if(runningSumA.value() == null){
            runningSumA.update(new Double(0));
        }
        if(runningSumB.value() == null){
            runningSumB.update(new Double(0));
        }
    }
}
