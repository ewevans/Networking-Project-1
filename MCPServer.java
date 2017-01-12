package com.company;
import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.util.List;

public class MCPServer {

    //Ethan Evans and Andrew Zourob
    // 11/6/16
    // CIS 427 Fall '16

    //          MCP Server (Part 2)
    // This program will implement the server-side of "Multiple Channel Protocol", which
    //  is a file transfer service.
    //

    // This project holds alice.txt, which will be transferred via UDP in the future.

    public static void main(String argv[]) throws Exception {
        System.out.println("Server has started.");

        //The string that arrives from the client
        String clientMessage;

        //Socket and Stream set-up
        ServerSocket welcomeSocket = new ServerSocket(6789);
        Socket connectionSocket = welcomeSocket.accept();
        BufferedReader inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
        DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());

        //never-ending server loop
        while(true) {
            //get client's command
            clientMessage = inFromClient.readLine();
            System.out.println("Client message to server: " + clientMessage);

            if (clientMessage.equals("query") || clientMessage.contains("query"))
            {
                System.out.println("Client from " + connectionSocket.getInetAddress() + " requests query");

                //int that holds the current byte location of the line
                int byteSummer = 0;

                //string array where each element corresponds to the number of bytes in each line
                String[] seqNums = QueryFile();

                for(int i = 0; i < seqNums.length; i= i+1)
                {
                    //get byte size of each line of alice.txt
                    byte[] byteString = seqNums[i].getBytes();


                    //send each sequence number to the client
                    outToClient.writeBytes(byteSummer + "\n");
                        //outToClient.writeBytes(seqNums[i] + "\n");
                        //System.out.println(seqNums[i] + "\n");

                    //Sum each byte size to get "sequence number"
                    byteSummer += byteString.length;
                }

                //server console message to show client interaction
                outToClient.writeBytes("END_QUERY\n");
                System.out.println(seqNums.length + " sequence numbers since to client.");
                System.out.println("END_QUERY sent to " + connectionSocket.getInetAddress());
            }
            if (clientMessage.contains("download")){
                //split string to get port number
                int portNumber = Integer.parseInt(clientMessage.substring(clientMessage.lastIndexOf(" ")+1));
                int receivedACK = 0;
                int lastACK = 0;
                String output;
                int sumOfBytes = 0;

                //get lines of text from alice.txt
                String[] aliceLines = QueryFile();
                int lineNumber = 0;

                System.out.println("Client from " + connectionSocket.getInetAddress() + "/" + portNumber + " requests download");

                //Make UDP socket
                DatagramSocket UDPserverSocket = new DatagramSocket(7111);


                while(true)
                {
                    byte[] receiveData = new byte[1024];
                    byte[] sendData = new byte[1024];
                    byte[] seqNum = new byte[4];

                    //used in creating btye[] with first four bytes as sequence number
                    byte[] temp = new byte[1024];

                    //create byte array with 0-3: seqNum and then line of alice.txt after based on lineNumber
                    output = aliceLines[lineNumber];
                    temp = output.getBytes();
                    System.arraycopy(temp, 0, sendData, 4, temp.length);
                    seqNum = ByteBuffer.allocate(4).putInt(sumOfBytes).array();
                    System.arraycopy(seqNum, 0, sendData, 0, 4);

                    //increase sequence number for next iteration
                    sumOfBytes += output.length() + 4;

                    //send packet containing created seqNum + message
                    DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length,
                            InetAddress.getByName("localhost"), portNumber);
                    UDPserverSocket.send(sendPacket);
                    //System.out.println(output + " sent");

                    //receive ACK
                    DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                    UDPserverSocket.receive(receivePacket);
                    receivedACK = ByteBuffer.wrap(receiveData).getInt();



                    //testing prints
                    //System.out.println("receivedACK = " + receivedACK);
                    //System.out.println("correct ACK to receive? = " + (sumOfBytes));
                    //System.out.println("line number = " + (lineNumber));

                    //if ACK is correct increase lineNumber, otherwise don't (next iteration will resend)
                    if (sumOfBytes != receivedACK)
                    {
                        //since resending message, reset sequence number to before last packet sent
                        sumOfBytes = sumOfBytes - temp.length - 4;

                        //do not increment lineNumber, because resending line in next loop iteration
                        //System.out.println("retransmitting last line: " + output);
                    }
                    else
                    {
                        lineNumber++;
                    }



                    //end connection if all data sent
                    if (lineNumber >= aliceLines.length)
                    {
                        //send "END_DOWNLOAD" to stop UDP transmission
                        output = "****END_DOWNLOAD";
                        sendData = output.getBytes();
                        sendPacket = new DatagramPacket(sendData, sendData.length,
                                InetAddress.getByName("localhost"), portNumber);
                        UDPserverSocket.send(sendPacket);

                        System.out.println("END_DOWNLOAD sent to " + connectionSocket.getInetAddress() + "/" + portNumber);

                        UDPserverSocket.close();
                        //exit while loop
                        break;
                    }

                }//end while loop
            }//end server loop
        }
    }


    //Converts alice.txt into string array and returns it
    public static String[] QueryFile() throws IOException
    {
        //create file path for alice.txt
        Path path = Paths.get("alice.txt");

        //Get list of lines from text file
        List<String> lines = Files.readAllLines(path);

        //convert to string array
        String[] arr = lines.toArray(new String[lines.size()]);

        return arr;
    }
}

