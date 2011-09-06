/* Implements the GroupClient Interface */

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.security.*;
import javax.crypto.*;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;

public class GroupClient extends Client implements GroupClientInterface {

    private PublicKey pubKey;
    private Key AESkey;
    private Key HMACkey;

    @SuppressWarnings("unchecked")
    public void addToCache(String fingerprint) {
        ArrayList<String> fprintCache;

        // if cache file already exists, add fingerprint to it
        try {
            File fprintCacheFile = new File("GSfprintCache.bin");
            FileInputStream fis = new FileInputStream(fprintCacheFile);
            ObjectInputStream ois = new ObjectInputStream(fis);
            fprintCache = (ArrayList<String>) ois.readObject();

            fprintCache.add(fingerprint);

            FileOutputStream fos = new FileOutputStream(fprintCacheFile);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(fprintCache);
            // if file DNE, create it and add fingerprint
        } catch (FileNotFoundException fnfe) {
            File fprintCacheFile = new File("GSfprintCache.bin");

            try {
                fprintCacheFile.createNewFile();
            } catch (IOException ex) {
                System.err.println("IOException in addToCache");
            }

            fprintCache = new ArrayList<String>();
            fprintCache.add(fingerprint);
        } catch (IOException ioe) {
            System.err.println("IOException in addToCache");
        } catch (ClassNotFoundException ex) {
            System.err.println("ClassNotFoundException in addToCache");
        }
    }

    public PublicKey getPubKey() {
        try {
            Envelope message = null, response = null;
            //Tell the server to generate a keypair and send the public
            message = new Envelope("GKEY");
            message.addObject(nonce);
            nonce++;

            byte[] cipherBytes = Envelope.toByteArray(message);

            output.writeObject(cipherBytes);

            response = (Envelope) input.readObject();

            if (response.getMessage().equals("OK") && (Integer) response.getObjContents().get(1) == nonce) {
                nonce++;
                pubKey = (PublicKey) response.getObjContents().get(0);
                return (PublicKey) response.getObjContents().get(0);
            } else {
                System.out.println("Nonce FAIL GKEY");
                disconnect();
                return null;
            }

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace(System.err);
            return null;
        }
    }

    public HashMap getFileKeys(UserToken token) {
        try {
            HashMap keys = new HashMap();

            Envelope message = new Envelope("GFILEKEYS");
            message.addObject(token);
            message.addObject(nonce);
            nonce++;

            /*           String concat = token.toString()+"GFILEKEYS"; // concatinates all of the objects in envelope
            byte[] hasharray = concat.getBytes();// turn the concat into a byte array
            Mac mac = Mac.getInstance("HmacSHA1");      
            mac.init(HMACkey);
            mac.update(hasharray);
            String stringhash = new String( mac.doFinal(), "UTF8"); // turn the hash into a string for easy comparision!
            message.addObject(stringhash);*/ //used for the hashing for T1

            byte[] messageBytes = Envelope.toByteArray(message);

            //encrypt response byte[] and send
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, AESkey);
            byte[] cipherBytes = cipher.doFinal(messageBytes);
            output.writeObject(cipherBytes);

            byte[] inCipherBytes = (byte[]) input.readObject();
            //recieve byte[] and decrypt
            cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, AESkey);
            byte[] plainBytes = cipher.doFinal(inCipherBytes);
            Envelope response = Envelope.getEnvelopefromBytes(plainBytes);

