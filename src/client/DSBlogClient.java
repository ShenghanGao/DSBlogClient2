package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Scanner;

public class DSBlogClient {
	private static String IPAddress;
	private static InetAddress desAddress;
	private static final int DC_LISTEN_TO_CLIENTS_PORT = 8887;
	private static final int CLIENT_LISTEN_TO_DC_PORT = 8888;
	private static final boolean DEBUG = true;

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
				socket = new Socket(desAddress, DC_LISTEN_TO_CLIENTS_PORT);
			} catch (ConnectException e) {
				System.out.println(e.getMessage() + ", possibly no process is listening on " + IPAddress + ":"
						+ DC_LISTEN_TO_CLIENTS_PORT);
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

			try {
				socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static class ListenToDCThread extends Thread {
		ServerSocket listenToDCSocket;

		@Override
		public void run() {
			try {
				listenToDCSocket = new ServerSocket(CLIENT_LISTEN_TO_DC_PORT, 5);
			} catch (IOException e) {
				e.printStackTrace();
			}
			while (true) {
				Socket socket;
				try {
					socket = listenToDCSocket.accept();

					if (DEBUG)
						System.out.println("ListenToDCThread accepted!");

					InputStream is = socket.getInputStream();

					BufferedReader br = new BufferedReader(new InputStreamReader(is));
					String signal = br.readLine();

					if (DEBUG)
						System.out.println("signal = " + signal);

					ObjectInputStream ois = new ObjectInputStream(is);

					if (signal.equals("r")) {

					} else if (signal.equals("l")) {
						List<String> messages = null;
						try {
							messages = (List<String>) ois.readObject();
						} catch (ClassNotFoundException e) {
							e.printStackTrace();
						}
						for (String message : messages) {
							System.out.println(message);
						}
					}

					socket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
