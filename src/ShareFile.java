
import javax.crypto.spec.IvParameterSpec;

public class ShareFile implements java.io.Serializable, Comparable<ShareFile> {

    /**
     *
     */
    private static final long serialVersionUID = -6699986336399821598L;
    private String group;
    private String path;
    private String owner;
    private int keyNum;
    private byte[] initialVector;

    public ShareFile(String _owner, String _group, String _path,
            int _keyNum, byte[] _initialVector) {
        group = _group;
        owner = _owner;
        path = _path;
        keyNum = _keyNum;
        initialVector = _initialVector;
    }

    public String getPath() {
        return path;
    }

    public String getOwner() {
        return owner;
    }

    public String getGroup() {
        return group;
    }

    public int getKeyNum() {
        return keyNum;
    }

    public byte[] getIV() {
        return initialVector;
    }

    public int compareTo(ShareFile rhs) {
        if (path.compareTo(rhs.getPath()) == 0) {
            return 0;
        } else if (path.compareTo(rhs.getPath()) < 0) {
            return -1;
        } else {
            return 1;
        }
    }
}
