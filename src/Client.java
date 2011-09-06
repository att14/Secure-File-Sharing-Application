
import java.io.IOException;
import java.net.Socket;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.UnknownHostException;

public abstract class Client {

    /* protected keyword is like private but subclasses have access
     * Socket and input/output streams
     */
    protected Socket sock;
    protected ObjectOutputStream output;
    protected ObjectInputStream input;
    protected int nonce;

    public boolean connect(final String server, final int port) {
        boolean connected = false;

        System.out.println("Attempting to connect");


        try {
            sock = new Socket(server, port);
            output = new ObjectOutputStream(sock.getOutputStream());
            input = new ObjectInputStream(sock.getInputStream());
            nonce = 0;

            connected = true;
        } catch (UnknownHostException e) {
            System.err.println("Unkown host: " + server + ": " + e.getMessage());
        } catch (IOException e) {
            System.err.println("No I/O: " + e.getMessage());
        } catch (NullPointerException e) {
            System.err.println("Input is null: " + e.getMessage());
        } catch (SecurityException e) {
            System.err.println("Method illegally overriden: " + e.getMessage());
        } finally {
            return connected;
        }
    }

    public boolean isConnected() {
        if (sock == null || !sock.isConnected()) {
            return false;
        } else {
            return true;
        }
    }

    public void disconnect() {
        if (isConnected()) {
            try {
                Envelope message = new Envelope("DISCONNECT");
                message.addObject(nonce);
                nonce++;
                
                byte[] messageBytes = Envelope.toByteArray(message);

                output.writeObject(messageBytes);
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
                e.printStackTrace(System.err);
            }
        }
    }
}
