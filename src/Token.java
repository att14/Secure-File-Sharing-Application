import java.util.List;

public class Token implements UserToken, java.io.Serializable {
    private String issuer;
    private String subject;
    private String fileServerName;
    private List<String> groupList;
    private byte[] signature;

    public Token(String i, String s, String f, List<String> g, byte[] sig) {
        issuer = i;
        subject = s;
        fileServerName = f;
        groupList = g;
        signature = sig;
    }

    public String getIssuer() {
        return issuer;
    }

    public String getSubject() {
        return subject;
    }

    public String getFileServerName(){
        return fileServerName;
    }

    public List<String> getGroups() {
        return groupList;
    }

    public byte[] getSignature(){
        return signature;
    }
    
    public String toString(){
      String newstring = issuer+subject+fileServerName;
      for (String group : groupList) {
                newstring += group;
            }
      //newstring += signature;
      return newstring;
    }
}