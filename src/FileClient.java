/* FileClient provides all the client functionality regarding the file server */

import java.io.*;
import java.util.List;
import java.security.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.*;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;

public class FileClient extends Client implements FileClientInterface {

    private PublicKey pubKey;
    private Key AESkey;
    private Key HMACkey;

    public boolean delete(String filename, UserToken token) {
        try {
            String remotePath;
            if (filename.charAt(0) == '/') {
                remotePath = filename.substring(1);
            } else {
                remotePath = filename;
            }
            Envelope env = new Envelope("DELETEF"); //Success
            env.addObject(remotePath);
            env.addObject(token);
            String concat = remotePath + token.toString() + "DELETEF" + nonce; // concatinates all of the objects in envelope
            byte[] hasharray = concat.getBytes();// turn the concat into a byte array
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(HMACkey);
            mac.update(hasharray);
            String stringhash = new String(mac.doFinal(), "UTF8"); // turn the hash into a string for easy comparision!
            env.addObject(stringhash);
            env.addObject(nonce);
            nonce++;

            byte[] envBytes = Envelope.toByteArray(env);

            //Encrypt envelope w/ AES
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, AESkey);
            byte[] cipherBytes = cipher.doFinal(envBytes);

            output.writeObject(cipherBytes);

            byte[] responseCipherBytes = (byte[]) input.readObject();

            //Decrypt response
            cipher.init(Cipher.DECRYPT_MODE, AESkey);
            byte[] responseBytes = cipher.doFinal(responseCipherBytes);

            env = Envelope.getEnvelopefromBytes(responseBytes);
            System.out.println(env.getMessage());
            if ((Integer) env.getObjContents().get(1) == nonce) {
                String hash = (String) env.getObjContents().get(0);
                concat = env.getMessage() + nonce; // reconstructs the hash
                hasharray = concat.getBytes();
                mac = Mac.getInstance("HmacSHA1");
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
                    return false;
                }

                if (env.getMessage().compareTo("OK") == 0) {
                    System.out.printf("File %s deleted successfully\n", filename);
                } else {
                    System.out.printf("Error deleting file %s (%s)\n", filename, env.getMessage());
                    return false;
                }
            }
        } catch (IllegalBlockSizeException ex) {
            ex.printStackTrace(System.err);
        } catch (BadPaddingException ex) {
            ex.printStackTrace(System.err);
        } catch (InvalidKeyException ex) {
            ex.printStackTrace(System.err);
        } catch (NoSuchAlgorithmException ex) {
            ex.printStackTrace(System.err);
        } catch (NoSuchPaddingException ex) {
            ex.printStackTrace(System.err);
        } catch (IOException e1) {
            e1.printStackTrace(System.err);
        } catch (ClassNotFoundException e1) {
            e1.printStackTrace(System.err);
        }

        return true;
    }

    public boolean sendToken(UserToken token) {
        try {
            Envelope env = new Envelope("SENDT"); //Success
            env.addObject(token);

            String concat = token.toString() + "SENDT" + nonce; // concatinates all of the objects in envelope
            byte[] hasharray = concat.getBytes();// turn the concat into a byte array
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(HMACkey);
            mac.update(hasharray);
            String stringhash = new String(mac.doFinal(), "UTF8"); // turn the hash into a string for easy comparision!
            env.addObject(stringhash);
            env.addObject(nonce);
            nonce++;

            byte[] envBytes = Envelope.toByteArray(env);

            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, AESkey);
            byte[] cipherBytes = cipher.doFinal(envBytes);
            output.writeObject(cipherBytes);

            //receive from thread
            byte[] responseCipherBytes = (byte[]) input.readObject();

            //Decrypt response
            cipher.init(Cipher.DECRYPT_MODE, AESkey);
            byte[] responseBytes = cipher.doFinal(responseCipherBytes);

            env = Envelope.getEnvelopefromBytes(responseBytes);
            if ((Integer) env.getObjContents().get(1) == nonce) {
                String hash = (String) env.getObjContents().get(0);
                concat = "OK" + nonce; // reconstructs the hash
                hasharray = concat.getBytes();
                mac = Mac.getInstance("HmacSHA1");
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
                    return false;
                }

                if (env.getMessage().compareTo("OK") == 0) {
                    System.out.printf("OK RECEIVED");
                    return true;
                } else {
                    System.out.printf("FAILURE");
                    return false;
                }
            }
        } catch (Exception e) {
            System.out.println(e);
        }
        return false;
    }

    public boolean download(String sourceFile, String destFile, UserToken token, HashMap<String, ArrayList<Key>> keys) {
        try {
            destFile = "." + destFile;

            if (sourceFile.charAt(0) == '/') {
                sourceFile = sourceFile.substring(1);
            }

            File file = new File(destFile);

            if (!file.exists()) {
                file.createNewFile();

                FileOutputStream fos = new FileOutputStream(file);
                Envelope env = new Envelope("DOWNLOADF"); //Success
                env.addObject(sourceFile);
                env.addObject(token);
                String concat = sourceFile + token.toString() + "DOWNLOADF" + nonce; // concatinates all of the objects in envelope
                byte[] hasharray = concat.getBytes();// turn the concat into a byte array
                Mac mac = Mac.getInstance("HmacSHA1");
                mac.init(HMACkey);
                mac.update(hasharray);
                String stringhash = new String(mac.doFinal(), "UTF8"); // turn the hash into a string for easy comparision!
                env.addObject(stringhash);
                env.addObject(nonce);
                nonce++;

                byte[] envBytes = Envelope.toByteArray(env);

                //Encrypt envelope w/ AES
                Cipher cipher = Cipher.getInstance("AES");
                cipher.init(Cipher.ENCRYPT_MODE, AESkey);
                byte[] cipherBytes = cipher.doFinal(envBytes);

                output.writeObject(cipherBytes);                                               //here in download

                byte[] responseCipherBytes = (byte[]) input.readObject();

                //Decrypt response
                cipher.init(Cipher.DECRYPT_MODE, AESkey);
                byte[] responseBytes = cipher.doFinal(responseCipherBytes);

                env = Envelope.getEnvelopefromBytes(responseBytes);
                ShareFile sf = (ShareFile) env.getObjContents().get(2);
                int keyNum = sf.getKeyNum();
                ArrayList<Key> groupKeys = keys.get(sf.getGroup());
                Key key = groupKeys.get(keyNum);
                byte[] initialVector = sf.getIV();
                IvParameterSpec ivs = new IvParameterSpec(initialVector);
                byte[] decryptBuf = new byte[1024];

                while (env.getMessage().compareTo("CHUNK") == 0 && (Integer) env.getObjContents().get(4) == nonce) {
                    String hash = (String) env.getObjContents().get(3);
                    concat = (Integer) env.getObjContents().get(1) + env.getMessage() + nonce; // reconstructs the hash 
                    System.out.println("Concat:" + concat);
                    hasharray = concat.getBytes();
                    mac = Mac.getInstance("HmacSHA1");
                    File HASHfile = new File("FHASHKey.bin");
                    FileInputStream fis = new FileInputStream(HASHfile);
                    ObjectInputStream ois = new ObjectInputStream(fis);
                    HMACkey = (Key) ois.readObject();
                    mac.init(HMACkey);
                    mac.update(hasharray);
                    String newhash = new String(mac.doFinal(), "UTF8");
                    nonce++;

                    //check hashes for equality
                    if (hash.equals(newhash) != true) {
                        System.out.println("HASH EQUALITY FAIL1");
                        disconnect();
                        return false;
                    } else {
                        decryptBuf = new byte[1024];
                        System.out.println("env.getMessage: " + env.getMessage());
                        cipher = Cipher.getInstance("AES/CBC/NoPadding");
                        cipher.init(Cipher.DECRYPT_MODE, key, ivs);
                        decryptBuf = cipher.doFinal((byte[]) env.getObjContents().get(0));

                        // Write encrypted file to disk
                        fos.write(decryptBuf);
                        System.out.printf(".");
                        env = new Envelope("DOWNLOADF"); //Success
                        concat = env.getMessage() + nonce; // concatinates all of the objects in envelope 
                        hasharray = concat.getBytes();// turn the concat into a byte array
                        mac = Mac.getInstance("HmacSHA1");
                        mac.init(HMACkey);
                        mac.update(hasharray);
                        stringhash = new String(mac.doFinal(), "UTF8"); // turn the hash into a string for easy comparision!
                        env.addObject(stringhash);
                        env.addObject(nonce);
                        nonce++;

                        envBytes = Envelope.toByteArray(env);

                        //Encrypt envelope w/ AES
                        cipher = Cipher.getInstance("AES");
                        cipher.init(Cipher.ENCRYPT_MODE, AESkey);
                        cipherBytes = cipher.doFinal(envBytes);

                        output.writeObject(cipherBytes);

                        responseCipherBytes = (byte[]) input.readObject();

                        //Decrypt response
                        cipher.init(Cipher.DECRYPT_MODE, AESkey);
                        responseBytes = cipher.doFinal(responseCipherBytes);

                        env = Envelope.getEnvelopefromBytes(responseBytes);
                    }
                }
                fos.close();
                if (env.getMessage().compareTo("EOF") == 0 && (Integer) env.getObjContents().get(1) == nonce) {
                    String hash = (String) env.getObjContents().get(0);
                    concat = env.getMessage() + nonce; // reconstructs the hash 
                    hasharray = concat.getBytes();
                    mac = Mac.getInstance("HmacSHA1");
                    File HASHfile = new File("FHASHKey.bin");
                    FileInputStream fis = new FileInputStream(HASHfile);
                    ObjectInputStream ois = new ObjectInputStream(fis);
                    HMACkey = (Key) ois.readObject();
                    mac.init(HMACkey);
                    mac.update(hasharray);
                    String newhash = new String(mac.doFinal(), "UTF8");

                    if (hash.equals(newhash) != true)//check hashes for equality
                    {
                        System.out.println("HASH EQUALITY FAIL2");
                        disconnect();
                    }

                    fos.close();
                    System.out.printf("\nTransfer successful file %s\n", sourceFile);
                    nonce++;
                    env = new Envelope("OK"); //Success
                    concat = env.getMessage() + nonce; // concatinates all of the objects in envelope 
                    hasharray = concat.getBytes();// turn the concat into a byte array
                    mac = Mac.getInstance("HmacSHA1");
                    mac.init(HMACkey);
                    mac.update(hasharray);
                    stringhash = new String(mac.doFinal(), "UTF8"); // turn the hash into a string for easy comparision!
                    env.addObject(stringhash);
                    env.addObject(nonce);
                    nonce++;

                    envBytes = Envelope.toByteArray(env);

                    //Encrypt envelope w/ AES
                    cipher = Cipher.getInstance("AES");
                    cipher.init(Cipher.ENCRYPT_MODE, AESkey);
                    cipherBytes = cipher.doFinal(envBytes);

                    output.writeObject(cipherBytes);

                } else if ((Integer) env.getObjContents().get(1) != nonce) {
                    System.out.println("Nonce FAIL DOWNLOADF");
                    disconnect();
                    return false;
                } else {
                    System.out.printf("Error reading file %s (%s)\n", sourceFile, env.getMessage());
                    file.delete();
                    return false;
                }
            } else {
                System.out.printf("Error couldn't create file %s\n", destFile);
                return false;
            }


        } catch (InvalidAlgorithmParameterException ex) {
            Logger.getLogger(FileClient.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalBlockSizeException ex) {
            Logger.getLogger(FileClient.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println(1);
        } catch (BadPaddingException ex) {
            Logger.getLogger(FileClient.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println(2);
        } catch (InvalidKeyException ex) {
            Logger.getLogger(FileClient.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println(3);
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(FileClient.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println(4);
        } catch (NoSuchPaddingException ex) {
            Logger.getLogger(FileClient.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println(5);
        } catch (IOException e1) {

            System.out.printf("Error couldn't create file %s\n", destFile);
            return false;


        } catch (ClassNotFoundException e1) {
            e1.printStackTrace(System.err);
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    public List<String> listFiles(UserToken token) {
        try {
            Envelope env = null, e = null;
            //Tell the server to return the member list
            env = new Envelope("LFILES");
            env.addObject(token); //Add requester's token
            String concat = token.toString() + "LFILES" + nonce; // concatinates all of the objects in envelope
            byte[] hasharray = concat.getBytes();// turn the concat into a byte array
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(HMACkey);
            mac.update(hasharray);
            String stringhash = new String(mac.doFinal(), "UTF8"); // turn the hash into a string for easy comparision!
            env.addObject(stringhash);
            env.addObject(nonce);
            nonce++;

            byte[] envBytes = Envelope.toByteArray(env);

            //Encrypt envelope w/ AES
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, AESkey);
            byte[] cipherBytes = cipher.doFinal(envBytes);

            output.writeObject(cipherBytes);

            byte[] responseCipherBytes = (byte[]) input.readObject();

            //Decrypt response
            cipher.init(Cipher.DECRYPT_MODE, AESkey);
            byte[] responseBytes = cipher.doFinal(responseCipherBytes);

            env = Envelope.getEnvelopefromBytes(responseBytes);

            if (env.getMessage().equals("FAIL")) {
                System.out.println("Error occured in ListFiles, disconnecting");
                disconnect();
                return null;
            } else if ((Integer) env.getObjContents().get(2) == nonce) {
                List<String> files = (List<String>) env.getObjContents().get(0);
                String hash = (String) env.getObjContents().get(1);
                concat = files.toString() + env.getMessage() + nonce; // reconstructs the hash
                hasharray = concat.getBytes();
                mac = Mac.getInstance("HmacSHA1");
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
                    disconnect();
                } else {
                    //If server indicates success, return the member list
                    if (env.getMessage().equals("OK")) {
                        return files; //This cast creates compiler warnings. Sorry.
                    }
                }
            } else {
                System.out.println("Nonce FAIL LFILES");
                disconnect();
                return null;
            }

            return null;
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace(System.err);
            return null;
        }
    }

    public boolean upload(String sourceFile, String destFile, String group, UserToken token, Key key, int keyNum) {

        if (destFile.charAt(0) != '/') {
            destFile = "/" + destFile;
        }

        try {
            FileInputStream fis = new FileInputStream(sourceFile);
            File encryptFile = new File(sourceFile + "_encrypt");
            encryptFile.createNewFile();
            FileOutputStream fos = new FileOutputStream(encryptFile);

            // Initial Vector must be 16 bytes
            byte[] initialVector = {0x0, 0x1, 0x2, 0x3, 0x4, 0x5, 0x6, 0x7, 0x8, 0x9, 0xa, 0xb, 0xc, 0xd, 0xe, 0xf};
            IvParameterSpec ivs = new IvParameterSpec(initialVector);
            byte[] buf = new byte[1024];
            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, ivs);
            byte[] cipherBytes;

            // create a new local encrypted file
            do {
                buf = new byte[1024];
                int n = fis.read(buf);

                if (n > 0) {
                    System.out.printf(".");
                } else if (n < 0) {
                    System.out.println("Read error");
                }

                cipherBytes = cipher.doFinal(buf);
                fos.write(cipherBytes);
            } while (fis.available() > 0);
            System.out.println();

            // send encrypted file to server
            Envelope message = null, env = null;
            //Tell the server to return the member list
            message = new Envelope("UPLOADF");
            message.addObject(destFile);
            message.addObject(group);
            message.addObject(token);
            message.addObject(keyNum);
            message.addObject(initialVector);

            String concat = destFile + group + token.toString() + keyNum + "UPLOADF" + nonce; // concatinates all of the objects in envelope
            byte[] hasharray = concat.getBytes();// turn the concat into a byte array
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(HMACkey);
            mac.update(hasharray);
            String stringhash = new String(mac.doFinal(), "UTF8"); // turn the hash into a string for easy comparision!
            message.addObject(stringhash);
            message.addObject(nonce);
            nonce++;

            byte[] messageBytes = Envelope.toByteArray(message);

            //Encrypt envelope w/ AES
            cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, AESkey);
            cipherBytes = cipher.doFinal(messageBytes);

            output.writeObject(cipherBytes);

            byte[] responseCipherBytes = (byte[]) input.readObject();            // if response isnt ready it should check whether it was forged

            //Decrypt response
            cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, AESkey);
            byte[] responseBytes = cipher.doFinal(responseCipherBytes);

            env = Envelope.getEnvelopefromBytes(responseBytes);
            if (env.getMessage().equals("READY")) {
                System.out.printf("Meta data upload successful\n");
            } else if ((Integer) env.getObjContents().get(1) == nonce) {
                String hash = (String) env.getObjContents().get(0);
                concat = env.getMessage() + nonce; // reconstructs the hash
                hasharray = concat.getBytes();
                mac = Mac.getInstance("HmacSHA1");
                File HASHfile = new File("FHASHKey.bin");
                fis = new FileInputStream(HASHfile);
                ObjectInputStream ois = new ObjectInputStream(fis);
                Key HMACkey = (Key) ois.readObject();
                mac.init(HMACkey);
                mac.update(hasharray);
                String newhash = new String(mac.doFinal(), "UTF8");
                nonce++;

                //check hashes for equality
                if (hash.equals(newhash) != true) {
                    System.out.println("HASH EQUALITY FAIL2, disconnecting for your own safety");
                    disconnect();
                    return false;
                }
            } else {
                System.out.println("Nonce FAIL UPLOADF");
                disconnect();
                return false;
            }
            //If server indicates success, return the member list

            FileInputStream encryptFIS = new FileInputStream(encryptFile);
            do {
                if ((Integer) env.getObjContents().get(1) == nonce) {
                    buf = new byte[1024];
                    if (!env.getMessage().equals("READY")) {
                        System.out.printf("Server error: %s\n", env.getMessage());
                        return false;
                    }

                    String hash = (String) env.getObjContents().get(0);
                    concat = env.getMessage() + nonce; // reconstructs the hash
                    hasharray = concat.getBytes();
                    mac = Mac.getInstance("HmacSHA1");
                    File HASHfile = new File("FHASHKey.bin");
                    fis = new FileInputStream(HASHfile);
                    ObjectInputStream ois = new ObjectInputStream(fis);
                    Key HMACkey = (Key) ois.readObject();
                    mac.init(HMACkey);
                    mac.update(hasharray);
                    String newhash = new String(mac.doFinal(), "UTF8");
                    nonce++;

                    ois.close();

                    //check hashes for equality
                    if (hash.equals(newhash) != true) {
                        System.out.println("HASH EQUALITY FAIL3, disconnecting for your own safety");
                        disconnect();
                        return false;
                    }

                    message = new Envelope("CHUNK");
                    int n = encryptFIS.read(buf); //can throw an IOException
                    if (n > 0) {
                        System.out.printf(".");
                    } else if (n < 0) {
                        System.out.println("Read error");
                        return false;
                    }

                    message.addObject(buf);
                    message.addObject(new Integer(n));
                    concat = n + "CHUNK" + nonce; // concatinates all of the objects in envelope
                    hasharray = concat.getBytes();// turn the concat into a byte array
                    mac = Mac.getInstance("HmacSHA1");
                    mac.init(HMACkey);
                    mac.update(hasharray);
                    stringhash = new String(mac.doFinal(), "UTF8"); // turn the hash into a string for easy comparision!
                    message.addObject(stringhash);
                    message.addObject(nonce);
                    nonce++;
                    
                    messageBytes = Envelope.toByteArray(message);

                    //Encrypt envelope w/ AES
                    cipher = Cipher.getInstance("AES");
                    cipher.init(Cipher.ENCRYPT_MODE, AESkey);
                    cipherBytes = cipher.doFinal(messageBytes);
                    System.out.println("Concatsent" + concat);

                    output.writeObject(cipherBytes);///////////////////////////////////////////HERE/////////////////////////////////

                    responseCipherBytes = (byte[]) input.readObject();

                    //Decrypt response
                    cipher.init(Cipher.DECRYPT_MODE, AESkey);
                    responseBytes = cipher.doFinal(responseCipherBytes);

                    env = Envelope.getEnvelopefromBytes(responseBytes);

                } else {
                    System.out.println("Nonce FAIL UPLOADF");
                    disconnect();
                    return false;
                }
            } while (encryptFIS.available() > 0);
            encryptFIS.close();

            //If server indicates success, return the member list
            if (env.getMessage().compareTo("READY") == 0 && (Integer) env.getObjContents().get(1) == nonce) {
                nonce++;
                message = new Envelope("EOF");
                concat = "EOF" + nonce; // concatinates all of the objects in envelope
                hasharray = concat.getBytes();// turn the concat into a byte array
                mac = Mac.getInstance("HmacSHA1");
                mac.init(HMACkey);
                mac.update(hasharray);
                stringhash = new String(mac.doFinal(), "UTF8"); // turn the hash into a string for easy comparision!
                
                message.addObject(stringhash);
                message.addObject(nonce);
                System.out.println(nonce);
                nonce++;

                messageBytes = Envelope.toByteArray(message);

                //Encrypt envelope w/ AES
                cipher = Cipher.getInstance("AES");
                cipher.init(Cipher.ENCRYPT_MODE, AESkey);
                cipherBytes = cipher.doFinal(messageBytes);

                output.writeObject(cipherBytes);

                responseCipherBytes = (byte[]) input.readObject();

                //Decrypt response
                cipher.init(Cipher.DECRYPT_MODE, AESkey);
                responseBytes = cipher.doFinal(responseCipherBytes);

                env = Envelope.getEnvelopefromBytes(responseBytes);

                if (env.getMessage().compareTo("OK") == 0 && (Integer) env.getObjContents().get(1) == nonce) {
                    System.out.printf("\nFile data upload successful\n");
                } else if ((Integer) env.getObjContents().get(1) != nonce) {
                    System.out.println("Nonce FAIL UPLOADF");
                    disconnect();
                    return false;
                } else {
                    System.out.printf("\nUpload failed: %s\n", env.getMessage());
                    return false;
                }
            } else if ((Integer) env.getObjContents().get(1) != nonce) {
                System.out.println("Nonce FAIL UPLOADF");
                disconnect();
                return false;
            } else {
                System.out.printf("Upload failed: %s\n", env.getMessage());
                return false;
            }
        } catch (Exception e1) {
            System.err.println("Error: " + e1.getMessage());
            e1.printStackTrace(System.err);
            return false;
        }
        return true;
    }

    public String getFingerprint() {
        try {
            Envelope e = new Envelope("GETFPRINT");
            e.addObject(nonce);
            nonce++;

            byte[] messageBytes = Envelope.toByteArray(e);
            
            // send message to request host key's hash
            output.writeObject(messageBytes);

            // receive host key hash
            e = Envelope.getEnvelopefromBytes((byte[]) input.readObject());

            if (e.getMessage().equals("OK") && (Integer) e.getObjContents().get(2) == nonce) {
                nonce++;
                // check publicKey File for server and fingerprint match
                // TODO
                pubKey = (PublicKey) e.getObjContents().get(0);
                return (String) e.getObjContents().get(1);
            } else {
                System.out.println("Nonce FAIL GETFPRINT");
                disconnect();
                return null;
            }
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(FileClient.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        } catch (IOException ex) {
            Logger.getLogger(FileClient.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

    public PublicKey getKey() {
        return pubKey;
    }

    public boolean shareAESkey() {
        try {
            Envelope message = null, e = null;

            //Generate AES key
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            AESkey = keyGen.generateKey();
            keyGen = KeyGenerator.getInstance("HmacSHA1");
            HMACkey = keyGen.generateKey();
            byte[] keyBytes = AESkey.getEncoded();
            byte[] hashBytes = HMACkey.getEncoded();
            System.out.println("AES key generated");
            System.out.println("HMAC key generated");
            System.out.println("Begin Encryption...");
            //Encrypt message  w/ provided public key
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");

            cipher.init(Cipher.ENCRYPT_MODE, pubKey);
            byte[] cipherBytes = cipher.doFinal(keyBytes);
            byte[] cipherBytes1 = cipher.doFinal(hashBytes);
            System.out.println("Encryption Complete");

            message = new Envelope("SKEY");
            message.addObject(cipherBytes); //Add AESkey to message
            message.addObject(cipherBytes1);
            message.addObject(nonce);
            nonce++;

            byte[] messageBytes = Envelope.toByteArray(message);

            output.writeObject(messageBytes);

            byte[] inCipherBytes = (byte[]) input.readObject();

            //Decrypt response
            cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, AESkey);
            byte[] responseBytes = cipher.doFinal(inCipherBytes);

            Envelope response = Envelope.getEnvelopefromBytes(responseBytes);

            //If server indicates success, return the member list
            if (response.getMessage().equals("OK") && (Integer) response.getObjContents().get(0) == nonce) {
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace(System.err);
            return false;
        }
    }
}
