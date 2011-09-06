/* Group server. Server loads the users from UserList.bin.
 * If user list does not exists, it creates a new list and makes the user the server administrator.
 * On exit, the server saves the user list to file. 
 */

/*
 * TODO: This file will need to be modified to save state related to
 *       groups that are created in the system
 *
 */
import java.net.ServerSocket;
import java.net.Socket;
import java.io.*;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.KeyGenerator;

// This thread saves the user list
public class GroupServer extends Server {

    public static final int SERVER_PORT = 8765;
    public UserList userList;
    public GroupList groupList;
    public HashMap<String, ArrayList<Key>> groupKeys;

    public GroupServer() {
        super(SERVER_PORT, "ALPHA");
    }

    public GroupServer(int _port) {
        super(_port, "ALPHA");
    }

    public void start() {
        // Overwrote server.start() because if no user file exists, initial
        // admin account needs to be created

        String userFile = "UserList.bin";
        String groupFile = "GroupList.bin";
        String keyMapFile = "KeyMap.bin";
        Scanner console = new Scanner(System.in);
        ObjectInputStream userStream;
        ObjectInputStream groupStream;
        ObjectInputStream keyMapStream;

        // This runs a thread that saves the lists on program exit
        Runtime runtime = Runtime.getRuntime();
        runtime.addShutdownHook(new ShutDownListener(this));

        // Open user/group file to get user/group list
        try {
            FileInputStream user_fis = new FileInputStream(userFile);
            FileInputStream group_fis = new FileInputStream(groupFile);
            userStream = new ObjectInputStream(user_fis);
            groupStream = new ObjectInputStream(group_fis);
            userList = (UserList) userStream.readObject();
            groupList = (GroupList) groupStream.readObject();
        } catch (FileNotFoundException e) {
            System.out.println("UserList/GroupList File Does Not Exist. Creating UserList and GroupList...");
            System.out.println("No users currently exist. Your account will be the administrator.");
            System.out.print("Enter your username: ");
            String username = console.next();
            System.out.print("Enter your password: ");
            String password = console.next();

            // Create a new list, add current user to the ADMIN group. They now own the ADMIN group.
            try {
                userList = new UserList();
                groupList = new GroupList();

                ObjectOutputStream gOut = new ObjectOutputStream(new FileOutputStream("GroupList.bin"));
                ObjectOutputStream uOut = new ObjectOutputStream(new FileOutputStream("UserList.bin"));
                userList.addUser(username, password);
                userList.addGroup(username, "ADMIN");
                userList.addOwnership(username, "ADMIN");
                groupList.addGroup("ADMIN");
                groupList.addAdmin("ADMIN", username);
                groupList.addMember("ADMIN", username);

                gOut.writeObject(groupList);
                uOut.writeObject(userList);
            } catch (IOException ioe) {
                System.err.println(ioe.getMessage());
            }

        } catch (IOException e) {
            System.out.println("Error reading from UserList file");
            System.exit(-1);
        } catch (ClassNotFoundException e) {
            System.out.println("Error reading from UserList file");
            System.exit(-1);
        }

        // open key file to get key map for file encrypt and decrypt
        try {
            FileInputStream keymap_fis = new FileInputStream(keyMapFile);
            try {
                keyMapStream = new ObjectInputStream(keymap_fis);
                groupKeys = (HashMap) keyMapStream.readObject();

                System.out.println("Group Server up and running...");
            } catch (ClassNotFoundException ex) {
                Logger.getLogger(GroupServer.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(GroupServer.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (FileNotFoundException ex) {
            try {
                groupKeys = new HashMap<String, ArrayList<Key>>();
                //Generate AES key
                KeyGenerator keyGen = KeyGenerator.getInstance("AES");
                Key AESkey = keyGen.generateKey();
                
                ArrayList<Key> aList = new ArrayList<Key>();
                aList.add(AESkey);

                // add key/time to groupKeys and save to disk
                groupKeys.put("ADMIN", aList);
                ObjectOutputStream ktOut = new ObjectOutputStream(new FileOutputStream("KeyMap.bin"));
                ktOut.writeObject(groupKeys);

                ktOut.close();
                System.out.println("Group Server up and running...");
            } catch (IOException ex1) {
                Logger.getLogger(GroupServer.class.getName()).log(Level.SEVERE, null, ex1);
            } catch (NoSuchAlgorithmException ex1) {
                Logger.getLogger(GroupServer.class.getName()).log(Level.SEVERE, null, ex1);
            }
        }

        // Autosave Daemon. Saves lists every 5 minutes
        AutoSave aSave = new AutoSave(this);
        aSave.setDaemon(true);
        aSave.start();

        // This block listens for connections and creates threads on new
        // connections
        try {

            final ServerSocket serverSock = new ServerSocket(port);

            Socket sock = null;
            GroupThread thread = null;

            while (true) {
                sock = serverSock.accept();
                thread = new GroupThread(sock, this);
                thread.start();
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace(System.err);
        }

    }

    public boolean generateNewFileKey(String groupname) {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            Key AESkey = keyGen.generateKey();

            if (groupKeys.containsKey(groupname)) {
                groupKeys.get(groupname).add(AESkey);
            } else {
                ArrayList<Key> aList = new ArrayList<Key>();
                aList.add(AESkey);
                groupKeys.put(groupname, aList);
            }
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(GroupServer.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }

        return true;
    }

//    public File encryptFile(File file, String group) {
//        FileInputStream fis = null;
//        try {
//            //find current key
//            KeyTimePair kTP = groupKeys.get(group).get(groupKeys.get(group).size() - 1);
//            Key currentKey = kTP.key;
//
//            //read file
//            fis = new FileInputStream(file);
//            String entireFile = "";
//
//            while (fis.available() > 0) {
//                entireFile += fis.read();
//            }
//
//            //encrypt file
//            Cipher cipher = Cipher.getInstance("AES");
//            cipher.init(Cipher.ENCRYPT_MODE, currentKey);
//            byte[] cipherBytes = cipher.doFinal(entireFile.getBytes());
//
//            //write byte[] to newFile
//            File newFile = new File(file.getName());
//            FileOutputStream fos = new FileOutputStream(newFile);
//            fos.write(cipherBytes);
//
//            return newFile;
//        } catch (IllegalBlockSizeException ex) {
//            Logger.getLogger(GroupServer.class.getName()).log(Level.SEVERE, null, ex);
//            return null;
//        } catch (BadPaddingException ex) {
//            Logger.getLogger(GroupServer.class.getName()).log(Level.SEVERE, null, ex);
//            return null;
//        } catch (InvalidKeyException ex) {
//            Logger.getLogger(GroupServer.class.getName()).log(Level.SEVERE, null, ex);
//            return null;
//        } catch (NoSuchAlgorithmException ex) {
//            Logger.getLogger(GroupServer.class.getName()).log(Level.SEVERE, null, ex);
//            return null;
//        } catch (NoSuchPaddingException ex) {
//            Logger.getLogger(GroupServer.class.getName()).log(Level.SEVERE, null, ex);
//            return null;
//        } catch (IOException ex) {
//            Logger.getLogger(GroupServer.class.getName()).log(Level.SEVERE, null, ex);
//            return null;
//        } finally {
//            try {
//                fis.close();
//            } catch (IOException ex) {
//                Logger.getLogger(GroupServer.class.getName()).log(Level.SEVERE, null, ex);
//                return null;
//            }
//        }
//    }
}

class ShutDownListener extends Thread {

    public GroupServer my_gs;

    public ShutDownListener(GroupServer _gs) {
        my_gs = _gs;
    }

    public void run() {
        System.out.println("Shutting down server");
        ObjectOutputStream outStream;
        try {
            outStream = new ObjectOutputStream(new FileOutputStream(
                    "UserList.bin"));
            outStream.writeObject(my_gs.userList);
            outStream = new ObjectOutputStream(new FileOutputStream(
                    "GroupList.bin"));
            outStream.writeObject(my_gs.groupList);
            outStream = new ObjectOutputStream(new FileOutputStream(
                    "KeyMap.bin"));
            outStream.writeObject(my_gs.groupKeys);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace(System.err);
        }
    }
}

class AutoSave extends Thread {

    public GroupServer my_gs;

    public AutoSave(GroupServer _gs) {
        my_gs = _gs;
    }

    public void run() {
        do {
            try {
                Thread.sleep(300000); // Save group and user lists every 5
                // minutes
                System.out.println("Autosave group and user lists...");
                ObjectOutputStream outStream;
                try {
                    outStream = new ObjectOutputStream(new FileOutputStream(
                            "UserList.bin"));
                    outStream.writeObject(my_gs.userList);
                    outStream = new ObjectOutputStream(new FileOutputStream(
                            "GroupList.bin"));
                    outStream.writeObject(my_gs.groupList);
                    outStream = new ObjectOutputStream(new FileOutputStream(
                            "KeyMap.bin"));
                    outStream.writeObject(my_gs.groupKeys);
                } finally {
                    // DO NOTHING
                }
            } catch (Exception e) {
                System.out.println("Autosave Interrupted");
            }
        } while (true);
    }
}
