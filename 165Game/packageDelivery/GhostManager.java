package packageDelivery;

import tage.*;
import org.joml.*;
import java.io.IOException;
import java.util.*;

public class GhostManager {
	private MyGame game;
	private Vector<GhostAvatar> ghostAvatars = new Vector<GhostAvatar>();
	private static final float GHOST_SCALE = 3.0f; //make normal size

	public GhostManager(VariableFrameRateGame vfrg) {
		game = (MyGame)vfrg;
	}


	//creates ghost avatar with specified ID at the given position
	public void createGhost(UUID id, Vector3f p, int avatar) throws IOException {
		ObjShape s;
		TextureImage t;
		
		if (avatar == 1) { //witch
			s = game.getWitchShape();
			t = game.getWitchTexture();
		} else { //dolphin
			s = game.getDolphinShape();
			t = game.getDolphinTexture();
		}

		GhostAvatar newAvatar = new GhostAvatar(id, s, t, p);
		newAvatar.setAvatar(avatar);
		float scale = (avatar == 1) ? 0.4f : 3.0f;
		Matrix4f initialScale = (new Matrix4f()).scaling(scale);
		newAvatar.setLocalScale(initialScale);

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

    //update position of thost avatar by ID
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
				if (avatar == 1) { // witch
					s = game.getWitchShape();
					t = game.getWitchTexture();
				} else { // dolphin
					s = game.getDolphinShape();
					t = game.getDolphinTexture();
				}
				ghostAvatar.setShape(s);
				ghostAvatar.setTextureImage(t);
				float scale = (avatar == 1) ? 0.4f : 3.0f;
				Matrix4f newScale = (new Matrix4f()).scaling(scale);
				ghostAvatar.setLocalScale(newScale);
			}
			return true;
		}
		System.out.println("unable to find ghost in list");
		return false;
	}

	//check if a ghost avatar already exhists
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
