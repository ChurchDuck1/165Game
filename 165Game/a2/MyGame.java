package a2;

import tage.Engine;
import tage.GameObject;
import tage.VariableFrameRateGame;
import tage.Light;
import tage.TextureImage;
import tage.ObjShape;
import tage.Camera;
import tage.CameraOrbit3D;
import tage.input.InputManager;
import tage.input.action.AbstractInputAction;
import tage.nodeControllers.RotationController;
import tage.nodeControllers.ShakeSinkController;
import tage.Viewport;
import tage.shapes.*;

import java.lang.Math;
import java.util.ArrayList;
import org.joml.*;

import net.java.games.input.Event;


public class MyGame extends VariableFrameRateGame
{
	private static Engine engine;

	private long lastFrameTime, currFrameTime;
	private float elapsTime;

	//dolphin stuff
	private GameObject dol;
	private ObjShape dolS;
	private TextureImage doltx;

	//light stuff
	private Light light1, p1Light, p2Light, p3Light, homeLight;

	//pyramid stuff
	private ObjShape pyramidS;
	private GameObject pyramid1, pyramid2, pyramid3;
	private TextureImage pyramid1tx, pyramid2tx, pyramid3tx;

	private ShakeSinkController shakeSink1, shakeSink2, shakeSink3;

	//home stuff
	private GameObject home;
	private ObjShape homeS;
	private TextureImage homeTx;

	private GameObject homeMarkerTop;
	private ObjShape homeMarkerS;
	private TextureImage homeMarkerTx;
	private RotationController homeMarkerRotation;

	//ground stuff
	private ObjShape groundS;
	private GameObject ground;
	private TextureImage groundTx;

	//control stuff
	private float moveSpeed = 5.0f;

	//camera stuff
	private CameraOrbit3D orbitController;
	private float camTurnSpeed = 0.02f;

	private Camera overheadCam;
	private float overheadX = 0.0f;
	private float overheadZ = 0.0f;
	private float overheadHeight = 35.0f;

	private float overheadPanSpeed = 15.0f;
	private float overheadZoomSpeed = 20.0f;

	private boolean overheadInitialized = false;
	private boolean overheadManualPan = false;

	//axis stuff
	private GameObject xAxis, yAxis, zAxis;
	private boolean axesVisible = true;

	//photo and crash stuff
	private boolean gameOver = false;
	private boolean gameWon = false;
	private String statusMsg = "";
	
	private ObjShape photoS;
	
	private float crashRangeP = 4.0f;
	private float photoRange = 7.0f;

	private int score = 0;

	private ArrayList<GameObject> photosOnDolphin = new ArrayList<>();
	private boolean[] pyramidPic = new boolean[3];
	private boolean picsPlaced = false;

	private GameObject[] wallPics = new GameObject[3];


	public MyGame() { super(); }

	public static void main(String[] args)
	{	MyGame game = new MyGame();
		engine = new Engine(game);
		engine.initializeSystem();
		game.buildGame();
		game.startGame();
	}

	@Override
	public void loadShapes()
	{	dolS = new ImportedModel("dolphinHighPoly.obj");
		pyramidS = new ManualPyramid();
		photoS = new Plane();
		homeS = new Home();
		groundS = new Plane();
		homeMarkerS = new ManualPyramid();
	}

	@Override
	public void loadTextures()
	{	doltx = new TextureImage("Dolphin_HighPolyUV.jpg");
	
		//load pyramid stuff
		pyramid1tx = new TextureImage( "taterTots.jpg");
		pyramid2tx = new TextureImage("makemake.jpg");
		pyramid3tx = new TextureImage("eris.jpg");

		//load home stuff
		homeTx = new TextureImage("brick1.jpg");
		homeMarkerTx = new TextureImage("ice.jpg");

		//load ground stuff
		groundTx = new TextureImage("sand.jpg");
	}

