package packageDelivery;

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
import java.util.HashSet;
import org.joml.*;
import tage.physics.PhysicsEngine;
import tage.physics.PhysicsObject;
import tage.nodeControllers.*;
import tage.NodeController;
import net.java.games.input.Event;
import tage.audio.*;



public class MyGame extends VariableFrameRateGame
{
	private static Engine engine;

	private long lastFrameTime, currFrameTime;
	private float elapsTime;

	//avatar stuff
	private GameObject dol;
	private AnimatedShape witchS;
	private TextureImage witchTexA;
	private TextureImage witchTexB;
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
	private Light light1;
	private Light[] houseLights = new Light[12];

	//ground stuff
	private ObjShape groundS;
	private GameObject ground;
	private TextureImage groundTx;
	private TextureImage groundHeightMap;

	//house stuff
	private ObjShape houseS;
	private static final int HOUSE_COUNT = 12; //4x4 grid with 4 middle slots removed
	private GameObject[] houses = new GameObject[HOUSE_COUNT];
	private TextureImage houseTx, warehouseTx;
	//grid settings
	private static final float GRID_SPACING = 70f; //distance between houses
	private static final float GRID_OFFSET  = -105f; //position of first column/row
	//indices in the 4x4 grid that are skipped (the 2x2 centre: rows 1-2, cols 1-2)
	private static final java.util.Set<Integer> SKIP_GRID = new java.util.HashSet<>(
		java.util.Arrays.asList(5, 6, 9, 10));

	//warehouse object
	private ObjShape warehouseS;
	private GameObject warehouse;

	//boxpile object
	private ObjShape boxpileS;
	private GameObject boxpile;
	private TextureImage boxpileTx;
	private ObjShape arrowS;
	private GameObject arrow;
	private TextureImage arrowTx;
	private NodeController rc;

	//broom object
	private ObjShape broomS;
	private GameObject broom;
	private TextureImage broomTx;

	//mountain object
	private ObjShape mountainS;
	private GameObject mountain;
	private TextureImage mountainTx;

	//box stuff
	private ObjShape boxS;
	private ArrayList<GameObject> spawnedBoxes = new ArrayList<GameObject>();
	private ArrayList<PhysicsObject> spawnedBoxPhysics = new ArrayList<PhysicsObject>();
	private TextureImage boxTx;

	//physics stuff
	private PhysicsEngine physicsEngine;
	private PhysicsObject[] housePhysics = new PhysicsObject[HOUSE_COUNT];
	private PhysicsObject groundPhysics;
	private PhysicsObject boxpilePhysics;

	//score stuff
	private int score = 0;
	private int boxesOnHand = 0; // boxes player is currently carrying; refills to 3 at boxpile

	//active house stuff
	private int activeHouse = -1; // index into houses[]; -1 = none (before game start)
	private java.util.Random rng = new java.util.Random();

	//control stuff
	private float moveSpeed = 15.0f;
	private boolean witchWalking = false;
	private boolean witchMovedThisFrame = false;
	private static final float ARROW_SPIN_SPEED = 0.002f; // radians per second — increase to spin faster
		
	//ground/air mode stuff
	private boolean isOnGround    = true;  //true = walking on terrain at y=-16, false = flying at y=0
	private boolean isTransitioning = false; //true while player is mid-sink or mid-rise
	private static final float SURFACE_Y      = 0.0f;
	private static final float GROUND_Y       = -16.0f;
	private static final float SINK_RISE_SPEED = 8.0f;

	//world border limits (for movement)
	private static final float BORDER_X_MAX =  150f;  //east wall
	private static final float BORDER_X_MIN = -150f;  //west wall
	private static final float BORDER_Z_MAX =  150f;  //south wall
	private static final float BORDER_Z_MIN = -150f;  //north wall

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
	

	//game stuff
	private boolean gameOver = false;
	private String statusMsg = "";
	private boolean gameStart = false;

	//skybox stuff
	private int skyboxTex;

	//chopper NPC stuff
	private ObjShape chopperS;
	private TextureImage chopperTx; 
	private GameObject chopper;

	private static final float CHOPPER_SPEED = 6.0f; 

