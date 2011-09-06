
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;


public class Test {

    public static void main(String[] args) {
        FileInputStream fis = null;
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            Key key =keyGen.generateKey();

            fis = new FileInputStream("/Users/Tribone/Desktop/cs1550.c");
            File encryptFile = new File("/Users/Tribone/Desktop/cs1550_encrypt.c");
            encryptFile.createNewFile();
            FileOutputStream fos = new FileOutputStream(encryptFile);
            byte[] initialVector = {0x0, 0x1, 0x2, 0x3, 0x4, 0x5, 0x6, 0x7, 0x8, 0x9, 0xa, 0xb, 0xc, 0xd, 0xe, 0xf};
            IvParameterSpec ivs = new IvParameterSpec(initialVector);
            byte[] buf = new byte[1024];
            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, ivs);
            do {

                int n = fis.read(buf);
                if (n > 0) {
                    System.out.printf(".");
                } else if (n < 0) {
                    System.out.println("Read error");
                }
                byte[] cipherBytes = cipher.doFinal(buf);
                fos.write(cipherBytes);
            } while (fis.available() > 0);
            fis.close();
            fos.close();
            File decryptFile = new File("/Users/Tribone/Desktop/cs1550_decrypt.c");
            decryptFile.createNewFile();
            fis = new FileInputStream(encryptFile);
            fos = new FileOutputStream(decryptFile);
            cipher.init(Cipher.DECRYPT_MODE, key, ivs);
            do {
                int n = fis.read(buf);
                if (n > 0) {
                    System.out.printf(".");
                } else if (n < 0) {
                    System.out.println("Read error");
                }
                byte [] cipherBytes = cipher.doFinal(buf);
                fos.write(cipherBytes);
            } while (fis.available() > 0);
            fis.close();
            fos.close();
        } catch (IllegalBlockSizeException ex) {
            Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
        } catch (BadPaddingException ex) {
            Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InvalidKeyException ex) {
            Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InvalidAlgorithmParameterException ex) {
            Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchPaddingException ex) {
            Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
