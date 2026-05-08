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
import tage.Viewport;
import tage.shapes.*;
import tage.networking.IGameConnection.ProtocolType;

import java.lang.Math;
import java.net.InetAddress;
import java.net.UnknownHostException;
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
	private ObjShape witchS;
	private int selectedAvatar = 0; //0 = dolphin, 1 = witch
	private float avatarGroundOffset = 1.0f;
	private float avatarCameraOffset = 0.0f;

	//networking stuff
	private GhostManager gm;
	private ObjShape ghostS;
	private TextureImage ghostTx;
	private ProtocolClient protClient;
	private boolean isConnected = false;
	private boolean twoPlayer = false;
	private String serverAddress = "localhost";
	private int serverPort = 5000;
	private ProtocolType serverProtocol = ProtocolType.UDP;

	//light stuff
	private Light light1, homeLight;

	//home stuff - logical position only, no rendered object
	private Vector3f homePosition = new Vector3f(0f, 0f, -10f);

	//ground stuff
	private ObjShape groundS;
	private GameObject ground;
	private TextureImage groundTx;
	private TextureImage groundHeightMap;

	//house stuff
	private ObjShape houseS;
	private GameObject[] houses = new GameObject[5];
	private TextureImage houseTx;

	//broom object
	private ObjShape broomS;	
	private GameObject broom;

	//box stuff
	private ObjShape boxS;
	private ArrayList<GameObject> spawnedBoxes = new ArrayList<GameObject>();

	//control stuff
	private float moveSpeed = 15.0f;

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
	private float[][] housePositions = {
		{  60f,  70f},
		{ -80f,  40f},
		{ 100f, -60f},
		{ -40f, -100f},
		{  20f,  120f}
	};

	//game stuff
	private boolean gameOver = false;
	private String statusMsg = "";
	private boolean gameStart = false;

	//skybox stuff
	private int skyboxTex;

	public MyGame(String serverAddr, int sPort, String protocol)
	{
		super();
		gm = new GhostManager(this);
		this.serverAddress = serverAddr;
		this.serverPort = sPort;
		if (protocol.toUpperCase().compareTo("TCP") == 0)
			this.serverProtocol = ProtocolType.TCP;
		else
			this.serverProtocol = ProtocolType.UDP;
	}

	public MyGame() 
	{ 
		super();
		gm = new GhostManager(this);
	}

	public static void main(String[] args) {	
		MyGame game;
		if (args.length >= 3) {
			//networking parameters provided: serverAddress, serverPort, protocol
			game = new MyGame(args[0], Integer.parseInt(args[1]), args[2]);
		}
		else {
			//default constructor (no networking)
			game = new MyGame();
		}

		engine = new Engine(game);
		engine.initializeSystem();
		game.buildGame();
		game.startGame();
	}

	@Override
	public void loadShapes()
	{	dolS = new ImportedModel("dolphinHighPoly.obj");
		witchS = new ImportedModel("witchModel.obj");
		groundS = new TerrainPlane(1000);
		houseS = new ImportedModel("house.obj");
		broomS = new ImportedModel("broomModel.obj");
		ghostS = new ImportedModel("dolphinHighPoly.obj");  //TODO: change when we have multiple player models
		boxS = new ImportedModel("box.obj");
	}

	@Override
	public void loadTextures()
	{	doltx = new TextureImage("Dolphin_HighPolyUV.jpg");
	
		//load ground stuff
		groundTx = new TextureImage("sand.jpg");
		groundHeightMap = new TextureImage("hills.jpg");

		//load house stuff
		houseTx = new TextureImage("brick1.jpg");

		//load ghost stuff
		ghostTx = new TextureImage("Dolphin_HighPolyUV.jpg");  //TODO: change when we have multiple player modelsS
	}

	@Override
	public void loadSkyBoxes()
	{
		skyboxTex = (engine.getSceneGraph()).loadCubeMap("fluffyClouds");

		(engine.getSceneGraph()).setActiveSkyBoxTexture(skyboxTex);
		(engine.getSceneGraph()).setSkyBoxEnabled(true);
	}

	@Override
	public void buildObjects()
	{	Matrix4f initialTranslation, initialScale;

		// build dolphin in the center of the window
		ObjShape startShape = (selectedAvatar == 1) ? witchS : dolS;
		TextureImage startTexture = (selectedAvatar == 1) ? null : doltx;
		dol = new GameObject(GameObject.root(), startShape, startTexture);
		initialTranslation = (new Matrix4f()).translation(0f, 1.0f, 0f);
		dol.setLocalTranslation(initialTranslation);
		applyAvatarTransform();

		//build ground - single large terrain tile
		ground = new GameObject(GameObject.root(), groundS, groundTx);
		ground.setLocalTranslation(new Matrix4f().translation(0f, -16f, 0f));
		ground.setLocalScale(new Matrix4f().scaling(160.0f, 6.0f, 160.0f));
		ground.setIsTerrain(true);
		ground.setHeightMap(groundHeightMap);

		//build 5 houses scattered across the map
		for (int i = 0; i < 5; i++) {
			houses[i] = new GameObject(GameObject.root(), houseS, houseTx);
			houses[i].setLocalTranslation(new Matrix4f().translation(housePositions[i][0], -16, housePositions[i][1]));
			houses[i].setLocalScale(new Matrix4f().scaling(0.02f));
		}

		//broom object in the world
		broom = new GameObject(GameObject.root(), broomS, null);
		broom.setLocalTranslation(new Matrix4f().translation(-8f, 1.0f, 10f));
		broom.setLocalScale(new Matrix4f().scaling(0.5f));

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

		homeLight = new Light();
		homeLight.setLocation(new Vector3f(homePosition).add(0f, 12f, 0f));
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
		setupNetworking();
	}

	@Override
	public void update()
	{	
		lastFrameTime = currFrameTime;
		currFrameTime = System.currentTimeMillis();
		elapsTime = (currFrameTime - lastFrameTime) / 1000.0f;

		//game over check
		if (gameOver) {
			updateMainHUD();
			updateOverheadHUD();
			return;
		}

		engine.getInputManager().update(elapsTime);

		//process networking (receive packets from server/clients)
		processNetworking(elapsTime);

		//bind dolphin to ground plane
		Vector3f dolLoc = dol.getLocalLocation();
		float terrainHeight = ground.getHeight(dolLoc.x, dolLoc.z);
		dol.setLocalLocation(new Vector3f(dolLoc.x, terrainHeight + avatarGroundOffset, dolLoc.z));

		Vector3f dolPos = dol.getWorldLocation();
		orbitController.updateCameraPosition();

		// Adjust camera height to compensate for avatar offset
		Camera c = engine.getRenderSystem().getViewport("LEFT").getCamera();
		Vector3f camLoc = c.getLocation();
		c.setLocation(new Vector3f(camLoc.x, camLoc.y + avatarCameraOffset, camLoc.z));

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

		statusMsg = "Explore the world!";

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
		(engine.getHUDmanager()).setHUD1(statusMsg, new Vector3f(1,1,0), hudX, hudY);
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

	private void applySelectedAvatar()
	{
		if (dol == null) return;
		if (selectedAvatar == 1) {
			dol.setShape(witchS);
			dol.setTextureImage(null);
			statusMsg = "Witch selected";
		}
		else {
			dol.setShape(dolS);
			dol.setTextureImage(doltx);
			statusMsg = "Dolphin selected";
		}
		applyAvatarTransform();
	}

	private void applyAvatarTransform()
	{
		if (dol == null) return;
		
		if (selectedAvatar == 1) {
			// Witch: 0.4f scale, translate down by 2.5
			dol.setLocalScale(new Matrix4f().scaling(0.4f));
		}
		else {
			// Dolphin: normal 3.0f scale
			dol.setLocalScale(new Matrix4f().scaling(3.0f));
			dol.setLocalRotation(new Matrix4f());
		}
	}

	private class OrbitAzimuthAction extends AbstractInputAction {
		private float dir;

		public OrbitAzimuthAction(float d) { dir = d; }

		@Override
		public void performAction(float time, Event e) {
			if (!gameStart) return;
			orbitController.orbitAzimuth(dir * 90.0f * time);
		}
	}

	private class OrbitElevationAction extends AbstractInputAction {
		private float dir;

		public OrbitElevationAction(float d) { dir = d; }

		@Override
		public void performAction(float time, Event e) {
			if (!gameStart) return;
			orbitController.orbitElevation(dir * 60.0f * time);
		}
	}

	private class OrbitZoomAction extends AbstractInputAction {
		private float dir;

		public OrbitZoomAction(float d) { dir = d; }

		@Override
		public void performAction(float time, Event e) {
			if (!gameStart) return;
			orbitController.zoom(dir * 4.0f * time);
		}
	}

	private class OverheadPanXAction extends AbstractInputAction {
	private float dir;

	public OverheadPanXAction(float d) { dir = d; }

		@Override
		public void performAction(float time, Event e) {
			if (!gameStart) return;
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
			if (!gameStart) return;
			overheadManualPan = true;
			overheadZ += dir * overheadPanSpeed * time;
		}
	}

	private class OverheadZoomAction extends AbstractInputAction {
		private float dir;

		public OverheadZoomAction(float d) { dir = d; }

		@Override
		public void performAction(float time, Event e) {
			if (!gameStart) return;
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

	private class SelectAvatarAction extends AbstractInputAction {
		private int avatarIndex;

		public SelectAvatarAction(int index) {
			avatarIndex = index;
		}

		@Override
		public void performAction(float time, Event e) {
			if (gameStart) return;
			selectedAvatar = avatarIndex;
			applySelectedAvatar();
			if (isConnected && protClient != null) {
				protClient.sendCreateMessage(getPlayerPosition());
			}
		}
	}

	// Controller Movement Functions
	private class FwdAction extends AbstractInputAction {
		@Override
		public void performAction(float time, Event e) {
			if (!gameStart) return;
			float dist = moveSpeed * time;
	
			//Dolphin movement
			Vector3f pos = dol.getWorldLocation();
			Vector3f fwd = new Vector3f(dol.getWorldForwardVector()).normalize();
			dol.setLocalLocation(new Vector3f(pos).add(new Vector3f(fwd).mul(dist)));
			
			//send move message to other players
			if (protClient != null && isConnected) {
				protClient.sendMoveMessage(dol.getWorldLocation());
			}
		}
	}

	private class BwdAction extends AbstractInputAction {
		@Override
		public void performAction(float time, Event e) {
			if (!gameStart) return;
			float dist = moveSpeed * time;
	
			//Dolphin movement
			Vector3f pos = dol.getWorldLocation();
			Vector3f fwd = new Vector3f(dol.getWorldForwardVector()).normalize();
			dol.setLocalLocation(new Vector3f(pos).add(new Vector3f(fwd).mul(-dist)));
			
			//send move message to other players
			if (protClient != null && isConnected) {
				protClient.sendMoveMessage(dol.getWorldLocation());
			}
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
			if (!gameStart) return;
			float amount = dir * camTurnSpeed;
	
			//dolphin
			if (mode.equals("yaw")) dol.globalYaw(amount);
		}
	}

	private class GamepadMoveAction extends AbstractInputAction {
		@Override
		public void performAction(float time, Event e) {
			if (!gameStart) return;
			float val = e.getValue();
			if (Math.abs(val) < 0.15f) return;
	
			float dist = moveSpeed * time * (-val);
	
			//dolphin
			Vector3f pos = dol.getWorldLocation();
			Vector3f fwd = new Vector3f(dol.getWorldForwardVector()).normalize();
			dol.setLocalLocation(new Vector3f(pos).add(new Vector3f(fwd).mul(dist)));
			
			//send move message to other players
			if (protClient != null && isConnected) {
				protClient.sendMoveMessage(dol.getWorldLocation());
			}
		}
	}

	private class GamepadYawAction extends AbstractInputAction {
		@Override
		public void performAction(float time, Event e) {
			if (!gameStart) return;
			float val = e.getValue();
			if (Math.abs(val) < 0.15f) return;
	
			float amt = -val * camTurnSpeed;
			dol.globalYaw(amt);
		}
	}

	//game control functions
	private class StartGameControll extends AbstractInputAction {
		@Override
		public void performAction(float time, Event e) {
			gameStart = true;
			System.out.println("Game has started!");
			
			//check if a second player is connected
			if (isConnected && gm.getGhostCount() > 0) {
				twoPlayer = true;
				System.out.println("Two-player game started!");
			}
			else {
				twoPlayer = false;
				System.out.println("Single-player game started!");
			}
		}
	}

	private class SpawnBoxAction extends AbstractInputAction {
		@Override
		public void performAction(float time, Event e) {
			if (!gameStart) return;
			// Compute spawn position 3.5 units directly in front of the player
			Vector3f pos = dol.getWorldLocation();
			Vector3f fwd = new Vector3f(dol.getWorldForwardVector()).normalize();
			Vector3f spawnPos = new Vector3f(pos).add(new Vector3f(fwd).mul(3.5f));

			GameObject box = new GameObject(GameObject.root(), boxS, null);
			box.setLocalLocation(spawnPos);
			box.setLocalScale(new Matrix4f().scaling(0.5f));
			spawnedBoxes.add(box);
		}
	}

	private void setupNetworking()
	{
		isConnected = false;
		try {
			protClient = new ProtocolClient(InetAddress.getByName(serverAddress), 
											 serverPort, serverProtocol, this);
		}
		catch (UnknownHostException e) {
			System.out.println("Unknown host: " + serverAddress);
			e.printStackTrace();
		}
		catch (java.io.IOException e) {
			System.out.println("IOException setting up networking");
			e.printStackTrace();
		}

		if (protClient == null) {
			System.out.println("ERROR: missing protocol host");
		}
		else {
			protClient.sendJoinMessage();
		}
	}

	private void processNetworking(float elapsedTime) {
		//process packets received by the client from the server
		if (protClient != null)
		{
			protClient.processPackets();
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

		// ----- Keyboard Game Control -----
		im.associateActionWithAllKeyboards(
			net.java.games.input.Component.Identifier.Key.RETURN,
			new StartGameControll(),
			InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);

		im.associateActionWithAllKeyboards(
			net.java.games.input.Component.Identifier.Key._1,
			new SelectAvatarAction(0),
			InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);

		im.associateActionWithAllKeyboards(
			net.java.games.input.Component.Identifier.Key._2,
			new SelectAvatarAction(1),
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

		im.associateActionWithAllKeyboards(
			net.java.games.input.Component.Identifier.Key.SPACE,
			new SpawnBoxAction(),
			InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);

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

	//networking accessors blob
	public void setIsConnected(boolean connected) { this.isConnected = connected; }
	public boolean getIsConnected() { return isConnected; }
	public ObjShape getGhostShape() { return ghostS; }
	public TextureImage getGhostTexture() { return ghostTx; }
	public ObjShape getDolphinShape() { return dolS; }
	public TextureImage getDolphinTexture() { return doltx; }
	public ObjShape getWitchShape() { return witchS; }
	public TextureImage getWitchTexture() { return null; }
	public GhostManager getGhostManager() { return gm; }
	public Engine getEngine() { return engine; }
	public Vector3f getPlayerPosition() { return dol.getWorldLocation(); }
	public int getSelectedAvatar() { return selectedAvatar; }
	public boolean isTwoPlayer() { return twoPlayer; }
	public int getGhostCount() { return gm.getGhostCount(); }

	//send bye message when game exits to notify other players
	@Override
	public void shutdown() {
		super.shutdown();
		if (protClient != null && isConnected) {
			protClient.sendByeMessage();
		}
	}
}