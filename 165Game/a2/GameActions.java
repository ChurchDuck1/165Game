package a2;

import tage.input.action.AbstractInputAction;
import net.java.games.input.Event;
import org.joml.Vector3f;

class ToggleAxesAction extends AbstractInputAction {
	private MyGame game;
	public ToggleAxesAction(MyGame game) { this.game = game; }
	@Override
	public void performAction(float time, Event e) {
		if (!game.gameStart) return;
		game.axesVisible = !game.axesVisible;
		if (game.axesVisible) {
			game.xAxis.getRenderStates().enableRendering();
			game.yAxis.getRenderStates().enableRendering();
			game.zAxis.getRenderStates().enableRendering();
		} else {
			game.xAxis.getRenderStates().disableRendering();
			game.yAxis.getRenderStates().disableRendering();
			game.zAxis.getRenderStates().disableRendering();
		}
	}
}

class OrbitAzimuthAction extends AbstractInputAction {
	private MyGame game;
	private float dir;
	public OrbitAzimuthAction(MyGame game, float d) { this.game = game; dir = d; }
	@Override
	public void performAction(float time, Event e) {
		if (!game.gameStart) return;
		game.orbitController.orbitAzimuth(dir * 90.0f * time);
	}
}

class OrbitElevationAction extends AbstractInputAction {
	private MyGame game;
	private float dir;
	public OrbitElevationAction(MyGame game, float d) { this.game = game; dir = d; }
	@Override
	public void performAction(float time, Event e) {
		if (!game.gameStart) return;
		game.orbitController.orbitElevation(dir * 60.0f * time);
	}
}

class OrbitZoomAction extends AbstractInputAction {
	private MyGame game;
	private float dir;
	public OrbitZoomAction(MyGame game, float d) { this.game = game; dir = d; }
	@Override
	public void performAction(float time, Event e) {
		if (!game.gameStart) return;
		game.orbitController.zoom(dir * 4.0f * time);
	}
}

class OverheadPanXAction extends AbstractInputAction {
	private MyGame game;
	private float dir;
	public OverheadPanXAction(MyGame game, float d) { this.game = game; dir = d; }
	@Override
	public void performAction(float time, Event e) {
		if (!game.gameStart) return;
		game.overheadManualPan = true;
		game.overheadX += dir * game.overheadPanSpeed * time;
	}
}

//overhead cam
class OverheadPanZAction extends AbstractInputAction {
	private MyGame game;
	private float dir;
	public OverheadPanZAction(MyGame game, float d) { this.game = game; dir = d; }
	@Override
	public void performAction(float time, Event e) {
		if (!game.gameStart) return;
		game.overheadManualPan = true;
		game.overheadZ += dir * game.overheadPanSpeed * time;
	}
}

class OverheadZoomAction extends AbstractInputAction {
	private MyGame game;
	private float dir;
	public OverheadZoomAction(MyGame game, float d) { this.game = game; dir = d; }
	@Override
	public void performAction(float time, Event e) {
		if (!game.gameStart) return;
		game.overheadHeight += dir * game.overheadZoomSpeed * time;
		if (game.overheadHeight < 10.0f) game.overheadHeight = 10.0f;
		if (game.overheadHeight > 80.0f) game.overheadHeight = 80.0f;
	}
}

class OverheadRecenterAction extends AbstractInputAction {
	private MyGame game;
	public OverheadRecenterAction(MyGame game) { this.game = game; }
	@Override
	public void performAction(float time, Event e) {
		if (!game.gameStart) return;
		game.overheadManualPan = false;
	}
}

// Controller Movement Functions
class FwdAction extends AbstractInputAction {
	private MyGame game;
	public FwdAction(MyGame game) { this.game = game; }
	@Override
	public void performAction(float time, Event e) {
		if (!game.gameStart) return;
		float dist = game.moveSpeed * time;

		//Dolphin movement
		Vector3f pos = game.dol.getWorldLocation();
		Vector3f fwd = new Vector3f(game.dol.getWorldForwardVector()).normalize();
		game.dol.setLocalLocation(new Vector3f(pos).add(new Vector3f(fwd).mul(dist)));
	}
}

class BwdAction extends AbstractInputAction {
	private MyGame game;
	public BwdAction(MyGame game) { this.game = game; }
	@Override
	public void performAction(float time, Event e) {
		if (!game.gameStart) return;
		float dist = game.moveSpeed * time;

		//Dolphin movement
		Vector3f pos = game.dol.getWorldLocation();
		Vector3f fwd = new Vector3f(game.dol.getWorldForwardVector()).normalize();
		game.dol.setLocalLocation(new Vector3f(pos).add(new Vector3f(fwd).mul(-dist)));
	}
}

class KeyboardAction extends AbstractInputAction {
	private MyGame game;
	private String mode;
	private float dir;

	public KeyboardAction(MyGame game, String mode, float dir) {
		this.game = game;
		this.mode = mode;
		this.dir = dir;
	}

	@Override
	public void performAction(float time, Event e) {
		if (!game.gameStart) return;
		float amount = dir * game.camTurnSpeed;

		//dolphin
		if (mode.equals("yaw")) game.dol.globalYaw(amount);
	}
}

class GamepadMoveAction extends AbstractInputAction {
	private MyGame game;
	public GamepadMoveAction(MyGame game) { this.game = game; }
	@Override
	public void performAction(float time, Event e) {
		if (!game.gameStart) return;
		float val = e.getValue();
		if (Math.abs(val) < 0.15f) return;

		float dist = game.moveSpeed * time * (-val);

		//dolphin
		Vector3f pos = game.dol.getWorldLocation();
		Vector3f fwd = new Vector3f(game.dol.getWorldForwardVector()).normalize();
		game.dol.setLocalLocation(new Vector3f(pos).add(new Vector3f(fwd).mul(dist)));
	}
}

class GamepadYawAction extends AbstractInputAction {
	private MyGame game;
	public GamepadYawAction(MyGame game) { this.game = game; }
	@Override
	public void performAction(float time, Event e) {
		if (!game.gameStart) return;
		float val = e.getValue();
		if (Math.abs(val) < 0.15f) return;

		float amt = -val * game.camTurnSpeed;
		game.dol.globalYaw(amt);
	}
}

//game control functions
class StartGameControll extends AbstractInputAction {
	private MyGame game;
	public StartGameControll(MyGame game) { this.game = game; }
	@Override
	public void performAction(float time, Event e) {
		game.gameStart = true;
		System.out.println("Game has started!");
	}
}

//TAKE OUT THE HOP OFF ELEMENT, ONLY MAKE IT SO SPACE PUTS THE PICTURES UP ON WALL
class ToggleRideAction extends AbstractInputAction {
	private MyGame game;
	public ToggleRideAction(MyGame game) { this.game = game; }
	@Override
	public void performAction(float time, Event e) {
		if (!game.gameStart) return;
		if (game.gameWon && game.isHome()) {
			game.placePhotosOnWalls();
			return;
		}

		if (game.gameWon && !game.isHome()) {
			game.statusMsg = "Return home before trying to place your pictures!";
		}

		game.statusMsg = "Take all pyramid photos first, then return home!";
	}
}

class TakePictureAction extends AbstractInputAction {
	private MyGame game;
	public TakePictureAction(MyGame game) { this.game = game; }
	@Override
	public void performAction(float time, Event e) {
		if (!game.gameStart) return;
		game.tryTakePicture();
	}
}
