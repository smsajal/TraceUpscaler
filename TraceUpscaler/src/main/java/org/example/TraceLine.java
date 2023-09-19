package entity;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import utility.FileUtility;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Entity class for each traceline
 */
public class TraceLine implements Serializable {

    private int lineNumber;
    private long initialStartTime, actualStartTime;
    private int userNumber;


    private ArrayList< Request > reqList;
//	private ArrayList< WebRequest > reqList;


    private int currentReqIdx;
    private long currentReqStartTime;

    private long endTime;
    private String reqSizeMappingString;

    public TraceLine ( ) {

        this.reqList = new ArrayList<> ( );


    }

    public TraceLine ( int lineNumber, long initialStartTime, int userNumber, ArrayList< Request > reqList, int currentReqIdx, long currentReqStartTime ) {
        this.lineNumber = lineNumber;
        this.initialStartTime = initialStartTime;
        this.actualStartTime = 0;
        this.userNumber = userNumber;
        this.reqList = reqList;
        this.currentReqIdx = currentReqIdx;
        this.currentReqStartTime = currentReqStartTime;


    }

    public TraceLine ( int lineNumber, long initialStartTime, int userNumber, ArrayList< Request > reqList ) {
        this.lineNumber = lineNumber;
        this.initialStartTime = initialStartTime;
        this.actualStartTime = 0;
        this.userNumber = userNumber;
        this.reqList = reqList;

    }


    private void loadReqSizeMapping ( ) {

        String reqSizeMappingJson = "/Users/sxs2561/Documents/PyCharmProjects/trace_upscaler_utilities/src/files/requestSizeMapping.json";

//		String reqSizeMappingJson="src/main/java/entity/requestSizeMapping.json";

        this.reqSizeMappingString = new FileUtility ( ).readJsonObject ( reqSizeMappingJson ).toString ( );
    }

    public long getEndTime ( ) {
        return endTime;
    }

    public void setEndTime ( long endTime ) {
        this.endTime = endTime;
    }

    public int getLineNumber ( ) {
        return lineNumber;
    }

    public void setLineNumber ( int lineNumber ) {
        this.lineNumber = lineNumber;
    }

    public long getInitialStartTime ( ) {
        return initialStartTime;
    }

    public void setInitialStartTime ( long initialStartTime ) {
        this.initialStartTime = initialStartTime;
    }

    public long getActualStartTime ( ) {
        return actualStartTime;
    }

    public void setActualStartTime ( long actualStartTime ) {
        this.actualStartTime = actualStartTime;
    }

    public int getUserNumber ( ) {
        return userNumber;
    }

    public void setUserNumber ( int userNumber ) {
        this.userNumber = userNumber;
    }

    public ArrayList< Request > getReqList ( ) {
        return reqList;
    }

    public void setReqList ( ArrayList< Request > reqList ) {
        this.reqList = reqList;
    }

    public int getCurrentReqIdx ( ) {
        return currentReqIdx;
    }

    public void setCurrentReqIdx ( int currentReqIdx ) {
        this.currentReqIdx = currentReqIdx;
    }

    public long getCurrentReqStartTime ( ) {
        return currentReqStartTime;
    }

    public void setCurrentReqStartTime ( long currentReqStartTime ) {
        this.currentReqStartTime = currentReqStartTime;
    }

    /**
     * @param s= content of one line of tracefile ie a traceline content
     */

