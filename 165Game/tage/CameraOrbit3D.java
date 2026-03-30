package tage;

import org.joml.*;
import java.lang.Math;

/**
* A CameraOrbit3D is a 3rd-person camera controller that, when enabled, causes a Camera
* to orbit around a target avatar at a specified azimuth, elevation, and radius.
* It supports orbiting left/right, adjusting elevation, and zooming in/out.
* @author Tatiana Neville
*/
public class CameraOrbit3D
{
	private Camera camera;
	private GameObject avatar;

	private float cameraAzimuth;
	private float cameraElevation;
	private float cameraRadius;

	/** Creates an orbit camera controller with default azimuth, elevation, and radius. */
	public CameraOrbit3D() { }

	/** Creates an orbit camera controller for the specified camera and avatar. */
	public CameraOrbit3D(Camera cam, GameObject av)
	{
		camera = cam;
		avatar = av;
		cameraAzimuth = 0.0f;
		cameraElevation = 20.0f;
		cameraRadius = 6.0f;
		updateCameraPosition();
	}

	/** sets the azimuth angle for this orbit camera */
	public void setAzimuth(float a) { cameraAzimuth = a; }

	/** sets the elevation angle for this orbit camera */
	public void setElevation(float e) { cameraElevation = e; }

	/** sets the radius for this orbit camera */
	public void setRadius(float r) { cameraRadius = r; }

	/** gets the azimuth angle for this orbit camera */
	public float getAzimuth() { return cameraAzimuth; }

	/** gets the elevation angle for this orbit camera */
	public float getElevation() { return cameraElevation; }

	/** gets the radius for this orbit camera */
	public float getRadius() { return cameraRadius; }

	/** positions the camera using azimuth, elevation, and radius relative to the avatar */
	public void updateCameraPosition()
	{	Vector3f avatarRot = avatar.getWorldForwardVector();
		double avatarAngle = Math.toDegrees((double)
			avatarRot.angleSigned(new Vector3f(0,0,-1), new Vector3f(0,1,0)));

		float totalAz = cameraAzimuth - (float) avatarAngle;
		double theta = Math.toRadians(totalAz);
		double phi = Math.toRadians(cameraElevation);

		float x = cameraRadius * (float)(Math.cos(phi) * Math.sin(theta));
		float y = cameraRadius * (float)(Math.sin(phi));
		float z = cameraRadius * (float)(Math.cos(phi) * Math.cos(theta));

		Vector3f avatarPos = avatar.getWorldLocation();
		camera.setLocation(new Vector3f(x, y, z).add(avatarPos));
		camera.lookAt(new Vector3f(avatarPos).add(0.0f, 1.0f, 0.0f));
	}

    /** adjusts the azimuth angle for this orbit camera */
	public void orbitAzimuth(float amt)
	{	cameraAzimuth += amt;
		cameraAzimuth = cameraAzimuth % 360.0f;
        updateCameraPosition();
	}

	/** adjusts the elevation angle for this orbit camera */
	public void orbitElevation(float amt)
	{	cameraElevation += amt;

		if (cameraElevation < 5.0f) cameraElevation = 5.0f;
		if (cameraElevation > 80.0f) cameraElevation = 80.0f;

        updateCameraPosition();
	}

	/** adjusts the radius for this orbit camera */
	public void zoom(float amt)
	{	cameraRadius += amt;

		if (cameraRadius < 2.0f) cameraRadius = 2.0f;
		if (cameraRadius > 15.0f) cameraRadius = 15.0f;

        updateCameraPosition();
	}
}