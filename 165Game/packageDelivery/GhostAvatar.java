package packageDelivery;

import tage.*;
import org.joml.*;
import java.util.UUID;

public class GhostAvatar extends GameObject
{
	private UUID id;
	private int avatar;

	private GameObject broom;

	public GhostAvatar(UUID id, ObjShape s, TextureImage t, Vector3f p,
	                   ObjShape broomShape, TextureImage broomTex)
	{   
		super(GameObject.root(), s, t);

		this.id = id;
		this.avatar = 0;

		setLocalLocation(p);

		// Attach broom to this ghost avatar
		broom = new GameObject(this, broomShape, broomTex);

		broom.propagateTranslation(true);
		broom.propagateRotation(true);
		broom.propagateScale(false);
		broom.applyParentRotationToPosition(true);
		broom.applyParentScaleToPosition(false);

		// Same placement as your player broom
		broom.setLocalTranslation(new Matrix4f().translation(0.00f, -2.40f, -2.20f));

		Matrix4f broomRot = new Matrix4f()
			.rotationY((float)java.lang.Math.toRadians(0.0f))
			.rotateX((float)java.lang.Math.toRadians(78.0f))
			.rotateZ((float)java.lang.Math.toRadians(0.0f));

		broom.setLocalRotation(broomRot);
		broom.setLocalScale(new Matrix4f().scaling(0.50f));
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