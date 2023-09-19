package utility;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import entity.BlockDeviceRequest;
import entity.Request;
import entity.TraceLine;
import log_handler.WikiLogEntry_depr;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SerializationUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FileUtility {
    private final long nanoSecondInSecond = 1000000000;

    public JsonObject readJsonObject ( String jsonFilePath ) {

        JsonObject jsonObject = null;
        JsonParser jsonParser = new JsonParser ( );

        try {

            FileReader fileReader = new FileReader ( jsonFilePath );
            Object object = jsonParser.parse ( fileReader );
            jsonObject = ( JsonObject ) object;
            fileReader.close ( );

        } catch ( IOException e ) {
            e.printStackTrace ( );
        }
        return jsonObject;
    }

    public JsonArray readJsonArray ( String jsonFilePath ) {
        JsonArray jsonArray = null;
        JsonParser jsonParser = new JsonParser ( );

        try {

            FileReader fileReader = new FileReader ( jsonFilePath );
            Object object = jsonParser.parse ( fileReader );
            jsonArray = ( JsonArray ) object;
            fileReader.close ( );

        } catch ( IOException e ) {
            e.printStackTrace ( );
        }
        return jsonArray;
    }


    public ArrayList< TraceLine > readTraceLines ( String traceFileAddress ) {

//		String traceFileAddress = this.properties.get ( "traceFileReplayAddress" ).getAsString ( );

        ArrayList< TraceLine > traceLines = new ArrayList<> ( );

        BufferedReader bufferedReader;
        try {
            bufferedReader = new BufferedReader ( new FileReader ( traceFileAddress ) );
            String line = "";

            while ( ( line = bufferedReader.readLine ( ) ) != null ) {

                TraceLine traceLine = new TraceLine ( );
                traceLine.loadFromString ( line );

                traceLines.add ( traceLine );

            }

        } catch ( IOException e ) {
            e.printStackTrace ( );
        }

        return traceLines;
    }

    public ArrayList< TraceLine > readTraceLines2 ( String traceFileAddress ) {

        List< TraceLine > temp = new ArrayList<> ( );
        try ( Stream< String > stream = Files.lines ( Paths.get ( traceFileAddress ) ) ) {
            temp = stream.map ( FileUtility :: getTraceLineFromTraceString ).collect ( Collectors.toList ( ) );
        } catch ( IOException e ) {
            e.printStackTrace ( );
        }

        return new ArrayList<> ( temp );
    }

    public static TraceLine getTraceLineFromTraceString ( String traceString ) {
        TraceLine traceLine = new TraceLine ( );
        traceLine.loadFromString ( traceString );
        return traceLine;
    }

    public void writeTraceLines ( String fileName, ArrayList< TraceLine > traceLines ) {

        try {
            BufferedWriter bufferedWriter = new BufferedWriter ( new FileWriter ( fileName ) );
            for ( TraceLine traceLine : traceLines ) {
                bufferedWriter.write ( traceLine.writeFormat ( ) + "\n" );

            }
            bufferedWriter.close ( );

        } catch ( IOException e ) {
            e.printStackTrace ( );
        }

    }

    public ArrayList< Double > getStatPerUnitTime ( ArrayList< TraceLine > traceLines, float unitTimeInSecond, String statType ) {

        ArrayList< Double > statPerUnitTime = new ArrayList<> ( );

        long startTime = traceLines.get ( 0 ).getInitialStartTime ( );
        long unitTimeInNanoSeconds = ( long ) ( unitTimeInSecond * this.nanoSecondInSecond );

        double counter = 0;
        long bucketEnd = startTime + unitTimeInNanoSeconds;

        for ( TraceLine x : traceLines ) {

            long currentStartTime = x.getInitialStartTime ( );

            if ( currentStartTime <= bucketEnd ) {

                if ( statType.equals ( "arrival" ) ) {
                    counter++;
                } else if ( statType.equals ( "work" ) ) {
                    counter += x.getRequestWorkSize ( );
                }

            } else {

                startTime = x.getInitialStartTime ( );
                bucketEnd = startTime + unitTimeInNanoSeconds;
                statPerUnitTime.add ( counter );

                if ( statType.equals ( "arrival" ) ) {
                    counter = 1;
                } else if ( statType.equals ( "work" ) ) {
                    counter = x.getRequestWorkSize ( );
                }

            }


        }
        statPerUnitTime.add ( counter );


        return statPerUnitTime;
    }

    public ArrayList< ArrayList< TraceLine > > getUnitTimeChunks ( ArrayList< TraceLine > traceLines, double unitTimeInSeconds ) {

        ArrayList< ArrayList< TraceLine > > fullTrace = new ArrayList<> ( );

        long unitTimeInNanoseconds = ( long ) ( unitTimeInSeconds * this.nanoSecondInSecond );

        ArrayList< TraceLine > traceLinesInCurrentUnitTime = new ArrayList<> ( );

        long bucketEnd = traceLines.get ( 0 ).getInitialStartTime ( ) + unitTimeInNanoseconds;

        for ( TraceLine x : traceLines ) {

            long currentStartTime = x.getInitialStartTime ( );

            if ( currentStartTime > bucketEnd ) {
                bucketEnd += unitTimeInNanoseconds;
                fullTrace.add ( traceLinesInCurrentUnitTime );
                traceLinesInCurrentUnitTime = new ArrayList<> ( );
            }
            traceLinesInCurrentUnitTime.add ( x );
        }

        fullTrace.add ( traceLinesInCurrentUnitTime );
        return fullTrace;
    }

    public ArrayList< TraceLine > mergeMultipleTracesInFile ( ArrayList< ArrayList< TraceLine > > traceList, String finalTraceFilePath ) {

        ArrayList< TraceLine > finalTrace;

        ArrayList< TraceLine > temp1Trace = new ArrayList<> ( );

        for ( ArrayList< TraceLine > trace : traceList ) {

            ArrayList< TraceLine > x = ( ArrayList< TraceLine > ) trace.stream ( ).map ( SerializationUtils :: clone ).collect ( Collectors.toList ( ) );

//			ArrayList<TraceLine> x=trace
            //todo: shift arrival times
            ArrayList< TraceLine > temp = this.shiftArrivalTimeFromStart ( x );
            //todo: add for finalTrace
            temp1Trace.addAll ( temp );


        }
        //todo: merge arrival times
        finalTrace = this.sortArrivalTimes ( temp1Trace );

        //todo: write traceLines
        finalTrace.sort ( Comparator.comparing ( TraceLine :: getInitialStartTime ) );
        this.writeTraceLines ( finalTraceFilePath, finalTrace );
        return finalTrace;

    }

    public ArrayList< TraceLine > shiftArrivalTimeFromStart ( ArrayList< TraceLine > trace ) {

        long startTime = trace.get ( 0 ).getInitialStartTime ( );

        for ( TraceLine traceLine : trace ) {
            long temp = traceLine.getInitialStartTime ( ) - startTime;
            traceLine.setInitialStartTime ( temp );


        }
        return trace;
    }

    public ArrayList< TraceLine > sortArrivalTimes ( ArrayList< TraceLine > trace ) {
        trace.sort ( Comparator.comparingLong ( TraceLine :: getInitialStartTime ) );
        return trace;

    }

    public long getMaxParamSizeInTrace ( ArrayList< TraceLine > traceLines ) {


        long maxParamSizeInBytes = 0L;
        for ( TraceLine traceLine : traceLines ) {
            ArrayList< Request > requestArrayList = traceLine.getReqList ( );
            for ( Request request : requestArrayList ) {
                long paramSize = request.getParamterSize ( );
                if ( paramSize > maxParamSizeInBytes ) {
                    maxParamSizeInBytes = paramSize;
                }
            }


        }

        System.out.println ( "max param size: " + maxParamSizeInBytes + " bytes" );

        return maxParamSizeInBytes;
    }


    public List< WikiLogEntry_depr > readWikiLogEntriesFromLogFile ( String logFilePath ) {

        List< WikiLogEntry_depr > wikiLogEntryDeprArrayList = new ArrayList<> ( );
        try ( Stream< String > stream = Files.lines ( Paths.get ( logFilePath ) ) ) {
//			stream.forEach ( System.out :: println );
//			logFileContent=stream
//					.collect (
//							Collectors
//									.toCollection ( ArrayList::new )
//					);
            wikiLogEntryDeprArrayList = stream.map ( WikiLogEntry_depr :: new ).collect ( Collectors.toList ( ) );
        } catch ( IOException e ) {
            e.printStackTrace ( );
        }

        wikiLogEntryDeprArrayList.sort ( Comparator.comparing ( WikiLogEntry_depr :: getmSecTime ) );
//				wikiLogEntryDeprArrayList.sort ( Comparator.comparing ( WikiLogEntry_depr::getArrivalTimeInNanoSeconds) );

        return wikiLogEntryDeprArrayList;
    }

    public void createDirectoryIfNotExists ( String directoryPath ) {

        Path path = Paths.get ( directoryPath );
        try {
            Files.createDirectories ( path );
        } catch ( IOException e ) {
            e.printStackTrace ( );
        }
    }


    public Map< ArrayList< TraceLine >, String > getCandidateTraces ( String directoryPath, String inputTraceName ) {


        System.out.println ( "$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$" );
        Map< ArrayList< TraceLine >, String > candidateTraceToTraceNames = new HashMap<> ( );

        File dir = new File ( directoryPath );
        File[] files = dir.listFiles ( ( d, name ) -> name.endsWith ( ".txt" ) && !name.contains ( inputTraceName ) );
        assert files != null;
        for ( File x : files ) {
            System.out.println ( x.getName ( ) + "  " + x.getAbsolutePath ( ) );
            ArrayList< TraceLine > trace = this.readTraceLines ( x.getAbsolutePath ( ) );
//			candidateTraces.add ( trace );
            trace = this.shiftArrivalTimeFromStart ( trace );
            candidateTraceToTraceNames.put ( trace, x.getName ( ) );
        }

//		System.out.println ( candidateTraces.size ( ) );
//		return candidateTraces;
        return candidateTraceToTraceNames;
    }

    public Map< String, String > getCandidateTracePathToTraceNames ( String directoryPath, String inputTraceName ) {

        Map< String, String > candidateTracePathToTraceNames = new HashMap<> ( );
        File dir = new File ( directoryPath );
        File[] files = dir.listFiles ( ( d, name ) -> name.endsWith ( ".txt" ) && !name.contains ( inputTraceName ) );
        assert files != null;

        for ( File x : files ) {

            candidateTracePathToTraceNames.put ( x.getAbsolutePath ( ), x.getName ( ) );

        }

        return candidateTracePathToTraceNames;
    }


    public void writeSimilarityMetricWithTraceName ( Map< String, String > candidateTraceFilePathListToNames, HashMap< Double, String > distanceToFilePathMap, String filePath ) {

        BufferedWriter bufferedWriter = null;
        try {

            bufferedWriter = new BufferedWriter ( new FileWriter ( filePath ) );

        } catch ( IOException e ) {
            e.printStackTrace ( );
        }

        String header = "Trace Name,Distance";
        try {
            assert bufferedWriter != null;
            bufferedWriter.write ( header + "\n" );
        } catch ( IOException e ) {
            e.printStackTrace ( );
        }

        for ( Map.Entry< Double, String > entry : distanceToFilePathMap.entrySet ( ) ) {


//			ArrayList< TraceLine > trace = this.readTraceLines2 ( entry.getValue (  ));
            String traceFilePath = entry.getValue ( );
            double similarityScore = entry.getKey ( );
            String traceName = candidateTraceFilePathListToNames.get ( traceFilePath );

            String lineContent = traceName + "," + similarityScore;
            try {

                bufferedWriter.write ( lineContent + "\n" );

            } catch ( IOException e ) {

                e.printStackTrace ( );

            }
        }

        try {

            bufferedWriter.close ( );

        } catch ( IOException e ) {
            e.printStackTrace ( );
        }
    }

    public String getFilePathMatchingPattern ( String directoryPath, String fileNamePattern ) {
        String absoluteFilePath = "nothing";
        File dir = new File ( directoryPath );
        File[] matchingFiles = dir.listFiles ( ( d, name ) -> name.endsWith ( fileNamePattern ) );
        if ( matchingFiles == null ) {
            System.out.println ( "#####   directory path: " + directoryPath );
            System.out.println ( "########  matching files is null" );
        }
        assert matchingFiles != null;
        for ( File f : matchingFiles ) {
            System.out.println ( "!!!:  " + f.getAbsolutePath ( ) );
            absoluteFilePath = f.getAbsolutePath ( );
        }
        return absoluteFilePath;
    }

    public long getTraceStartTime ( String filePath ) {

        long startTime = 0;

        BufferedReader bufferedReader;
        try {
            bufferedReader = new BufferedReader ( new FileReader ( filePath ) );
            String line = "";

//			while ( ( line = bufferedReader.readLine ( ) ) != null ) {
//
//				TraceLine traceLine = new TraceLine ( );
//				traceLine.loadFromString ( line );
//
//				startTime=traceLine.getInitialStartTime ();
//				break;
//			}
            line = bufferedReader.readLine ( );
            System.out.println ( "line: " + line );
            TraceLine traceLine = new TraceLine ( );
            traceLine.loadFromString ( line );
            startTime = traceLine.getInitialStartTime ( );


        } catch ( IOException e ) {
            e.printStackTrace ( );
        }

        return startTime;
    }

    public void setupPeriodDirectory ( JsonElement upscalingConf ) {
        /**
         * 1. check if base has period traces, if not, create those
         * 2. create desired directory
         * 3. copy input, output and periodTraces from base to the desired directory
         */

        // creating base period traces....
        JsonObject configuration = upscalingConf.getAsJsonObject ( );
        String periodRootDirectoryPath = configuration.get ( "rootDirectory" ).getAsString ( );
        String baseDirectoryPath = periodRootDirectoryPath.replaceAll ( "/period\\+?\\d+", "/base" );
        System.out.println ( "--- baseDir: " + baseDirectoryPath );

        String basePeriodDirPath = baseDirectoryPath + "/" + configuration.get ( "periodTraceDirectory" ).getAsString ( );
        System.out.println ( "basePeriod: " + basePeriodDirPath );
        File dir = new File ( basePeriodDirPath );
        File[] periodFiles = dir.listFiles ( ( d, name ) -> name.endsWith ( ".txt" ) );
//		assert periodFiles != null;
        if ( periodFiles == null ) {
            System.out.println ( "period files is null" );
        }
//		System.out.println ( periodFiles.length );
        if ( periodFiles == null || periodFiles.length == 0 ) {

            String mainTracePath = baseDirectoryPath + "/" + configuration.get ( "inputTraceDirectory" ).getAsString ( ) + "/" + configuration.get ( "mainTraceName" ).getAsString ( );
            int periodInSec = configuration.get ( "periodInSec" ).getAsInt ( );

            new TraceLineUtility ( ).generatePeriodTracesFromTrace2 ( mainTracePath, periodInSec, basePeriodDirPath );


        }

        // create particular period directory
        File periodRootDirectory = new File ( periodRootDirectoryPath );
//		if(!Files.exists ( Paths.get ( periodRootDirectoryPath ) )){
//			periodRootDirectory.mkdir ();
//		}
        if ( !periodRootDirectory.exists ( ) ) {
            if ( periodRootDirectory.mkdir ( ) ) {
                System.out.println ( "dir creation successful" );
            }
        }

        //copy base directory to period directory
        File source = new File ( baseDirectoryPath );
        try {
            FileUtils.copyDirectory ( source, periodRootDirectory );
        } catch ( IOException e ) {
            e.printStackTrace ( );
        }


    }

    public void deletePeriodAndInputDirectory ( JsonElement configElement ) {
        JsonObject configuration = configElement.getAsJsonObject ( );
        String periodDirectoryPath = configuration.get ( "rootDirectory" ).getAsString ( ) + "/" + configuration.get ( "periodTraceDirectory" ).getAsString ( );

        String inputDirectoryPath = configuration.get ( "rootDirectory" ).getAsString ( ) + "/" + configuration.get ( "inputTraceDirectory" ).getAsString ( );

        System.out.println ( "----- periodDirectory: " + periodDirectoryPath );
        try {
            FileUtils.deleteDirectory ( new File ( periodDirectoryPath ) );
            FileUtils.deleteDirectory ( new File ( inputDirectoryPath ) );
        } catch ( IOException e ) {
            e.printStackTrace ( );
        }
    }

    public ArrayList< TraceLine > modifyOffsetInBytes ( ArrayList< TraceLine > traceLines, long maxRequestSize, long maxAllowableOffset ) {


        long moddingFactor = maxAllowableOffset - maxRequestSize;
        for ( TraceLine traceLine : traceLines ) {

            ArrayList< Request > blockDeviceRequestArrayList = traceLine.getReqList ( );
            ArrayList< Request > updatedRequests = new ArrayList<> ( blockDeviceRequestArrayList.size ( ) );

            for ( Request request : blockDeviceRequestArrayList ) {

                BlockDeviceRequest request1 = ( BlockDeviceRequest ) request;
                long currentOffsetInBytes = request1.getOffsetInBytes ( );
                long updatedOffsetInBytes = currentOffsetInBytes % moddingFactor;
                request1.setOffsetInBytes ( updatedOffsetInBytes );


                updatedRequests.add ( request1 );
            }

            traceLine.setReqList ( updatedRequests );


        }

        return traceLines;

    }

    public List< String > listFilesWithPatternInDirectory ( String directoryPath, String pattern ) {


        File dir = new File ( directoryPath );
        File[] matchingFiles = dir.listFiles ( ( d, name ) -> name.endsWith ( pattern ) );
        assert matchingFiles != null;

        return Arrays.stream ( matchingFiles ).map ( File :: getAbsolutePath ).collect ( Collectors.toList ( ) );
    }


    public ArrayList< TraceLine > getTraceInInterval ( String traceFile, long startTimeInNanoSec, long endTimeInNanoSec ) {

        ArrayList< TraceLine > fullTrace = this.readTraceLines2 ( traceFile );
        ArrayList< TraceLine > trimmedTrace = new ArrayList<> ( );

        for ( TraceLine x : fullTrace ) {

            if ( startTimeInNanoSec <= x.getInitialStartTime ( ) && x.getInitialStartTime ( ) <= endTimeInNanoSec ) {

                trimmedTrace.add ( x );

            }
            if ( x.getInitialStartTime ( ) > endTimeInNanoSec ) break;
        }


        return trimmedTrace;

    }

    public List< String > getFileContentAsStringList ( String filePath ) {

        List< String > fileContent;
        try ( Stream< String > lines = Files.lines ( Paths.get ( filePath ) ) ) {
            fileContent = lines.collect ( Collectors.toList ( ) );
        } catch ( IOException e ) {
            throw new RuntimeException ( e );
        }
//        for ( int i = 0 ; i < 10 ; i++ ) {
//            System.out.println ( fileContent.get ( i ) );
//        }
        return fileContent;
    }
}
