	@Override
	public void buildObjects()
	{	Matrix4f initialTranslation, initialScale;

		// build dolphin in the center of the window
		dol = new GameObject(GameObject.root(), dolS, doltx);
		initialTranslation = (new Matrix4f()).translation(0f, 1.0f, 0f);
		initialScale = (new Matrix4f()).scaling(3.0f);
		dol.setLocalTranslation(initialTranslation);
		dol.setLocalScale(initialScale);

		//build ground
		ground = new GameObject(GameObject.root(), groundS, groundTx);
		initialTranslation = (new Matrix4f()).translation(0f, 0f, 0f);
		initialScale = (new Matrix4f()).scaling(40.0f, 1.0f, 40.0f);
		ground.setLocalTranslation(initialTranslation);
		ground.setLocalScale(initialScale);

		//build pyramids
		pyramid1 = new GameObject(GameObject.root(), pyramidS, pyramid1tx);
		initialTranslation = (new Matrix4f()).translation(-15f, 2.5f, -20f);
		initialScale = (new Matrix4f()).scaling(2.0f);
		pyramid1.setLocalTranslation(initialTranslation);
		pyramid1.setLocalScale(initialScale);

		pyramid2 = new GameObject(GameObject.root(), pyramidS, pyramid2tx);
		initialTranslation = (new Matrix4f()).translation(20f, 2.5f, 0f);
		initialScale = (new Matrix4f()).scaling(2.0f);
		pyramid2.setLocalTranslation(initialTranslation);
		pyramid2.setLocalScale(initialScale);

		pyramid3 = new GameObject(GameObject.root(), pyramidS, pyramid3tx);
		initialTranslation = (new Matrix4f()).translation(25f, 2.5f, 25f);
		initialScale = (new Matrix4f()).scaling(2.0f);
		pyramid3.setLocalTranslation(initialTranslation);
		pyramid3.setLocalScale(initialScale);

		//shake and sink controller for pyramids
		shakeSink1 = new ShakeSinkController();
		shakeSink2 = new ShakeSinkController();
		shakeSink3 = new ShakeSinkController();

		shakeSink1.addTarget(pyramid1);
		shakeSink2.addTarget(pyramid2);
		shakeSink3.addTarget(pyramid3);

		engine.getSceneGraph().addNodeController(shakeSink1);
		engine.getSceneGraph().addNodeController(shakeSink2);
		engine.getSceneGraph().addNodeController(shakeSink3);

		//world axes
		float axisLength = 100.0f;

		//red x-axis
		ObjShape xLine = new Line(new Vector3f(0f, 0f, 0f), new Vector3f(axisLength, 0f, 0f));
		xAxis = new GameObject(GameObject.root(), xLine);
		xAxis.getRenderStates().setColor(new Vector3f(1.0f, 0.0f, 0.0f));

		//green y-axis
		ObjShape yLine = new Line(new Vector3f(0f, 0f, 0f), new Vector3f(0f, axisLength, 0f));
		yAxis = new GameObject(GameObject.root(), yLine);
		yAxis.getRenderStates().setColor(new Vector3f(0.0f, 1.0f, 0.0f));

		//blue z-axis
		ObjShape zLine = new Line(new Vector3f(0f, 0f, 0f), new Vector3f(0f, 0f, axisLength));
		zAxis = new GameObject(GameObject.root(), zLine);
		zAxis.getRenderStates().setColor(new Vector3f(0.0f, 0.0f, 1.0f));

		//home build
		home = new GameObject(GameObject.root(), homeS, homeTx);
		home.setLocalTranslation(new Matrix4f().translation(0f, 3.1f, -10f)); 
		home.setLocalScale(new Matrix4f().scaling(0.5f));
		home.getRenderStates().setColor(new Vector3f(1f, 1f, 1f));

		//home marker build
		homeMarkerTop = new GameObject(home, homeMarkerS, homeMarkerTx);
		homeMarkerTop.setLocalTranslation(new Matrix4f().translation(0f, 5.0f, 0f));
		homeMarkerTop.setLocalScale(new Matrix4f().scaling(1.5f));
		homeMarkerTop.pitch((float)Math.toRadians(180.0f));
		homeMarkerTop.getRenderStates().setColor(new Vector3f(1f, 1f, 1f));

		//rotation controller for home marker
		homeMarkerRotation = new RotationController(engine, new Vector3f(0.0f, 1.0f, 0.0f), 0.001f);
		homeMarkerRotation.addTarget(homeMarkerTop);
		homeMarkerRotation.enable();
		engine.getSceneGraph().addNodeController(homeMarkerRotation);

	}

