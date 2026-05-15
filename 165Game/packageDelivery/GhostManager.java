package packageDelivery;

import tage.*;

import org.joml.*;
import java.io.IOException;
import java.util.*;

public class GhostManager {
	private MyGame game;
	private Vector<GhostAvatar> ghostAvatars = new Vector<GhostAvatar>();

	private static final float WITCH_SCALE = 0.4f;

	public GhostManager(VariableFrameRateGame vfrg) {
		game = (MyGame)vfrg;
	}

	//creates ghost avatar with specified ID at the given position
	public void createGhost(UUID id, Vector3f p, int avatar) throws IOException {
		ObjShape s;
		TextureImage t;
		
		s = game.getWitchShape();

		if (avatar == 1) {
			t = game.getWitchTexture();      // witchTexB
		} else {
			t = game.getDolphinTexture();    // witchTexA, but still using witch shape
		}

		GhostAvatar newAvatar = new GhostAvatar(
			id,
			s,
			t,
			p,
			game.getBroomShape(),
			game.getBroomTexture()
		);

		newAvatar.setAvatar(avatar);
		newAvatar.setLocalScale(new Matrix4f().scaling(WITCH_SCALE));

		newAvatar.getRenderStates().setModelOrientationCorrection(
			new Matrix4f().rotationY((float)java.lang.Math.toRadians(180.0f))
		);

		ghostAvatars.add(newAvatar);
	}

	//remove a ghost avatar from game by ID
	public void removeGhostAvatar(UUID id) {
		GhostAvatar ghostAv = findAvatar(id);
		if(ghostAv != null) {
			game.getEngine().getSceneGraph().removeGameObject(ghostAv);
			ghostAvatars.remove(ghostAv);
		} else {
			System.out.println("unable to find ghost in list");
		}
	}

	//find a ghost avatar by ID
	private GhostAvatar findAvatar(UUID id) {
		GhostAvatar ghostAvatar;
		Iterator<GhostAvatar> it = ghostAvatars.iterator();

		while(it.hasNext()) {
			ghostAvatar = it.next();
			if(ghostAvatar.getID().compareTo(id) == 0) {
				return ghostAvatar;
			}
		}
		return null;
	}

	//update position of ghost avatar by ID
	public boolean updateGhostAvatar(UUID id, Vector3f position) {
		GhostAvatar ghostAvatar = findAvatar(id);
		if (ghostAvatar != null) {
			ghostAvatar.setLocalLocation(position);
			return true;
		}
		System.out.println("unable to find ghost in list");
		return false;
	}

	//update position and avatar of ghost avatar by ID
	public boolean updateGhostAvatar(UUID id, Vector3f position, int avatar) {
		GhostAvatar ghostAvatar = findAvatar(id);
		if (ghostAvatar != null) {
			ghostAvatar.setLocalLocation(position);

			if (ghostAvatar.getAvatar() != avatar) {
				ghostAvatar.setAvatar(avatar);

				ObjShape s;
				TextureImage t;

				s = game.getWitchShape();

				if (avatar == 1) {
					t = game.getWitchTexture();      // witchTexB
				} else {
					t = game.getDolphinTexture();    // witchTexA
				}

				ghostAvatar.setShape(s);
				ghostAvatar.setTextureImage(t);
				ghostAvatar.setLocalScale(new Matrix4f().scaling(WITCH_SCALE));

				ghostAvatar.getRenderStates().setModelOrientationCorrection(
					new Matrix4f().rotationY((float)java.lang.Math.toRadians(180.0f))
				);
			}

			return true;
		}

		System.out.println("unable to find ghost in list");
		return false;
	}

	//check if a ghost avatar already exists
	public boolean hasGhost(UUID id) {
		return findAvatar(id) != null;
	}

	//list of all ghost avatars
	public Vector<GhostAvatar> getGhostAvatars() {
		return ghostAvatars;
	}

	//count of ghost avatars in game
	public int getGhostCount() {
		return ghostAvatars.size();
	}
}