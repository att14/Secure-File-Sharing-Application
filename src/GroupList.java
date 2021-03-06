
import java.security.*;
import java.util.ArrayList;
import java.util.Hashtable;

public class GroupList implements java.io.Serializable {

    private static final long serialVersionUID = 7600343803563417992L;
    private Hashtable<String, Group> groupList = new Hashtable<String, Group>();
    private KeyPair keyPair;
 
    public synchronized void addKeyPair(KeyPair kp) {
        keyPair = kp;
    }

    public synchronized KeyPair getKeyPair() {
        return keyPair;
    }

    public synchronized void addGroup(String groupname) {
        Group newGroup = new Group();
        groupList.put(groupname, newGroup);
    }

    public synchronized void deleteGroup(String groupname) {
        groupList.remove(groupname);
    }

    public synchronized boolean checkGroup(String groupname) {
        if (groupList.containsKey(groupname)) {
            return true;
        } else {
            return false;
        }
    }

    public synchronized ArrayList<String> getGroupMembers(String groupname) {
        return groupList.get(groupname).getMembers();
    }

    public synchronized ArrayList<String> getGroupAdmins(String groupname) {
        Group group = (Group) groupList.get(groupname);
        ArrayList<String> admins = group.getAdmins();
        return admins;
    }

    public synchronized void addMember(String group, String membername) {
        groupList.get(group).addMember(membername);
    }

    public synchronized void removeMember(String group, String membername) {
        Group g = groupList.get(group);
        g.removeMember(membername);
    }

    public synchronized void addAdmin(String group, String membername) {
        groupList.get(group).addAdmin(membername);
    }

    public synchronized void removeAdmin(String group, String membername) {
        groupList.get(group).removeAdmin(membername);
    }

    class Group implements java.io.Serializable {

        private static final long serialVersionUID = -6699986336399821598L;
        private ArrayList<String> members;
        private ArrayList<String> admins;

        private Group() {
            members = new ArrayList<String>();
            admins = new ArrayList<String>();
        }

        public ArrayList<String> getMembers() {
            return members;
        }

        public ArrayList<String> getAdmins() {
            return admins;
        }

        public void addMember(String user) {
            members.add(user);
        }

        public void removeMember(String user) {
            if (!members.isEmpty()) {
                if (members.contains(user)) {
                    members.remove(members.indexOf(user));
                }
            }
        }

        public void addAdmin(String user) {
            admins.add(user);
        }

        public void removeAdmin(String user) {
            if (!admins.isEmpty()) {
                if (admins.contains(user)) {
                    admins.remove(admins.indexOf(user));
                }
            }
        }
    }
}
