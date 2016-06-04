package client;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class DSBlogClient {
	private static DSBlogClient dsBlogClient = new DSBlogClient();
	private static String IPAddress;
	private static InetAddress desAddress;
	private static List<InetAddress> desAddresses;
	private static final int DC_LISTEN_TO_CLIENTS_PORT = 8887;
	private static final int CLIENT_LISTEN_TO_DC_PORT = 8888;
	private static final boolean DEBUG = true;

	private DSBlogClient() {
		desAddresses = new ArrayList<>();
	}

	public static void main(String[] args) throws UnknownHostException {
		if (args.length == 0) {
			IPAddress = "127.0.0.1";
		} else if (args.length == 1) {
			IPAddress = args[0];
		} else {
			System.out.println("args[0] should be the IP address of the datacenter!");
			return;
		}

		String IPAddressesFile = "../DSBlog2/IPAddresses2";

		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(IPAddressesFile));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		String line;
		int lineNo = 0;
		try {
			while ((line = br.readLine()) != null) {
				line = line.trim();
				if (line.isEmpty())
					continue;
				desAddresses.add(InetAddress.getByName(line));
			}
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		desAddress = InetAddress.getByName(IPAddress);

		Thread listenToDCThread = new Thread(new ListenToDCThread());
		listenToDCThread.start();

		Scanner scanner = new Scanner(System.in);

		int desNum = -1;
		boolean willReadReq = true;
		String req = null;

		while (true) {
			if (willReadReq) {
				String s = scanner.nextLine().trim();
				boolean isPost = false, isLookup = false, isCfgChange = false;
				;
				isPost = s.matches("\\d+\\s+((POST)|(post))\\s+(\\S|\\s)+");
				if (!isPost) {
					isLookup = s.matches("\\d+\\s+((LOOKUP)|(lookup))");
					if (!isLookup) {
						isCfgChange = s.matches("\\d+\\s+(cfgChange)\\s+\\d+\\s+(\\d|\\.|\\s)+");
					}
				}

				StringBuilder request = new StringBuilder();

				if (isPost) {
					String[] ss = s.split("\\s+", 3);
					desNum = Integer.parseInt(ss[0]);
					request.append("p ");
					request.append(ss[2]);
					req = request.toString();
				} else if (isLookup) {
					String[] ss = s.split("\\s+", 2);
					desNum = Integer.parseInt(ss[0]);
					req = "l";
				} else if (isCfgChange) {
					String[] ss = s.split("\\s+", 3);
					desNum = Integer.parseInt(ss[0]);
					request.append("c ");
					request.append(ss[2]);
					req = request.toString();
					System.out.println(req);
				} else {
					System.out.println("Invalid request!");
					continue;
				}
			}

			Socket socket = null;
			try {
				socket = new Socket(desAddresses.get(desNum - 1), DC_LISTEN_TO_CLIENTS_PORT);
			} catch (ConnectException e) {
				System.out.println(e.getMessage() + ", possibly no process is listening on "
						+ desAddresses.get(desNum - 1).getHostAddress() + ":" + DC_LISTEN_TO_CLIENTS_PORT);
				desNum = desNum % desAddresses.size() + 1;
				willReadReq = false;
				System.out.println("Try to resend to " + desAddresses.get(desNum - 1).getHostAddress());
				continue;
			} catch (IOException e) {
				e.printStackTrace();
			}

			willReadReq = true;

			if (DEBUG) {
				System.out.println(
						"The client will send request \"" + req + "\" to " + socket.getInetAddress().getHostAddress());
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

					if (DEBUG) {
						System.out.println(
								"ListenToDCThread accepted! Messages from " + socket.getInetAddress().getHostAddress());
					}

					InputStream is = socket.getInputStream();
					ObjectInputStream ois = new ObjectInputStream(is);

					char signal = ois.readChar();

					if (DEBUG)
						System.out.println("signal = " + signal);

					if (signal == 'r') {

					} else if (signal == 'l') {
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
