/**
 * Copyright (c) 2015, The University of Southampton.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *   * 	Redistributions of source code must retain the above copyright notice,
 * 	this list of conditions and the following disclaimer.
 *
 *   *	Redistributions in binary form must reproduce the above copyright notice,
 * 	this list of conditions and the following disclaimer in the documentation
 * 	and/or other materials provided with the distribution.
 *
 *   *	Neither the name of the University of Southampton nor the names of its
 * 	contributors may be used to endorse or promote products derived from this
 * 	software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package uk.ac.soton.ecs.summerschool.vision101;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.io.IOException;

import javax.swing.JPanel;

import org.openimaj.content.slideshow.Slide;
import org.openimaj.content.slideshow.SlideshowApplication;
import org.openimaj.image.MBFImage;
import org.openimaj.image.colour.RGBColour;
import org.openimaj.image.processing.face.tracking.clm.CLMFaceTracker;
import org.openimaj.image.processing.face.tracking.clm.MultiTracker;
import org.openimaj.image.processing.face.tracking.clm.MultiTracker.TrackedFace;
import org.openimaj.image.renderer.RenderHints;
import org.openimaj.image.typography.FontStyle;
import org.openimaj.image.typography.FontStyle.HorizontalAlignment;
import org.openimaj.image.typography.general.GeneralFont;
import org.openimaj.math.geometry.line.Line2d;
import org.openimaj.math.geometry.point.Point2dImpl;
import org.openimaj.math.geometry.shape.Rectangle;
import org.openimaj.video.VideoDisplay;
import org.openimaj.video.VideoDisplayListener;

import uk.ac.soton.ecs.summerschool.vision101.utils.Simple3D;
import uk.ac.soton.ecs.summerschool.vision101.utils.VideoCaptureComponent;
import Jama.Matrix;

/**
 * Slide showing a 2.5D face tracker
 *
 * @author Jonathon Hare (jsh2@ecs.soton.ac.uk)
 *
 */
public class CLMDemo implements Slide, VideoDisplayListener<MBFImage> {
	CLMFaceTracker tracker = new CLMFaceTracker();
	protected VideoCaptureComponent vc;

	@Override
	public JPanel getComponent(int width, int height) throws IOException {
		// the main panel
		final JPanel base = new JPanel();
		base.setPreferredSize(new Dimension(width, height));
		base.setLayout(new GridBagLayout());
		vc = new VideoCaptureComponent(640, 480, "iSight");
		vc.getDisplay().addVideoListener(this);
		base.add(vc);

		tracker.fcheck = true;

		return base;
	}

	@Override
	public void close() {
		vc.close();
	}

	@Override
	public void afterUpdate(VideoDisplay<MBFImage> display) {
		// do nothing
	}

	@Override
	public void beforeUpdate(MBFImage frame) {
		tracker.track(frame);

		frame.bands.get(1).fill(0f);
		frame.bands.get(2).fill(0f);

		final FontStyle<Float[]> gfs = new GeneralFont("Bank Gothic", Font.PLAIN).createStyle(frame
				.createRenderer(RenderHints.ANTI_ALIASED));
		gfs.setFontSize(40);
		gfs.setHorizontalAlignment(HorizontalAlignment.HORIZONTAL_CENTER);

		if (tracker.getTrackedFaces().size() > 0) {
			final TrackedFace tf = tracker.getTrackedFaces().get(0);
			drawFaceModel(frame, tf, tracker.triangles, tracker.connections);
			frame.drawText("TRACKING", frame.getWidth() / 2, frame.getHeight() - 10, gfs);
		} else {
			frame.drawText("SEARCHING", frame.getWidth() / 2, frame.getHeight() / 2, gfs);
		}
	}

	public static void drawFaceModel(final MBFImage image, final MultiTracker.TrackedFace f, final int[][] triangles,
			final int[][] connections)
	{
		final int n = f.shape.getRowDimension() / 2;
		final Matrix visi = f.clm._visi[f.clm.getViewIdx()];

		// draw connections
		for (int i = 0; i < connections[0].length; i++) {
			if (visi.get(connections[0][i], 0) == 0
					|| visi.get(connections[1][i], 0) == 0)
				continue;

			image.drawLine(
					new Point2dImpl((float) f.shape.get(connections[0][i], 0),
							(float) f.shape.get(connections[0][i] + n, 0)),
					new Point2dImpl((float) f.shape.get(connections[1][i], 0),
							(float) f.shape.get(connections[1][i] + n, 0)),
					3, RGBColour.WHITE);
		}

		final double[] shapeVector = f.clm._plocal.getColumnPackedCopy();

		final int middle = 560;
		final int starty = 100;
		for (int i = 0; i < shapeVector.length; i++) {
			final int y = starty + i * 10;
			final int x = (int) (middle + shapeVector[i] * 6);
			image.drawLine(middle, y, x, y, 5, RGBColour.WHITE);
		}

		final double[] poseVector = f.clm._pglobl.getColumnPackedCopy();
		final double sc = poseVector[0];
		final double pitch = poseVector[1];
		final double roll = poseVector[2];
		final double yaw = poseVector[3];

		image.drawShape(new Rectangle(50, 100, 80, 60), 3, RGBColour.WHITE);
		image.drawShape(new Rectangle((int) (50 + poseVector[4] / 8 - 4 * sc), (int) (100 + poseVector[5] / 8 - 4 * sc),
				(int) (4 * sc), (int) (4 * sc)), 2, RGBColour.WHITE);

		final Matrix rpy = Simple3D.euler2Rot(pitch, roll, yaw);

		Line2d l1 = new Line2d(
				Simple3D.projectOrtho(rpy.times(new Matrix(new double[][] { { -30 }, { 0 }, { 0 } }))),
				Simple3D.projectOrtho(rpy.times(new Matrix(new double[][] { { 30 }, { 0 }, { 0 } })))
				);
		l1.translate(90, 300);
		image.drawLine(l1, 2, RGBColour.WHITE);

		l1 = new Line2d(
				Simple3D.projectOrtho(rpy.times(new Matrix(new double[][] { { 0 }, { -30 }, { 0 } }))),
				Simple3D.projectOrtho(rpy.times(new Matrix(new double[][] { { 0 }, { 30 }, { 0 } })))
				);
		l1.translate(90, 300);
		image.drawLine(l1, 2, RGBColour.WHITE);

		l1 = new Line2d(
				Simple3D.projectOrtho(rpy.times(new Matrix(new double[][] { { 0 }, { 0 }, { -30 } }))),
				Simple3D.projectOrtho(rpy.times(new Matrix(new double[][] { { 0 }, { 0 }, { 30 } })))
				);
		l1.translate(90, 300);
		image.drawLine(l1, 2, RGBColour.WHITE);
	}

	public static void main(String[] args) throws IOException {
		new SlideshowApplication(new CLMDemo(), 1024, 768, App.getBackground());
	}
}
