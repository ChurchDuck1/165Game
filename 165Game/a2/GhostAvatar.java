package a2;

import tage.*;
import org.joml.*;
import java.util.UUID;

public class GhostAvatar extends GameObject
{
	private UUID id;

	public GhostAvatar(UUID id, ObjShape s, TextureImage t, Vector3f p) {   
        super(GameObject.root(), s, t);
		this.id = id;
		setLocalLocation(p);
	}

	public UUID getID() {
		return id;
	}

	public void setID(UUID id) {
		this.id = id;
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
