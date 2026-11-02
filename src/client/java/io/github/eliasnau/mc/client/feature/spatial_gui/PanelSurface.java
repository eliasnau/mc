package io.github.eliasnau.mc.client.feature.spatial_gui;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

/**
 * Maps GUI pixels onto the panel in camera space and back. The panel can be flat, or bent around a vertical
 * cylinder whose edges wrap towards the viewer.
 */
final class PanelSurface {
	private final Matrix4f localToView = new Matrix4f();
	private final Matrix4f viewToLocal = new Matrix4f();
	private float pixelSize;
	private float centerX;
	private float centerY;
	private float curveRadius;

	/**
	 * @param localToView places the panel's local space (centered on the focus area, +Z towards the viewer) in camera space
	 * @param pixelSize   blocks per GUI pixel
	 * @param curveRadius cylinder radius in blocks, or {@code 0} for a flat panel
	 */
	void set(final Matrix4fc localToView, final float pixelSize, final float centerX, final float centerY, final float curveRadius) {
		this.localToView.set(localToView);
		this.localToView.invert(this.viewToLocal);
		this.pixelSize = pixelSize;
		this.centerX = centerX;
		this.centerY = centerY;
		this.curveRadius = curveRadius;
	}

	Vector3f toView(final float guiX, final float guiY, final Vector3f out) {
		float x = (guiX - this.centerX) * this.pixelSize;
		float y = (this.centerY - guiY) * this.pixelSize;
		if (this.curveRadius > 0.0F) {
			float angle = x / this.curveRadius;
			out.set(this.curveRadius * (float)Math.sin(angle), y, this.curveRadius * (1.0F - (float)Math.cos(angle)));
		} else {
			out.set(x, y, 0.0F);
		}

		return this.localToView.transformPosition(out);
	}

	/** The panel's facing direction in camera space, pointing towards the viewer. */
	Vector3f normal(final Vector3f out) {
		return this.localToView.transformDirection(out.set(0.0F, 0.0F, 1.0F)).normalize();
	}

	/** Intersects a camera-space ray from the eye with the panel and returns the hit in GUI pixels. */
	double @Nullable [] raycast(final Vector3f direction) {
		Vector3f origin = this.viewToLocal.transformPosition(new Vector3f());
		Vector3f dir = this.viewToLocal.transformDirection(new Vector3f(direction));

		if (this.curveRadius > 0.0F) {
			// Cylinder around the vertical axis through (0, *, R): x² + (z - R)² = R².
			float r = this.curveRadius;
			float oz = origin.z - r;
			float a = dir.x * dir.x + dir.z * dir.z;
			float b = 2.0F * (origin.x * dir.x + oz * dir.z);
			float c = origin.x * origin.x + oz * oz - r * r;
			float discriminant = b * b - 4.0F * a * c;
			if (a > 1.0E-8F && discriminant >= 0.0F) {
				// The panel is the half of the cylinder facing away from the viewer, i.e. the far hit.
				float t = (-b + (float)Math.sqrt(discriminant)) / (2.0F * a);
				if (t > 0.0F) {
					float hitX = origin.x + dir.x * t;
					float hitY = origin.y + dir.y * t;
					float hitZ = origin.z + dir.z * t;
					float angle = (float)Math.atan2(hitX, r - hitZ);
					return new double[]{this.centerX + angle * r / this.pixelSize, this.centerY - hitY / this.pixelSize};
				}
			}
		}

		if (Math.abs(dir.z) < 1.0E-6F) {
			return null;
		}

		float t = -origin.z / dir.z;
		if (t <= 0.0F) {
			return null;
		}

		return new double[]{
			this.centerX + (origin.x + dir.x * t) / this.pixelSize, this.centerY - (origin.y + dir.y * t) / this.pixelSize
		};
	}
}
