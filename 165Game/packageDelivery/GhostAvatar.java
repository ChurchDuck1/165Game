package packageDelivery;

import tage.*;
import org.joml.*;
import java.util.UUID;

public class GhostAvatar extends GameObject
{
	private UUID id;
	private int avatar;

	public GhostAvatar(UUID id, ObjShape s, TextureImage t, Vector3f p) {   
        super(GameObject.root(), s, t);
		this.id = id;
		this.avatar = 0; // default
		setLocalLocation(p);
	}

	public UUID getID() {
		return id;
	}

	public void setID(UUID id) {
		this.id = id;
	}

	public int getAvatar() {
		return avatar;
	}

	public void setAvatar(int avatar) {
		this.avatar = avatar;
	}

	public Vector3f getLocalLocation() {
		return super.getLocalLocation();
	}

	public void setLocalLocation(Vector3f pos) {
		super.setLocalLocation(pos);
	}

	public Vector3f getWorldLocation() {
		return super.getWorldLocation();
	}
}
