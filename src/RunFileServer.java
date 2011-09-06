import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;

/* Driver program for FileSharing File Server */

public class RunFileServer {
	
	public static void main(String[] args) throws IOException, ClassNotFoundException, NoSuchAlgorithmException {
		if (args.length > 0) {
			try {
				FileServer server = new FileServer(Integer.parseInt(args[0]), args[1]);
				server.start();
			}
			catch (NumberFormatException e) {
				System.out.printf("Enter a valid port number or pass no arguments to use the default port (%d)\n", FileServer.SERVER_PORT);
			}
		}
		else {
			System.out.println("Enter server name: ");
			Scanner in = new Scanner(System.in);
			String name = in.next();
			FileServer server = new FileServer(name);
			server.start();
		}
	}

}
