/* File worker thread handles the business of uploading, downloading, and removing files for clients with valid tokens */

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.util.List;
import java.util.ArrayList;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.SocketException;
import java.security.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.*;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class FileThread extends Thread {

    private final Socket socket;
    private int nonce;

    public FileThread(Socket _socket) {
        socket = _socket;
    }

    @Override
    public void run() {
        boolean proceed = true;
        try {

            System.out.println("*** New connection from " + socket.getInetAddress() + ":" + socket.getPort() + "***");
            final ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
            final ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
            Envelope response;
            nonce = 0;

            do {
                Envelope e;
                byte[] initMessage = (byte[]) input.readObject();

                //unencrypted case
                if (Envelope.getEnvelopefromBytes(initMessage) != null) {
                    e = Envelope.getEnvelopefromBytes(initMessage);

                    //encrypted case;
                } else {
                    File AESfile = new File("FAESKey.bin");
                    FileInputStream fis = new FileInputStream(AESfile);
                    ObjectInputStream ois = new ObjectInputStream(fis);
                    Key AESkey = (Key) ois.readObject();

                    File HASHfile = new File("FHASHKey.bin");
                    fis = new FileInputStream(HASHfile);
                    ois = new ObjectInputStream(fis);
                    Key HMACkey = (Key) ois.readObject();

                    //receive byte[] and decrypt
                    Cipher cipher = Cipher.getInstance("AES");
                    cipher.init(Cipher.DECRYPT_MODE, AESkey);
                    byte[] plainBytes = cipher.doFinal(initMessage);

                    //reconstruct Envelope object from decrypted byte[]
                    e = Envelope.getEnvelopefromBytes(plainBytes);
                }

                System.out.println("Request received: " + e.getMessage());
                System.out.println("First object: " + e.getObjContents().get(0));

                // Handler to list files that this user is allowed to see
                if (e.getMessage().equals("LFILES")) {
                    if ((Integer) e.getObjContents().get(2) == nonce) {
                        Token t = (Token) e.getObjContents().get(0);  // gets token from e
                        String hash = (String) e.getObjContents().get(1);
                        String concat = t.toString() + "LFILES" + nonce; // reconstructs the hash
                        byte[] hasharray = concat.getBytes();
                        Mac mac = Mac.getInstance("HmacSHA1");
                        File HASHfile = new File("FHASHKey.bin");
                        FileInputStream fis = new FileInputStream(HASHfile);
                        ObjectInputStream ois = new ObjectInputStream(fis);
                        Key HMACkey = (Key) ois.readObject();
                        mac.init(HMACkey);
                        mac.update(hasharray);
                        String newhash = new String(mac.doFinal(), "UTF8");
                        nonce++;
                        //check hashes for equality
                        if (hash.equals(newhash) != true) {
                            System.out.println("HASH EQUALITY FAIL");
                            response = new Envelope("FAIL");
                        } else {

                            String issuer = t.getIssuer();                          // gets the issuer from the token
                            String subject = t.getSubject();                        // gets the subject of the token
                            List<String> groups = t.getGroups();                    // gets the groups the token says you are a part of
                            List<ShareFile> filelist = new ArrayList<ShareFile>();  // creates a new filelist to hold the temp file names
                            List<String> finallist = new ArrayList<String>();       // will hold the filenames the user owns
                            filelist = FileServer.fileList.getFiles();              // jams every file in existence in this sucker

                            for (int i = 0; i < filelist.size(); i++) {
                                ShareFile sf = filelist.get(i);                     // sets the sharefile to a file in the list
                                if (t.getGroups().contains(sf.getGroup())) {
                                    finallist.add(sf.getPath());
                                }
                            }
                            response = new Envelope("OK");                          // sets the response to OK
                            response.addObject(finallist);                          // adds the file list to the response
                            concat = finallist.toString() + "OK" + nonce; // concatinates all of the objects in envelope
                            hasharray = concat.getBytes();// turn the concat into a byte array
                            mac = Mac.getInstance("HmacSHA1");
                            mac.init(HMACkey);
                            mac.update(hasharray);
                            newhash = new String(mac.doFinal(), "UTF8"); // turn the hash into a string for easy comparision!
                            response.addObject(newhash);
                            response.addObject(nonce);
                            nonce++;
                        }
                        File AESfile = new File("FAESKey.bin");
                        fis = new FileInputStream(AESfile);
                        ois = new ObjectInputStream(fis);
                        Key AESkey = (Key) ois.readObject();

                        byte[] responseBytes = Envelope.toByteArray(response);

                        //Encrypt envelope w/ AES
                        Cipher cipher = Cipher.getInstance("AES");
                        cipher.init(Cipher.ENCRYPT_MODE, AESkey);
                        byte[] cipherBytes = cipher.doFinal(responseBytes);

                        output.writeObject(cipherBytes);
                    } else {
                        System.out.println("Nonce FAIL LFILES");
                        socket.close();
                        proceed = false;
                    }
                    ////START UPLOAD/////////
                } else if (e.getMessage().equals("UPLOADF")) {

                    if (e.getObjContents().size() != 7) {
                        response = new Envelope("FAIL-BADCONTENTS");
                        System.out.println("FAIL-BADCONTENTS");
                    } else if (e.getObjContents().get(0) == null) {
                        response = new Envelope("FAIL-BADPATH");
                    } else if (e.getObjContents().get(1) == null) {
                        response = new Envelope("FAIL-BADGROUP");
                    } else if (e.getObjContents().get(2) == null) {
                        response = new Envelope("FAIL-BADTOKEN");
                    } else if ((Integer) e.getObjContents().get(6) == nonce) {
                        String remotePath = (String) e.getObjContents().get(0);
                        String group = (String) e.getObjContents().get(1);
                        UserToken yourToken = (UserToken) e.getObjContents().get(2); //Extract token
                        int keyNum = (Integer) e.getObjContents().get(3);
                        byte[] initialVector = (byte[]) e.getObjContents().get(4);
                        IvParameterSpec ivs = new IvParameterSpec(initialVector);

                        String hash = (String) e.getObjContents().get(5);
                        String concat = remotePath + group + yourToken.toString() + keyNum + "UPLOADF" + nonce; // reconstructs the hash
                        byte[] hasharray = concat.getBytes();
                        Mac mac = Mac.getInstance("HmacSHA1");
                        File HASHfile = new File("FHASHKey.bin");
                        FileInputStream fis = new FileInputStream(HASHfile);
                        ObjectInputStream ois = new ObjectInputStream(fis);
                        Key HMACkey = (Key) ois.readObject();
                        mac.init(HMACkey);
                        mac.update(hasharray);
                        String newhash = new String(mac.doFinal(), "UTF8");
                        nonce++;

                        //check hashes for equality
                        if (hash.equals(newhash) != true) {
                            System.out.println("HASH EQUALITY FAIL1");
                            response = new Envelope("FAIL");
                        } else {
                            if (FileServer.fileList.checkFile(remotePath)) {
                                System.out.printf("Error: file already exists at %s\n", remotePath);
                                response = new Envelope("FAIL-FILEEXISTS"); //Success
                            } else if (!yourToken.getGroups().contains(group)) {
                                System.out.printf("Error: user missing valid token for group %s\n", group);
                                response = new Envelope("FAIL-UNAUTHORIZED");
                            } else {
                                File file = new File("shared_files/" + remotePath.replace('/', '_'));
                                file.createNewFile();
                                FileOutputStream fos = new FileOutputStream(file);
                                System.out.printf("Successfully created file %s\n", remotePath.replace('/', '_'));

                                response = new Envelope("READY"); //Success

                                File AESfile = new File("FAESKey.bin");
                                fis = new FileInputStream(AESfile);
                                ois = new ObjectInputStream(fis);
                                Key AESkey = (Key) ois.readObject();

                                concat = response.getMessage() + nonce; // concatinates all of the objects in envelope
                                hasharray = concat.getBytes();// turn the concat into a byte array
                                mac = Mac.getInstance("HmacSHA1");
                                mac.init(HMACkey);
                                mac.update(hasharray);
                                String stringhash = new String(mac.doFinal(), "UTF8"); // turn the hash into a string for easy comparision!
                                response.addObject(stringhash);
                                response.addObject(nonce);
                                nonce++;

                                //Encrypt envelope w/ AES
                                byte[] responseBytes = Envelope.toByteArray(response);
                                Cipher cipher = Cipher.getInstance("AES");
                                cipher.init(Cipher.ENCRYPT_MODE, AESkey);
                                byte[] cipherBytes = cipher.doFinal(responseBytes);

                                output.writeObject(cipherBytes);

                                byte[] responseCipherBytes = (byte[]) input.readObject();  // sent ready

                                //Decrypt response
                                cipher.init(Cipher.DECRYPT_MODE, AESkey);
                                responseBytes = cipher.doFinal(responseCipherBytes);

                                e = Envelope.getEnvelopefromBytes(responseBytes);
                                System.out.println("STARTING WHILE LOOP------- JUST SENT READY");
                                while (e.getMessage().compareTo("CHUNK") == 0) {
                                    if ((Integer) e.getObjContents().get(3) == nonce) {
                                        byte[] buf = (byte[]) e.getObjContents().get(0);
                                        Integer keynum = (Integer) e.getObjContents().get(1);
                                        hash = (String) e.getObjContents().get(2);
                                        concat = keynum + "CHUNK" + nonce; // reconstructs the hash
                                        hasharray = concat.getBytes();
                                        mac = Mac.getInstance("HmacSHA1");
                                        HASHfile = new File("FHASHKey.bin");
                                        fis = new FileInputStream(HASHfile);
                                        ois = new ObjectInputStream(fis);
                                        HMACkey = (Key) ois.readObject();
                                        mac.init(HMACkey);
                                        mac.update(hasharray);
                                        newhash = new String(mac.doFinal(), "UTF8");
                                        nonce++;

                                        //check hashes for equality
                                        if (hash.equals(newhash) != true) {
                                            System.out.println("HASH EQUALITY FAIL1");
                                            response = new Envelope("FAIL");
                                        } else {
                                            fos.write(buf);
                                            response = new Envelope("READY"); //Success

                                            concat = "READY" + nonce; // reconstructs the hash
                                            hasharray = concat.getBytes();
                                            mac = Mac.getInstance("HmacSHA1");
                                            HASHfile = new File("FHASHKey.bin");
                                            fis = new FileInputStream(HASHfile);
                                            ois = new ObjectInputStream(fis);
                                            HMACkey = (Key) ois.readObject();
                                            mac.init(HMACkey);
                                            mac.update(hasharray);
                                            newhash = new String(mac.doFinal(), "UTF8");
                                            response.addObject(newhash);
                                            response.addObject(nonce);
                                            nonce++;

                                            byte[] envBytes = Envelope.toByteArray(response);

                                            //Encrypt envelope w/ AES
                                            cipher = Cipher.getInstance("AES");
                                            cipher.init(Cipher.ENCRYPT_MODE, AESkey);
                                            cipherBytes = cipher.doFinal(envBytes);

                                            output.writeObject(cipherBytes);


                                            responseCipherBytes = (byte[]) input.readObject();

                                            //Decrypt response
                                            cipher = Cipher.getInstance("AES");
                                            cipher.init(Cipher.DECRYPT_MODE, AESkey);
                                            responseBytes = cipher.doFinal(responseCipherBytes);

                                            e = Envelope.getEnvelopefromBytes(responseBytes);
                                        }
                                    } else {
                                        System.out.println("Nonce FAIL UPLOADF1");
                                        socket.close();
                                        proceed = false;
                                    }
                                }
                                
                                if (e.getMessage().compareTo("EOF") == 0 && (Integer) e.getObjContents().get(1) == nonce) {
                                    hash = (String) e.getObjContents().get(0);
                                    concat = e.getMessage() + nonce; // reconstructs the hash
                                    hasharray = concat.getBytes();
                                    mac = Mac.getInstance("HmacSHA1");
                                    HASHfile = new File("FHASHKey.bin");
                                    fis = new FileInputStream(HASHfile);
                                    ois = new ObjectInputStream(fis);
                                    HMACkey = (Key) ois.readObject();
                                    mac.init(HMACkey);
                                    mac.update(hasharray);
                                    newhash = new String(mac.doFinal(), "UTF8");
                                    nonce++;

                                    if (hash.equals(newhash) != true)//check hashes for equality
                                    {
                                        System.out.println("HASH EQUALITY FAIL");
                                        response = new Envelope("FAIL");
                                    } else {
                                        System.out.printf("Transfer successful file %s\n", remotePath);
                                        System.out.println("KeyNum = " + keyNum);
                                        FileServer.fileList.addFile(yourToken.getSubject(), group, remotePath, keyNum, initialVector);
                                        response = new Envelope("OK"); //Success
                                    }
                                } else if ((Integer) e.getObjContents().get(1) != nonce) {
                                    System.out.println("Nonce FAIL UPLOADF2 " + nonce);
                                    socket.close();
                                    proceed = false;

                                } else {
                                    System.out.printf("Error reading file %s from client\n", remotePath);
                                    response = new Envelope("ERROR-TRANSFER"); //Success
                                }
                                fos.close();
                            }
                        }
                    } else {
                        response = new Envelope("FAIL");
                        System.out.println("Nonce FAIL UPLOADF3");
                        socket.close();
                        proceed = false;
                    }

                    File AESfile = new File("FAESKey.bin");
                    FileInputStream fis = new FileInputStream(AESfile);
                    ObjectInputStream ois = new ObjectInputStream(fis);
                    Key AESkey = (Key) ois.readObject();

                    String concat = response.getMessage() + nonce; // reconstructs the hash
                    byte[] hasharray = concat.getBytes();
                    Mac mac = Mac.getInstance("HmacSHA1");
                    File HASHfile = new File("FHASHKey.bin");
                    fis = new FileInputStream(HASHfile);
                    ois = new ObjectInputStream(fis);
                    Key HMACkey = (Key) ois.readObject();
                    mac.init(HMACkey);
                    mac.update(hasharray);
                    String newhash = new String(mac.doFinal(), "UTF8");

                    response.addObject(newhash);
                    response.addObject(nonce);
                    nonce++;

                    byte[] envBytes = Envelope.toByteArray(response);

                    //Encrypt envelope w/ AES
                    Cipher cipher = Cipher.getInstance("AES");
                    cipher.init(Cipher.ENCRYPT_MODE, AESkey);
                    byte[] cipherBytes = cipher.doFinal(envBytes);

                    output.writeObject(cipherBytes);

                } else if (e.getMessage().compareTo("DOWNLOADF") == 0) {
                    if ((Integer) e.getObjContents().get(3) == nonce) {
                        String remotePath = (String) e.getObjContents().get(0);
                        Token t = (Token) e.getObjContents().get(1);
                        String hash = (String) e.getObjContents().get(2);
                        String concat = remotePath + t.toString() + "DOWNLOADF" + nonce; // reconstructs the hash
                        byte[] hasharray = concat.getBytes();
                        Mac mac = Mac.getInstance("HmacSHA1");
                        File HASHfile = new File("FHASHKey.bin");
                        FileInputStream fis = new FileInputStream(HASHfile);
                        ObjectInputStream ois = new ObjectInputStream(fis);
                        Key HMACkey = (Key) ois.readObject();
                        mac.init(HMACkey);
                        mac.update(hasharray);
                        String newhash = new String(mac.doFinal(), "UTF8");
                        nonce++;

                        //check hashes for equality
                        if (hash.equals(newhash) != true) {
                            System.out.println("HASH EQUALITY FAIL");
                            response = new Envelope("FAIL");
                        } else {
                            ShareFile sf = FileServer.fileList.getFile("/" + remotePath);
                            if (sf == null) {
                                System.out.printf("Error: File %s doesn't exist\n", remotePath);
                                e = new Envelope("ERROR_FILEMISSING");

                            } else if (!t.getGroups().contains(sf.getGroup())) {
                                System.out.printf("Error user %s doesn't have permission\n", t.getSubject());
                                e = new Envelope("ERROR_PERMISSION");
                            } else {

                                try {
                                    File f = new File("shared_files/_" + remotePath.replace('/', '_'));
                                    if (!f.exists()) {
                                        System.out.printf("Error file %s missing from disk\n", "_" + remotePath.replace('/', '_'));
                                        e = new Envelope("ERROR_NOTONDISK");

                                    } else {
                                        fis = new FileInputStream(f);

                                        do {
                                            System.out.println("sending chunk");
                                            byte[] buf = new byte[1024];
                                            if (e.getMessage().compareTo("DOWNLOADF") != 0) {
                                                System.out.printf("Server error: %s\n", e.getMessage());
                                                break;
                                            }
                                            e = new Envelope("CHUNK");
                                            int n = fis.read(buf); //can throw an IOException
                                            if (n > 0) {
                                                System.out.printf(".");
                                            } else if (n < 0) {
                                                System.out.println("Read error");

                                            }

                                            e.addObject(buf);
                                            e.addObject(new Integer(n));
                                            e.addObject(sf);
                                            /////////////////////////////////////////////////////////////////////////////////////////////add hashing here
                                            concat = n + e.getMessage() + nonce; // concatinates all of the objects in envelope 
                                            hasharray = concat.getBytes();// turn the concat into a byte array
                                            mac = Mac.getInstance("HmacSHA1");
                                            mac.init(HMACkey);
                                            mac.update(hasharray);
                                            String stringhash = new String(mac.doFinal(), "UTF8"); // turn the hash into a string for easy comparision!
                                            e.addObject(stringhash);
                                            e.addObject(nonce);
                                            nonce++;

                                            File AESfile = new File("FAESKey.bin");
                                            FileInputStream AESfis = new FileInputStream(AESfile);
                                            ObjectInputStream AESois = new ObjectInputStream(AESfis);
                                            Key AESkey = (Key) AESois.readObject();

                                            byte[] envBytes = Envelope.toByteArray(e);

                                            //Encrypt envelope w/ AES
                                            Cipher cipher = Cipher.getInstance("AES");
                                            cipher.init(Cipher.ENCRYPT_MODE, AESkey);
                                            byte[] outCipherBytes = cipher.doFinal(envBytes);

                                            output.writeObject(outCipherBytes);

                                            byte[] inCipherBytes = (byte[]) input.readObject();

                                            //Decrypt response
                                            cipher.init(Cipher.DECRYPT_MODE, AESkey);
                                            byte[] responseBytes = cipher.doFinal(inCipherBytes);

                                            e = Envelope.getEnvelopefromBytes(responseBytes);

                                            if ((Integer) e.getObjContents().get(1) == nonce) {
                                                hash = (String) e.getObjContents().get(0);
                                                concat = e.getMessage() + nonce; // reconstructs the hash 
                                                hasharray = concat.getBytes();
                                                mac = Mac.getInstance("HmacSHA1");
                                                /*HASHfile = new File("FHASHKey.bin");
                                                FileInputStream fish = new FileInputStream(HASHfile);
                                                ois = new ObjectInputStream(fish);
                                                HMACkey = (Key) ois.readObject();*/
                                                mac.init(HMACkey);
                                                mac.update(hasharray);
                                                newhash = new String(mac.doFinal(), "UTF8");
                                                nonce++;

                                                if (hash.equals(newhash) != true)//check hashes for equality
                                                {
                                                    System.out.println("HASH EQUALITY FAIL");
                                                    response = new Envelope("FAIL");
                                                }
                                            } else {
                                                System.out.println("Nonce FAIL DOWNLOADF1");
                                                socket.close();
                                                proceed = false;
                                            }
                                        } while (fis.available() > 0);
                                        nonce--;
                                        System.out.println(nonce);
                                        System.out.println(e.getObjContents().get(1));
                                        //End download once file has been fully sent
                                        if (e.getMessage().compareTo("DOWNLOADF") == 0 && (Integer) e.getObjContents().get(1) == nonce) {
                                            nonce++;
                                            e = new Envelope("EOF");
                                            concat = e.getMessage() + nonce; // concatinates all of the objects in envelope 
                                            hasharray = concat.getBytes();// turn the concat into a byte array
                                            mac = Mac.getInstance("HmacSHA1");
                                            mac.init(HMACkey);
                                            mac.update(hasharray);
                                            String stringhash = new String(mac.doFinal(), "UTF8"); // turn the hash into a string for easy comparision!

                                            e.addObject(stringhash);
                                            e.addObject(nonce);
                                            nonce++;

                                            File AESfile = new File("FAESKey.bin");
                                            FileInputStream AESfis = new FileInputStream(AESfile);
                                            ObjectInputStream AESois = new ObjectInputStream(AESfis);
                                            Key AESkey = (Key) AESois.readObject();

                                            byte[] envBytes = Envelope.toByteArray(e);

                                            //Encrypt envelope w/ AES
                                            Cipher cipher = Cipher.getInstance("AES");
                                            cipher.init(Cipher.ENCRYPT_MODE, AESkey);
                                            byte[] outCipherBytes = cipher.doFinal(envBytes);

                                            output.writeObject(outCipherBytes);

                                            byte[] inCipherBytes = (byte[]) input.readObject();

                                            //Decrypt response
                                            cipher.init(Cipher.DECRYPT_MODE, AESkey);
                                            byte[] responseBytes = cipher.doFinal(inCipherBytes);

                                            e = Envelope.getEnvelopefromBytes(responseBytes);

                                            if ((Integer) e.getObjContents().get(1) == nonce) {
                                                hash = (String) e.getObjContents().get(0);
                                                concat = e.getMessage() + nonce; // reconstructs the hash 
                                                hasharray = concat.getBytes();
                                                mac = Mac.getInstance("HmacSHA1");
                                                HASHfile = new File("FHASHKey.bin");
                                                fis = new FileInputStream(HASHfile);
                                                ois = new ObjectInputStream(fis);
                                                HMACkey = (Key) ois.readObject();
                                                mac.init(HMACkey);
                                                mac.update(hasharray);
                                                newhash = new String(mac.doFinal(), "UTF8");
                                                nonce++;

                                                if (hash.equals(newhash) != true)//check hashes for equality
                                                {
                                                    System.out.println("HASH EQUALITY FAIL");

                                                }
                                                if (e.getMessage().compareTo("OK") == 0) {
                                                    System.out.printf("File data upload successful\n");
                                                } else {
                                                    System.out.printf("Upload failed: %s\n", e.getMessage());
                                                }
                                            } else {
                                                System.out.println("Nonce FAIL DOWNLOADF2");
                                                socket.close();
                                                proceed = false;
                                            }
                                        } else if ((Integer) e.getObjContents().get(1) != nonce) {
                                            System.out.println("Nonce FAIL DOWNLOADF3");
                                            socket.close();
                                            proceed = false;
                                        } else {
                                            System.out.printf("Upload failed: %s\n", e.getMessage());
                                        }
                                        fis.close();
                                    }
                                } catch (Exception e1) {
                                    System.err.println("Error: " + e.getMessage());
                                    e1.printStackTrace(System.err);
                                }
                            }
                            File AESfile = new File("FAESKey.bin");
                            FileInputStream AESfis = new FileInputStream(AESfile);
                            ObjectInputStream AESois = new ObjectInputStream(AESfis);
                            Key AESkey = (Key) AESois.readObject();

                            byte[] envBytes = Envelope.toByteArray(e);

                            //Encrypt envelope w/ AES
                            Cipher cipher = Cipher.getInstance("AES");
                            cipher.init(Cipher.ENCRYPT_MODE, AESkey);
                            byte[] cipherBytes = cipher.doFinal(envBytes);

                            output.writeObject(cipherBytes);
                        }
                    }
                } else if (e.getMessage().compareTo("DELETEF") == 0) {
                    if ((Integer) e.getObjContents().get(3) == nonce) {
                        String remotePath = (String) e.getObjContents().get(0);
                        Token t = (Token) e.getObjContents().get(1);
                        String hash = (String) e.getObjContents().get(2);

                        String concat = remotePath + t.toString() + "DELETEF" + nonce; // reconstructs the hash
                        byte[] hasharray = concat.getBytes();
                        Mac mac = Mac.getInstance("HmacSHA1");
                        File HASHfile = new File("FHASHKey.bin");
                        FileInputStream fis = new FileInputStream(HASHfile);
                        ObjectInputStream ois = new ObjectInputStream(fis);
                        Key HMACkey = (Key) ois.readObject();
                        mac.init(HMACkey);
                        mac.update(hasharray);
                        String newhash = new String(mac.doFinal(), "UTF8");
                        nonce++;

                        if (hash.equals(newhash) != true)//check hashes for equality
                        {
                            System.out.println("HASH EQUALITY FAIL");
                            response = new Envelope("FAIL");
                        } else {
                            ShareFile sf = FileServer.fileList.getFile("/" + remotePath);

                            if (!remotePath.startsWith("/")) {
                                System.out.println("DELETEF process iniated on " + remotePath);
                            }
                            if (sf == null) {
                                System.out.printf("Error: File %s doesn't exist\n", remotePath);
                                e = new Envelope("ERROR_DOESNTEXIST");
                            } else if (!t.getGroups().contains(sf.getGroup())) {
                                System.out.printf("Error user %s doesn't have permission\n", t.getSubject());
                                e = new Envelope("ERROR_PERMISSION");
                            } else {
                                try {
                                    File f = new File("shared_files/" + "_" + remotePath.replace('/', '_'));

                                    if (!f.exists()) {
                                        System.out.printf("Error file %s missing from disk\n", "_" + remotePath.replace('/', '_'));
                                        e = new Envelope("ERROR_FILEMISSING");
                                    } else if (f.delete()) {
                                        System.out.printf("File %s deleted from disk\n", "_" + remotePath.replace('/', '_'));
                                        FileServer.fileList.removeFile("/" + remotePath);
                                        e = new Envelope("OK");
                                    } else {
                                        System.out.printf("Error deleting file %s from disk\n", "_" + remotePath.replace('/', '_'));
                                        e = new Envelope("ERROR_DELETE");
                                    }
                                } catch (Exception e1) {
                                    System.err.println("Error: " + e1.getMessage());
                                    e1.printStackTrace(System.err);
                                    e = new Envelope(e1.getMessage());
                                }
                            }
                        }

                        concat = e.getMessage() + nonce; // concatinates all of the objects in envelope
                        hasharray = concat.getBytes();// turn the concat into a byte array
                        mac = Mac.getInstance("HmacSHA1");
                        mac.init(HMACkey);
                        mac.update(hasharray);
                        String stringhash = new String(mac.doFinal(), "UTF8"); // turn the hash into a string for easy comparision!
                        e.addObject(stringhash);
                        e.addObject(nonce);
                        nonce++;

                        File AESfile = new File("FAESKey.bin");
                        FileInputStream AESfis = new FileInputStream(AESfile);
                        ObjectInputStream AESois = new ObjectInputStream(AESfis);
                        Key AESkey = (Key) AESois.readObject();

                        byte[] envBytes = Envelope.toByteArray(e);

                        //Encrypt envelope w/ AES
                        Cipher cipher = Cipher.getInstance("AES");
                        cipher.init(Cipher.ENCRYPT_MODE, AESkey);
                        byte[] cipherBytes = cipher.doFinal(envBytes);

                        output.writeObject(cipherBytes);
                    }

                } else if (e.getMessage().equals("GETFPRINT")) {
                    System.out.println("MESSAGE NONCE: " + (Integer) e.getObjContents().get(0));
                    System.out.println("THREAD NONCE: " + nonce);
                    if ((Integer) e.getObjContents().get(0) == nonce) {
                        nonce++;
                        PublicKey pubKey;

                        // attempt to retrieve pubKey from disk, send hash to client
                        File pubF = new File("FPubKey.bin");

                        // if file DNE, make it and genereate key pair and write to disk
                        if (!pubF.exists()) {
                            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
                            keyGen.initialize(1024);

                            KeyPair key = keyGen.genKeyPair();

                            pubKey = key.getPublic();
                            PrivateKey privKey = key.getPrivate();

                            pubF.createNewFile();
                            FileOutputStream fos = new FileOutputStream(pubF);
                            ObjectOutputStream oos = new ObjectOutputStream(fos);
                            oos.writeObject(pubKey);
                            System.out.println("FPubKey.bin created");

                            File privF = new File("FPrivkey.bin");
                            privF.createNewFile();
                            fos = new FileOutputStream(privF);
                            oos = new ObjectOutputStream(fos);
                            oos.writeObject(privKey);
                            System.out.println("FPrivkey.bin created");

                        } else {
                            FileInputStream fis = new FileInputStream(pubF);
                            ObjectInputStream ois = new ObjectInputStream(fis);

                            pubKey = (PublicKey) ois.readObject();
                        }

                        // convert pubKey to byte[] for hashing
                        ByteArrayOutputStream bStream = new ByteArrayOutputStream();
                        ObjectOutputStream oStream = new ObjectOutputStream(bStream);
                        oStream.writeObject(pubKey);
                        byte[] keyBytes = bStream.toByteArray();

                        // hash pubKey to get fingerprint
                        MessageDigest md = MessageDigest.getInstance("SHA1");
                        md.update(keyBytes);
                        String fingerprint = new String(md.digest());

                        // construct envelope and send to client
                        e = new Envelope("OK");
                        e.addObject(pubKey);
                        e.addObject(fingerprint);
                        e.addObject(nonce);
                        nonce++;

                        output.writeObject(Envelope.toByteArray(e));
                    } else {
                        System.out.println("Nonce FAIL GETFPRINT");
                        socket.close();
                        proceed = false;
                    }
                } else if (e.getMessage().equals("SKEY")) { // what does SKEY stand for?
                    if ((Integer) e.getObjContents().get(2) == nonce) {
                        nonce++;
                        byte[] cipherKeyBytes = (byte[]) e.getObjContents().get(0);
                        byte[] hashKeyBytes = (byte[]) e.getObjContents().get(1);

                        File privF = new File("FPrivkey.bin");
                        FileInputStream fis = new FileInputStream(privF);
                        ObjectInputStream ois = new ObjectInputStream(fis);

                        PrivateKey privKey = (PrivateKey) ois.readObject();

                        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
                        cipher.init(Cipher.DECRYPT_MODE, privKey);
                        byte[] keyBytes = cipher.doFinal(cipherKeyBytes);
                        byte[] hashBytes = cipher.doFinal(hashKeyBytes);

                        SecretKey AESkey = new SecretKeySpec(keyBytes, "AES");
                        SecretKey hashkey = new SecretKeySpec(hashBytes, "HmacSHA1");

                        response = new Envelope("OK");
                        response.addObject(nonce);

                        //Encrypt w/ newly received AES key
                        byte[] responseBytes = Envelope.toByteArray(response);

                        //encrypt response byte[] and send
                        cipher = Cipher.getInstance("AES");
                        cipher.init(Cipher.ENCRYPT_MODE, AESkey);
                        byte[] cipherBytes = cipher.doFinal(responseBytes);
                        output.writeObject(cipherBytes);

                        File AESfile = new File("FAESKey.bin");
                        AESfile.createNewFile();
                        FileOutputStream fos = new FileOutputStream(AESfile);
                        ObjectOutputStream oos = new ObjectOutputStream(fos);
                        oos.writeObject(AESkey);

                        File HASHfile = new File("FHASHKey.bin");
                        HASHfile.createNewFile();
                        fos = new FileOutputStream(HASHfile);
                        oos = new ObjectOutputStream(fos);
                        oos.writeObject(hashkey);

                        fos.close();
                        oos.close();
                    } else {
                        System.out.println("Nonce FAIL SKEY");
                        socket.close();
                        proceed = false;
                    }
                } else if (e.getMessage().equals("SENDT")) {
                    if ((Integer) e.getObjContents().get(2) == nonce) {
                        UserToken token = (UserToken) e.getObjContents().get(0); // gets the token getToken returned
                        String hash = (String) e.getObjContents().get(1);

                        String concat = token.toString() + "SENDT" + nonce; // reconstructs the hash
                        byte[] hasharray = concat.getBytes();
                        Mac mac = Mac.getInstance("HmacSHA1");
                        File HASHfile = new File("FHASHKey.bin");
                        FileInputStream fis = new FileInputStream(HASHfile);
                        ObjectInputStream ois = new ObjectInputStream(fis);
                        Key HMACkey = (Key) ois.readObject();
                        mac.init(HMACkey);
                        mac.update(hasharray);
                        String newhash = new String(mac.doFinal(), "UTF8");
                        nonce++;

                        //check hashes for equality
                        if (hash.equals(newhash) != true) {
                            System.out.println("HASH EQUALITY FAIL");
                            response = new Envelope("FAIL");
                        } else if (e.getMessage().equals("DISCONNECT")) {
                            socket.close();
                            proceed = false;
                        } else {
                            Boolean success = verifyToken(token);

                            e = new Envelope("OK");
                            concat = "OK" + nonce; // concatinates all of the objects in envelope
                            hasharray = concat.getBytes();// turn the concat into a byte array
                            mac = Mac.getInstance("HmacSHA1");
                            mac.init(HMACkey);
                            mac.update(hasharray);
                            String stringhash = new String(mac.doFinal(), "UTF8"); // turn the hash into a string for easy comparision!
                            e.addObject(stringhash);
                            e.addObject(nonce);
                            nonce++;

                            File AESfile = new File("FAESKey.bin");
                            fis = new FileInputStream(AESfile);
                            ois = new ObjectInputStream(fis);


                            Key AESkey = (Key) ois.readObject();
                            Cipher cipher = Cipher.getInstance("AES");
                            cipher.init(Cipher.ENCRYPT_MODE, AESkey);
                            byte[] cipherBytes = cipher.doFinal(Envelope.toByteArray(e));
                            output.writeObject(cipherBytes);
                        }

                    } else {
                        System.out.println("Nonce FAIL SENDT");
                        socket.close();
                        proceed = false;
                    }
                } else {
                    System.out.println("END");
                }

            } while (proceed);
        } catch (SocketException e) {
            System.out.println("Socket reset");
        } catch (EOFException eof) {
            //DO NOTHING
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace(System.err);
        }
    }

    public boolean verifyToken(UserToken token) {
        FileInputStream fis = null;
        ObjectInputStream ois = null;
        try {
            String tokenContents = token.getIssuer() + ";" + token.getSubject();
            for (String group : token.getGroups()) {
                tokenContents = tokenContents + ";" + group;
            }
            tokenContents = tokenContents + ";" + token.getFileServerName();

            byte[] tokenBytes = tokenContents.getBytes();

            File pubF = new File("GPubKey.bin");
            fis = new FileInputStream(pubF);
            ois = new ObjectInputStream(fis);
            PublicKey pubKey = (PublicKey) ois.readObject();

            Signature sig = Signature.getInstance("SHA1withRSA");
            sig.initVerify(pubKey);
            sig.update(tokenBytes);
            return sig.verify(token.getSignature());

        } catch (ClassNotFoundException ex) {
            Logger.getLogger(FileThread.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(FileThread.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SignatureException ex) {
            ex.printStackTrace(System.err);
            return false;
        } catch (InvalidKeyException ex) {
            ex.printStackTrace(System.err);
            return false;
        } catch (NoSuchAlgorithmException ex) {
            ex.printStackTrace(System.err);
            return false;
        } finally {
            try {
                fis.close();
            } catch (IOException ex) {
                Logger.getLogger(FileThread.class.getName()).log(Level.SEVERE, null, ex);
            }
            try {
                ois.close();
            } catch (IOException ex) {
                Logger.getLogger(FileThread.class.getName()).log(Level.SEVERE, null, ex);
            }
            return true;
        }
    }
}
