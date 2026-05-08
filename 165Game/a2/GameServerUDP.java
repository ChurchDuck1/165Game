package a2;

import java.io.IOException;
import java.net.InetAddress;
import java.util.UUID;
import java.util.Vector;
import java.util.HashMap;
import tage.networking.server.GameConnectionServer;
import tage.networking.server.IClientInfo;
import tage.networking.IGameConnection.ProtocolType;

/**
 * Simple UDP-based game server for two-player networking.
 * Handles client connections and forwards messages between players.
 */
public class GameServerUDP extends GameConnectionServer<UUID>
{
	private Vector<UUID> connectedClients = new Vector<UUID>();
	private HashMap<UUID, Integer> clientAvatars = new HashMap<UUID, Integer>();

	public GameServerUDP(int localPort) throws IOException {
		super(localPort, ProtocolType.UDP);
	}

	@Override
	public void processPacket(Object o, InetAddress senderIP, int senderPort) {
		String message = (String) o;
		String[] messageTokens = message.split(",");

		if(messageTokens.length > 0) {
			//case where server receives a JOIN message
			if(messageTokens[0].compareTo("join") == 0) {
				try {
					IClientInfo clientInfo = getServerSocket().createClientInfo(senderIP, senderPort);
					UUID clientID = UUID.fromString(messageTokens[1]);
					
					addClient(clientInfo, clientID);
					connectedClients.add(clientID);
					
					//send success message
					sendJoinedMessage(clientID, true);
				}
				catch (IOException e) {
					e.printStackTrace();
				}
			}

			//case where server receives a CREATE message
			if(messageTokens[0].compareTo("create") == 0) {
				if(messageTokens.length >= 6) {
					try {
						UUID clientID = UUID.fromString(messageTokens[1]);
						String[] pos = {messageTokens[2], messageTokens[3], messageTokens[4]};
						int avatar = Integer.parseInt(messageTokens[5]);
						
						clientAvatars.put(clientID, avatar);
						sendCreateMessages(clientID, pos, avatar);
						
						sendWantsDetailsMessages(clientID);
					}
					catch (Exception e) {
						e.printStackTrace();
					}
				}
			}

			//case where server receives a MOVE message
			if(messageTokens[0].compareTo("move") == 0) {
				if(messageTokens.length >= 5) {
					try {
						UUID clientID = UUID.fromString(messageTokens[1]);
						String[] pos = {messageTokens[2], messageTokens[3], messageTokens[4]};
						
						sendMoveMessages(clientID, pos);
					}
					catch (Exception e) {
						e.printStackTrace();
					}
				}
			}

			//case where server receives a BYE message
			if(messageTokens[0].compareTo("bye") == 0) {
				try{
					UUID clientID = UUID.fromString(messageTokens[1]);
					
					sendByeMessages(clientID);
					
					removeClient(clientID);
					connectedClients.remove(clientID);
					clientAvatars.remove(clientID);
					System.out.println("Client disconnected: " + clientID);
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}

			//case where server receives a WANTS-DETAILS message
			if(messageTokens[0].compareTo("wsds") == 0){
				try {
					UUID requestorID = UUID.fromString(messageTokens[1]);
					
					sendWantsDetailsMessages(requestorID);
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}

			//case where server receives a DETAILS-FOR message
			if(messageTokens[0].compareTo("dsfr") == 0) {
				if(messageTokens.length >= 6) {
					try {
						UUID senderID = UUID.fromString(messageTokens[1]);
						UUID remoteID = UUID.fromString(messageTokens[2]);
						String[] pos = {messageTokens[3], messageTokens[4], messageTokens[5]};
						
						sendDetailsForMessage(remoteID, senderID, pos);
					}
					catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	private void sendJoinedMessage(UUID clientID, boolean success) {
		try {
			String message = new String("join,");
			if (success)
				message += "success";
			else
				message += "failure";
			
			sendPacket(message, clientID);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void sendCreateMessages(UUID clientID, String[] position, int avatar)
	{
		try {
			String message = new String("create," + clientID.toString());
			message += "," + position[0];
			message += "," + position[1];
			message += "," + position[2];
			message += "," + avatar;
			
			forwardPacketToAll(message, clientID);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void sendMoveMessages(UUID clientID, String[] position) {
		try {
			String message = new String("move," + clientID.toString());
			message += "," + position[0];
			message += "," + position[1];
			message += "," + position[2];

			forwardPacketToAll(message, clientID);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void sendByeMessages(UUID clientID) {
		try {
			String message = new String("bye," + clientID.toString());

			forwardPacketToAll(message, clientID);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void sendWantsDetailsMessages(UUID clientID) {
		try {
			String message = new String("wsds," + clientID.toString());

			forwardPacketToAll(message, clientID);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void sendDetailsForMessage(UUID remoteID, UUID senderID, String[] position) {
		try {
			String message = new String("dsfr," + senderID.toString());
			message += "," + position[0];
			message += "," + position[1];
			message += "," + position[2];
			message += "," + clientAvatars.get(senderID);
			
			sendPacket(message, remoteID);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
    	try {
        	int preferredPort = 5000;
        	if (args.length > 0) {
            	preferredPort = Integer.parseInt(args[0]);
        	}

        	int port = findAvailablePort(preferredPort, preferredPort + 20);
        	if (port == -1) {
           		System.out.println("ERROR: No available port found in range "
                	+ preferredPort + "–" + (preferredPort + 20));
            	return;
        	}

        	if (port != preferredPort) {
            	System.out.println("Port " + preferredPort
                	+ " is in use. Trying port " + port + " instead.");
        	}

        	GameServerUDP server = new GameServerUDP(port);
        	System.out.println("Game Server started on port " + port);
        	System.out.println("Waiting for clients...");

        	Thread.currentThread().join();
    	}
    	catch (Exception e) {
        	e.printStackTrace();
    	}
	}

/**
 * Scans [startPort, endPort] and returns the first port not already bound,
 * or -1 if none are free.
 */
	private static int findAvailablePort(int startPort, int endPort) {
    	for (int port = startPort; port <= endPort; port++) {
        	try (java.net.DatagramSocket probe = new java.net.DatagramSocket(port)) {
        	    return port; // successfully bound → port is free
        	}
        	catch (java.net.BindException e) {
        	    // port in use, try next
        	}
        	catch (java.io.IOException e) {
        	    // unexpected error on this port, skip it
        	}
    	}
    	return -1;
	}
}