	@Override
	public void createViewports()
	{
		(engine.getRenderSystem()).addViewport("LEFT", 0f, 0f, 1f, 1f);
		(engine.getRenderSystem()).addViewport("RIGHT", 0.75f, 0f, 0.25f, 0.25f);

		Viewport leftVp = (engine.getRenderSystem()).getViewport("LEFT");
		Viewport rightVp = (engine.getRenderSystem()).getViewport("RIGHT");

		Camera leftCamera = leftVp.getCamera();
		Camera rightCamera = rightVp.getCamera();

		rightVp.setHasBorder(true);
		rightVp.setBorderWidth(4);
		rightVp.setBorderColor(0.0f, 1.0f, 0.0f);

		leftCamera.setLocation(new Vector3f(0f, 0f, 5f));
		leftCamera.setU(new Vector3f(1f, 0f, 0f));
		leftCamera.setV(new Vector3f(0f, 1f, 0f));
		leftCamera.setN(new Vector3f(0f, 0f, -1f));

		rightCamera.setLocation(new Vector3f(0f, 35f, 0f));
		rightCamera.setU(new Vector3f(1f, 0f, 0f));
		rightCamera.setV(new Vector3f(0f, 0f, -1f));
		rightCamera.setN(new Vector3f(0f, -1f, 0f));

		overheadCam = rightCamera;
	}

	@Override
	public void initializeLights()
	{	Light.setGlobalAmbient(0.5f, 0.5f, 0.5f);
		light1 = new Light();
		light1.setLocation(new Vector3f(5.0f, 4.0f, 2.0f));
		(engine.getSceneGraph()).addLight(light1);

		//pyramid lights
		p1Light = new Light();
		Vector3f p1Pos = pyramid1.getWorldLocation();
		p1Light.setLocation(new Vector3f(p1Pos).add(0f, 10f, 0f));

		p2Light = new Light();
		Vector3f p2Pos = pyramid2.getWorldLocation();
		p2Light.setLocation(new Vector3f(p2Pos).add(0f, 10f, 0f));

		p3Light = new Light();
		Vector3f p3Pos = pyramid3.getWorldLocation();
		p3Light.setLocation(new Vector3f(p3Pos).add(0f, 10f, 0f));

		homeLight = new Light();
		Vector3f hp = home.getWorldLocation();
		homeLight.setLocation(new Vector3f(hp).add(0f, 12f, 0f));
	}

	@Override
	public void initializeGame()
	{	lastFrameTime = System.currentTimeMillis();
		currFrameTime = System.currentTimeMillis();
		elapsTime = 0.0f;
		(engine.getRenderSystem()).setWindowDimensions(1900,1000);

		// ------------- positioning the camera -------------
		Camera c = engine.getRenderSystem().getViewport("LEFT").getCamera();
		orbitController = new CameraOrbit3D(c, dol);

		setupInputs();
	}

