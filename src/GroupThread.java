/* This thread does all the work. It communicates with the client through Envelopes.
 * 
 */

import java.security.*;
import javax.crypto.*;
import java.net.Socket;
import java.io.*;
import java.net.SocketException;
import java.util.*;
import javax.crypto.spec.SecretKeySpec;

public class GroupThread extends Thread {

    private final Socket socket;
    private GroupServer my_gs;
    private String threadowner;
    private int nonce;

    ///// CONSTRUCTOR /////
    public GroupThread(Socket _socket, GroupServer _gs) {
        socket = _socket;
        my_gs = _gs;
        nonce = 0;
    }

    @Override
    public void run() {
        boolean proceed = true;

        try {
            // Announces connection and opens object streams
            System.out.println("*** New connection from " + socket.getInetAddress() + ":" + socket.getPort() + "***");
            final ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
            final ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());

            do {
                Envelope message;
                byte[] initMessage = (byte[]) input.readObject();

                //unencrypted case
                if (Envelope.getEnvelopefromBytes(initMessage) != null) {
                    message = Envelope.getEnvelopefromBytes(initMessage);

                    //encrypted case;
                } else {
                    Key AESkey = my_gs.userList.getAESkey(threadowner); //changed

                    //recieve byte[] and decrypt
                    Cipher cipher = Cipher.getInstance("AES");
                    cipher.init(Cipher.DECRYPT_MODE, AESkey);
                    byte[] plainBytes = cipher.doFinal(initMessage);

                    //reconstruct Envelope object from decrypted byte[]
                    message = Envelope.getEnvelopefromBytes(plainBytes);
                }

                System.out.println("Request received: " + message.getMessage());
                Envelope response;
                String responsestring;

                /** START GET **/
                //Client wants a token
                if (message.getMessage().equals("GET")) {
                    if ((Integer) message.getObjContents().get(3) == nonce) {
                        // Get the username, fileServerName
                        String username = (String) message.getObjContents().get(0);
                        String fileServerName = (String) message.getObjContents().get(1);
                        String hash = (String) message.getObjContents().get(2);
                        ////////////////////////

                        String concat = username + fileServerName + "GET" + nonce; // reconstructs the hash like how it was done in the client
                        byte[] hasharray = concat.getBytes();
                        Mac mac = Mac.getInstance("HmacSHA1");
                        mac.init(my_gs.userList.getHMACkey(threadowner));
                        mac.update(hasharray);
                        String newhash = new String(mac.doFinal(), "UTF8");
                        nonce++;

                        if (hash.equals(newhash) != true)//check hashes for equality
                        {
                            System.out.println("hash");
                            response = new Envelope("HASH");
                            concat = "HASH"; // reconstructs the hash like how it was done in the client
                            hasharray = concat.getBytes();
                            mac = Mac.getInstance("HmacSHA1");
                            mac.init(my_gs.userList.getHMACkey(threadowner));
                            mac.update(hasharray);
                            newhash = new String(mac.doFinal(), "UTF8");
                            response.addObject(newhash);
                            response.addObject(nonce);
                            nonce++;
                        } else {
                            if (username == null) {
                                System.out.println("Username is null");
                                response = new Envelope("FAIL");
                                concat = "FAIL" + nonce; // reconstructs the hash like how it was done in the client
                                hasharray = concat.getBytes();
                                mac = Mac.getInstance("HmacSHA1");
                                mac.init(my_gs.userList.getHMACkey(threadowner));
                                mac.update(hasharray);
                                newhash = new String(mac.doFinal(), "UTF8");
                                response.addObject(newhash);
                                response.addObject(nonce);
                                nonce++;
                            } else {
                                // Create a token
                                UserToken yourToken = createToken(username, fileServerName);
                                response = new Envelope("OK");
                                response.addObject(yourToken);
                                System.out.println("okay");
                                concat = yourToken.toString() + "OK" + nonce; // reconstructs the hash like how it was done in the client
                                hasharray = concat.getBytes();
                                mac = Mac.getInstance("HmacSHA1");
                                mac.init(my_gs.userList.getHMACkey(threadowner));
                                mac.update(hasharray);
                                newhash = new String(mac.doFinal(), "UTF8");
                                response.addObject(newhash);
                                response.addObject(nonce);
                                nonce++;
                            }
                        }
                        Key AESkey = my_gs.userList.getAESkey(threadowner);
                        byte[] responseBytes = Envelope.toByteArray(response);

                        //encrypt response byte[] and send
                        Cipher cipher = Cipher.getInstance("AES");
                        cipher.init(Cipher.ENCRYPT_MODE, AESkey);
                        byte[] cipherBytes = cipher.doFinal(responseBytes);
                        output.writeObject(cipherBytes);

                    } else {
                        response = new Envelope("FAIL");

                        Key AESkey = my_gs.userList.getAESkey(threadowner);
                        byte[] responseBytes = Envelope.toByteArray(response);

                        //encrypt response byte[] and send
                        Cipher cipher = Cipher.getInstance("AES");
                        cipher.init(Cipher.ENCRYPT_MODE, AESkey);
                        byte[] cipherBytes = cipher.doFinal(responseBytes);
                        output.writeObject(cipherBytes);
                    }
                    ////////////////////////


                    /** END GET **/
                    /* START CUSER */
                    // Client wants to create a user
                } else if (message.getMessage().equals("CUSER")) {

                    if (message.getObjContents().size() != 5) {
                        System.out.println("Message too short");
                        response = new Envelope("FAIL");
                    } else if ((Integer) message.getObjContents().get(4) == nonce) {
                        response = new Envelope("FAIL");

                        if (message.getObjContents().get(0) != null) {
                            if (message.getObjContents().get(2) != null) {
                                // Extract the username
                                String username = (String) message.getObjContents().get(0);
                                // Extract the token
                                String password = (String) message.getObjContents().get(1);
                                UserToken yourToken = (UserToken) message.getObjContents().get(2);
                                String hash = (String) message.getObjContents().get(3);

                                String concat = username + password + yourToken.toString() + "CUSER" + nonce; // reconstructs the hash like how it was done in the client
                                byte[] hasharray = concat.getBytes();
                                Mac mac = Mac.getInstance("HmacSHA1");
                                mac.init(my_gs.userList.getHMACkey(threadowner));
                                mac.update(hasharray);
                                String newhash = new String(mac.doFinal(), "UTF8");
                                nonce++;

                                if (hash.equals(newhash) != true)//check hashes for equality
                                {
                                    System.out.println("HASH EQUALITY FAIL");// if fail then hash equality fails
                                    response = new Envelope("FAIL");
                                } else {
                                    if (createUser(username, password, yourToken)) {
                                        // Success
                                        response = new Envelope("OK");
                                    }
                                }
                            }
                        }
                    } else {
                        System.out.println("Nonce FAIL CUSER");
                        response = new Envelope("FAIL");
                    }

                    if (response.getMessage().equals("OK")) {
                        String concat = "OK" + nonce; // reconstructs the hash like how it was done in the client
                        byte[] hasharray = concat.getBytes();
                        Mac mac = Mac.getInstance("HmacSHA1");
                        mac.init(my_gs.userList.getHMACkey(threadowner));
                        mac.update(hasharray);
                        String newhash = new String(mac.doFinal(), "UTF8");
                        response.addObject(newhash);
                        response.addObject(nonce);
                        nonce++;
                    } else {
                        String concat = "FAIL" + nonce; // reconstructs the hash like how it was done in the client
                        byte[] hasharray = concat.getBytes();
                        Mac mac = Mac.getInstance("HmacSHA1");
                        mac.init(my_gs.userList.getHMACkey(threadowner));
                        mac.update(hasharray);
                        String newhash = new String(mac.doFinal(), "UTF8");
                        response.addObject(newhash);
                        response.addObject(nonce);
                        nonce++;
                    }

                    byte[] messageBytes = Envelope.toByteArray(response);
                    Key AESkey = my_gs.userList.getAESkey(threadowner);
                    //encrypt response byte[] and send
                    Cipher cipher = Cipher.getInstance("AES");
                    cipher.init(Cipher.ENCRYPT_MODE, AESkey);
                    byte[] cipherBytes = cipher.doFinal(messageBytes);
                    output.writeObject(cipherBytes);
                    /** END CUSER **/
                    /* START DUSER */
                    // Client wants to delete a user
                } else if (message.getMessage().equals("DUSER")) {

                    if (message.getObjContents().size() != 4) {
                        response = new Envelope("FAIL");

                    } else if ((Integer) message.getObjContents().get(3) == nonce) {
                        response = new Envelope("FAIL");

                        if (message.getObjContents().get(0) != null) {
                            if (message.getObjContents().get(1) != null) {

                                String username = (String) message.getObjContents().get(0);
                                // Extract the token
                                UserToken yourToken = (UserToken) message.getObjContents().get(1);

                                String hash = (String) message.getObjContents().get(2);//check hash
                                String concat = username + yourToken.toString() + "DUSER" + nonce; // reconstructs the hash like how it was done in the client
                                byte[] hasharray = concat.getBytes();
                                Mac mac = Mac.getInstance("HmacSHA1");
                                mac.init(my_gs.userList.getHMACkey(threadowner));
                                mac.update(hasharray);
                                String newhash = new String(mac.doFinal(), "UTF8");
                                nonce++;

                                if (hash.equals(newhash) != true)//check hashes for equality
                                {
                                    System.out.println("HASH EQUALITY FAIL");// if fail then hash equality fails
                                    response = new Envelope("FAIL");
                                    response.addObject(null);
                                } else {
                                    if (deleteUser(username, yourToken)) {
                                        // Success
                                        response = new Envelope("OK");
                                    }
                                }
                            }
                        }
                    } else {
                        System.out.println("Nonce FAIL CUSER");
                        response = new Envelope("FAIL");
                    }

                    if (response.getMessage().equals("OK")) {
                        String concat = "OK" + nonce; // reconstructs the hash like how it was done in the client
                        byte[] hasharray = concat.getBytes();
                        Mac mac = Mac.getInstance("HmacSHA1");
                        mac.init(my_gs.userList.getHMACkey(threadowner));
                        mac.update(hasharray);
                        String newhash = new String(mac.doFinal(), "UTF8");
                        response.addObject(newhash);
                        response.addObject(nonce);
                        nonce++;
                    } else {
                        String concat = "FAIL" + nonce; // reconstructs the hash like how it was done in the client
                        byte[] hasharray = concat.getBytes();
                        Mac mac = Mac.getInstance("HmacSHA1");
                        mac.init(my_gs.userList.getHMACkey(threadowner));
                        mac.update(hasharray);
                        String newhash = new String(mac.doFinal(), "UTF8");
                        response.addObject(newhash);
                        response.addObject(nonce);
                        nonce++;
                    }

                    byte[] messageBytes = Envelope.toByteArray(response);

                    Key AESkey = my_gs.userList.getAESkey(threadowner);
                    // encrypt response byte[] and send
                    Cipher cipher = Cipher.getInstance("AES");
                    cipher.init(Cipher.ENCRYPT_MODE, AESkey);
                    byte[] cipherBytes = cipher.doFinal(messageBytes);
                    output.writeObject(cipherBytes);
                    /** END DUSER **/
                    /* START CGROUP */
                    //Client wants to create a group
                } else if (message.getMessage().equals("CGROUP")) {
                    if (message.getObjContents().size() != 4) {
                        nonce++;
                        response = new Envelope("FAIL");
                    } else if ((Integer) message.getObjContents().get(3) == nonce) {
                        response = new Envelope("FAIL");

                        if (message.getObjContents().get(0) != null) {
                            if (message.getObjContents().get(1) != null) {
                                // Extract the groupname
                                String groupname = (String) message.getObjContents().get(0);
                                // Extract the token
                                UserToken yourToken = (UserToken) message.getObjContents().get(1);

                                String hash = (String) message.getObjContents().get(2);//get hash
                                String concat = groupname + yourToken.toString() + "CGROUP" + nonce; // reconstructs the hash like how it was done in the client
                                byte[] hasharray = concat.getBytes();
                                Mac mac = Mac.getInstance("HmacSHA1");
                                mac.init(my_gs.userList.getHMACkey(threadowner));
                                mac.update(hasharray);
                                String newhash = new String(mac.doFinal(), "UTF8");
                                nonce++;

                                if (hash.equals(newhash) != true)//check hashes for equality
                                {
                                    System.out.println("HASH EQUALITY FAIL");// if fail then hash equality fails
                                    response = new Envelope("FAIL");
                                } else {
                                    if (createGroup(groupname, yourToken)) {
                                        // Success
                                        response = new Envelope("OK");
                                    }
                                }
                            }
                        }
                    } else {
                        System.out.println("Nonce FAIL CGROUP");
                        response = new Envelope("FAIL");
                    }
                    if (response.getMessage().equals("OK")) {
                        String concat = "OK" + nonce; // reconstructs the hash like how it was done in the client
                        byte[] hasharray = concat.getBytes();
                        Mac mac = Mac.getInstance("HmacSHA1");
                        mac.init(my_gs.userList.getHMACkey(threadowner));
                        mac.update(hasharray);
                        String newhash = new String(mac.doFinal(), "UTF8");
                        response.addObject(newhash);
                        response.addObject(nonce);
                        nonce++;
                    } else {
                        String concat = "FAIL" + nonce; // reconstructs the hash like how it was done in the client
                        byte[] hasharray = concat.getBytes();
                        Mac mac = Mac.getInstance("HmacSHA1");
                        mac.init(my_gs.userList.getHMACkey(threadowner));
                        mac.update(hasharray);
                        String newhash = new String(mac.doFinal(), "UTF8");
                        response.addObject(newhash);
                        response.addObject(nonce);
                        nonce++;
                    }
                    byte[] messageBytes = Envelope.toByteArray(response);

                    //Key AESkey = my_gs.groupList.getAESKey();
                    Key AESkey = my_gs.userList.getAESkey(threadowner);
                    //encrypt response byte[] and send
                    Cipher cipher = Cipher.getInstance("AES");
                    cipher.init(Cipher.ENCRYPT_MODE, AESkey);
                    byte[] cipherBytes = cipher.doFinal(messageBytes);
                    output.writeObject(cipherBytes);
                    /** END CGROUP **/
                    /* START DGROUP */
                    //Client wants to delete a group
                } else if (message.getMessage().equals("DGROUP")) {
                    if (message.getObjContents().size() != 4) {
                        nonce++;
                        response = new Envelope("FAIL");
                    } else if ((Integer) message.getObjContents().get(3) == nonce) {
                        response = new Envelope("FAIL");

                        if (message.getObjContents().get(0) != null) {
                            if (message.getObjContents().get(1) != null) {
                                // Extract the groupname
                                String groupname = (String) message.getObjContents().get(0);
                                // Extract the token
                                UserToken yourToken = (UserToken) message.getObjContents().get(1);

                                String hash = (String) message.getObjContents().get(2);//get hash
                                String concat = groupname + yourToken.toString() + "DGROUP" + nonce; // reconstructs the hash like how it was done in the client
                                byte[] hasharray = concat.getBytes();
                                Mac mac = Mac.getInstance("HmacSHA1");
                                mac.init(my_gs.userList.getHMACkey(threadowner));
                                mac.update(hasharray);
                                String newhash = new String(mac.doFinal(), "UTF8");
                                nonce++;

                                if (hash.equals(newhash) != true)//check hashes for equality
                                {
                                    System.out.println("HASH EQUALITY FAIL");// if fail then hash equality fails
                                    response = new Envelope("FAIL");
                                } else {
                                    if (deleteGroup(groupname, yourToken)) {
                                        // Success
                                        response = new Envelope("OK");
                                    }
                                }
                            }
                        }
                    } else {
                        System.out.println("Nonce FAIL DGROUP");
                        response = new Envelope("FAIL");
                    }
                    if (response.getMessage().equals("OK")) {
                        String concat = "OK" + nonce; // reconstructs the hash like how it was done in the client
                        byte[] hasharray = concat.getBytes();
                        Mac mac = Mac.getInstance("HmacSHA1");
                        mac.init(my_gs.userList.getHMACkey(threadowner));
                        mac.update(hasharray);
                        String newhash = new String(mac.doFinal(), "UTF8");
                        response.addObject(newhash);
                        response.addObject(nonce);
                        nonce++;
                    } else {
                        String concat = "FAIL" + nonce; // reconstructs the hash like how it was done in the client
                        byte[] hasharray = concat.getBytes();
                        Mac mac = Mac.getInstance("HmacSHA1");
                        mac.init(my_gs.userList.getHMACkey(threadowner));
                        mac.update(hasharray);
                        String newhash = new String(mac.doFinal(), "UTF8");
                        response.addObject(newhash);
                        response.addObject(nonce);
                        nonce++;
                    }

                    byte[] messageBytes = Envelope.toByteArray(response);
                    Key AESkey = my_gs.userList.getAESkey(threadowner);
                    //encrypt response byte[] and send
                    Cipher cipher = Cipher.getInstance("AES");
                    cipher.init(Cipher.ENCRYPT_MODE, AESkey);
                    byte[] cipherBytes = cipher.doFinal(messageBytes);
                    output.writeObject(cipherBytes);
                    /** END DGROUP **/
                    /* START LMEMBERS */
                    // Client wants a list of members in a group
                } else if (message.getMessage().equals("LMEMBERS")) {
                    Cipher cipher;
                    ArrayList<String> members = null;
                    Key AESkey = my_gs.userList.getAESkey(threadowner);
                    if (message.getObjContents().size() != 4) {
                        nonce++;
                        response = new Envelope("FAIL");

//                        byte[] responseBytes = Envelope.toByteArray(response);
//
//                        //encrypt response byte[] and send
//                        cipher = Cipher.getInstance("AES");
//                        cipher.init(Cipher.ENCRYPT_MODE, AESkey);
//                        byte[] cipherBytes = cipher.doFinal(responseBytes);
//                        output.writeObject(cipherBytes);
                    } else if ((Integer) message.getObjContents().get(3) == nonce) {
                        response = new Envelope("FAIL");

                        if (message.getObjContents().get(0) != null) {
                            if (message.getObjContents().get(1) != null) {
                                // Extract the groupname
                                String groupname = (String) message.getObjContents().get(0);
                                // Extract the token
                                UserToken yourToken = (UserToken) message.getObjContents().get(1);

                                String hash = (String) message.getObjContents().get(2);//get hash
                                String concat = groupname + yourToken.toString() + "LMEMBERS" + nonce; // reconstructs the hash like how it was done in the client
                                byte[] hasharray = concat.getBytes();
                                Mac mac = Mac.getInstance("HmacSHA1");
                                mac.init(my_gs.userList.getHMACkey(threadowner));
                                mac.update(hasharray);
                                String newhash = new String(mac.doFinal(), "UTF8");
                                nonce++;

                                //check hashes for equality
                                if (hash.equals(newhash) != true) {
                                    System.out.println("HASH EQUALITY FAIL");// if fail then hash equality fails
                                    response = new Envelope("FAIL");
                                    response.addObject(listMembers(groupname, yourToken));
                                } else {
                                    if (listMembers(groupname, yourToken) != null) {
                                        // Success
                                        response = new Envelope("OK");
                                        members = listMembers(groupname, yourToken);
                                        response.addObject(listMembers(groupname, yourToken));
                                    }
                                }
                            }
                        }
                        if (response.getMessage().equals("OK")) {
                            String concat = new String();
                            for (int i = 0; i < members.size(); i++) {
                                concat += members.get(i);
                            }
                            concat += "OK" + nonce;
                            byte[] hasharray = concat.getBytes();
                            Mac mac = Mac.getInstance("HmacSHA1");
                            mac.init(my_gs.userList.getHMACkey(threadowner));
                            mac.update(hasharray);
                            String newhash = new String(mac.doFinal(), "UTF8");
                            response.addObject(newhash);
                            response.addObject(nonce);
                            nonce++;
                        } else {
                            String concat = "FAIL" + nonce; // reconstructs the hash like how it was done in the client
                            byte[] hasharray = concat.getBytes();
                            Mac mac = Mac.getInstance("HmacSHA1");
                            mac.init(my_gs.userList.getHMACkey(threadowner));
                            mac.update(hasharray);
                            String newhash = new String(mac.doFinal(), "UTF8");
                            response.addObject(newhash);
                            response.addObject(nonce);
                            nonce++;
                        }
                        byte[] responseBytes = Envelope.toByteArray(response);
                        System.out.println(responseBytes);

                        //encrypt response byte[] and send
                        cipher = Cipher.getInstance("AES");
                        cipher.init(Cipher.ENCRYPT_MODE, AESkey);
                        byte[] cipherBytes = cipher.doFinal(responseBytes);
                        output.writeObject(cipherBytes);
                    }
                    /* END LMEMBERS */





                    /* START LGROUPS */
                    // Client wants a list of members in a group
                } else if (message.getMessage().equals("LGROUPS")) {
                    Cipher cipher;
                    ArrayList<String> groups = null;
                    Key AESkey = my_gs.userList.getAESkey(threadowner);
                    if (message.getObjContents().size() != 3) {
                        response = new Envelope("FAIL");

                        byte[] responseBytes = Envelope.toByteArray(response);

                        //encrypt response byte[] and send
                        cipher = Cipher.getInstance("AES");
                        cipher.init(Cipher.ENCRYPT_MODE, AESkey);
                        byte[] cipherBytes = cipher.doFinal(responseBytes);
                        output.writeObject(cipherBytes);
                    } else if ((Integer) message.getObjContents().get(2) == nonce) {
                        response = new Envelope("FAIL");

                        if (message.getObjContents().get(0) != null) {

                            // Extract the token
                            UserToken yourToken = (UserToken) message.getObjContents().get(0);

                            String hash = (String) message.getObjContents().get(1);//get hash
                            String concat = yourToken.toString() + "LGROUPS" + nonce; // reconstructs the hash like how it was done in the client
                            byte[] hasharray = concat.getBytes();
                            Mac mac = Mac.getInstance("HmacSHA1");
                            mac.init(my_gs.userList.getHMACkey(threadowner));
                            mac.update(hasharray);
                            String newhash = new String(mac.doFinal(), "UTF8");
                            nonce++;

                            if (hash.equals(newhash) != true)//check hashes for equality
                            {
                                System.out.println("HASH EQUALITY FAIL");// if fail then hash equality fails
                                response = new Envelope("FAIL");
                                response.addObject(listGroups(yourToken));
                            } // Success
                            else {
                                response = new Envelope("OK");
                                groups = listGroups(yourToken);
                                response.addObject(listGroups(yourToken));
                            }
                        }

                        if (response.getMessage().equals("OK")) {
                            String concat = new String();
                            for (int i = 0; i < groups.size(); i++) {
                                concat += groups.get(i);
                            }
                            concat += "OK" + nonce;
                            byte[] hasharray = concat.getBytes();
                            Mac mac = Mac.getInstance("HmacSHA1");
                            mac.init(my_gs.userList.getHMACkey(threadowner));
                            mac.update(hasharray);
                            String newhash = new String(mac.doFinal(), "UTF8");
                            response.addObject(newhash);
                            response.addObject(nonce);
                            nonce++;
                        } else {
                            String concat = "FAIL" + nonce; // reconstructs the hash like how it was done in the client
                            byte[] hasharray = concat.getBytes();
                            Mac mac = Mac.getInstance("HmacSHA1");
                            mac.init(my_gs.userList.getHMACkey(threadowner));
                            mac.update(hasharray);
                            String newhash = new String(mac.doFinal(), "UTF8");
                            response.addObject(newhash);
                            response.addObject(nonce);
                            nonce++;
                        }

                        byte[] responseBytes = Envelope.toByteArray(response);

                        //encrypt response byte[] and send
                        cipher = Cipher.getInstance("AES");
                        cipher.init(Cipher.ENCRYPT_MODE, AESkey);
                        byte[] cipherBytes = cipher.doFinal(responseBytes);
                        output.writeObject(cipherBytes);
                    }
                    /* END LGROUPS */

                    /* START AUSERTOGROUP */
                    // Client wants to add a user to a group
                } else if (message.getMessage().equals("AUSERTOGROUP")) {
                    if (message.getObjContents().size() != 5) {
                        nonce++;
                        response = new Envelope("FAIL");
                    } else if ((Integer) message.getObjContents().get(4) == nonce) {
                        response = new Envelope("FAIL");

                        if (message.getObjContents().get(0) != null && message.getObjContents().get(1) != null && message.getObjContents().get(2) != null) {
                            String username = (String) message.getObjContents().get(0);//Extract the username
                            String groupname = (String) message.getObjContents().get(1);// Extract the groupname
                            UserToken yourToken = (UserToken) message.getObjContents().get(2);// Extract the token

                            String hash = (String) message.getObjContents().get(3);//get hash
                            String concat = username + groupname + yourToken.toString() + "AUSERTOGROUP" + nonce; // reconstructs the hash like how it was done in the client
                            byte[] hasharray = concat.getBytes();
                            Mac mac = Mac.getInstance("HmacSHA1");
                            mac.init(my_gs.userList.getHMACkey(threadowner));
                            mac.update(hasharray);
                            String newhash = new String(mac.doFinal(), "UTF8");
                            nonce++;

                            if (hash.equals(newhash) != true)//check hashes for equality
                            {
                                System.out.println("HASH EQUALITY FAIL");// if fail then hash equality fails
                                response = new Envelope("FAIL");
                            } else {
                                if (addUserToGroup(username, groupname, yourToken)) {
                                    // Success
                                    response = new Envelope("OK");
                                }
                            }
                        }
                    } else {
                        response = new Envelope("FAIL");
                        System.out.println("Nonce FAIL AUSERTOGROUP");
                    }
                    if (response.getMessage().equals("OK")) {
                        String concat = "OK" + nonce; // reconstructs the hash like how it was done in the client
                        byte[] hasharray = concat.getBytes();
                        Mac mac = Mac.getInstance("HmacSHA1");
                        mac.init(my_gs.userList.getHMACkey(threadowner));
                        mac.update(hasharray);
                        String newhash = new String(mac.doFinal(), "UTF8");
                        response.addObject(newhash);
                        response.addObject(nonce);
                        nonce++;
                    } else {
                        String concat = "FAIL" + nonce; // reconstructs the hash like how it was done in the client
                        byte[] hasharray = concat.getBytes();
                        Mac mac = Mac.getInstance("HmacSHA1");
                        mac.init(my_gs.userList.getHMACkey(threadowner));
                        mac.update(hasharray);
                        String newhash = new String(mac.doFinal(), "UTF8");
                        response.addObject(newhash);
                        response.addObject(nonce);
                        nonce++;
                    }

                    byte[] messageBytes = Envelope.toByteArray(response);

                    //Key AESkey = my_gs.groupList.getAESKey();
                    Key AESkey = my_gs.userList.getAESkey(threadowner);
                    //encrypt response byte[] and send
                    Cipher cipher = Cipher.getInstance("AES");
                    cipher.init(Cipher.ENCRYPT_MODE, AESkey);
                    byte[] cipherBytes = cipher.doFinal(messageBytes);
                    output.writeObject(cipherBytes);
                    /* END AUSERTOGROUP */


                    /* START RUSERFROMGROUP */
                    // Client wants to remove user from a group
                } else if (message.getMessage().equals("RUSERFROMGROUP")) {
                    if (message.getObjContents().size() != 5) {
                        nonce++;
                        response = new Envelope("FAIL");
                    } else if ((Integer) message.getObjContents().get(4) == nonce) {
                        response = new Envelope("FAIL");

                        if (message.getObjContents().get(0) != null && message.getObjContents().get(1) != null && message.getObjContents().get(2) != null) {

                            String username = (String) message.getObjContents().get(0);
                            String groupname = (String) message.getObjContents().get(1);
                            UserToken yourToken = (UserToken) message.getObjContents().get(2);

                            String hash = (String) message.getObjContents().get(3);//get hash
                            String concat = username + groupname + yourToken.toString() + "RUSERFROMGROUP" + nonce; // reconstructs the hash like how it was done in the client
                            byte[] hasharray = concat.getBytes();
                            Mac mac = Mac.getInstance("HmacSHA1");
                            mac.init(my_gs.userList.getHMACkey(threadowner));
                            mac.update(hasharray);
                            String newhash = new String(mac.doFinal(), "UTF8");
                            nonce++;

                            if (hash.equals(newhash) != true)//check hashes for equality
                            {
                                System.out.println("HASH EQUALITY FAIL");// if fail then hash equality fails
                                response = new Envelope("FAIL");
                            } else {
                                if (deleteUserFromGroup(username, groupname, yourToken)) {
                                    // Success
                                    response = new Envelope("OK");
                                }
                            }

                        }
                    } else {
                        System.out.println("Nonce FAIL RUSERFROMGROUP");
                        response = new Envelope("FAIL");
                    }
                    if (response.getMessage().equals("OK")) {
                        String concat = "OK" + nonce; // reconstructs the hash like how it was done in the client
                        byte[] hasharray = concat.getBytes();
                        Mac mac = Mac.getInstance("HmacSHA1");
                        mac.init(my_gs.userList.getHMACkey(threadowner));
                        mac.update(hasharray);
                        String newhash = new String(mac.doFinal(), "UTF8");
                        response.addObject(newhash);
                        response.addObject(nonce);
                        nonce++;
                    } else {
                        String concat = "FAIL" + nonce; // reconstructs the hash like how it was done in the client
                        byte[] hasharray = concat.getBytes();
                        Mac mac = Mac.getInstance("HmacSHA1");
                        mac.init(my_gs.userList.getHMACkey(threadowner));
                        mac.update(hasharray);
                        String newhash = new String(mac.doFinal(), "UTF8");
                        response.addObject(newhash);
                        response.addObject(nonce);
                        nonce++;
                    }

                    byte[] messageBytes = Envelope.toByteArray(response);

                    //Key AESkey = my_gs.groupList.getAESKey();
                    Key AESkey = my_gs.userList.getAESkey(threadowner);
                    //encrypt response byte[] and send
                    Cipher cipher = Cipher.getInstance("AES");
                    cipher.init(Cipher.ENCRYPT_MODE, AESkey);
                    byte[] cipherBytes = cipher.doFinal(messageBytes);
                    output.writeObject(cipherBytes);
                    /* END RUSERFROMGROUP */


                    /* START DISCONNECT */
                    // Client wants to disconnect
                } else if (message.getMessage().equals("DISCONNECT")) {
                    if ((Integer) message.getObjContents().get(0) == nonce) {
                        socket.close(); // Close the socket
                        proceed = false; // End this communication loop
                    }
                    else {
                        System.out.println("Nonce FAIL DISCONNECT");
                        socket.close();
                        proceed = false;
                    }
/* END DISCONNECT */


                    /* START IDEC */
                    // Client wants to initially decrypt
                } else if (message.getMessage().equals("IDEC")) {
                    if ((Integer) message.getObjContents().get(4) == nonce) {
                        nonce++;
                        Boolean success;

                        response = new Envelope("FAIL");

                        File privKeyFile = new File("GPrivKey.bin");
                        FileInputStream fis = new FileInputStream(privKeyFile);
                        ObjectInputStream ois = new ObjectInputStream(fis);

                        PrivateKey key = (PrivateKey) ois.readObject(); //get the private key from the file and save it

                        byte[] plainText;

                        System.out.println("Begin Decryption...");
                        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
                        cipher.init(Cipher.DECRYPT_MODE, key);

                        //Decrypt username
                        plainText = cipher.doFinal((byte[]) message.getObjContents().get(0));
                        String username = new String(plainText);
                        threadowner = username;

                        //Decrypt password
                        plainText = cipher.doFinal((byte[]) message.getObjContents().get(1));
                        String pwd = new String(plainText);

                        //Decrypt client's AES key
                        plainText = cipher.doFinal((byte[]) message.getObjContents().get(2));
                        System.out.println("AES Decryption Complete");
                        SecretKey AESkey = new SecretKeySpec(plainText, "AES");

                        //Decrypt client's hash key
                        plainText = cipher.doFinal((byte[]) message.getObjContents().get(3));
                        System.out.println("HASH function decryption complete");
                        SecretKey hashkey = new SecretKeySpec(plainText, "HmacSHA1");

                        //check if user exists
                        if (my_gs.userList.checkUser(username)) {
                            //check user's password
                            if (my_gs.userList.checkPassword(username, pwd)) {
                                success = true;

                                //add AES key to user list
                                my_gs.userList.addAESkey(username, AESkey);  // change this to add the key to the userlist

                                //add hash function to userlist
                                my_gs.userList.addHMACkey(username, hashkey);

                                //create response and convert to byte[]
                                response = new Envelope("OK");
                                response.addObject(success);
                                response.addObject(nonce);
                                nonce++;

                                // password did not match
                            } else {
                                System.out.println("Password did not match");
                                success = false;

                                //create response and convert to byte[]
                                response = new Envelope("FAIL");
                                response.addObject(success);
                            }
                        } else {
                            // Success
                            success = false;

                            //create response and convert to byte[]
                            response = new Envelope("FAIL");
                            response.addObject(success);
                        }
                        byte[] responseBytes = Envelope.toByteArray(response);

                        System.out.println("Returning success: " + success);

                        //encrypt response byte[] and send
                        cipher = Cipher.getInstance("AES");
                        cipher.init(Cipher.ENCRYPT_MODE, AESkey);
                        byte[] cipherBytes = cipher.doFinal(responseBytes);
                        output.writeObject(cipherBytes);
                    } else {
                        response = new Envelope("FAIL");
                        byte[] responseBytes = Envelope.toByteArray(response);
                        output.writeObject(responseBytes);
                    }
                    /* END IDEC */


                    /* START GKEY */
                    // Client wants to get the public key from the gserver
                } else if (message.getMessage().equals("GKEY")) {
                    if ((Integer) message.getObjContents().get(0) == nonce) {
                        nonce++;
                        PublicKey pubKey;

                        // attempt to retrieve pubKey from disk, send hash to client
                        try {
                            File pubF = new File("GPubKey.bin");
                            FileInputStream fis = new FileInputStream(pubF);
                            ObjectInputStream ois = new ObjectInputStream(fis);

                            pubKey = (PublicKey) ois.readObject();

                            // if file DNE, make it and genereate key pair and write to disk
                        } catch (FileNotFoundException fnfe) {
                            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
                            keyGen.initialize(1024);

                            KeyPair key = keyGen.genKeyPair();

                            pubKey = key.getPublic();
                            PrivateKey privKey = key.getPrivate();

                            // write public key to disk
                            File pubF = new File("GPubKey.bin");
                            pubF.createNewFile();
                            FileOutputStream fos = new FileOutputStream(pubF);
                            ObjectOutputStream oos = new ObjectOutputStream(fos);
                            oos.writeObject(pubKey);

                            // write private key to disk
                            File privF = new File("GPrivKey.bin");
                            privF.createNewFile();
                            fos = new FileOutputStream(privF);
                            oos = new ObjectOutputStream(fos);
                            oos.writeObject(privKey);
                        }

                        // construct envelope and send to client
                        message = new Envelope("OK");
                        message.addObject(pubKey);
                        message.addObject(nonce);
                        nonce++;

                        output.writeObject(message);
                        /* END GKEY */
                    } else {
                        message = new Envelope("FAIL");
                        output.writeObject(message);
                    }

                    /* START CPWD */
                    // Client wants to create a user
                } else if (message.getMessage().equals("CPWD")) {

                    if (message.getObjContents().size() != 5) {
                        response = new Envelope("FAIL");
                        nonce++;
                    } else if ((Integer) message.getObjContents().get(4) == nonce) {
                        response = new Envelope("FAIL");

                        String oldPwd = (String) message.getObjContents().get(0);
                        String newPwd = (String) message.getObjContents().get(1);
                        UserToken token = (UserToken) message.getObjContents().get(2);
                        String hash = (String) message.getObjContents().get(3);


                        String concat = oldPwd + newPwd + token.toString() + "CPWD" + nonce; // reconstructs the hash like how it was done in the client
                        byte[] hasharray = concat.getBytes();
                        Mac mac = Mac.getInstance("HmacSHA1");
                        mac.init(my_gs.userList.getHMACkey(threadowner));
                        mac.update(hasharray);
                        String newhash = new String(mac.doFinal(), "UTF8");
                        nonce++;

                        if (hash.equals(newhash) != true)//check hashes for equality
                        {
                            System.out.println("HASH EQUALITY FAIL");
                            response = new Envelope("FAIL");
                        } else {
                            String username = token.getSubject();

                            //check if oldPwd == subject's actual pwd
                            if (my_gs.userList.checkPassword(username, oldPwd)) {
                                my_gs.userList.changePwd(username, newPwd);
                                response = new Envelope("OK");
                            }
                        }
                    } else {
                        nonce++;
                        System.out.println("Nonce FAIL CPWD");
                        response = new Envelope("FAIL");
                    }
                    if (response.getMessage().equals("OK")) {
                        String concat = "OK" + nonce; // reconstructs the hash like how it was done in the client
                        byte[] hasharray = concat.getBytes();
                        Mac mac = Mac.getInstance("HmacSHA1");
                        mac.init(my_gs.userList.getHMACkey(threadowner));
                        mac.update(hasharray);
                        String newhash = new String(mac.doFinal(), "UTF8");
                        response.addObject(newhash);
                        response.addObject(nonce);
                        nonce++;
                    } else {
                        String concat = "FAIL" + nonce; // reconstructs the hash like how it was done in the client
                        byte[] hasharray = concat.getBytes();
                        Mac mac = Mac.getInstance("HmacSHA1");
                        mac.init(my_gs.userList.getHMACkey(threadowner));
                        mac.update(hasharray);
                        String newhash = new String(mac.doFinal(), "UTF8");
                        response.addObject(newhash);
                        response.addObject(nonce);
                        nonce++;
                    }

                    byte[] messageBytes = Envelope.toByteArray(response);

                    Key AESkey = my_gs.userList.getAESkey(threadowner);
                    //encrypt response byte[] and send
                    Cipher cipher = Cipher.getInstance("AES");
                    cipher.init(Cipher.ENCRYPT_MODE, AESkey);
                    byte[] cipherBytes = cipher.doFinal(messageBytes);
                    output.writeObject(cipherBytes);
                    /* END CPWD */

                    /* START GFILEKEYS */
                } else if (message.getMessage().equals("GFILEKEYS")) {
                    if (message.getObjContents().size() != 2) {                                 // change this to 2
                        System.out.println("MESSAGE SIZE = " + message.getObjContents().size());
                        response = new Envelope("FAIL");

                        byte[] messageBytes = Envelope.toByteArray(response);

                        Key AESkey = my_gs.userList.getAESkey(threadowner);
                        //encrypt response byte[] and send
                        Cipher cipher = Cipher.getInstance("AES");
                        cipher.init(Cipher.ENCRYPT_MODE, AESkey);
                        byte[] cipherBytes = cipher.doFinal(messageBytes);
                        output.writeObject(cipherBytes);
                    } else if ((Integer) message.getObjContents().get(1) == nonce) {
                        nonce++;
                        response = new Envelope("FAIL");

                        UserToken token = (UserToken) message.getObjContents().get(0);
                        ArrayList<String> groups = (ArrayList<String>) token.getGroups();
                        HashMap<String, ArrayList<Key>> usersGroupKeys = new HashMap<String, ArrayList<Key>>();

                        for (int i = 0; i < groups.size(); i++) {
                            usersGroupKeys.put(groups.get(i), my_gs.groupKeys.get(groups.get(i)));
                        }

                        /*     String hash = (String)message.getObjContents().get(1);
                        String concat = token.toString()+"GFILEKEYS"; // reconstructs the hash like how it was done in the client
                        byte[] hasharray = concat.getBytes();
                        Mac mac = Mac.getInstance("HmacSHA1");
                        mac.init(my_gs.userList.getHMACkey(threadowner));
                        mac.update(hasharray);
                        String newhash = new String( mac.doFinal(), "UTF8");

                        if(hash.equals(newhash)!=true)//check hashes for equality
                        {
                        System.out.println("HASH EQUALITY FAIL");
                        response = new Envelope("FAIL");
                        response.addObject(null);
                        }
                        else
                        {   */

                        response = new Envelope("OK");
                        response.addObject(usersGroupKeys);
                        response.addObject(nonce);
                        nonce++;

                        byte[] messageBytes = Envelope.toByteArray(response);

                        Key AESkey = my_gs.userList.getAESkey(threadowner);
                        //encrypt response byte[] and send
                        Cipher cipher = Cipher.getInstance("AES");
                        cipher.init(Cipher.ENCRYPT_MODE, AESkey);
                        byte[] cipherBytes = cipher.doFinal(messageBytes);
                        output.writeObject(cipherBytes);
                    } else {
                        System.out.println("GFILEKEYS FAIL");
                        response = new Envelope("FAIL");

                        byte[] messageBytes = Envelope.toByteArray(response);

                        Key AESkey = my_gs.userList.getAESkey(threadowner);
                        //encrypt response byte[] and send
                        Cipher cipher = Cipher.getInstance("AES");
                        cipher.init(Cipher.ENCRYPT_MODE, AESkey);
                        byte[] cipherBytes = cipher.doFinal(messageBytes);
                        output.writeObject(cipherBytes);
                    }
                    // invalid packet
                } else {
                    response = new Envelope("FAIL");
                    byte[] messageBytes = Envelope.toByteArray(response);

                    //Key AESkey = my_gs.groupList.getAESKey();
                    Key AESkey = my_gs.userList.getAESkey(threadowner);
                    //encrypt response byte[] and send
                    Cipher cipher = Cipher.getInstance("AES");
                    cipher.init(Cipher.ENCRYPT_MODE, AESkey);
                    byte[] cipherBytes = cipher.doFinal(messageBytes);
                    output.writeObject(cipherBytes);
                }
            } while (proceed);


        } catch (SocketException se) {
            System.err.println("Socket Reset");


        } catch (EOFException eof) {
            //DO NOTHING
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace(System.err);


        }

    }

    private ArrayList<String> listMembers(String groupname, UserToken yourToken) {
        if (my_gs.groupList.getGroupMembers(groupname).contains(yourToken.getSubject())) {
            ArrayList<String> memberList = my_gs.groupList.getGroupMembers(groupname);


            return memberList;


        }

        return null;


    }

    private ArrayList<String> listGroups(UserToken yourToken) {
        return my_gs.userList.getUserGroups(yourToken.getSubject());


    }

    private boolean addUserToGroup(String username, String groupname, UserToken token) {
        String requester = token.getSubject();

        // Check if requester exists


        if (my_gs.userList.checkUser(requester)) {

            if (!my_gs.groupList.checkGroup(groupname)) {
                my_gs.groupList.addGroup(groupname);


            }

            ArrayList<String> admins = my_gs.groupList.getGroupAdmins(groupname);


            // requester needs to be an administrator


            if (admins.contains(requester)) {

                //check if user already in group
                ArrayList<String> groupMems = my_gs.groupList.getGroupMembers(groupname);


                boolean userInGroup = false;


                for (String mem : groupMems) {
                    if (mem.equals(username)) {
                        userInGroup = true;


                    }
                }

                if (!userInGroup) {
                    my_gs.userList.addGroup(username, groupname);

                    my_gs.groupList.addMember(groupname, username);



                    return true;


                } else {
                    System.out.println("Failed to add user to group " + groupname);


                    return false;


                }
            } else {
                System.out.println("Requester failed admin check");


                return false; // requester not an administrator


            }
        } else {
            System.out.println("Requester does not exist");


            return false; // requester does not exist


        }
    }

    private boolean deleteUserFromGroup(String username, String groupname, UserToken token) {
        String requester = token.getSubject();

        //Check if requester is admin of group
        ArrayList<String> admins = my_gs.groupList.getGroupAdmins(groupname);


        if (my_gs.userList.checkUser(token.getSubject())) {
            //requester needs to be an administrator
            if (admins.contains(requester)) {
                //Does user already exist?
                my_gs.userList.removeGroup(username, groupname);
                my_gs.groupList.removeMember(groupname, username);
                my_gs.generateNewFileKey(groupname);


                return true;


            } else {
                return false; //requester not an administrator


            }
        } else {
            return false; //requester does not exist


        }
    }

    private boolean createGroup(String groupname, UserToken yourToken) {
        String requester = yourToken.getSubject();


        if (my_gs.userList.checkUser(requester)) {
            if (my_gs.groupList.checkGroup(groupname)) {
                return false;


            } else {
                my_gs.groupList.addGroup(groupname);
                my_gs.groupList.addAdmin(groupname, requester);
                my_gs.groupList.addMember(groupname, requester);
                my_gs.userList.addGroup(requester, groupname);
                my_gs.userList.addOwnership(requester, groupname);
                my_gs.generateNewFileKey(groupname);



                return true;


            }
        } else {
            return false;


        }
    }

    private boolean deleteGroup(String groupname, UserToken yourToken) {
        String requester = yourToken.getSubject();



        if (my_gs.userList.checkUser(requester)) {
            ArrayList<String> temp = my_gs.userList.getUserGroups(requester);



            if (temp.contains(groupname)) {
                if (my_gs.groupList.getGroupAdmins(groupname).contains(requester)) {
                    temp = my_gs.groupList.getGroupMembers(groupname);



                    for (int i = 0; i
                            < temp.size(); i++) {
                        my_gs.userList.removeGroup(temp.get(i), groupname);


                    }

                    temp = my_gs.groupList.getGroupAdmins(groupname);



                    for (int i = 0; i
                            < temp.size(); i++) {
                        my_gs.userList.removeGroup(temp.get(i), groupname);


                    }

                    my_gs.groupList.deleteGroup(groupname);
                    my_gs.groupKeys.remove(groupname);


                    return true;


                } else {
                    return false;


                }
            } else {
                return false;


            }
        } else {
            return false;


        }
    }

    //signs tokens
    private byte[] signToken(String issuer, String subject, List<String> groupList, String fileServerName) {
        try {
            //concatenate contents of token
            String contents = issuer + ";" + subject;


            for (String group : groupList) {
                contents = contents + ";" + group;


            }
            contents = contents + ";" + fileServerName;



            byte[] tokenBytes = contents.getBytes();

            // retrieve private key from disk
            File privKeyFile = new File("GPrivKey.bin");
            FileInputStream fis = new FileInputStream(privKeyFile);
            ObjectInputStream ois = new ObjectInputStream(fis);

            PrivateKey privKey = (PrivateKey) ois.readObject();


            //generate signature from contents
            Signature sig = Signature.getInstance("SHA1withRSA");
            sig.initSign(privKey);
            sig.update(tokenBytes);


            return sig.sign();



        } catch (IOException ioe) {
            System.err.println("IOException in signToken");


            return null;


        } catch (ClassNotFoundException cnfe) {
            System.err.println("CNFE in signToken");


            return null;


        } catch (SignatureException ex) {
            ex.printStackTrace(System.err);


            return null;


        } catch (InvalidKeyException ex) {
            ex.printStackTrace(System.err);


            return null;


        } catch (NoSuchAlgorithmException ex) {
            ex.printStackTrace(System.err);


            return null;


        }
    }

    // Method to create tokens
    private UserToken createToken(String username, String fileServerName) {
        // Check that user exists
        if (my_gs.userList.checkUser(username)) {
            //generate signature w/ private key
            byte[] signature = signToken(my_gs.name, username, my_gs.userList.getUserGroups(username), fileServerName);

            // Issue a new token with server's name, user's name, user's groups, and a signature
            UserToken yourToken = new Token(my_gs.name, username, fileServerName, my_gs.userList.getUserGroups(username), signature);



            return yourToken;


        } else {
            return null;


        }
    }

    // Method to create a user
    private boolean createUser(String username, String password, UserToken yourToken) {
        String requester = yourToken.getSubject();

        // Check if requester exists


        if (my_gs.userList.checkUser(requester)) {
            // Get the user's groups
            ArrayList<String> temp = my_gs.userList.getUserGroups(requester);

            // requester needs to be an administrator


            if (temp.contains("ADMIN")) {
                // Does user already exist?
                if (my_gs.userList.checkUser(username)) {
                    // User already exists
                    return false;


                } else {
                    my_gs.userList.addUser(username, password);


                    return true;


                }
            } else {
                // requester not an administrator
                return false;


            }
        } else {
            return false; // requester does not exist


        }
    }

    // Method to delete a user
    private boolean deleteUser(String username, UserToken yourToken) {
        String requester = yourToken.getSubject();

        // Does requester exist?


        if (my_gs.userList.checkUser(requester)) {
            ArrayList<String> temp = my_gs.userList.getUserGroups(requester);

            // requester needs to be an administer


            if (temp.contains("ADMIN")) {

                // Does user exist?
                if (my_gs.userList.checkUser(username)) {
                    // User needs deleted from the groups they belong
                    ArrayList<String> deleteFromGroups = new ArrayList<String>();

                    // This will produce a hard copy of the list of groups this user belongs


                    for (int index = 0; index
                            < my_gs.userList.getUserGroups(username).size(); index++) {
                        deleteFromGroups.add(my_gs.userList.getUserGroups(username).get(index));


                    }

                    // Delete the user from the groups and generate new file encryption keys
                    // If user is the owner, removeMember will automatically delete group!
                    for (int index = 0; index
                            < deleteFromGroups.size(); index++) {
                        my_gs.groupList.removeMember(deleteFromGroups.get(index), username);
                        my_gs.generateNewFileKey(deleteFromGroups.get(index));


                    } // If groups are owned, they must be deleted
                    ArrayList<String> deleteOwnedGroup = new ArrayList<String>();

                    // Make a hard copy of the user's ownership list


                    for (int index = 0; index
                            < my_gs.userList.getUserOwnership(username).size(); index++) {
                        deleteOwnedGroup.add(my_gs.userList.getUserOwnership(username).get(index));


                    }

                    // Delete owned groups
                    for (int index = 0; index
                            < deleteOwnedGroup.size(); index++) {
                        // Use the delete group method. Token must be created for this action
                        byte[] signature = signToken(my_gs.name, username, deleteOwnedGroup, "");
                        deleteGroup(
                                deleteOwnedGroup.get(index), new Token(my_gs.name, username, "", deleteOwnedGroup, signature));


                    } // Delete the user from the user list
                    my_gs.userList.deleteUser(username);



                    return true;


                } else {
                    return false; // User does not exist



                }
            } else {
                // requester is not an administer
                return false;


            }
        } else {
            return false; // requester does not exist

        }
    }
}
