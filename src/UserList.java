/* This list represents the users on the server */

import java.security.*;
import java.util.*;

public class UserList implements java.io.Serializable {

    private static final long serialVersionUID = 7600343803563417992L;
    private Hashtable<String, User> list = new Hashtable<String, User>();

    public synchronized boolean checkPassword(String username, String pwd){
        User user = list.get(username);
        try {
            byte[] pwdBytes = pwd.getBytes();
            MessageDigest md = MessageDigest.getInstance("SHA1");
            md.update(pwdBytes);
            byte[] passwordHash = md.digest();


            if(Arrays.equals(passwordHash, user.passwordHash)){
                return true;
            }
        } catch (NoSuchAlgorithmException ex) {
            System.err.println("Password hashing error in checkPassword()");
            return false;
        }
        
        return false;
    }

    public synchronized void changePwd(String username, String newPwd) {
        User user = list.get(username);
        user.addPasswordHash(newPwd);
    }

    public synchronized void addUser(String username, String pwd) {
        User newUser = new User();
        newUser.addPasswordHash(pwd);
        list.put(username, newUser);
    }

    public synchronized void deleteUser(String username) {
        list.remove(username);
    }

    public synchronized boolean checkUser(String username) {
        if (list.containsKey(username)) {
            return true;
        } else {
            return false;
        }
    }

    public synchronized ArrayList<String> getUserGroups(String username) {
        return list.get(username).getGroups();
    }

    public synchronized ArrayList<String> getUserOwnership(String username) {
        return list.get(username).getOwnership();
    }

    public synchronized void addGroup(String user, String groupname) {
        list.get(user).addGroup(groupname);
    }

    public synchronized void removeGroup(String user, String groupname) {
        list.get(user).removeGroup(groupname);
    }

    public synchronized void addOwnership(String user, String groupname) {
        list.get(user).addOwnership(groupname);
    }

    public synchronized void removeOwnership(String user, String groupname) {
        list.get(user).removeOwnership(groupname);
    }

    public synchronized Key getAESkey(String user)
    {
       return list.get(user).getAESKey();
    }
    
    public synchronized void addAESkey(String user, Key k)
    {
       list.get(user).addAESKey(k);
    }
    
    public synchronized Key getHMACkey(String user)
    {
      return list.get(user).getHMACKey(); 
    }
    
    public synchronized void addHMACkey(String user, Key k)
    {
      list.get(user).addHMACKey(k); 
    }
    
    class User implements java.io.Serializable {

        private static final long serialVersionUID = -6699986336399821598L;
        private ArrayList<String> groups;
        private ArrayList<String> ownership;
        private byte[] passwordHash;
        private Key AESkey;
        private Key HMACkey;

        public User() {
            groups = new ArrayList<String>();
            ownership = new ArrayList<String>();
            passwordHash = null;
            AESkey = null;
        }

        public synchronized ArrayList<String> getGroups() {
            return groups;
        }

        public synchronized ArrayList<String> getOwnership() {
            return ownership;
        }

        public synchronized byte[] getPasswordHash() {
            return passwordHash;
        }

        public synchronized void addGroup(String group) {
            groups.add(group);
        }

        public synchronized void addPasswordHash(String pwd) {
            try {
                byte[] pwdBytes = pwd.getBytes();
                MessageDigest md = MessageDigest.getInstance("SHA1");
                md.update(pwdBytes);
                passwordHash = md.digest();
            } catch (NoSuchAlgorithmException ex) {
                System.err.println("Password hashing error in addPasswordHash()");
            }
        }

        public synchronized void addAESKey(Key key) {
            AESkey = key;
        }

        public synchronized Key getAESKey(){
            return AESkey;
        }
        
        public synchronized Key getHMACKey(){
            return HMACkey;
        } 
        
        public synchronized void addHMACKey(Key key){
           HMACkey = key;
        }

        public synchronized void removeGroup(String group) {
            if (!groups.isEmpty()) {
                if (groups.contains(group)) {
                    groups.remove(groups.indexOf(group));
                }
            }
        }

        public synchronized void addOwnership(String group) {
            ownership.add(group);
        }

        public synchronized void removeOwnership(String group) {
            if (!ownership.isEmpty()) {
                if (ownership.contains(group)) {
                    ownership.remove(ownership.indexOf(group));
                }
            }
        }
    }
}