	@Override
	public void update()
	{	
		lastFrameTime = currFrameTime;
		currFrameTime = System.currentTimeMillis();
		elapsTime = (currFrameTime - lastFrameTime) / 1000.0f;

		//game over check
		if (gameOver && !gameWon) {
			updateMainHUD();
			updateOverheadHUD();
			return;
		}

		engine.getInputManager().update(elapsTime);

		//bind dolphin to ground plane
		Vector3f dolLoc = dol.getLocalLocation();
		dolLoc.y = 1.0f;
		dol.setLocalLocation(dolLoc);

		if (gameWon && !picsPlaced) {
			statusMsg = "Press SPACE to hang your photos!";
		}
		
		Vector3f dolPos = dol.getWorldLocation();
		orbitController.updateCameraPosition();

		if (!overheadInitialized) {
			overheadX = dolPos.x;
			overheadZ = dolPos.z;
			overheadInitialized = true;
		}

		//checking if we're panning for auto center
		if (!overheadManualPan) {
			overheadX = dolPos.x;
			overheadZ = dolPos.z;
		}

		updateOverheadCamera();

		//collision stuff
		Vector3f p1Pos = pyramid1.getWorldLocation();
		Vector3f p2Pos = pyramid2.getWorldLocation();
		Vector3f p3Pos = pyramid3.getWorldLocation();

		float d1 = dolPos.distance(p1Pos);
		float d2 = dolPos.distance(p2Pos);
		float d3 = dolPos.distance(p3Pos);

		float minDist = Math.min(d1, Math.min(d2, d3));

		if (!gameWon && !picsPlaced) {
			if (minDist < crashRangeP) {
				statusMsg = "You've crashed! GAME OVER!";
				gameOver = true;
			} else if (minDist < photoRange) {
				statusMsg = "Press P to take picture";
			} else {
				statusMsg = "Not close enough, move towards a pyramid";
			}
		} else if (gameWon && !picsPlaced) {
			if (isHome()) statusMsg = "Press SPACE to hang up your photos!";
			else statusMsg = "Return home and press SPACE to put your pictures on the wall!";
		} else if (picsPlaced) {
			statusMsg = "The Pictures are all hung up! You Won!!";
		}


		//update huds
		updateMainHUD();
		updateOverheadHUD();
	}

	//update the main hud
	private void updateMainHUD()
	{
		Viewport mainVp = engine.getRenderSystem().getViewport("LEFT");

		int hudX = (int) mainVp.getActualLeft() + 15;
		int hudY = (int) (mainVp.getActualBottom() - mainVp.getActualHeight());

		Vector3f statusColor = new Vector3f(1,1,0); // default is yellow

		if (gameOver && !gameWon)
			statusColor = new Vector3f(1,0,0); // red if lose
		else if (gameOver && gameWon)
			statusColor = new Vector3f(0,1,0); // green if win

		String text = "Score: " + score + "      " + statusMsg;

		(engine.getHUDmanager()).setHUD1(
			text,
			statusColor,
			hudX,
			hudY
		);
	}

	//update overhead hud
	private void updateOverheadHUD()
	{
		Viewport topVp = engine.getRenderSystem().getViewport("RIGHT");
		Vector3f pos = dol.getWorldLocation();

		String posText = String.format(
			"X: %.1f  Y: %.1f  Z: %.1f",
			pos.x, pos.y, pos.z
		);

		int hudX = (int) topVp.getActualLeft() + 10;
		int hudY = (int) (topVp.getActualBottom() - topVp.getActualHeight());

		(engine.getHUDmanager()).setHUD2(
			posText,
			new Vector3f(0,1,0),
			hudX,
			hudY
		);
	}

	private void updateOverheadCamera()
	{
		Vector3f target = new Vector3f(overheadX, 0.0f, overheadZ);
		Vector3f camPos = new Vector3f(overheadX, overheadHeight, overheadZ);

		overheadCam.setLocation(camPos);
		overheadCam.lookAt(target);
	}

	private class ToggleAxesAction extends AbstractInputAction {
		@Override
		public void performAction(float time, Event e) {
			axesVisible = !axesVisible;

			if (axesVisible) {
				xAxis.getRenderStates().enableRendering();
				yAxis.getRenderStates().enableRendering();
				zAxis.getRenderStates().enableRendering();
			}
			else {
				xAxis.getRenderStates().disableRendering();
				yAxis.getRenderStates().disableRendering();
				zAxis.getRenderStates().disableRendering();
			}
		}
	}

