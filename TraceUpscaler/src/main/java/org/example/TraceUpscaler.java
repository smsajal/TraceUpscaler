package scaling;

import entity.TraceLine;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.math3.random.RandomDataGenerator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class TspanRepeat {

    public List< Long > getNewArrivalTimes ( List< Long > oldArrivalTimes, float upscalingFactor ) {


        List< Long > newArrivalTimes = new ArrayList<> ( );
        int requiredSize = oldArrivalTimes.size ( );
        int whole = ( int ) ( Math.floor ( upscalingFactor ) );
        double fraction = upscalingFactor - whole;
        System.out.println ( "whole: " + whole + " fraction: " + fraction );

        int incrCounter = 0;
        int idx = 0;
        for ( Long x : oldArrivalTimes
        ) {

            idx++;
            int newRequestCount = whole;
            if ( new RandomDataGenerator ( ).nextUniform ( 0, 1 ) < fraction ) {

//                newRequestCount += ( int ) Math.ceil ( whole * fraction );
                newRequestCount++;
                incrCounter++;
            }
            List< Long > samples = new ArrayList<> ( Collections.nCopies ( newRequestCount, x ) );


            newArrivalTimes.addAll ( samples );

            if ( newArrivalTimes.size ( ) >= requiredSize ) {
                break;
            }
        }

        System.out.println ( "incr counter: " + incrCounter + " total_count: " + oldArrivalTimes.size ( ) + "  idx: " + idx );
        System.out.println ( "incr pct: " + ( ( incrCounter * 100.0 ) / ( oldArrivalTimes.size ( ) * 1.0 ) ) );
        newArrivalTimes = newArrivalTimes.subList ( 0, requiredSize );

        return newArrivalTimes;
    }

    public ArrayList< TraceLine > tspanRepeat ( ArrayList< TraceLine > traceLines, float upscalingFactor ) {


        List< Long > initialStartTimes = traceLines.stream ( ).map ( TraceLine :: getInitialStartTime ).collect ( Collectors.toList ( ) );

        List< Long > newInitialStartTimes = this.getNewArrivalTimes ( initialStartTimes, upscalingFactor );

        int i = 0;
        ArrayList< TraceLine > newTraceLines = new ArrayList<> ( );
        for ( TraceLine x : traceLines
        ) {
            newTraceLines.add ( SerializationUtils.clone ( x ) );
            TraceLine y = newTraceLines.get ( newTraceLines.size ( ) - 1 );
            y.setInitialStartTime ( newInitialStartTimes.get ( i ) );
            i++;
        }


        return newTraceLines;
    }
}
