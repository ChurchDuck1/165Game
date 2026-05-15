package packageDelivery;

import tage.*;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import java.io.IOException;
import java.util.*;

public class GhostManager {
	private MyGame game;
	private Vector<GhostAvatar> ghostAvatars = new Vector<GhostAvatar>();

	private static final float WITCH_SCALE = 0.4f;

	public GhostManager(VariableFrameRateGame vfrg) {
		game = (MyGame)vfrg;
	}

	// creates ghost avatar with specified ID at the given position
	public void createGhost(UUID id, Vector3f p, int avatar) throws IOException {
		ObjShape s = game.getAvatarShape(avatar);
		TextureImage t = game.getAvatarTexture(avatar);

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

	// remove a ghost avatar from game by ID
	public void removeGhostAvatar(UUID id) {
		GhostAvatar ghostAv = findAvatar(id);

		if (ghostAv != null) {
			System.out.println("Removing ghost avatar: " + id);

			ghostAv.removeFromScene(game.getEngine());
			ghostAvatars.remove(ghostAv);
		} else {
			System.out.println("unable to find ghost in list for bye id: " + id);
			printGhostIDs();
		}
	}

	private void printGhostIDs() {
		System.out.println("Current ghost IDs:");
		for (GhostAvatar ghost : ghostAvatars) {
			System.out.println(" - " + ghost.getID());
		}
	}

	// find a ghost avatar by ID
	private GhostAvatar findAvatar(UUID id) {
		GhostAvatar ghostAvatar;
		Iterator<GhostAvatar> it = ghostAvatars.iterator();

		while (it.hasNext()) {
			ghostAvatar = it.next();

			if (ghostAvatar.getID().compareTo(id) == 0) {
				return ghostAvatar;
			}
		}

		return null;
	}

	// update position of ghost avatar by ID
	public boolean updateGhostAvatar(UUID id, Vector3f position) {
		GhostAvatar ghostAvatar = findAvatar(id);

		if (ghostAvatar != null) {
			ghostAvatar.setLocalLocation(position);
			return true;
		}

		System.out.println("unable to find ghost in list");
		return false;
	}

	// update position and avatar texture of ghost avatar by ID
	public boolean updateGhostAvatar(UUID id, Vector3f position, int avatar) {
		GhostAvatar ghostAvatar = findAvatar(id);

		if (ghostAvatar != null) {
			ghostAvatar.setLocalLocation(position);

			// Always use witch model, texture depends on remote player's avatar value
			ghostAvatar.setAvatar(avatar);
			ghostAvatar.setShape(game.getAvatarShape(avatar));
			ghostAvatar.setTextureImage(game.getAvatarTexture(avatar));
			ghostAvatar.setLocalScale(new Matrix4f().scaling(WITCH_SCALE));

			ghostAvatar.getRenderStates().setModelOrientationCorrection(
				new Matrix4f().rotationY((float)java.lang.Math.toRadians(180.0f))
			);

			return true;
		}

		System.out.println("unable to find ghost in list");
		return false;
	}

	// check if a ghost avatar already exists
	public boolean hasGhost(UUID id) {
		return findAvatar(id) != null;
	}

	// list of all ghost avatars
	public Vector<GhostAvatar> getGhostAvatars() {
		return ghostAvatars;
	}

	// count of ghost avatars in game
	public int getGhostCount() {
		return ghostAvatars.size();
	}
}