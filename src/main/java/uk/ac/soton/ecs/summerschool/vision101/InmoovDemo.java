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

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;

import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.openimaj.content.slideshow.Slide;
import org.openimaj.content.slideshow.SlideshowApplication;
import org.openimaj.image.FImage;
import org.openimaj.image.MBFImage;
import org.openimaj.image.colour.RGBColour;
import org.openimaj.image.contour.Contour;
import org.openimaj.image.contour.SuzukiContourProcessor;
import org.openimaj.image.processing.face.detection.DetectedFace;
import org.openimaj.image.processing.face.detection.HaarCascadeDetector;
import org.openimaj.image.processing.threshold.OtsuThreshold;
import org.openimaj.image.typography.FontStyle.HorizontalAlignment;
import org.openimaj.image.typography.hershey.HersheyFont;
import org.openimaj.image.typography.hershey.HersheyFontStyle;
import org.openimaj.math.geometry.point.Point2d;
import org.openimaj.math.geometry.point.Point2dImpl;
import org.openimaj.video.VideoDisplay;
import org.openimaj.video.VideoDisplayListener;

import uk.ac.soton.ecs.summerschool.vision101.inmoov.Servo;
import uk.ac.soton.ecs.summerschool.vision101.inmoov.ServoController;
import uk.ac.soton.ecs.summerschool.vision101.utils.VideoCaptureComponent;

/**
 * Demos for the inmoov robot (counting and face tracking).
 *
 * @author Jonathon Hare (jsh2@ecs.soton.ac.uk)
 *
 */
public class InmoovDemo implements Slide, VideoDisplayListener<MBFImage>, ActionListener, KeyListener {
	protected static final int PAN_CENTRE_PW = 2075;
	protected static final int TILT_CENTRE_PW = 850;

	ServoController controller;
	final Servo pan;
	final Servo tilt;

	protected VideoCaptureComponent vc;
	protected transient boolean doFaces = false;
	protected transient boolean doCounting = false;

	final SuzukiContourProcessor proc = new SuzukiContourProcessor();
	TIntList stack = new TIntArrayList();

	final HaarCascadeDetector faceDetector = HaarCascadeDetector.BuiltInCascade.frontalface_alt2.load();
	Point2d frameCentre;
	private JLabel panLabel;
	private JLabel tiltLabel;

	public InmoovDemo() {
		pan = new Servo(7, PAN_CENTRE_PW);
		tilt = new Servo(15, TILT_CENTRE_PW);
		faceDetector.setMinSize(40);
	}

	@Override
	public JPanel getComponent(int width, int height) throws IOException {
		// the main panel
		final JPanel base = new JPanel();
		base.setOpaque(false);
		base.setPreferredSize(new Dimension(width, height));
		base.setLayout(new GridBagLayout());
		vc = new VideoCaptureComponent(640, 480, "Hercules");
		base.add(vc);

		// add a listener for new video frames
		vc.getDisplay().addVideoListener(this);

		// the controls panel
		final JPanel controlsPanel = new JPanel(new GridLayout(0, 1));
		controlsPanel.setOpaque(false);

		createCheckBox(controlsPanel, "Track Faces");
		createCheckBox(controlsPanel, "Enable Counting");

		panLabel = new JLabel("Pan = 0000");
		controlsPanel.add(panLabel);
		tiltLabel = new JLabel("Tilt = 0000");
		controlsPanel.add(tiltLabel);

		base.add(controlsPanel);

		base.addKeyListener(this);

		frameCentre = new Point2dImpl(vc.getDisplay().getVideo().getWidth() / 2,
				vc.getDisplay().getVideo().getHeight() / 2);

		// init the servo controller
		try {
			controller = new ServoController();
			controller.registerServo(pan);
			controller.registerServo(tilt);
		} catch (final Exception e) {
			controller = null;
			System.err.println("Failed to initialise serial port connection");
		}

		return base;
	}

	private void createCheckBox(final JPanel controlsPanel, String name) {
		final JCheckBox button = new JCheckBox(name);
		button.setActionCommand(name);
		controlsPanel.add(button);
		button.addActionListener(this);
	}

	@Override
	public void close() {
		vc.close();
		if (controller != null)
			controller.close();
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		System.out.println(e.getActionCommand());

		if (e.getActionCommand().equals("Track Faces"))
			this.doFaces = ((JCheckBox) e.getSource()).isSelected();
		if (e.getActionCommand().equals("Enable Counting"))
			this.doCounting = ((JCheckBox) e.getSource()).isSelected();
	}

	@Override
	public void afterUpdate(VideoDisplay<MBFImage> display) {
		// do nothing
	}

	@Override
	public void beforeUpdate(MBFImage frame) {
		panLabel.setText(String.format("Pan = %04d", pan.getPW()));
		tiltLabel.setText(String.format("Tilt = %04d", tilt.getPW()));

		final MBFImage outframe = frame.clone();
		if (doFaces) {
			processFaces(frame, outframe);
		}
		if (doCounting) {
			processCount(frame, outframe);
		}
		frame.internalAssign(outframe);
	}

	private String lastString;
	private long lastTime;

