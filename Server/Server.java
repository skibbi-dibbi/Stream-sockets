import java.net.*;
import java.awt.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private ServerSocket serverSocket;
    private static int port;
    private Vector<Socket> clientSockets;
    boolean exit;
    ExecutorService executorService = Executors.newFixedThreadPool(10);
    Scanner keyboard;
    
    public static void main(String[] args) {
        
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch(NumberFormatException nfe) {
                System.err.println("The argument needs to be a numeric value for port.");
                System.exit(1);
            }
            new Server(port);
        } else {
            new Server();
        }

    }

    //Calling constructor with port 2000 if no argument was passed into argv
    public Server() {
        this(2000);
    }

    //Constructor taking port as argument
    public Server(int p) {
        port = p;
        clientSockets = new Vector<Socket>();
        keyboard = new Scanner(System.in);
        this.exit = false;
        startServer();

        //Start a new thread handling assignment of new clients connecting
        executorService.execute(new Runnable(){
            public void run() {
                try {
                    waitForConnections();
                } catch(IOException ioe) {};       
            }
        });

        //Make it possible to shut down server via keyboard command "exit"
        while (true) {
            if (keyboard.next().equalsIgnoreCase("exit")) {
                exit = true;
                try {
                    sendMessageToAll(null, "SERVER SHUTDOWN COMMENCED/123456789");
                    Thread.sleep(1000);
                } catch(IOException ioe) {} catch(InterruptedException ie) {}
                break;
            }
        }

        try {
            closeAll();
        } catch(IOException ioe) {
            System.err.println("Could not successfully shut down server. Exiting with code 1.");
            System.exit(1);
        }
    }

    //Initiating the socket using the designated port and printing out the server info
    public void startServer() {
        try {
            serverSocket = new ServerSocket(port);
            
            System.out.println("Server started on"
            + "\nIP  :\t" + serverSocket.getInetAddress().getLocalHost().getHostAddress()
            + "\nPort:\t" + port);
        } catch (Exception e) {
            System.err.println("Something went wrong starting the server.");
            System.exit(1);
        }
    }

    //The loop that will accept new connecting clients
    public void waitForConnections() throws IOException {
        do {
            newClient(serverSocket.accept());
        } while (!exit);
    }
    
    //Add new clientsocket to vector, execute new thread with new clientHandler which is a runnable and will handle incoming messages. Also announce newcomers to chat.
    public synchronized void newClient(Socket s) {
        clientSockets.add(s);
        executorService.execute(new clientHandler(this, s));
    
        try {
            sendMessageToAll(null, s.getInetAddress().getHostAddress() + " has connected to the chat.");
            connectedUsers();
        } catch(IOException ioe) {
            ioe.printStackTrace();
        }
    }

    //Loop through all current client sockets and send the message
    public synchronized void sendMessageToAll(Socket s, String str) throws IOException {
        for (int i = 0; i < clientSockets.size(); i++) {
            if (s == null) {
                sendMessageToClient(clientSockets.get(i), "SERVER: " + str);
            } else {
                sendMessageToClient(clientSockets.get(i), s.getInetAddress().getHostName() + ": " + str);
            }
        }
    }

    //Prints out current number of users to users. Since I am not using a GUI.
    public synchronized void connectedUsers() throws IOException {
        sendMessageToAll(null, "Number of connected users in chat are now: " + clientSockets.size());
    }

    //The function flushing a message to given sockets output stream
    public synchronized void sendMessageToClient(Socket s, String msg) throws IOException {
        new PrintWriter(s.getOutputStream(), true).println(msg);
    }

    //Called from clientHandler when getting ready to shut down, in order to just maintain open client sockets in the Vector
    public synchronized void removeClientSocket(Socket s) throws IOException {
        clientSockets.remove(s);
    }
    
    //Closing all client sockets ahead of server shutdown
    public synchronized void closeClientSockets() throws IOException {
        System.out.println("Closing all client sockets...");
        for (int i = 0; i < clientSockets.size(); i++) {
            Socket s = clientSockets.get(i);
            s.close();
        }
        System.out.println("Successfully closed client sockets!");
        clientSockets.clear();
    }

    //Function to start the closing process
    public synchronized void closeAll() throws IOException {
        closeClientSockets();
        System.out.println("Closing server socket...");
        serverSocket.close();
        System.out.println("Serversocket closed. Server shutting down.");
        System.exit(0);
    }
}