            if (response.getMessage().equals("FAIL")) {
                System.out.println("Nonce FAIL GFILEKEYS");
                disconnect();
                return null;
            } else {
                if ((Integer) response.getObjContents().get(1) == nonce) {
                    nonce++;

                    response = Envelope.getEnvelopefromBytes(plainBytes);
                    System.out.println("ENVELOPE: " + response.getObjContents());
                    if (response.getMessage().equals("OK")) {
                        keys = (HashMap) response.getObjContents().get(0);

                        return keys;
                    } else {
                        System.out.println("GFILEKEYS FAIL");
                        disconnect();
                        return null;
                    }
                } else {
                    System.out.println("Nonce FAIL GFILEKEYS");
                    disconnect();
                    return null;
                }
            }
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(GroupClient.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        } catch (IOException ex) {
            Logger.getLogger(GroupClient.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        } catch (IllegalBlockSizeException ex) {
            Logger.getLogger(GroupClient.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        } catch (BadPaddingException ex) {
            Logger.getLogger(GroupClient.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        } catch (InvalidKeyException ex) {
            Logger.getLogger(GroupClient.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(GroupClient.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        } catch (NoSuchPaddingException ex) {
            Logger.getLogger(GroupClient.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

    /*
    public boolean decryptFile(File file) {
    return true;
    }
    
    public File encryptFile(File file, String group, UserToken userToken) {
    try {
    Envelope message = new Envelope("EFILE");
    message.addObject(file);
    message.addObject(group);
    message.addObject(userToken);
    byte[] fileBytes = Envelope.toByteArray(message);
    
    //encrypt response byte[] and send
    Cipher cipher = Cipher.getInstance("AES");
    cipher.init(Cipher.ENCRYPT_MODE, AESkey);
    byte[] cipherBytes = cipher.doFinal(fileBytes);
    output.writeObject(cipherBytes);
    
    //recieve byte[] and decrypt
    byte[] inCipherBytes = (byte[]) input.readObject();
    cipher = Cipher.getInstance("AES");
    cipher.init(Cipher.DECRYPT_MODE, AESkey);
    byte[] plainBytes = cipher.doFinal(inCipherBytes);
    
    //reconstruct Envelope object from decrypted byte[]
    Envelope response = Envelope.getEnvelopefromBytes(plainBytes);
    
    if (response.getMessage().equals("OK")) {
    return (File) response.getObjContents().get(0);
    }
    else return null;
    
    } catch (ClassNotFoundException ex) {
    Logger.getLogger(GroupClient.class.getName()).log(Level.SEVERE, null, ex);
    return null;
    } catch (IOException ex) {
    Logger.getLogger(GroupClient.class.getName()).log(Level.SEVERE, null, ex);
    return null;
    } catch (IllegalBlockSizeException ex) {
    Logger.getLogger(GroupClient.class.getName()).log(Level.SEVERE, null, ex);
    return null;
    } catch (BadPaddingException ex) {
    Logger.getLogger(GroupClient.class.getName()).log(Level.SEVERE, null, ex);
    return null;
    } catch (InvalidKeyException ex) {
    Logger.getLogger(GroupClient.class.getName()).log(Level.SEVERE, null, ex);
    return null;
    } catch (NoSuchAlgorithmException ex) {
    Logger.getLogger(GroupClient.class.getName()).log(Level.SEVERE, null, ex);
    return null;
    } catch (NoSuchPaddingException ex) {
    Logger.getLogger(GroupClient.class.getName()).log(Level.SEVERE, null, ex);
    return null;
    }
    }
     */
    public byte[][] encrypt(PublicKey pubKey, String username, String password) {
        try {
            byte[] userBytes = username.getBytes();
            byte[] passwordBytes = password.getBytes();

            //Generate AES key
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            AESkey = keyGen.generateKey();
            keyGen = KeyGenerator.getInstance("HmacSHA1");
            HMACkey = keyGen.generateKey();
            byte[] keyBytes = AESkey.getEncoded();
            System.out.println("AES key generated");
            byte[] hashBytes = HMACkey.getEncoded();
            System.out.println("HASH function generated");

            System.out.println("Begin Encryption...");
            //Encrypt username w/ provided public key
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, pubKey);
            byte[] userCipherBytes = cipher.doFinal(userBytes);

            //Encrypt password w/ provided public key
            byte[] passwordCipherBytes = cipher.doFinal(passwordBytes);

            //Encrypt generated AES key with public key
            byte[] keyCipherBytes = cipher.doFinal(keyBytes);
            System.out.println("AES Encryption Complete");

            //Encrypt hash function with public key
            byte[] hash = cipher.doFinal(hashBytes);
            System.out.println("Hash encryption complete");

            byte[][] encryptedArray = {userCipherBytes, passwordCipherBytes, keyCipherBytes, hash};
            return encryptedArray;

        } catch (IllegalBlockSizeException ex) {
            ex.printStackTrace(System.err);
            return null;
        } catch (BadPaddingException ex) {
            ex.printStackTrace(System.err);
            return null;
        } catch (InvalidKeyException ex) {
            ex.printStackTrace(System.err);
            return null;
        } catch (NoSuchAlgorithmException ex) {
            ex.printStackTrace(System.err);
            return null;
        } catch (NoSuchPaddingException ex) {
            ex.printStackTrace(System.err);
            return null;
        }
    }

    public boolean sendEncryptedUserInfo(byte[][] M) {
        try {
            Envelope message = null, response = null;

            //Tell the server to decrypt the auth message and return success or failure
            message = new Envelope("IDEC");
            message.addObject(M[0]); //username
            message.addObject(M[1]); //password
            message.addObject(M[2]); //AES key
            message.addObject(M[3]); // Hash function
            message.addObject(nonce);
            nonce++;

            byte[] cipherBytes = Envelope.toByteArray(message);

            output.writeObject(cipherBytes);// sends all of the users encrypted info to the server

            cipherBytes = (byte[]) input.readObject();
            response = Envelope.getEnvelopefromBytes(cipherBytes);

            if (response != null) {
                System.out.println("Nonce FAIL IDEC");
                disconnect();
                return false;
            } else {
                Cipher cipher = Cipher.getInstance("AES");
                cipher.init(Cipher.DECRYPT_MODE, AESkey);
                byte[] plainBytes = cipher.doFinal(cipherBytes);

                response = Envelope.getEnvelopefromBytes(plainBytes);
                if (response.getMessage().equals("OK") && (Integer) response.getObjContents().get(1) == nonce) {
                    nonce++;
                    return (Boolean) response.getObjContents().get(0);
                } else {
                    System.out.println("IDEC FAIL");
                    disconnect();
                    return false;
                }
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace(System.err);
            return false;
        }
    }

    public boolean verifyToken(UserToken token) {
        try {
            //get plaintext token bytes
            String tokenContents = token.getIssuer() + ";" + token.getSubject();
            for (String group : token.getGroups()) {
                tokenContents = tokenContents + ";" + group;
            }
            tokenContents = tokenContents + ";" + token.getFileServerName();

            byte[] tokenBytes = tokenContents.getBytes();

            Signature sig = Signature.getInstance("SHA1withRSA");
            sig.initVerify(pubKey);
            sig.update(tokenBytes);
            return sig.verify(token.getSignature());

        } catch (SignatureException ex) {
            ex.printStackTrace(System.err);
            return false;
        } catch (InvalidKeyException ex) {
            ex.printStackTrace(System.err);
            return false;
        } catch (NoSuchAlgorithmException ex) {
            ex.printStackTrace(System.err);
            return false;
        }
    }

    public UserToken getToken(String username, String fileServerName) {
        try {
            UserToken token = null;
            Envelope message = null,
                    response = null;

            //Tell the server to return a token.
            message = new Envelope("GET");
            message.addObject(username); //Add user name string
            message.addObject(fileServerName); //Add file server name string

            String concat = username + fileServerName + "GET" + nonce; // concatinates all of the objects in envelope
            byte[] hasharray = concat.getBytes();// turn the concat into a byte array
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(HMACkey);
            mac.update(hasharray);
            String stringhash = new String(mac.doFinal(), "UTF-8"); // turn the hash into a string for easy comparision!
            System.out.println(stringhash);
            message.addObject(stringhash);
            message.addObject(nonce);
            nonce++;

            byte[] messageBytes = Envelope.toByteArray(message);

            //encrypt response byte[] and send
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, AESkey);
            byte[] cipherBytes = cipher.doFinal(messageBytes);
            output.writeObject(cipherBytes);

            //receive byte[] and decrypt
            byte[] inCipherBytes = (byte[]) input.readObject();
            cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, AESkey);
            byte[] plainBytes = cipher.doFinal(inCipherBytes);

            //reconstruct Envelope object from decrypted byte[]
            response = Envelope.getEnvelopefromBytes(plainBytes);

            //Successful response
            if (response.getMessage().equals("OK") && (Integer) response.getObjContents().get(2) == nonce) {
                //If there is a token in the Envelope, return it
                ArrayList<Object> temp = null;
                temp = response.getObjContents();

                token = (UserToken) temp.get(0);
                String hashstring = (String) temp.get(1);
                concat = token.toString() + "OK" + nonce; // reconstructs the hash like how it was done in the client
                hasharray = concat.getBytes();
                mac = Mac.getInstance("HmacSHA1");
                mac.init(HMACkey);
                mac.update(hasharray);
                stringhash = new String(mac.doFinal(), "UTF8");
                nonce++;

                if (stringhash.equals(hashstring) != true) {
                    System.out.println("hashing error");
                    return null;
                } else {
                    if (verifyToken(token)) {
                        System.out.println("Token verification successful");
                        return token;
                    }
                    System.out.println("Verification failed");
                }

            } else if (response.getMessage().equals("HASH") && (Integer) response.getObjContents().get(1) == nonce) {
                ArrayList<Object> temp = null;
                temp = response.getObjContents();
                String hashstring = (String) temp.get(0);
                concat = "HASH" + nonce; // reconstructs the hash like how it was done in the client
                hasharray = concat.getBytes();
                mac = Mac.getInstance("HmacSHA1");
                mac.init(HMACkey);
                mac.update(hasharray);
                String newhash = new String(mac.doFinal(), "UTF8");

                nonce++;
                return null;
            } else {
                System.out.println("Nonce FAIL GET");
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

    public boolean changePwd(String oldPwd, String newPwd, UserToken userToken) {
        try {
            Envelope message = null, response = null;

            //Tell the server to delete a user
            message = new Envelope("CPWD");
            message.addObject(oldPwd); //Add old password
            message.addObject(newPwd); //Add new password
            message.addObject(userToken);  //Add requester's token

            String concat = oldPwd + newPwd + userToken.toString() + "CPWD" + nonce; // concatinates all of the objects in envelope
            byte[] hasharray = concat.getBytes();// turn the concat into a byte array
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(HMACkey);
            mac.update(hasharray);
            String stringhash = new String(mac.doFinal(), "UTF8"); // turn the hash into a string for easy comparision!
            message.addObject(stringhash);

            message.addObject(nonce);
            nonce++;

            byte[] messageBytes = Envelope.toByteArray(message);

            //encrypt response byte[] and send
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, AESkey);
            byte[] cipherBytes = cipher.doFinal(messageBytes);
            output.writeObject(cipherBytes);

            //receive byte[] and decrypt
            byte[] inCipherBytes = (byte[]) input.readObject();
            cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, AESkey);
            byte[] plainBytes = cipher.doFinal(inCipherBytes);

            //reconstruct Envelope object from decrypted byte[]
            response = Envelope.getEnvelopefromBytes(plainBytes);

            //If server indicates success, return true
            if (response.getMessage().equals("OK") && (Integer) response.getObjContents().get(1) == nonce) {
                String hash = (String) response.getObjContents().get(0);
                concat = "OK" + nonce; // reconstructs the hash like how it was done in the client
                hasharray = concat.getBytes();
                mac = Mac.getInstance("HmacSHA1");
                mac.init(HMACkey);
                mac.update(hasharray);
                stringhash = new String(mac.doFinal(), "UTF8");
                nonce++;

                if (hash.equals(stringhash) != true)//check hashes for equality
                {
                    return false;
                }
                return true;
            } else if (response.getMessage().equals("FAIL") && (Integer) response.getObjContents().get(1) == nonce) {
                System.out.println("CPWD FAIL");
                String hash = (String) response.getObjContents().get(0);
                concat = "FAIL" + nonce; // reconstructs the hash like how it was done in the client
                hasharray = concat.getBytes();
                mac = Mac.getInstance("HmacSHA1");
                mac.init(HMACkey);
                mac.update(hasharray);
                stringhash = new String(mac.doFinal(), "UTF8");
                nonce++;

                if (hash.equals(stringhash) != true)//check hashes for equality
                {
                    return false;
                }
                return false;
            } else {
                System.out.println("Nonce FAIL CPWD");
                disconnect();
                return false;
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace(System.err);
            return false;
        }
    }

    public boolean createUser(String username, String password, UserToken token) {
        try {
            Envelope message = null, response = null;
            //Tell the server to create a user
            message = new Envelope("CUSER");
            message.addObject(username); //Add user name string
            message.addObject(password);//Add password
            message.addObject(token); //Add the requester's token

            String concat = username + password + token.toString() + "CUSER" + nonce; // concatinates all of the objects in envelope
            byte[] hasharray = concat.getBytes();// turn the concat into a byte array
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(HMACkey);
            mac.update(hasharray);
            String stringhash = new String(mac.doFinal(), "UTF8"); // turn the hash into a string for easy comparision!
            message.addObject(stringhash);
            message.addObject(nonce);
            nonce++;


            byte[] messageBytes = Envelope.toByteArray(message);

            //encrypt response byte[] and send
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, AESkey);
            byte[] cipherBytes = cipher.doFinal(messageBytes);
            output.writeObject(cipherBytes);

            //receive byte[] and decrypt
            byte[] inCipherBytes = (byte[]) input.readObject();
            cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, AESkey);
            byte[] plainBytes = cipher.doFinal(inCipherBytes);

            //reconstruct Envelope object from decrypted byte[]
            response = Envelope.getEnvelopefromBytes(plainBytes);

            //If server indicates success, return true
            if (response.getMessage().equals("OK") && (Integer) response.getObjContents().get(1) == nonce) {
                String hash = (String) response.getObjContents().get(0);
                concat = "OK" + nonce; // reconstructs the hash like how it was done in the client
                hasharray = concat.getBytes();
                mac = Mac.getInstance("HmacSHA1");
                mac.init(HMACkey);
                mac.update(hasharray);
                stringhash = new String(mac.doFinal(), "UTF8");
                nonce++;

                if (hash.equals(stringhash) != true)//check hashes for equality
                {
                    return false;
                }
                return true;
            } else if (response.getMessage().equals("FAIL") && (Integer) response.getObjContents().get(1) == nonce) {
                String hash = (String) response.getObjContents().get(0);
                concat = "FAIL" + nonce; // reconstructs the hash like how it was done in the client
                hasharray = concat.getBytes();
                mac = Mac.getInstance("HmacSHA1");
                mac.init(HMACkey);
                mac.update(hasharray);
                stringhash = new String(mac.doFinal(), "UTF8");
                nonce++;

                if (hash.equals(stringhash) != true)//check hashes for equality
                {
                    return false;
                }
                return false;
            } else {
                System.out.println("Nonce FAIL CUSER");
                disconnect();
                return false;
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace(System.err);
            return false;
        }
    }

    public boolean deleteUser(String username, UserToken token) {
        try {
            Envelope message = null, response = null;

            //Tell the server to delete a user
            message = new Envelope("DUSER");
            message.addObject(username); //Add user name
            message.addObject(token);  //Add requester's token

            String concat = username + token.toString() + "DUSER" + nonce; // concatinates all of the objects in envelope
            byte[] hasharray = concat.getBytes();// turn the concat into a byte array
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(HMACkey);
            mac.update(hasharray);
            String stringhash = new String(mac.doFinal(), "UTF8"); // turn the hash into a string for easy comparision!
            message.addObject(stringhash);
            message.addObject(nonce);
            nonce++;

            byte[] messageBytes = Envelope.toByteArray(message);

            //encrypt response byte[] and send
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, AESkey);
            byte[] cipherBytes = cipher.doFinal(messageBytes);
            output.writeObject(cipherBytes);

            //recieve byte[] and decrypt
            byte[] inCipherBytes = (byte[]) input.readObject();
            cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, AESkey);
            byte[] plainBytes = cipher.doFinal(inCipherBytes);

            //reconstruct Envelope object from decrypted byte[]
            response = Envelope.getEnvelopefromBytes(plainBytes);

            if (response.getMessage().equals("OK") && (Integer) response.getObjContents().get(1) == nonce) {
                System.out.println("OK");
                String hash = (String) response.getObjContents().get(0);
                concat = "OK" + nonce; // reconstructs the hash like how it was done in the client
                hasharray = concat.getBytes();
                mac = Mac.getInstance("HmacSHA1");
                mac.init(HMACkey);
                mac.update(hasharray);
                stringhash = new String(mac.doFinal(), "UTF8");
                nonce++;

                if (hash.equals(stringhash) != true)//check hashes for equality
                {
                    return false;
                }
                return true;
            } else if (response.getMessage().equals("FAIL") && (Integer) response.getObjContents().get(1) == nonce) {
                System.out.println("FAILED");
                String hash = (String) response.getObjContents().get(0);
                concat = "FAIL" + nonce; // reconstructs the hash like how it was done in the client
                hasharray = concat.getBytes();
                mac = Mac.getInstance("HmacSHA1");
                mac.init(HMACkey);
                mac.update(hasharray);
                stringhash = new String(mac.doFinal(), "UTF8");
                nonce++;

                if (hash.equals(stringhash) != true)//check hashes for equality
                {
                    return false;
                }
                return false;
            } else {
                System.out.println("Nonce FAIL CUSER");
                disconnect();
                return false;
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace(System.err);
            return false;
        }
    }

    public boolean createGroup(String groupname, UserToken token) {
        try {
            Envelope message = null, response = null;
            //Tell the server to create a group
            message = new Envelope("CGROUP");
            message.addObject(groupname); //Add the group name string
            message.addObject(token); //Add the requester's token

            String concat = groupname + token.toString() + "CGROUP" + nonce; // concatinates all of the objects in envelope
            byte[] hasharray = concat.getBytes();// turn the concat into a byte array
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(HMACkey);
            mac.update(hasharray);
            String stringhash = new String(mac.doFinal(), "UTF8"); // turn the hash into a string for easy comparision!
            message.addObject(stringhash);
            message.addObject(nonce);
            nonce++;


            byte[] messageBytes = Envelope.toByteArray(message);

            //encrypt response byte[] and send
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, AESkey);
            byte[] cipherBytes = cipher.doFinal(messageBytes);
            output.writeObject(cipherBytes);

            //recieve byte[] and decrypt
            byte[] inCipherBytes = (byte[]) input.readObject();
            cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, AESkey);
            byte[] plainBytes = cipher.doFinal(inCipherBytes);

            //reconstruct Envelope object from decrypted byte[]
            response = Envelope.getEnvelopefromBytes(plainBytes);

            if (response.getMessage().equals("OK") && (Integer) response.getObjContents().get(1) == nonce) {
                String hash = (String) response.getObjContents().get(0);
                concat = "OK" + nonce; // reconstructs the hash like how it was done in the client
                hasharray = concat.getBytes();
                mac = Mac.getInstance("HmacSHA1");
                mac.init(HMACkey);
                mac.update(hasharray);
                stringhash = new String(mac.doFinal(), "UTF8");
                nonce++;

                //check hashes for equality
                if (hash.equals(stringhash) != true) {
                    return false;
                }
                return true;
            } else if (response.getMessage().equals("FAIL") && (Integer) response.getObjContents().get(1) == nonce) {
                String hash = (String) response.getObjContents().get(0);
                concat = "FAIL" + nonce; // reconstructs the hash like how it was done in the client
                hasharray = concat.getBytes();
                mac = Mac.getInstance("HmacSHA1");
                mac.init(HMACkey);
                mac.update(hasharray);
                stringhash = new String(mac.doFinal(), "UTF8");
                nonce++;

                //check hashes for equality
                if (hash.equals(stringhash) != true) {
                    return false;
                }
                return false;
            } else {
                System.out.println("Nonce FAIL CGROUP");
                disconnect();
                return false;
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace(System.err);
            return false;
        }
    }

    public boolean deleteGroup(String groupname, UserToken token) {
        try {
            Envelope message = null, response = null;
            //Tell the server to delete a group
            message = new Envelope("DGROUP");
            message.addObject(groupname); //Add group name string
            message.addObject(token); //Add requester's token

            String concat = groupname + token.toString() + "DGROUP" + nonce; // concatinates all of the objects in envelope
            byte[] hasharray = concat.getBytes();// turn the concat into a byte array
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(HMACkey);
            mac.update(hasharray);
            String stringhash = new String(mac.doFinal(), "UTF8"); // turn the hash into a string for easy comparision!
            message.addObject(stringhash);
            message.addObject(nonce);
            nonce++;

            byte[] messageBytes = Envelope.toByteArray(message);

            //encrypt response byte[] and send
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, AESkey);
            byte[] cipherBytes = cipher.doFinal(messageBytes);
            output.writeObject(cipherBytes);

            //recieve byte[] and decrypt
            byte[] inCipherBytes = (byte[]) input.readObject();
            cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, AESkey);
            byte[] plainBytes = cipher.doFinal(inCipherBytes);

            //reconstruct Envelope object from decrypted byte[]
            response = Envelope.getEnvelopefromBytes(plainBytes);

            //If server indicates success, return true
            if (response.getMessage().equals("OK") && (Integer) response.getObjContents().get(1) == nonce) {
                String hash = (String) response.getObjContents().get(0);
                concat = "OK" + nonce; // reconstructs the hash like how it was done in the client
                hasharray = concat.getBytes();
                mac = Mac.getInstance("HmacSHA1");
                mac.init(HMACkey);
                mac.update(hasharray);
                stringhash = new String(mac.doFinal(), "UTF8");
                nonce++;

                //check hashes for equality
                if (hash.equals(stringhash) != true) {
                    return false;
                }
                return true;
            } else if (response.getMessage().equals("FAIL") && (Integer) response.getObjContents().get(1) == nonce) {
                String hash = (String) response.getObjContents().get(0);
                concat = "FAIL" + nonce; // reconstructs the hash like how it was done in the client
                hasharray = concat.getBytes();
                mac = Mac.getInstance("HmacSHA1");
                mac.init(HMACkey);
                mac.update(hasharray);
                stringhash = new String(mac.doFinal(), "UTF8");
                nonce++;

                //check hashes for equality
                if (hash.equals(stringhash) != true) {
                    return false;
                }
                return false;
            } else {
                System.out.println("Nonce FAIL DGROUP");
                disconnect();
                return false;
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace(System.err);
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    public List<String> listMembers(String group, UserToken token) {
        try {
            Envelope message = null, response = null;
            //Tell the server to return the member list
            message = new Envelope("LMEMBERS");
            message.addObject(group); //Add group name string
            message.addObject(token); //Add requester's token

            String concat = group + token.toString() + "LMEMBERS" + nonce; // concatinates all of the objects in envelope
            byte[] hasharray = concat.getBytes();// turn the concat into a byte array
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(HMACkey);
            mac.update(hasharray);
            String stringhash = new String(mac.doFinal(), "UTF8"); // turn the hash into a string for easy comparision!
            message.addObject(stringhash);
            message.addObject(nonce);
            nonce++;

            //get byte[] of message and encrypt
            byte[] messageBytes = Envelope.toByteArray(message);
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, AESkey);
            byte[] outCipherBytes = cipher.doFinal(messageBytes);

            //send encrypted message
            output.writeObject(outCipherBytes);

            //receive byte[] and decrypt
            byte[] inCipherBytes = (byte[]) input.readObject();
            cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, AESkey);
            byte[] plainBytes = cipher.doFinal(inCipherBytes);

            //reconstruct Envelope object from decrypted byte[]
            response = Envelope.getEnvelopefromBytes(plainBytes);

            //If server indicates success, return the member list
            if (response.getMessage().equals("OK") && (Integer) response.getObjContents().get(2) == nonce) {
                ArrayList<String> members = (ArrayList<String>) response.getObjContents().get(0);
                String hash = (String) response.getObjContents().get(1);

                concat = new String();
                for (int i = 0; i < members.size(); i++) {
                    concat += members.get(i);
                }
                concat += "OK" + nonce;
                hasharray = concat.getBytes();
                mac = Mac.getInstance("HmacSHA1");
                mac.init(HMACkey);
                mac.update(hasharray);
                stringhash = new String(mac.doFinal(), "UTF8");
                nonce++;

                if (hash.equals(stringhash) != true)//check hashes for equality
                {
                    return null;
                }
                return (List<String>) response.getObjContents().get(0); //This cast creates compiler warnings. Sorry.
            } else if (response.getMessage().equals("FAIL") && (Integer) response.getObjContents().get(2) == nonce) {
                ArrayList<String> members = (ArrayList<String>) response.getObjContents().get(0);
                String hash = (String) response.getObjContents().get(1);

                concat = new String();
                for (int i = 0; i < members.size(); i++) {
                    concat += members.get(i);
                }
                concat += "FAIL" + nonce;
                hasharray = concat.getBytes();
                mac = Mac.getInstance("HmacSHA1");
                mac.init(HMACkey);
                mac.update(hasharray);
                stringhash = new String(mac.doFinal(), "UTF8");
                nonce++;

                //check hashes for equality
                if (hash.equals(stringhash) != true) {
                    return null;
                }
                return null;
            }
            return null;

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace(System.err);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public List<String> listGroups(UserToken token) {
        try {
            Envelope message = null, response = null;
            //Tell the server to return the member list
            message = new Envelope("LGROUPS");
            message.addObject(token); //Add requester's token

            String concat = token.toString() + "LGROUPS" + nonce; // concatinates all of the objects in envelope
            byte[] hasharray = concat.getBytes();// turn the concat into a byte array
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(HMACkey);
            mac.update(hasharray);
            String stringhash = new String(mac.doFinal(), "UTF8"); // turn the hash into a string for easy comparision!
            message.addObject(stringhash);
            message.addObject(nonce);
            nonce++;

            //get byte[] of message and encrypt
            byte[] messageBytes = Envelope.toByteArray(message);
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, AESkey);
            byte[] outCipherBytes = cipher.doFinal(messageBytes);

            //send encrypted message
            output.writeObject(outCipherBytes);


            //recieve byte[] and decrypt
            byte[] inCipherBytes = (byte[]) input.readObject();
            cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, AESkey);
            byte[] plainBytes = cipher.doFinal(inCipherBytes);

            //reconstruct Envelope object from decrypted byte[]
            response = Envelope.getEnvelopefromBytes(plainBytes);


            //If server indicates success, return the member list
            if (response.getMessage().equals("OK") && (Integer) response.getObjContents().get(2) == nonce) {
                ArrayList<String> groups = (ArrayList<String>) response.getObjContents().get(0);
                String hash = (String) response.getObjContents().get(1);

                concat = new String();
                for (int i = 0; i < groups.size(); i++) {
                    concat += groups.get(i);
                }
                concat += "OK" + nonce;
                hasharray = concat.getBytes();
                mac = Mac.getInstance("HmacSHA1");
                mac.init(HMACkey);
                mac.update(hasharray);
                stringhash = new String(mac.doFinal(), "UTF8");
                nonce++;

                //check hashes for equality
                if (hash.equals(stringhash) != true) {
                    return null;
                }
                return (List<String>) response.getObjContents().get(0); //This cast creates compiler warnings. Sorry.
            } else if (response.getMessage().equals("FAIL") && (Integer) response.getObjContents().get(2) == nonce) {
                ArrayList<String> groups = (ArrayList<String>) response.getObjContents().get(0);
                String hash = (String) response.getObjContents().get(1);

                concat = new String();
                for (int i = 0; i < groups.size(); i++) {
                    concat += groups.get(i);
                }
                concat += "FAIL" + nonce;
                hasharray = concat.getBytes();
                mac = Mac.getInstance("HmacSHA1");
                mac.init(HMACkey);
                mac.update(hasharray);
                stringhash = new String(mac.doFinal(), "UTF8");
                nonce++;

                //check hashes for equality
                if (hash.equals(stringhash) != true) {
                    return null;
                }
                return null;
            } else {
                System.out.println("Nonce FAIL LGROUPS");
                disconnect();
                return null;
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace(System.err);
            return null;
        }
    }

    public boolean addUserToGroup(String username, String groupname, UserToken token) {
        try {
            Envelope message = null, response = null;
            //Tell the server to add a user to the group
            message = new Envelope("AUSERTOGROUP");
            message.addObject(username); //Add user name string
            message.addObject(groupname); //Add group name string
            message.addObject(token); //Add requester's token

            String concat = username + groupname + token.toString() + "AUSERTOGROUP" + nonce; // concatinates all of the objects in envelope
            byte[] hasharray = concat.getBytes();// turn the concat into a byte array
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(HMACkey);
            mac.update(hasharray);
            String stringhash = new String(mac.doFinal(), "UTF8"); // turn the hash into a string for easy comparision!
            message.addObject(stringhash);
            message.addObject(nonce);
            nonce++;

            byte[] messageBytes = Envelope.toByteArray(message);

            //encrypt response byte[] and send
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, AESkey);
            byte[] cipherBytes = cipher.doFinal(messageBytes);
            output.writeObject(cipherBytes);

            //recieve byte[] and decrypt
            byte[] inCipherBytes = (byte[]) input.readObject();
            cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, AESkey);
            byte[] plainBytes = cipher.doFinal(inCipherBytes);

            //reconstruct Envelope object from decrypted byte[]
            response = Envelope.getEnvelopefromBytes(plainBytes);

            //If server indicates success, return true
            if (response.getMessage().equals("OK") && (Integer) response.getObjContents().get(1) == nonce) {
                String hash = (String) response.getObjContents().get(0);
                concat = "OK" + nonce; // reconstructs the hash like how it was done in the client
                hasharray = concat.getBytes();
                mac = Mac.getInstance("HmacSHA1");
                mac.init(HMACkey);
                mac.update(hasharray);
                stringhash = new String(mac.doFinal(), "UTF8");
                nonce++;

                //check hashes for equality
                if (hash.equals(stringhash) != true) {
                    return false;
                }
                return true;
            } else if (response.getMessage().equals("FAIL") && (Integer) response.getObjContents().get(1) == nonce) {
                String hash = (String) response.getObjContents().get(0);
                concat = "FAIL" + nonce; // reconstructs the hash like how it was done in the client
                hasharray = concat.getBytes();
                mac = Mac.getInstance("HmacSHA1");
                mac.init(HMACkey);
                mac.update(hasharray);
                stringhash = new String(mac.doFinal(), "UTF8");
                nonce++;

                //check hashes for equality
                if (hash.equals(stringhash) != true) {
                    return false;
                }
                return false;
            } else {
                System.out.println("Nonce FAIL AUSERTOGROUP");
                disconnect();
                return false;
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace(System.err);
            return false;
        }
    }

    public boolean deleteUserFromGroup(String username, String groupname, UserToken token) {
        try {
            Envelope message = null, response = null;
            //Tell the server to remove a user from the group
            message = new Envelope("RUSERFROMGROUP");
            message.addObject(username); //Add user name string
            message.addObject(groupname); //Add group name string
            message.addObject(token); //Add requester's token

            String concat = username + groupname + token.toString() + "RUSERFROMGROUP" + nonce; // concatinates all of the objects in envelope
            byte[] hasharray = concat.getBytes();// turn the concat into a byte array
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(HMACkey);
            mac.update(hasharray);
            String stringhash = new String(mac.doFinal(), "UTF8"); // turn the hash into a string for easy comparision!
            message.addObject(stringhash);
            message.addObject(nonce);
            nonce++;

            byte[] messageBytes = Envelope.toByteArray(message);

            //encrypt response byte[] and send
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, AESkey);
            byte[] cipherBytes = cipher.doFinal(messageBytes);
            output.writeObject(cipherBytes);

            //recieve byte[] and decrypt
            byte[] inCipherBytes = (byte[]) input.readObject();
            cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, AESkey);
            byte[] plainBytes = cipher.doFinal(inCipherBytes);

            //reconstruct Envelope object from decrypted byte[]
            response = Envelope.getEnvelopefromBytes(plainBytes);

            //If server indicates success, return true
            if (response.getMessage().equals("OK") && (Integer) response.getObjContents().get(1) == nonce) {
                String hash = (String) response.getObjContents().get(0);
                concat = "OK" + nonce; // reconstructs the hash like how it was done in the client
                hasharray = concat.getBytes();
                mac = Mac.getInstance("HmacSHA1");
                mac.init(HMACkey);
                mac.update(hasharray);
                stringhash = new String(mac.doFinal(), "UTF8");
                nonce++;

                //check hashes for equality
                if (hash.equals(stringhash) != true) {
                    System.out.println("HASH EQUALITY FAIL");
                    return false;
                }
                return true;
            } else if (response.getMessage().equals("FAIL") && (Integer) response.getObjContents().get(1) == nonce) {
                String hash = (String) response.getObjContents().get(0);
                concat = "FAIL" + nonce; // reconstructs the hash like how it was done in the client
                hasharray = concat.getBytes();
                mac = Mac.getInstance("HmacSHA1");
                mac.init(HMACkey);
                mac.update(hasharray);
                stringhash = new String(mac.doFinal(), "UTF8");
                nonce++;

                //check hashes for equality
                if (hash.equals(stringhash) != true) {
                    System.out.println("HASH EQUALITY FAIL");
                    return false;
                }
                return false;
            } else {
                System.out.println("Nonce FAIL RUSERFROMGROUP");
                disconnect();
                return false;
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace(System.err);
            return false;
        }
    }
}
