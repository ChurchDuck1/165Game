package a2;

import tage.networking.client.GameConnectionClient;
import tage.networking.IGameConnection.ProtocolType;
import org.joml.Vector3f;
import java.io.IOException;
import java.net.InetAddress;
import java.util.UUID;

//client protocol for multiplayer networking
public class ProtocolClient extends GameConnectionClient {
	private MyGame game;
	private UUID id;
	private GhostManager ghostManager;
	private boolean sentCreate = false;

	public ProtocolClient(InetAddress remAddr, int remPort, ProtocolType pType, MyGame game) throws IOException {
		super(remAddr, remPort, pType);
		this.game = game;
		this.id = UUID.randomUUID();
		ghostManager = game.getGhostManager();
	}

	//send a join message to server
	public void sendJoinMessage()
	{
		try {
			sendPacket(new String("join," + id.toString()));
		} 
        catch (IOException e) {
			e.printStackTrace();
		}
	}

    //send a create message to the server
	public void sendCreateMessage(Vector3f pos) {
		try {
			String message = new String("create," + id.toString());
			message += "," + pos.x + "," + pos.y + "," + pos.z;
			sentCreate = true;
			sendPacket(message);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	//send a move message to the server
	public void sendMoveMessage(Vector3f pos) {
		try {
			String message = new String("move," + id.toString());
			message += "," + pos.x + "," + pos.y + "," + pos.z;
			sendPacket(message);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	//send a bye message to the server
	public void sendByeMessage() {
		try {
			String message = new String("bye," + id.toString());
			sendPacket(message);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	//send a details-for message to the server for a specific remote player
	public void sendDetailsForMessage(UUID remId) {
		try {
			String message = new String("dsfr," + id.toString() + "," + remId.toString());
			sendPacket(message);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

    //send a wants-details message to the server to request details for all remote players
	public void sendWantsDetailsMessage() {
		try {
			String message = new String("wsds," + id.toString());
			sendPacket(message);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	//process incoming packets from the server
	@Override
	protected void processPacket(Object msg) {
		String strMessage = (String)msg;
		String[] messageTokens = strMessage.split(",");

		if(messageTokens.length > 0) {
			//recieve "join"
			if(messageTokens[0].compareTo("join") == 0) {
				if(messageTokens.length > 1) {
					if(messageTokens[1].compareTo("success") == 0) {
						game.setIsConnected(true);
						sendCreateMessage(game.getPlayerPosition());
					}
					else if(messageTokens[1].compareTo("failure") == 0) {
						game.setIsConnected(false);
						System.out.println("Failed to connect to server");
					}
				}
			}

			//handle incoming create message from other players
			if(messageTokens[0].compareTo("create") == 0) {
				if(messageTokens.length >= 5) {
					try {
						UUID ghostID = UUID.fromString(messageTokens[1]);
		
						//don't create a ghost for ourself
						if(ghostID.compareTo(id) == 0) {
							return;
						}
						
						Vector3f ghostPosition = new Vector3f(
							Float.parseFloat(messageTokens[2]),
							Float.parseFloat(messageTokens[3]),
							Float.parseFloat(messageTokens[4]));

						if (ghostManager.hasGhost(ghostID)) {
							ghostManager.updateGhostAvatar(ghostID, ghostPosition);
						}
						else {
							ghostManager.createGhost(ghostID, ghostPosition);
						}
					}
					catch (NumberFormatException | IOException e) {
						System.out.println("error creating ghost avatar");
						e.printStackTrace();
					}
				}
			}

			//handle dsfr (details for) message
			if(messageTokens[0].compareTo("dsfr") == 0) {
				if(messageTokens.length >= 5) {
					try {
						UUID ghostID = UUID.fromString(messageTokens[1]);
						
						//don't create a ghost for ourself
						if(ghostID.compareTo(id) == 0) {
							return;
						}
						
						Vector3f ghostPosition = new Vector3f(
							Float.parseFloat(messageTokens[2]),
							Float.parseFloat(messageTokens[3]),
							Float.parseFloat(messageTokens[4]));

						if (ghostManager.hasGhost(ghostID)) {
							ghostManager.updateGhostAvatar(ghostID, ghostPosition);
						}
						else {
							ghostManager.createGhost(ghostID, ghostPosition);
						}
					}
					catch (NumberFormatException | IOException e) {
						System.out.println("error creating ghost avatar from dsfr");
						e.printStackTrace();
					}
				}
			}

			//handle bye message to remove ghost avatar of remote player that left
			if(messageTokens[0].compareTo("bye") == 0) {
				if(messageTokens.length > 1) {
					try {
						UUID ghostID = UUID.fromString(messageTokens[1]);
						ghostManager.removeGhostAvatar(ghostID);
					}
					catch (IllegalArgumentException e) {
						System.out.println("error parsing ghost ID from bye message");
					}
				}
			}

			//handle move message to update ghost avatar position
			if(messageTokens[0].compareTo("move") == 0) {
				if(messageTokens.length >= 5) {
					try {
						UUID ghostID = UUID.fromString(messageTokens[1]);
						Vector3f ghostPosition = new Vector3f(
							Float.parseFloat(messageTokens[2]),
							Float.parseFloat(messageTokens[3]),
							Float.parseFloat(messageTokens[4]));

						ghostManager.updateGhostAvatar(ghostID, ghostPosition);
					}
					catch (NumberFormatException e) {
						System.out.println("error parsing move message");
					}
				}
			}

			//handle wants details message to send our details to new remote player
			if(messageTokens[0].compareTo("wsds") == 0) {
				if(messageTokens.length > 1) {
					UUID requestorID = UUID.fromString(messageTokens[1]);
					if (requestorID.compareTo(id) != 0) {
						System.out.println("Received wsds request from " + requestorID + ", sending CREATE as details response");
						sendCreateMessage(game.getPlayerPosition());
					}
				}
			}
		}
	}

	public UUID getId() {
		return id;
	}
}
