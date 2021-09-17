package com.getman;

import java.net.*;
import java.io.*;
import java.util.*;

public class Server {
	ArrayList<ObjectOutputStream> outputs;

	Server() {
		outputs = new ArrayList<ObjectOutputStream>();
	}
	
	public static void main(String[] args) {
		new Server().start();
	}

	void start() {
		try {
			ServerSocket serverSocket = new ServerSocket(4242);
			while (true) {
				Socket socketForClient = serverSocket.accept();

				ObjectOutputStream out = new ObjectOutputStream(socketForClient.getOutputStream());

				outputs.add(out);

				Thread listenToMessages = new Thread(new ClientHandler(socketForClient));
				listenToMessages.start();
				System.out.println("Got a connection");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	
	void tellEveryone(Object message, Object checkBoxStates) {
		Iterator it = outputs.iterator();

		while (it.hasNext()) {
			try {
				ObjectOutputStream out = (ObjectOutputStream) it.next();
				out.writeObject(message);
				out.writeObject(checkBoxStates);
			}catch(SocketException e) {
				it.remove();
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

	class ClientHandler implements Runnable {
		private Socket socket;
		private Object message;
		private Object checkBoxStates;

		ClientHandler(Socket socket) {
			this.socket = socket;
		}

		public void run() {
			try {
				ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
				while ((message = in.readObject()) != null) {
					checkBoxStates = in.readObject();
					System.out.println("Message: " + (String) message);
					tellEveryone(message, checkBoxStates);
				}
			}catch(EOFException e){
				System.out.println(socket.getPort() + " : Client disconnected");
			} 
			catch (IOException e) {
				e.printStackTrace();
			} 
			catch (Exception e) {
				e.printStackTrace();
			}
		}

		

	}
}
