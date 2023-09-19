package entity;

public interface Request {
	
	public abstract RequestType getRequestType ( );
	
	abstract public String writeFormat ( );
	
	abstract public long getThinkTime ( );
	
	public abstract String getParameter ( );
	
	public abstract long getParamterSize ( );
}
