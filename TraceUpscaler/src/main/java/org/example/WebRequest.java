package entity;


import java.io.Serializable;

public class WebRequest implements Request, Serializable {

    private RequestType requestType;
    private String parameter;

    private long thinkTime;


    public WebRequest ( RequestType requestType, String parameter, long thinkTime ) {
        this.requestType = requestType;
        this.parameter = parameter;
        this.thinkTime = thinkTime;
    }

    @Override
    public long getThinkTime ( ) {
        return thinkTime;
    }

    public void setThinkTime ( long thinkTime ) {
        this.thinkTime = thinkTime;
    }

    @Override
    public RequestType getRequestType ( ) {
        return requestType;
    }

    public void setRequestType ( RequestType requestType ) {
        this.requestType = requestType;
    }

    @Override
    public String getParameter ( ) {
        return parameter;
    }

    @Override
    public long getParamterSize ( ) {
        return this.parameter.length ( );
    }

    public void setParameter ( String parameter ) {
        this.parameter = parameter;
    }

    @Override
    public String writeFormat ( ) {
        /**
         * actual parameter is not printed anymore to avoid problem with large payloads
         * */
        return requestType + ";" + parameter + ";" + thinkTime;
    }

    @Override
    public String toString ( ) {
        return "WebRequest{" +
                "requestType=" + requestType +
                ", parameter='" + parameter + '\'' +
                ", thinkTime=" + thinkTime +
                '}';
    }
}