	private void tryTakePicture() {

		GameObject[] pyramids = { pyramid1, pyramid2, pyramid3 };
		TextureImage[] texs = { pyramid1tx, pyramid2tx, pyramid3tx};
		Vector3f dolPos = dol.getWorldLocation();

		int bestIndex = -1;
		float bestDist = Float.MAX_VALUE;

		for (int i = 0; i < 3; i++) {
			if (pyramidPic[i]) continue;
			
			float d = dolPos.distance(pyramids[i].getWorldLocation());
			
			if (d < bestDist) {
				bestDist = d;
				bestIndex = i;
			}
		}

		if (bestIndex == -1) {
			statusMsg = "All pictures already taken!";
			return;
		}

		if (bestDist > photoRange) {
			statusMsg = "Not close enough to take a picture.";
			return;
		}

		GameObject photo = new GameObject(dol, photoS, texs[bestIndex]);
		photo.applyParentRotationToPosition(true);

		int idx = photosOnDolphin.size();

		float stripForward = -1.0f;
		float stripUp = 0.8f; 
		float spacing = 0.35f;                

		float x = (idx - 1) * spacing;            
		float y = stripUp;
		float z = stripForward;

		photo.setLocalLocation(new Vector3f(x, y, z));
		photo.setLocalScale(new Matrix4f().scaling(0.05f, 0.05f, 0.05f));


		photosOnDolphin.add(photo);
		pyramidPic[bestIndex] = true;
		if (bestIndex == 0) shakeSink1.enable();
		if (bestIndex == 1) shakeSink2.enable();
		if (bestIndex == 2) shakeSink3.enable();
		score++;

		statusMsg = "Picture taken! Score: " + score;

		if (score >= 3) {
			statusMsg = "All pictures taken! Return home and press SPACE to put pictures on the wall!";
			gameWon = true;
		}
	}


	private void placePhotosOnWalls() {
		if (picsPlaced) {
			statusMsg = "Photos already placed!";
			return;
		}

		picsPlaced = true;

		Vector3f[] spots = new Vector3f[] {
			new Vector3f(-1.2f, 0.5f, -3.0f),  
			new Vector3f( 0.0f, 0.5f, -3.0f),  
			new Vector3f( 1.2f, 0.5f, -3.0f)   
		};

		TextureImage[] texs = new TextureImage[] { pyramid1tx, pyramid2tx, pyramid3tx };

		for (int i = 0; i < 3; i++) {
			wallPics[i] = new GameObject(home, photoS, texs[i]);
			wallPics[i].setLocalLocation(spots[i]);
			wallPics[i].setLocalScale(new Matrix4f().scaling(1.0f, 1.0f, 1.0f));

			wallPics[i].pitch((float)Math.toRadians(90.0f));
		}

		for (GameObject pic : photosOnDolphin) {
			pic.getRenderStates().disableRendering();
		}

		statusMsg = "You placed the photos on the wall! You win!";
		gameWon = true;
		gameOver = true; 
	}

	private boolean isHome() {
		Vector3f dolPos = dol.getWorldLocation();
		Vector3f homePos = home.getWorldLocation();
		return dolPos.distance(homePos) < 5.0f;
	}

	private class OrbitAzimuthAction extends AbstractInputAction {
		private float dir;

		public OrbitAzimuthAction(float d) { dir = d; }

		@Override
		public void performAction(float time, Event e) {
			orbitController.orbitAzimuth(dir * 90.0f * time);
		}
	}

	private class OrbitElevationAction extends AbstractInputAction {
		private float dir;

		public OrbitElevationAction(float d) { dir = d; }

		@Override
		public void performAction(float time, Event e) {
			orbitController.orbitElevation(dir * 60.0f * time);
		}
	}

	private class OrbitZoomAction extends AbstractInputAction {
		private float dir;

		public OrbitZoomAction(float d) { dir = d; }

		@Override
		public void performAction(float time, Event e) {
			orbitController.zoom(dir * 4.0f * time);
		}
	}

	private class OverheadPanXAction extends AbstractInputAction {
	private float dir;

	public OverheadPanXAction(float d) { dir = d; }

