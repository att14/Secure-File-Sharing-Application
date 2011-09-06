import java.security.*;

public class IPKeyPair implements java.io.Serializable {
	private PublicKey key;
	String name;
	
	public IPKeyPair(PublicKey k, String n) {
		key = k;
		name = n;
	}
	
	public PublicKey getKey() {
		return key;
	}
	
	public String getName() {
		return name;
	}
}