    public void loadFromString ( String s ) {

        //Format: startTime;;;userNumber;;;endTime;;;r1;;r2;;r3
        String[] traceLine = s.split ( ";;;" );


//        for ( String x : traceLine ) {
//            System.out.println ( x );
//        }
//        System.out.println ( "??????????????????????????????????????????" );
        this.setInitialStartTime ( Long.parseLong ( traceLine[ 0 ] ) );
        this.setCurrentReqStartTime ( Long.parseLong ( traceLine[ 0 ] ) );

        this.setUserNumber ( Integer.parseInt ( traceLine[ 2 ] ) );
        this.setEndTime ( Long.parseLong ( traceLine[ 3 ] ) );
        this.setCurrentReqIdx ( 0 );
        //fixme: did not initialize lineNumber. Do we keep this variable?

        String[] requestList = traceLine[ 4 ].split ( ";;" );
//        for ( String x : requestList ) {
//            System.out.println ( x );
//        }
//        System.out.println ( "??????????????????????????????????????????" );

        //todo: make an if-else block to handle different request types
        //Format of each Request: requestType + ";" + parameter + ";" + thinkTime

        for ( String value : requestList ) {
            String[] request = value.split ( ";" );

            //fixme: the WebRequest class is specific for server. Need to generalize it to Request using the Request interface
            String requestString = request[ 0 ];
//			System.out.println ( "value: " + value );
            try {
                if ( requestString.equalsIgnoreCase ( "READ" ) || requestString.equalsIgnoreCase ( "WRITE" ) ) {

                    BlockDeviceRequest blockDeviceRequest = new BlockDeviceRequest ( RequestType.valueOf ( requestString ), Long.parseLong ( request[ 2 ] ), Integer.parseInt ( request[ 1 ] ) );

                    this.reqList.add ( blockDeviceRequest );

                } else if ( requestString.equalsIgnoreCase ( "HOME_TIMELINE" ) || requestString.equalsIgnoreCase ( "USER_TIMELINE" ) ) {

                    DStarSocMedRequest dStarSocMedRequest = new DStarSocMedRequest ( RequestType.valueOf ( requestString ), Integer.parseInt ( request[ 1 ] ), Integer.parseInt ( request[ 2 ] ), Integer.parseInt ( request[ 3 ] ), Boolean.parseBoolean ( request[ 4 ] ) );

                    this.reqList.add ( dStarSocMedRequest );

                } else {

                    WebRequest r = new WebRequest ( RequestType.valueOf ( requestString ), request[ 1 ], Long.parseLong ( request[ 2 ] ) );

                    this.reqList.add ( r );
                }
            } catch ( NumberFormatException e ) {
                System.out.println ( "trace entry: " + value );

            }

        }


    }

    /**
     * @param traceLines = priority queue of tracelines, from where tracelines would be drawn
     * @param endTime    = endtime of the request that has been executed
     *                   <p>
     *                   This function performs necessary works when a request is done executing:
     *                   1. new traceline to be added to the priority queue
     *                   OR
     *                   2. setting of endtime for this traceline if this traceline is complete
     */

    public synchronized void complete ( PriorityQueue< TraceLine > traceLines, long endTime ) {

        this.currentReqStartTime = endTime + this.reqList.get ( currentReqIdx ).getThinkTime ( );


        this.currentReqIdx++;


        if ( currentReqIdx < this.reqList.size ( ) ) {


            traceLines.add ( this );


        } else {

            this.setEndTime ( endTime );

        }


    }


    public String writeFormat ( ) {

        String str = this.initialStartTime + ";;;" + this.actualStartTime + ";;;" + this.userNumber + ";;;" + this.endTime + ";;;";
        StringBuilder str2 = new StringBuilder ( );

        for ( Request request : reqList ) {

            str2.append ( request.writeFormat ( ) ).append ( ";;" );

        }

        str2 = new StringBuilder ( str2.substring ( 0, str2.length ( ) - 2 ) );

        return str + str2;

    }

    public double getRequestWorkSize ( ) {

        double work = 0;

        JsonObject reqSizeMapping = new Gson ( ).fromJson ( this.reqSizeMappingString, JsonObject.class );
        String reqType = this.reqList.get ( 0 ).getRequestType ( ).toString ( );
        if ( reqType.equals ( "POST_SELF_WALL" ) ) {

            int payloadSize = this.reqList.get ( 0 ).getParameter ( ).length ( );
            HashMap< String, Double > postSelfWallSizes = new Gson ( ).fromJson ( reqSizeMapping.get ( reqType ).getAsJsonObject ( ).toString ( ), HashMap.class );

            ArrayList< String > sizeString = new ArrayList<> ( postSelfWallSizes.keySet ( ) );
            ArrayList< Integer > sizeInt = ( ArrayList< Integer > ) sizeString.stream ( )
                    .map ( Integer :: parseInt )
                    .collect ( Collectors.toList ( ) );

            int closestSize = sizeInt.stream ( )
                    .min ( Comparator.comparingInt ( i -> Math.abs ( i - payloadSize ) ) )
                    .orElseThrow ( ( ) -> new NoSuchElementException ( "No Value Present" ) );

            work = postSelfWallSizes.get ( "" + closestSize );


        } else {

            work = reqSizeMapping.get ( reqType ).getAsDouble ( );
        }

        return work;
    }


}
