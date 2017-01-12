package com.company;
import java.io.*; import java.net.*;
import java.nio.ByteBuffer;
import java.util.Random;

public class MCPClient {

    //Ethan Evans and Andrew Zourob
    // 11/6/16
    // CIS 427 Fall '16
    
    //          MCP Client (Part 2)
    // This program will implement the client-side of "Multiple Channel Protocol", which 
    //  is a file transfer service.
    
    public static void main(String argv[]) throws Exception {

        //user input
        String clientInput;

        //message returned from server
        String serverMessage;

        try {
            //Socket and Reader setup
            BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
            Socket clientSocket = new Socket("localhost", 6789);
            DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
            BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            System.out.println ("+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+");
            System.out.println ("+-+-+-+-+-+-+-+ Multiple Channel Protocol +-+-+-+-+-+-+-+");
            System.out.println ("+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+");
            System.out.println ("Commands allowed by the server for this client:");
            System.out.println ("  query");
            System.out.println ("  download");

            clientInput = "";

            //Client Input loop
            while (!clientInput.equals("quit")) {
                System.out.println ("Command:");

                //get client user input
                clientInput = inFromUser.readLine();

                if (clientInput.equals("query") || clientInput.contains("query"))
                {
                    outToServer.writeBytes(clientInput + '\n');

                    //This loop reads from the socket until the "END_QUERY" message
                    //  each sequence number is printed to console
                    while(!(serverMessage = inFromServer.readLine()).equals("END_QUERY"))
                    {
                        System.out.println(serverMessage);
                    }

                }
                else if (clientInput.equals("download"))
                {
                    //random number generator used to discard 10% of packets
                    Random rand = new Random();

                    //sum of bytes to cumulatively acknowledge the packets received
                    int cumulativeACK = 0;

                    //the number of bytes in the received string
                    int receivedStringBytes;

                    //string received in each loop of the communication
                    String receivedString;

                    DatagramPacket receivePacket;

                    //send download command plus port number
                    outToServer.writeBytes(clientInput + " 6789" + '\n');

                    //Create BufferedReader and UDP socket
                    DatagramSocket UDPclientSocket = new DatagramSocket(6789);
                    InetAddress IPAddress = InetAddress.getByName("localhost");

                    while (true)
                    {
                        byte[] sendData = new byte[1024];
                        byte[] receiveData = new byte[1024];
                        byte[] stringBytes = new byte[1024];
                        byte[] seqNumArray = new byte[4];
                        int seqNum;

                        //Receive UDP packets from server
                        receivePacket = new DatagramPacket(receiveData, receiveData.length);
                        UDPclientSocket.receive(receivePacket);
                        receiveData = receivePacket.getData();


                        //Remove sequence number from message
                        System.arraycopy(receiveData, 4, stringBytes, 0, receiveData.length-4);


                        //first 4 bytes -> int seqNum
                        System.arraycopy(receiveData, 0, seqNumArray, 0, 4);
                        seqNum = ByteBuffer.wrap(seqNumArray).getInt();

                        receivedString = new String(stringBytes);


                        //System.out.println(receivedString);

                        //end while loop if "END_DOWNLOAD" received
                        if (receivedString.contains("END_DOWNLOAD"))
                        {
                            break;
                        }

                        //get # of bytes from received string
                        receivedStringBytes = receivedString.replaceAll("\0+$","").getBytes("UTF-8").length + 4;


                        //Check for error (10% of time manual error occurs)
                        //If error, don't change ACK packet from last iteration
                        //If no error, add receivedStringBytes to the cumulativeACK
                        if (rand.nextInt(10) == 0) {
                            //discard packet
                            System.out.println("Discarding Packet (seqNum = " + seqNum + ") containing: " +
                                    "           \n      " + receivedString + "\n");
                        } else {
                            //keep packet
                            System.out.println(cumulativeACK + "\t" + receivedString);
                            //add bytes in received string to the cumulative ACK
                            cumulativeACK += receivedStringBytes;
                        }

                        //create packet with cumulative ACK
                        sendData = ByteBuffer.allocate(4).putInt(cumulativeACK).array();

                        //send packet
                        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, 7111);
                        UDPclientSocket.send(sendPacket);

                    }//end while loop

                    //Close UDP socket
                    UDPclientSocket.close();

                }
                else if (clientInput.equals("quit"))
                {
                    //loop will stop
                    System.out.println("Thank you!");
                }
                else
                {
                    System.out.println ("Invalid command.");
                    System.out.println (" ");
                    System.out.println ("Commands allowed by the server for this client:");
                    System.out.println ("  query");
                    System.out.println ("  download");
                }

            } //end while

            clientSocket.close();
        } catch(IOException e) {
            System.out.println ("Connection failed");
            e.printStackTrace();
        }


    }
}


