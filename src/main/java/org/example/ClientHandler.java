package org.example;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;



//When a client connects, the server spawns a thread to handle the client.
//This way the server can handle multiple clients at the same time
public class ClientHandler implements Runnable {
    // Arraylist of all the threads handling clients so each message can be sent to the client the thread is handling.
    public static List<ClientHandler> clientHandlers = new ArrayList<>();

    private Socket socket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private String clientUserName;

    public ClientHandler(Socket socket){
     try{
         this.socket = socket;
         this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
         this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
         this.clientUserName = bufferedReader.readLine();
         // Add the new client handler to the array, so they can receive messages from others.
         clientHandlers.add(this);
         broadcastMessage("SERVER: " + clientUserName + " has entered the chat!");

     }catch (IOException e){
         closeEverything(socket, bufferedReader, bufferedWriter);
     }

    }


    // Everything in this method is run on a separate thread. We want to listen for messages
    // on a separate thread because listening (bufferedReader.readLine()) is a blocking operation.
    // A blocking operation means the caller waits for the callee to finish its operation.
    @Override
    public void run() {
        String messageFromClient;

        while (socket.isConnected()) {
            try {
                messageFromClient = bufferedReader.readLine();
                if(messageFromClient == null) throw new IOException();
                broadcastMessage(messageFromClient);
            } catch (IOException e) {
                closeEverything(socket, bufferedReader, bufferedWriter);
                break;
            }
        }
    }


    // Send a message through each client handler thread so that everyone gets the message.
    // Basically each client handler is a connection to a client. So for any message that
    // is received, loop through each connection and send it down it.
    public void broadcastMessage(String messageToSend){
        for (ClientHandler clientHandler : clientHandlers) {
            try{
                if (!clientHandler.clientUserName.equals(clientUserName)){
                    clientHandler.bufferedWriter.write(messageToSend);
                    clientHandler.bufferedWriter.newLine();
                    clientHandler.bufferedWriter.flush();

                }
            }catch (IOException e){
                closeEverything(socket, bufferedReader, bufferedWriter);
            }
        }
    }

    //If the client disconnects for any reason remove them from the list so a message isn't sent down a broken connection.
    public void removeClientHandler(){
        clientHandlers.remove(this);
        broadcastMessage("SERVER: " + clientUserName + " has left");
    }


    public void closeEverything(Socket socket, BufferedReader bufferedReader, BufferedWriter bufferedWriter) {
        //Note you only need to close the outer wrapper as the underlying streams are closed when you close the wrapper.
        //Note that closing a socket will also close the socket's InputStream and OutputStream.
       removeClientHandler();//The client disconnected or an error occurred so remove them from the list so no message is broadcast.
       try {
           if (bufferedReader != null) {
               bufferedReader.close();
           }
           if (bufferedWriter != null) {
               bufferedWriter.close();
           }
           if (socket != null) {
               socket.close();
           }
       }catch (IOException e){
           e.printStackTrace();
       }
    }
}
