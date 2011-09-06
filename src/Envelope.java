import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;


public class Envelope implements java.io.Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -7726335089122193103L;
	private String msg;
	private ArrayList<Object> objContents = new ArrayList<Object>();
	
	public Envelope(String text)
	{
		msg = text;
	}
	
	public String getMessage()
	{
		return msg;
	}
	
	public ArrayList<Object> getObjContents()
	{
		return objContents;
	}
	
	public void addObject(Object object)
	{
		objContents.add(object);
	}

        public static byte[] toByteArray(Envelope env)
        {
            try{
                ByteArrayOutputStream bStream = new ByteArrayOutputStream();
                ObjectOutputStream oStream = new ObjectOutputStream(bStream);
                oStream.writeObject(env);
                byte[] dataBytes =  bStream.toByteArray();
                return dataBytes;
            } catch (IOException ioe){
                System.err.println("Encrypted envelope");
                return null;
            }
        }

        public static Envelope getEnvelopefromBytes(byte[] bytes)
        {
            try{
                ByteArrayInputStream bStream = new ByteArrayInputStream(bytes);
                ObjectInputStream oStream = new ObjectInputStream(bStream);
                Envelope env = (Envelope) oStream.readObject();

                return env;
            } catch (IOException ioe){
                System.err.println("Encrypted Envelope");
                return null;
            } catch (ClassNotFoundException cnfe){
                System.err.println("Envelope class not found");
                return null;
            }
        }
}