	private void say(String string) {
		if (string.equals(lastString) && (System.currentTimeMillis() - lastTime) < 5000) {
			return;
		}

		try {
			Runtime.getRuntime().exec("say " + string);
			lastString = string;
			lastTime = System.currentTimeMillis();
		} catch (final IOException e) {
			// ignore - probably not OSX...
		}
	}

	private static final int MAX_CHILDLESS_CHILDREN = 0;
	private static final int MAX_CHILDREN = 5;
	private static final int MIN_CHILDREN = 1;

	public static List<Contour> find(Contour in) {
		final List<Contour> found = new ArrayList<Contour>();
		detect(in, found);
		return found;
	}

	private static void detect(Contour root, List<Contour> found) {
		if (test(root)) {
			found.add(root);
		}
		else {
			for (final Contour child : root.children) {
				detect(child, found);
			}
		}
	}

	public static boolean test(Contour in) {
		// has at least one child
		if (in.children.size() < MIN_CHILDREN || in.children.size() > MAX_CHILDREN) {
			return false;
		}
		int childlessChild = 0;
		// all grandchildren have no children
		for (final Contour child : in.children) {
			if (child.children.size() == 0)
				childlessChild++;

			if (childlessChild > MAX_CHILDLESS_CHILDREN)
				return false;
			for (final Contour grandchildren : child.children) {
				if (grandchildren.children.size() != 0)
					return false;
			}
		}
		return true;
	}

	private void processCount(MBFImage frame, MBFImage outframe) {
		final FImage grey = frame.flatten();
		grey.threshold(OtsuThreshold.calculateThreshold(grey, 256));

		proc.analyseImage(grey);

		final List<Contour> targets = find(proc.root);

		if (targets.size() == 1) {
			final Contour tgt = targets.get(0);
			final int count = count(outframe, tgt);

			for (int i = 0; i < stack.size(); i++) {
				if (stack.get(i) != count) {
					stack.clear();
					break;
				}
			}
			stack.add(count);

			if (stack.size() == 3) {
				say(count + "");
			}
		} else if (targets.size() > 1) {
			final TIntArrayList counts = new TIntArrayList();
			int sum = 0;
			for (int i = 0; i < targets.size(); i++) {
				final Contour tgt = targets.get(i);
				final int count = count(outframe, tgt);
				counts.add(count);
				sum += count;
			}

			for (int i = 0; i < stack.size(); i++) {
				if (stack.get(i) != sum) {
					stack.clear();
					break;
				}
			}
			stack.add(sum);

			if (stack.size() == 3) {
				String toSay = counts.get(0) + "";
				for (int i = 1; i < counts.size(); i++)
					toSay += " plus " + counts.get(i);
				toSay += " equals " + sum;

				say(toSay);
			}
		} else {
			stack.clear();
		}
	}

	private int count(MBFImage outframe, Contour tgt) {
		outframe.drawPolygon(tgt, 4, RGBColour.RED);
		final int count = tgt.children.get(0).children.size();

		final Point2d centre = tgt.calculateCentroid();
		final int size = (int) Math.sqrt(tgt.calculateArea());
		centre.setY(centre.getY() + 0.5f * size);
		final HersheyFontStyle<Float[]> fs = HersheyFont.ROMAN_SIMPLEX.createStyle(outframe.createRenderer());
		fs.setStrokeWidth(4);
		fs.setHorizontalAlignment(HorizontalAlignment.HORIZONTAL_CENTER);
		fs.setColour(RGBColour.RED);
		fs.setFontSize(size);
		final String string = "" + tgt.children.get(0).children.size();
		outframe.drawText(string, centre, fs);

		return count;
	}

	private void processFaces(MBFImage frame, MBFImage outframe) {
		try {
			final List<DetectedFace> faces = faceDetector.detectFaces(frame.flatten());

			if (faces == null || faces.size() == 0) {
				// move back towards center
				final int tiltPW = (TILT_CENTRE_PW - tilt.getPW()) / 5;
				tilt.changePWRelative(tiltPW);

				final int panPW = (PAN_CENTRE_PW - pan.getPW()) / 5;
				pan.changePWRelative(panPW);
			} else {
				outframe.drawShape(faces.get(0).getBounds(), RGBColour.RED);

				// move towards face
				final Point2d pt = faces.get(0).getBounds().calculateCentroid();

				final Point2d delta = pt.minus(frameCentre);

				final double damp = 0.3;

				pan.changePWRelative(-(int) (damp * delta.getX()));
				tilt.changePWRelative((int) (damp * delta.getY()));
			}
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void keyTyped(KeyEvent e) {
		// Do nothing
	}

	@Override
	public void keyPressed(KeyEvent e) {
		if (e.getKeyChar() == 'w')
			tilt.increment(50);
		if (e.getKeyChar() == 's')
			tilt.decrement(50);
		if (e.getKeyChar() == 'd')
			pan.increment(50);
		if (e.getKeyChar() == 'a')
			pan.decrement(50);
	}

	@Override
	public void keyReleased(KeyEvent e) {
		// Do nothing
	}

	public static void main(String[] args) throws Exception {
		new SlideshowApplication(new InmoovDemo(), 1024, 768, App.getBackground());
	}

}