		@Override
		public void performAction(float time, Event e) {
			overheadManualPan = true;
			overheadX += dir * overheadPanSpeed * time;
		}
	}

	//overhad cam
	private class OverheadPanZAction extends AbstractInputAction {
		private float dir;

		public OverheadPanZAction(float d) { dir = d; }

		@Override
		public void performAction(float time, Event e) {
			overheadManualPan = true;
			overheadZ += dir * overheadPanSpeed * time;
		}
	}

	private class OverheadZoomAction extends AbstractInputAction {
		private float dir;

		public OverheadZoomAction(float d) { dir = d; }

		@Override
		public void performAction(float time, Event e) {
			overheadHeight += dir * overheadZoomSpeed * time;

			if (overheadHeight < 10.0f) overheadHeight = 10.0f;
			if (overheadHeight > 80.0f) overheadHeight = 80.0f;
		}
	}

	private class OverheadRecenterAction extends AbstractInputAction {
		@Override
		public void performAction(float time, Event e) {
			overheadManualPan = false;
		}
	}

	// Controller Movement Functions
	private class FwdAction extends AbstractInputAction {
		@Override
		public void performAction(float time, Event e) {
			float dist = moveSpeed * time;
	
			//Dolphin movement
			Vector3f pos = dol.getWorldLocation();
			Vector3f fwd = new Vector3f(dol.getWorldForwardVector()).normalize();
			dol.setLocalLocation(new Vector3f(pos).add(new Vector3f(fwd).mul(dist)));
		}
	}

	private class BwdAction extends AbstractInputAction {
		@Override
		public void performAction(float time, Event e) {
			float dist = moveSpeed * time;
	
			//Dolphin movement
			Vector3f pos = dol.getWorldLocation();
			Vector3f fwd = new Vector3f(dol.getWorldForwardVector()).normalize();
			dol.setLocalLocation(new Vector3f(pos).add(new Vector3f(fwd).mul(-dist)));
		}
	}

	private class KeyboardAction extends AbstractInputAction {
		private String mode;
		private float dir;
	
		public KeyboardAction(String mode, float dir) {
			this.mode = mode;
			this.dir = dir;
		}
	
		@Override
		public void performAction(float time, Event e) {
			float amount = dir * camTurnSpeed;
	
			//dolphin
			if (mode.equals("yaw")) dol.globalYaw(amount);
		}
	}

	private class GamepadMoveAction extends AbstractInputAction {
		@Override
		public void performAction(float time, Event e) {
			float val = e.getValue();
			if (Math.abs(val) < 0.15f) return;
	
			float dist = moveSpeed * time * (-val);
	
			//dolphin
			Vector3f pos = dol.getWorldLocation();
			Vector3f fwd = new Vector3f(dol.getWorldForwardVector()).normalize();
			dol.setLocalLocation(new Vector3f(pos).add(new Vector3f(fwd).mul(dist)));
		}
	}

	private class GamepadYawAction extends AbstractInputAction {
		@Override
		public void performAction(float time, Event e) {
			float val = e.getValue();
			if (Math.abs(val) < 0.15f) return;
	
			float amt = -val * camTurnSpeed;
			dol.globalYaw(amt);
		}
	}

	//TAKE OUT THE HOP OFF ELEMENT, ONLY MAKE IT SO SPACE PUTS THE PICTURES UP ON WALL
	private class ToggleRideAction extends AbstractInputAction {
		@Override
		public void performAction(float time, Event e) {

			if (gameWon && isHome()) {
				placePhotosOnWalls();
				return;
        	}

			if (gameWon && !isHome()) {
				statusMsg = "Return home before trying to place your pictures!";
			}

			statusMsg = "Take all pyramid photos first, then return home!";
		}
	}

	private class TakePictureAction extends AbstractInputAction {
		@Override
		public void performAction(float time, Event e) {
			tryTakePicture();
		}
	}


