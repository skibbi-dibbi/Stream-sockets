import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.awt.*;
import java.awt.event.ActionEvent;


public class Client {
    private BufferedReader input;
    private PrintWriter output;
    private String host;
    private static int port;
    private String messagein = "";
    private String messageout = "";
    private Socket clientSocket;
    private Scanner keyboard = new Scanner(System.in);
    ExecutorService executorService = Executors.newSingleThreadExecutor(); 

    public static void main(String[] args) {
        Client c;

        if (args.length == 0) {
            c = new Client();
        } else if (args.length == 1) {
           c = new Client(args[0]);
        } else {
            try {
                port = Integer.parseInt(args[1]);
            } catch(NumberFormatException nfe) {
                System.err.println("The second argument needs to be an numeric value for port.");
                System.exit(1);
            }
            c = new Client(args[0], port);
        }
        c.start();
    }

    //Constructor handling both host and port
    public Client(String s, int p) {
        this.host = s;
        this.port = p;
    }

    //Constructor changing host, keeping default port
    public Client(String s) {
        this(s, 2000);
    }

    //Constructor keeping default port and host
    public Client() {
        this("127.0.0.1", 2000);
    }

    //After setting IP and port straight, 
    //we start the process of connecting and establishing the chat client
    public void start() {
        //Try to connect to server
        connectToServer();

        //Try to set up IO-streams
        setupStreams();

        //Invoke a new thread running the function inChat which will handle all incoming messages
        executorService.submit(new Runnable(){
            public void run() {
                try{
                    inChat();
                } catch(IOException ioe) {}
            }
        });

        //Enter the function from which we will be able to post messages on output stream
        sendMessages();


        //Finally close streams and socket if sendMessages has been exited by STOP-command
        closeAll();        
    }

    //Try to connect to server function
    private void connectToServer() {
        try {
            System.out.println("Attempting to connect to: " + host + " "+ port);
            clientSocket = new Socket(host, port);
            System.out.println("Successfully connected to: " + host + " "+ port);
        } catch(IOException ioe) {
            System.err.println("Could not connect to server.");
            System.exit(1);
        }
    }

    //Try to set up the input and output stream using the socket connection
    private void setupStreams() {
        System.out.println("Attempting to setup streams...");
        try {
            output = new PrintWriter(clientSocket.getOutputStream(), true);
            input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            System.out.println("Successfully setup streams!");
        } catch(IOException ioe) {
            System.err.println("Could not setup streams. Please try again.");
            System.exit(1);
        }
    }

    //inChat function which will run in a separate thread and will listen to the input stream and print incoming messages out
    private void inChat() throws IOException {
        try {
            while ((messagein = (String) input.readLine()) != null) {
                if (messagein.equals("SERVER: SERVER SHUTDOWN COMMENCED/123456789")) {
                    System.out.println("Server shutting down...");
                    closeAll();
                    break;
                } else {
                    System.out.println(messagein);
                }
            }
            closeAll();
        } catch(Exception e) {closeAll();}
    }

    //Function that will allow us via System.in (keyboard) post messages to output stream
    private void sendMessages() {
        try{
            while (true) {
                messageout = keyboard.nextLine();
                if (!messageout.equalsIgnoreCase("exit")) {
                    output.println(messageout);
                } else {
                    break;
                }
            };
        } catch(NoSuchElementException nsee) {}
    }

    //Close streams and socket
    private void closeAll() {
        try {
            output.close();
            input.close();
            clientSocket.close();
            System.out.println("Successfully closed streams and the socket!");
            System.exit(0);
        } catch(IOException ioe) {
            ioe.printStackTrace();
            System.err.println("Was not able to correctly close streams and socket!");
        }
    }
}
