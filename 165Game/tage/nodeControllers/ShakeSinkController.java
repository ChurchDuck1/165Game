package tage.nodeControllers;
import tage.*;
import org.joml.Vector3f;

/**
* A ShakeSinkController is a node controller that causes any object
* it is attached to to shake in place while slowly sinking downward when enabled
* @author Tatiana Neville
*/
public class ShakeSinkController extends NodeController
{
	private float shakeSpeed = 0.005f;
	private float shakeAmount = 0.05f;
	private float sinkSpeed = 0.002f;
	private float totalTime = 0.0f;
	private Vector3f curLocation, newLocation;
	private Engine engine;

	/** Creates a shake/sink controller with default shake and sink speeds. */
	public ShakeSinkController() { super(); }

	/** Creates a shake/sink controller with shake speed, shake amount, and sink speed as specified. */
	public ShakeSinkController(Engine e, float sSpeed, float sAmount, float downSpeed)
	{	super();
		shakeSpeed = sSpeed;
		shakeAmount = sAmount;
		sinkSpeed = downSpeed;
		engine = e;
	}

	/** sets the shake speed */
	public void setShakeSpeed(float s) { shakeSpeed = s; }

	/** sets the shake amount */
	public void setShakeAmount(float a) { shakeAmount = a; }

	/** sets the sink speed */
	public void setSinkSpeed(float s) { sinkSpeed = s; }

	/** This is called automatically by the RenderSystem (via SceneGraph) once per frame
	*   during display().  It is for engine use and should not be called by the application.
	*/
	public void apply(GameObject go)
	{	float elapsedTime = super.getElapsedTime();
		totalTime += elapsedTime;

		curLocation = go.getLocalLocation();

		float xShake = (float)Math.sin(totalTime * shakeSpeed) * shakeAmount;
		float zShake = (float)Math.cos(totalTime * shakeSpeed) * shakeAmount;

		newLocation = new Vector3f(
			curLocation.x + xShake,
			curLocation.y - (elapsedTime * sinkSpeed),
			curLocation.z + zShake
		);

		go.setLocalLocation(newLocation);
	}
}