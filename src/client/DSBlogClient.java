package client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Scanner;

public class DSBlogClient {
	private static String IPAddress;
	private static InetAddress desAddress;
	private static int CLIENTS_PORT = 8887;

	public static void main(String[] args) throws UnknownHostException {
		if (args.length == 0) {
			IPAddress = "127.0.0.1";
		} else if (args.length == 1) {
			IPAddress = args[0];
		} else {
			System.out.println("args[0] should be the IP address of the datacenter!");
			return;
		}

		desAddress = InetAddress.getByName(IPAddress);

		Scanner scanner = new Scanner(System.in);

		while (true) {
			String s = scanner.nextLine().trim();
			boolean isPost = false, isLookup = false;
			isPost = s.matches("((POST)|(post))\\s+(\\S|\\s)+");
			if (!isPost) {
				isLookup = s.matches("((LOOKUP)|(lookup))");
			}

			StringBuilder request = new StringBuilder();
			String req = null;
			if (isPost) {
				String[] ss = s.split("\\s+", 2);
				request.append("p ");
				request.append(ss[1]);
				req = request.toString();
			} else if (isLookup) {
				req = "l";
			} else {
				System.out.println("Invalid request!");
				continue;
			}

			Socket socket = null;
			try {
				socket = new Socket(desAddress, CLIENTS_PORT);
			} catch (ConnectException e) {
				System.out.println(
						e.getMessage() + ", possibly no process is listening on " + IPAddress + ":" + CLIENTS_PORT);
				continue;
			} catch (IOException e) {
				e.printStackTrace();
			}

			PrintWriter pw = null;
			try {
				pw = new PrintWriter(socket.getOutputStream());
			} catch (IOException e) {
				e.printStackTrace();
			}
			pw.println(req);
			pw.flush();

			if (req.equals("l")) {
				List<String> messages = null;
				try {
					ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
					messages = (List<String>) ois.readObject();
				} catch (IOException | ClassNotFoundException e) {
					e.printStackTrace();
				}
				for (String message : messages) {
					System.out.println(message);
				}
			}

			try {
				socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