	private void setupInputs() {
		InputManager im = engine.getInputManager();

		//camera controls
		OrbitAzimuthAction leftAction = new OrbitAzimuthAction(-1.0f);
		OrbitAzimuthAction rightAction = new OrbitAzimuthAction(1.0f);
		OrbitElevationAction upAction = new OrbitElevationAction(1.0f);
		OrbitElevationAction downAction = new OrbitElevationAction(-1.0f);
		OrbitZoomAction zoomInAction = new OrbitZoomAction(-1.0f);
		OrbitZoomAction zoomOutAction = new OrbitZoomAction(1.0f);

		// ----- Keyboard toggle for axis visibility -----
		im.associateActionWithAllKeyboards(
			net.java.games.input.Component.Identifier.Key.X,
			new ToggleAxesAction(),
			InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);

		// ----- Keyboard orbit cam control -----
		im.associateActionWithAllKeyboards(
			net.java.games.input.Component.Identifier.Key.LEFT,
			leftAction,
			InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		im.associateActionWithAllKeyboards(
			net.java.games.input.Component.Identifier.Key.RIGHT,
			rightAction,
			InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		im.associateActionWithAllKeyboards(
			net.java.games.input.Component.Identifier.Key.UP,
			upAction,
			InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		im.associateActionWithAllKeyboards(
			net.java.games.input.Component.Identifier.Key.DOWN,
			downAction,
			InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		im.associateActionWithAllKeyboards(
			net.java.games.input.Component.Identifier.Key.Q,
			zoomInAction,
			InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		im.associateActionWithAllKeyboards(
			net.java.games.input.Component.Identifier.Key.E,
			zoomOutAction,
			InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		// ----- Keyboard overhead cam control -----
		im.associateActionWithAllKeyboards(
			net.java.games.input.Component.Identifier.Key.J,
			new OverheadPanXAction(-1f),
			InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		im.associateActionWithAllKeyboards(
			net.java.games.input.Component.Identifier.Key.L,
			new OverheadPanXAction(1f),
			InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		im.associateActionWithAllKeyboards(
			net.java.games.input.Component.Identifier.Key.I,
			new OverheadPanZAction(-1f),
			InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		im.associateActionWithAllKeyboards(
			net.java.games.input.Component.Identifier.Key.K,
			new OverheadPanZAction(1f),
			InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		im.associateActionWithAllKeyboards(
			net.java.games.input.Component.Identifier.Key.U,
			new OverheadZoomAction(-1f),
			InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		im.associateActionWithAllKeyboards(
			net.java.games.input.Component.Identifier.Key.O,
			new OverheadZoomAction(1f),
			InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		im.associateActionWithAllKeyboards(
			net.java.games.input.Component.Identifier.Key.H,
			new OverheadRecenterAction(),
			InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);

		// ----- Keyboard Dolphin Movement -----
		im.associateActionWithAllKeyboards(
			net.java.games.input.Component.Identifier.Key.W,
			new FwdAction(), 
			InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		im.associateActionWithAllKeyboards(
			net.java.games.input.Component.Identifier.Key.S,
			new BwdAction(), 
			InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		im.associateActionWithAllKeyboards(
			net.java.games.input.Component.Identifier.Key.A,
			new KeyboardAction("yaw", 1f),
			InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		im.associateActionWithAllKeyboards(
			net.java.games.input.Component.Identifier.Key.D,
			new KeyboardAction("yaw", -1f),
			InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		im.associateActionWithAllKeyboards(
			net.java.games.input.Component.Identifier.Key.SPACE,
			new ToggleRideAction(),
			InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);

		im.associateActionWithAllKeyboards(
			net.java.games.input.Component.Identifier.Key.P,
			new TakePictureAction(),
			InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);

		// ----- Gamepad Dolphin Movement -----
		im.associateActionWithAllGamepads(
			net.java.games.input.Component.Identifier.Axis.RY,
			new GamepadMoveAction(),
			InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		im.associateActionWithAllGamepads(
			net.java.games.input.Component.Identifier.Axis.RX,
			new GamepadYawAction(),
			InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
	}
}