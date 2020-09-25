import java.util.*;
import java.net.*;
import java.io.*;

public class clientHandler implements Runnable {
    private Server server;
    private Socket clientSocket;
    BufferedReader input;
    String messageout;

    public clientHandler(Server se, Socket s) {
        this.server = se;
        this.clientSocket = s;
        
        try {
            setupInputStream(clientSocket);
        } catch(IOException ioe) {ioe.printStackTrace();}
        
    }

    @Override
    public void run() {
        try {
            while ((messageout = input.readLine()) != null) {
                server.sendMessageToAll(clientSocket, messageout);
            }
            System.out.println(clientSocket.getInetAddress().getHostAddress() + " has disconnected from chat.");
            this.closeSocket();
        } catch(IOException ioe) {
            System.err.println(clientSocket.getInetAddress().getHostAddress() + " disconnected brutally.");
            try {
                this.closeSocket();
            } catch(IOException io) {}
        }
    }

    public synchronized void setupInputStream(Socket s) throws IOException {
        input = new BufferedReader(new InputStreamReader(s.getInputStream()));
    }

    public synchronized void closeSocket() throws IOException {
        server.removeClientSocket(this.clientSocket);
        this.input.close();
        this.clientSocket.close();
        server.sendMessageToAll(null, clientSocket.getInetAddress().getHostAddress() + " has left the chat.");
        server.connectedUsers();
    }
}