	//audio stuff
	private IAudioManager audioMgr;
	private Sound copterSound; //looping engine sound attached to chopper
	private Sound pointSound; //one-time sound played on scoring a point

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
			game = new MyGame(args[0], Integer.parseInt(args[1]), args[2]);
		}
		else {
			game = new MyGame();
		}

		engine = new Engine(game);
		engine.initializeSystem();
		game.buildGame();
		game.startGame();
	}

	@Override
	public void loadShapes()
	{
		witchS = new AnimatedShape("witchModel.rkm", "witchModel.rks");
		witchS.loadAnimation("WALK", "witchWalk.rka");
		groundS = new TerrainPlane(1000);
		houseS  = new ImportedModel("houseModel.obj");
		broomS    = new ImportedModel("broomModel.obj");
		mountainS = new ImportedModel("mountain.obj");
		ghostS    = new ImportedModel("dolphinHighPoly.obj");
		boxS    = new ImportedModel("box.obj");
		chopperS = new ImportedModel("heli.obj"); 
		warehouseS = new ImportedModel("warehouse.obj");
		boxpileS   = new ImportedModel("boxpile.obj");
		arrowS     = new ImportedModel("arrow.obj");
	}

	@Override
	public void loadTextures()
	{
		witchTexA = new TextureImage("witchTexA.png");
		witchTexB = new TextureImage("witchTexB.png");
		warehouseTx = new TextureImage("warehouse_texture.png");
		groundTx      = new TextureImage("roads.PNG");
		groundHeightMap = new TextureImage("hills.jpg");
		houseTx       = new TextureImage("houseTex.png"); 
		mountainTx    = new TextureImage("rock.PNG");  
		ghostTx       = new TextureImage("Dolphin_HighPolyUV.jpg");
		chopperTx  = new TextureImage("heliTex.png"); 
		boxTx     = new TextureImage("sand1.jpg");
		boxpileTx = new TextureImage("boxpile_texture.png");
		arrowTx   = new TextureImage("blue.PNG");
		broomTx = new TextureImage("broomTex.png");
	}

	@Override
	public void loadSkyBoxes()
	{
		skyboxTex = (engine.getSceneGraph()).loadCubeMap("fluffyClouds");
		(engine.getSceneGraph()).setActiveSkyBoxTexture(skyboxTex);
		(engine.getSceneGraph()).setSkyBoxEnabled(true);
	}

	@Override
	public void loadSounds()
	{
		audioMgr = engine.getAudioManager();

		//copter.wav - loops and follows copter
		AudioResource copterRes = audioMgr.createAudioResource("copter.wav", AudioResourceType.AUDIO_SAMPLE);
		copterSound = new Sound(copterRes, SoundType.SOUND_EFFECT, 100, true);
		copterSound.initialize(audioMgr);
		copterSound.setMaxDistance(3000.0f);
		copterSound.setMinDistance(4.0f);
		copterSound.setRollOff(2.0f);

		//point.wav - plays each time player scores a point
		AudioResource pointRes = audioMgr.createAudioResource("point.wav", AudioResourceType.AUDIO_SAMPLE);
		pointSound = new Sound(pointRes, SoundType.SOUND_EFFECT, 100, false);
		pointSound.initialize(audioMgr);
		pointSound.setMaxDistance(3000.0f);
		pointSound.setMinDistance(2000.0f);
		pointSound.setRollOff(2.0f);
	}

	@Override
	public void buildObjects()
	{
		Matrix4f initialTranslation, initialScale;

		//build player avatar
		TextureImage startTex = (selectedAvatar == 1) ? witchTexB   : witchTexA;
		dol = new GameObject(GameObject.root(), witchS, startTex);
		initialTranslation = (new Matrix4f()).translation(60f, -16f, 20f);
		dol.setLocalTranslation(initialTranslation);
		applyAvatarTransform();

		//build ground
		ground = new GameObject(GameObject.root(), groundS, groundTx);
		ground.setLocalTranslation(new Matrix4f().translation(0f, -16f, 0f));
		ground.setLocalScale(new Matrix4f().scaling(160.0f, 6.0f, 160.0f));
		ground.setIsTerrain(true);
		ground.setHeightMap(groundHeightMap);

		//build 12 houses in a 4x4 grid with centre 2x2 removed
		int idx = 0;
		for (int row = 0; row < 4; row++) {
			for (int col = 0; col < 4; col++) {
				if (SKIP_GRID.contains(row * 4 + col)) continue;
				float x = GRID_OFFSET + col * GRID_SPACING;
				float z = GRID_OFFSET + row * GRID_SPACING;
				houses[idx] = new GameObject(GameObject.root(), houseS, houseTx);
				houses[idx].setLocalTranslation(new Matrix4f().translation(x, -15.5f, z));
				houses[idx].setLocalScale(new Matrix4f().scaling(2f));
				idx++;
			}
		}

		//warehouse at centre of grid
		warehouse = new GameObject(GameObject.root(), warehouseS, warehouseTx);
		warehouse.setLocalTranslation(new Matrix4f().translation(0f, -16f, 0f));
		warehouse.setLocalScale(new Matrix4f().scaling(5f));

		//boxpile at ground level centre
		boxpile = new GameObject(GameObject.root(), boxpileS, boxpileTx);
		boxpile.setLocalTranslation(new Matrix4f().translation(0f, -15f, 0f));
		boxpile.setLocalScale(new Matrix4f().scaling(1.5f));

		//broom held/ridden by witch avatar
		broom = new GameObject(dol, broomS, broomTx);

		broom.propagateTranslation(true);
		broom.propagateRotation(true);
		broom.propagateScale(false);
		broom.applyParentRotationToPosition(true);
		broom.applyParentScaleToPosition(false);

		// X = left/right, Y = up/down, Z = forward/back relative to witch
		// Move broom back so witch looks farther forward on it
		broom.setLocalTranslation(new Matrix4f().translation(0.00f, -2.40f, -2.20f));

		// Slightly tilt broom so bristles point downward
		Matrix4f broomRot = new Matrix4f()
			.rotationY((float)Math.toRadians(0.0f))
			.rotateX((float)Math.toRadians(78.0f))
			.rotateZ((float)Math.toRadians(0.0f));

		broom.setLocalRotation(broomRot);
		broom.setLocalScale(new Matrix4f().scaling(0.50f));

		//mountains
		mountain = new GameObject(GameObject.root(), mountainS, mountainTx);
		mountain.setLocalTranslation(new Matrix4f().translation(0f, -16.4f, 0f));
		mountain.setLocalScale(new Matrix4f().scaling(25.0f));

		//chopper NPC — spawns away from origin, hovering above terrain
		chopper = new GameObject(GameObject.root(), chopperS, chopperTx);
		chopper.setLocalTranslation(new Matrix4f().translation(30f, 1, 30f));
		chopper.setLocalScale(new Matrix4f().scaling(2.0f));

		//arrow indicator — parked off-screen until game start; pickNewActiveHouse() moves it
		arrow = new GameObject(GameObject.root(), arrowS, arrowTx);
		arrow.setLocalTranslation(new Matrix4f().translation(0f, -1000f, 0f));
		arrow.setLocalScale(new Matrix4f().scaling(3.0f));
	}

	@Override
	public void createViewports()
	{
		(engine.getRenderSystem()).addViewport("LEFT",  0f,    0f, 1f,    1f);
		(engine.getRenderSystem()).addViewport("RIGHT", 0.75f, 0f, 0.25f, 0.25f);

		Viewport leftVp  = (engine.getRenderSystem()).getViewport("LEFT");
		Viewport rightVp = (engine.getRenderSystem()).getViewport("RIGHT");

		Camera leftCamera  = leftVp.getCamera();
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
	//the houses and ground is physics
	public void initializePhysicsObjects()
	{
		float[] gravity = {0f, -9.8f, 0f};
		physicsEngine = (engine.getSceneGraph()).getPhysicsEngine();
		physicsEngine.setGravity(gravity);

		float[] up = {0f, 1f, 0f};
		Vector3f loc = ground.getWorldLocation();
		Quaternionf rot = new Quaternionf();
		(ground.getWorldRotation()).getNormalizedRotation(rot);
		groundPhysics = (engine.getSceneGraph()).addPhysicsStaticPlane(loc, rot, up, 1.5f);
		groundPhysics.setBounciness(0.4f);
		ground.setPhysicsObject(groundPhysics);

		for (int i = 0; i < HOUSE_COUNT; i++) {
			loc = houses[i].getWorldLocation();
			rot = new Quaternionf();
			(houses[i].getWorldRotation()).getNormalizedRotation(rot);
			Vector3f colliderLoc = new Vector3f(loc.x, loc.y + 2.0f, loc.z);
			float[] houseHalfExtents = {6.0f, 4.0f, 6.0f};
			housePhysics[i] = (engine.getSceneGraph()).addPhysicsBox(0.0f, colliderLoc, rot, houseHalfExtents);
			housePhysics[i].setBounciness(0.1f);
			houses[i].setPhysicsObject(housePhysics[i]);
		}

		//boxpile: player walking into it refills boxes
		loc = boxpile.getWorldLocation();
		rot = new Quaternionf();
		(boxpile.getWorldRotation()).getNormalizedRotation(rot);
		float[] boxpileHalfExtents = {4.0f, 4.0f, 4.0f};
		boxpilePhysics = (engine.getSceneGraph()).addPhysicsBox(0.0f, loc, rot, boxpileHalfExtents);
		boxpilePhysics.setBounciness(0.0f);
		boxpile.setPhysicsObject(boxpilePhysics);
	}

	@Override
	public void initializeLights()
	{
		Light.setGlobalAmbient(0.5f, 0.5f, 0.5f);
		light1 = new Light();
		light1.setLocation(new Vector3f(5.0f, 4.0f, 2.0f));
		(engine.getSceneGraph()).addLight(light1);

		//one light hovering above each of the 12 houses — all off by default
		for (int i = 0; i < HOUSE_COUNT; i++) {
			Vector3f hPos = houses[i].getWorldLocation();
			houseLights[i] = new Light();
			houseLights[i].setLocation(new Vector3f(hPos.x, hPos.y + 12f, hPos.z));
			houseLights[i].disable();
			(engine.getSceneGraph()).addLight(houseLights[i]);
		}
	}

	//turns off all house lights, picks a new random active house, lights it, and moves the arrow above it
	private void pickNewActiveHouse()
	{
		for (int i = 0; i < HOUSE_COUNT; i++)
			houseLights[i].disable();
		activeHouse = rng.nextInt(HOUSE_COUNT);
		houseLights[activeHouse].enable();

		//move arrow to hover above the active house
		Vector3f hPos = houses[activeHouse].getWorldLocation();
		arrow.setLocalLocation(new Vector3f(hPos.x, hPos.y + 20f, hPos.z));
	}

	@Override
	public void initializeGame()
	{
		lastFrameTime = System.currentTimeMillis();
		currFrameTime = System.currentTimeMillis();
		elapsTime = 0.0f;
		(engine.getRenderSystem()).setWindowDimensions(1900, 1000);

		Camera c = engine.getRenderSystem().getViewport("LEFT").getCamera();
		orbitController = new CameraOrbit3D(c, dol);

		// Pull camera back so witch is less zoomed-in
		orbitController.zoom(6.0f);

		setupInputs();
		setupNetworking();

		//set up rotating arrow indicator
		rc = new RotationController(engine, new Vector3f(0, 1, 0), ARROW_SPIN_SPEED);
		rc.addTarget(arrow);
		(engine.getSceneGraph()).addNodeController(rc);
		rc.enable();

		//start the looping chopper sound
		copterSound.setLocation(chopper.getWorldLocation());
		setEarParameters();
		copterSound.play();
	}

	public void setEarParameters()
	{
		Camera camera = (engine.getRenderSystem()).getViewport("LEFT").getCamera();
		audioMgr.getEar().setLocation(dol.getWorldLocation());
		audioMgr.getEar().setOrientation(camera.getN(), new Vector3f(0.0f, 1.0f, 0.0f));
	}

	@Override
	public void update()
	{
		lastFrameTime = currFrameTime;
		currFrameTime = System.currentTimeMillis();
		elapsTime = (currFrameTime - lastFrameTime) / 1000.0f;

		// game over check
		if (gameOver) {
			updateMainHUD();
			updateHUD2();
			return;
		}

		witchMovedThisFrame = false;

		engine.getInputManager().update(elapsTime);

		if (witchWalking && !witchMovedThisFrame) {
			stopWitchWalkAnimation();
		}

		if (witchS != null) {
			witchS.updateAnimation();
		}

		//process networking
		processNetworking(elapsTime);

		//determine the Y baseline the player should be on this frame
		float targetBaseY = isOnGround ? GROUND_Y : SURFACE_Y;

		//if mid-transit, move toward the target baseline before applying terrain-snap
		Vector3f dolLoc = dol.getLocalLocation();
		if (isTransitioning) {
			float targetY = ground.getHeight(dolLoc.x, dolLoc.z) + targetBaseY + avatarGroundOffset;
			float step    = SINK_RISE_SPEED * elapsTime;
			float newY;
			if (isOnGround) {
				newY = Math.max(dolLoc.y - step, targetY);
				if (newY <= targetY) isTransitioning = false;
			} else {
				newY = Math.min(dolLoc.y + step, targetY);
				if (newY >= targetY) isTransitioning = false;
			}
			dol.setLocalLocation(new Vector3f(dolLoc.x, newY, dolLoc.z));
		}

		//bind player avatar to terrain — always active, offset by current mode's base Y
		dolLoc = dol.getLocalLocation();
		float terrainHeight = ground.getHeight(dolLoc.x, dolLoc.z);
		if (!isTransitioning) {
			dol.setLocalLocation(new Vector3f(dolLoc.x, targetBaseY + terrainHeight + avatarGroundOffset, dolLoc.z));
		}

		Vector3f dolPos = dol.getWorldLocation();
		orbitController.updateCameraPosition();

		if (!overheadInitialized) {
			overheadX = dolPos.x;
			overheadZ = dolPos.z;
			overheadInitialized = true;
		}

		if (!overheadManualPan) {
			overheadX = dolPos.x;
			overheadZ = dolPos.z;
		}

		updateOverheadCamera();

		statusMsg = "Score: " + score;

		//tick physics
		if (gameStart && physicsEngine != null)
		{
			physicsEngine.update(elapsTime);

			for (int i = 0; i < spawnedBoxes.size(); i++) {
				PhysicsObject bp = spawnedBoxPhysics.get(i);
				Vector3f bLoc = bp.getLocation();
				Matrix4f locMat = new Matrix4f();
				locMat.set(3,0,bLoc.x); locMat.set(3,1,bLoc.y); locMat.set(3,2,bLoc.z);
				spawnedBoxes.get(i).setLocalTranslation(locMat);
				Quaternionf bRot = bp.getRotation();
				Matrix4f rotMat = new Matrix4f();
				bRot.get(rotMat);
				spawnedBoxes.get(i).setLocalRotation(rotMat);
			}

			physicsEngine.detectCollisions();
			ArrayList<Integer> toRemove = new ArrayList<Integer>();
			for (int i = 0; i < spawnedBoxes.size(); i++) {
				PhysicsObject bp = spawnedBoxPhysics.get(i);
				HashSet<PhysicsObject> hits = bp.getNewlyCollidedSet();
				if (activeHouse >= 0 && hits.contains(housePhysics[activeHouse])) {
					toRemove.add(i);
					score++;
					//play the point sound at the box's collision position
					pointSound.setLocation(spawnedBoxes.get(i).getWorldLocation());
					pointSound.play();
					pickNewActiveHouse();
				}
			}

			//check if player avatar is touching the boxpile to refill boxes
			if (boxpilePhysics != null) {
				Vector3f dolLoc2 = dol.getWorldLocation();
				Vector3f pileLoc = boxpile.getWorldLocation();
				float dx = Math.abs(dolLoc2.x - pileLoc.x);
				float dz = Math.abs(dolLoc2.z - pileLoc.z);
				if (dx < 5.0f && dz < 5.0f) {
					boxesOnHand = 3;
				}
			}
			for (int i = toRemove.size() - 1; i >= 0; i--) {
				int idx = toRemove.get(i);
				engine.getSceneGraph().removeGameObject(spawnedBoxes.get(idx));
				physicsEngine.removeObject(spawnedBoxPhysics.get(idx).getUID());
				spawnedBoxes.remove(idx);
				spawnedBoxPhysics.remove(idx);
			}
		}

		//tick chopper NPC
		updateChopper(elapsTime);

		//update 3D audio each frame
		copterSound.setLocation(chopper.getWorldLocation());
		setEarParameters();

		updateMainHUD();
		updateHUD2();
	}

	 //chopper action, move slowly towards nearest player aftet game starts
	private void updateChopper(float dt)
	{
		if (chopper == null) return;

		//keep chopper at  y=0 regardless of game state
		Vector3f cPos = chopper.getLocalLocation();
		float th = ground.getHeight(cPos.x, cPos.z);
		chopper.setLocalLocation(new Vector3f(cPos.x, th + 1, cPos.z));

		if (!gameStart) return;   //don't move until Enter is pressed

		//find the closest player (local avatar + all ghost avatars)
		Vector3f target = findClosestPlayerPos(chopper.getWorldLocation());
		if (target == null) return;

		//move toward target on the XZ plane
		Vector3f chopPos = chopper.getWorldLocation();
		Vector3f toTarget = new Vector3f(target.x - chopPos.x, 0f, target.z - chopPos.z);

		float dist = toTarget.length();
		if (dist < 0.5f) return;   //close enough — stop jittering

		toTarget.normalize().mul(CHOPPER_SPEED * dt);

		chopper.setLocalLocation(new Vector3f(
			chopPos.x + toTarget.x,
			chopPos.y,              //y is already terrain-locked above
			chopPos.z + toTarget.z));

		//face direction of travel (yaw only)
		float angle = (float) Math.atan2(toTarget.x, toTarget.z);
		chopper.setLocalRotation(new Matrix4f().rotationY(angle));

		//check if chopper has reached the player, trigger game over if close enough.
		final float CHOPPER_TOUCH_RADIUS = 5.0f; 
		if (chopper.getWorldLocation().distance(dol.getWorldLocation()) < CHOPPER_TOUCH_RADIUS) {
			gameOver = true;
			statusMsg = "GAME OVER — caught by the helicopter!";
		}
	}

	//return position of player closest to object
	private Vector3f findClosestPlayerPos(Vector3f from)
	{
		Vector3f closest  = null;
		float    bestDist = Float.MAX_VALUE;

		//local player
		Vector3f dolPos = dol.getWorldLocation();
		float d = from.distance(dolPos);
		if (d < bestDist) { bestDist = d; closest = dolPos; }

		//remote ghost players
		for (GhostAvatar ga : gm.getGhostAvatars()) {
			Vector3f gPos = ga.getWorldLocation();
			float gd = from.distance(gPos);
			if (gd < bestDist) { bestDist = gd; closest = gPos; }
		}

		return closest;
	}

	private boolean hudDebugPrinted = false;
	private void updateMainHUD()
	{
		Viewport mainVp = engine.getRenderSystem().getViewport("LEFT");
		if (!hudDebugPrinted) {
			System.out.println("LEFT vp: left=" + mainVp.getActualLeft()
				+ " bottom=" + mainVp.getActualBottom()
				+ " width="  + mainVp.getActualWidth()
				+ " height=" + mainVp.getActualHeight());
			hudDebugPrinted = true;
		}

		//show a prompt before the game starts; switch to score once running.
		String hudMsg = !gameStart ? "Press ENTER or A button to start" 
					: (score >= 5 ? "You Win!" : statusMsg);
		(engine.getHUDmanager()).setHUD1(hudMsg, new Vector3f(1,1,0), 15, 15);
	}

	private void updateHUD2()
	{
		String boxMsg = gameStart ? "Boxes: " + boxesOnHand + " / 3" : "";
		(engine.getHUDmanager()).setHUD2(boxMsg, new Vector3f(1,1,1), 15, 40);
	}

	private void updateOverheadCamera()
	{
		Vector3f target = new Vector3f(overheadX, 0.0f,          overheadZ);
		Vector3f camPos = new Vector3f(overheadX, overheadHeight, overheadZ);
		overheadCam.setLocation(camPos);
		overheadCam.lookAt(target);
	}

	//avatar stuff
	private void applySelectedAvatar()
	{
		if (dol == null) return;

		dol.setShape(witchS);

		if (selectedAvatar == 1) {
			dol.setTextureImage(witchTexB);
			statusMsg = "Witch B selected";
		}
		else {
			dol.setTextureImage(witchTexA);
			statusMsg = "Witch A selected";
		}

		applyAvatarTransform();
	}

		private void applyAvatarTransform()
		{
			if (dol == null) return;

			dol.setLocalScale(new Matrix4f().scaling(0.4f));

			// Rotate the witch model visually 180 degrees
			dol.getRenderStates().setModelOrientationCorrection(
				new Matrix4f().rotationY((float)Math.toRadians(180.0f))
			);

			// Raise witch higher off the ground
			avatarGroundOffset = 4.0f;

			// This is camera height adjustment, not zoom
			avatarCameraOffset = 0.0f;
		}

	//input actions
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
		public SelectAvatarAction(int index) { avatarIndex = index; }
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

	// Clamps a proposed position to the world borders and sets it on the avatar.
	private void clampToBorder(Vector3f newPos) {
		float cx = Math.max(BORDER_X_MIN, Math.min(BORDER_X_MAX, newPos.x));
		float cz = Math.max(BORDER_Z_MIN, Math.min(BORDER_Z_MAX, newPos.z));
		dol.setLocalLocation(new Vector3f(cx, newPos.y, cz));
	}

	private void startWitchWalkAnimation() {
		if (witchS == null) return;

		if (!witchWalking) {
			witchS.stopAnimation();
			witchS.playAnimation("WALK", 1.0f, AnimatedShape.EndType.LOOP, 0);
			witchWalking = true;
		}
	}

	private void stopWitchWalkAnimation() {
		if (witchS == null) return;

		if (witchWalking) {
			witchS.stopAnimation();
			witchWalking = false;
		}
	}

	private class FwdAction extends AbstractInputAction {
		@Override
		public void performAction(float time, Event e) {
			if (!gameStart) return;

			witchMovedThisFrame = true;
			startWitchWalkAnimation();

			float dist = moveSpeed * time;
			Vector3f pos = dol.getWorldLocation();
			Vector3f fwd = new Vector3f(dol.getWorldForwardVector()).normalize();
			clampToBorder(new Vector3f(pos).add(new Vector3f(fwd).mul(dist)));

			if (protClient != null && isConnected)
				protClient.sendMoveMessage(dol.getWorldLocation());
		}
	}

	private class BwdAction extends AbstractInputAction {
		@Override
		public void performAction(float time, Event e) {
			if (!gameStart) return;

			witchMovedThisFrame = true;
			startWitchWalkAnimation();

			float dist = moveSpeed * time;
			Vector3f pos = dol.getWorldLocation();
			Vector3f fwd = new Vector3f(dol.getWorldForwardVector()).normalize();
			clampToBorder(new Vector3f(pos).add(new Vector3f(fwd).mul(-dist)));

			if (protClient != null && isConnected)
				protClient.sendMoveMessage(dol.getWorldLocation());
		}
	}

	private class KeyboardAction extends AbstractInputAction {
		private String mode;
		private float dir;
		public KeyboardAction(String mode, float dir) { this.mode = mode; this.dir = dir; }
		@Override
		public void performAction(float time, Event e) {
			if (!gameStart) return;
			float amount = dir * camTurnSpeed;
			if (mode.equals("yaw")) dol.globalYaw(amount);
		}
	}

	private class GamepadMoveAction extends AbstractInputAction {
		@Override
		public void performAction(float time, Event e) {
			if (!gameStart) return;

			float val = e.getValue();
			if (Math.abs(val) < 0.15f) return;

			witchMovedThisFrame = true;
			startWitchWalkAnimation();

			float dist = moveSpeed * time * (-val);
			Vector3f pos = dol.getWorldLocation();
			Vector3f fwd = new Vector3f(dol.getWorldForwardVector()).normalize();
			clampToBorder(new Vector3f(pos).add(new Vector3f(fwd).mul(dist)));

			if (protClient != null && isConnected)
				protClient.sendMoveMessage(dol.getWorldLocation());
		}
	}

	private class GamepadYawAction extends AbstractInputAction {
		@Override
		public void performAction(float time, Event e) {
			if (!gameStart) return;
			float val = e.getValue();
			if (Math.abs(val) < 0.15f) return;
			dol.globalYaw(-val * camTurnSpeed);
		}
	}

	private class GamepadOrbitElevationAction extends AbstractInputAction {
    	@Override
    	public void performAction(float time, Event e) {
        	if (!gameStart) return;
        	float val = e.getValue();
        	if (Math.abs(val) < 0.15f) return;
       		orbitController.orbitElevation(-val * 20.0f * time);
    	}
	}

	private class StartGameControll extends AbstractInputAction {
		@Override
		public void performAction(float time, Event e) {
			gameStart = true;
			pickNewActiveHouse();
			System.out.println("Game has started!");
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
			if (boxesOnHand <= 0) return; // can't drop what you don't have
			Vector3f pos  = dol.getWorldLocation();
			Vector3f fwd  = new Vector3f(dol.getWorldForwardVector()).normalize();
			Vector3f spawnPos = new Vector3f(pos).add(new Vector3f(fwd).mul(3.5f));

			GameObject box = new GameObject(GameObject.root(), boxS, boxTx);
			box.setLocalLocation(spawnPos);
			box.setLocalScale(new Matrix4f().scaling(0.5f));

			Quaternionf rot = new Quaternionf();
			(box.getWorldRotation()).getNormalizedRotation(rot);
			float[] halfExtents = {0.5f, 0.5f, 0.5f};
			PhysicsObject bp = (engine.getSceneGraph()).addPhysicsBox(
				1.0f, spawnPos, rot, halfExtents);
			bp.setBounciness(0.4f);
			bp.disableSleeping();
			box.setPhysicsObject(bp);

			spawnedBoxes.add(box);
			spawnedBoxPhysics.add(bp);
			boxesOnHand--;
		}
	}

	private class SinkRiseAction extends AbstractInputAction {
		@Override
		public void performAction(float time, Event e) {
			if (!gameStart || isTransitioning) return;
			isOnGround      = !isOnGround;
			isTransitioning = true;
		}
	}

	//networking methods
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

		if (protClient == null)
			System.out.println("ERROR: missing protocol host");
		else
			protClient.sendJoinMessage();
	}

	private void processNetworking(float elapsedTime) {
		if (protClient != null)
			protClient.processPackets();
	}

	private void setupInputs()
	{
		InputManager im = engine.getInputManager();

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

		im.associateActionWithAllKeyboards(
			net.java.games.input.Component.Identifier.Key.LEFT,
			new OrbitAzimuthAction(-1.0f),
			InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		im.associateActionWithAllKeyboards(
			net.java.games.input.Component.Identifier.Key.RIGHT,
			new OrbitAzimuthAction(1.0f),
			InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		im.associateActionWithAllKeyboards(
			net.java.games.input.Component.Identifier.Key.UP,
			new OrbitElevationAction(1.0f),
			InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		im.associateActionWithAllKeyboards(
			net.java.games.input.Component.Identifier.Key.DOWN,
			new OrbitElevationAction(-1.0f),
			InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		im.associateActionWithAllKeyboards(
			net.java.games.input.Component.Identifier.Key.Q,
			new OrbitZoomAction(-1.0f),
			InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		im.associateActionWithAllKeyboards(
			net.java.games.input.Component.Identifier.Key.E,
			new OrbitZoomAction(1.0f),
			InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		im.associateActionWithAllKeyboards(
			net.java.games.input.Component.Identifier.Key.SPACE,
			new SpawnBoxAction(),
			InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);

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

		im.associateActionWithAllKeyboards(
			net.java.games.input.Component.Identifier.Key.W,
			new FwdAction(),
			InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		im.associateActionWithAllKeyboards(
			net.java.games.input.Component.Identifier.Key.R,
			new SinkRiseAction(),
			InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);

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

		im.associateActionWithAllGamepads(
			net.java.games.input.Component.Identifier.Axis.Y,
			new GamepadMoveAction(),
			InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		im.associateActionWithAllGamepads(
			net.java.games.input.Component.Identifier.Axis.RX,
			new GamepadYawAction(),
			InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		im.associateActionWithAllGamepads(
			net.java.games.input.Component.Identifier.Axis.RY,
			new GamepadOrbitElevationAction(),
			InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		im.associateActionWithAllGamepads(
    		net.java.games.input.Component.Identifier.Button._0,  //A button
    		new StartGameControll(),
    		InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);

		im.associateActionWithAllGamepads(
    		net.java.games.input.Component.Identifier.Button._2,  //X button
    		new SpawnBoxAction(),
    		InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);

		im.associateActionWithAllGamepads(
    		net.java.games.input.Component.Identifier.Button._1,  //B button
    		new SinkRiseAction(),
    		InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
	}

	//accessors
	public void setIsConnected(boolean connected) { this.isConnected = connected; }
	public boolean getIsConnected() { return isConnected; }
	public ObjShape getGhostShape() { return ghostS; }
	public TextureImage getGhostTexture() { return ghostTx; }

	// Old names, but these are no longer used for witch ghost texture logic
	public ObjShape getDolphinShape() { return ghostS; }
	public TextureImage getDolphinTexture() { return ghostTx; }

	public ObjShape getWitchShape() { 
		return witchS; 
	}

	public TextureImage getWitchTexture() { 
		return witchTexB;
	}

	public ObjShape getAvatarShape(int avatar) {
		return witchS;
	}

	public TextureImage getAvatarTexture(int avatar) {
		if (avatar == 1) {
			return witchTexB;
		}
		return witchTexA;
	}

	public GhostManager getGhostManager() { return gm; }
	public Engine getEngine() { return engine; }
	public Vector3f getPlayerPosition() { return dol.getWorldLocation(); }
	public int getSelectedAvatar() { return selectedAvatar; }
	public boolean isTwoPlayer() { return twoPlayer; }
	public int getGhostCount() { return gm.getGhostCount(); }

	public ObjShape getBroomShape() { 
		return broomS; 
	}

	public TextureImage getBroomTexture() { 
		return broomTx; 
	}

	@Override
	public void shutdown() {
		if (protClient != null && isConnected) {
			protClient.sendByeMessage();
		}

		super.shutdown();
		System.exit(0);
	}
}