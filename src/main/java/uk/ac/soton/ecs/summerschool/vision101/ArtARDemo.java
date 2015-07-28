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

import java.awt.GridBagConstraints;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.JEditorPane;
import javax.swing.JPanel;

import org.openimaj.content.slideshow.Slide;
import org.openimaj.content.slideshow.SlideshowApplication;
import org.openimaj.feature.local.list.LocalFeatureList;
import org.openimaj.feature.local.matcher.FastBasicKeypointMatcher;
import org.openimaj.feature.local.matcher.consistent.ConsistentLocalFeatureMatcher2d;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.MBFImage;
import org.openimaj.image.feature.local.engine.DoGSIFTEngine;
import org.openimaj.image.feature.local.keypoints.Keypoint;
import org.openimaj.io.IOUtils;
import org.openimaj.math.geometry.transforms.HomographyModel;
import org.openimaj.math.geometry.transforms.HomographyRefinement;
import org.openimaj.math.geometry.transforms.check.TransformMatrixConditionCheck;
import org.openimaj.math.geometry.transforms.estimation.RobustHomographyEstimator;
import org.openimaj.math.model.fit.RANSAC;
import org.openimaj.video.VideoDisplay;
import org.openimaj.video.VideoDisplayListener;

/**
 * Slide showing a simple aumented reality or content-based retrieval
 * application for artwork.
 *
 * @author Jonathon Hare (jsh2@ecs.soton.ac.uk)
 *
 */
public class ArtARDemo extends SimpleCameraDemo implements Slide, VideoDisplayListener<MBFImage>, Runnable {
	private DoGSIFTEngine engine;
	private ConsistentLocalFeatureMatcher2d<Keypoint> matcher;
	final Map<List<Keypoint>, String> data;
	private JEditorPane labelField;
	private MBFImage currentFrame;
	private volatile boolean isRunning;

	public ArtARDemo() throws IOException {
		super("FaceTime");

		engine = new DoGSIFTEngine();
		engine.getOptions().setDoubleInitialImage(false);

		final RobustHomographyEstimator fitter = new RobustHomographyEstimator(0.5, 1500,
				new RANSAC.PercentageInliersStoppingCondition(0.6), HomographyRefinement.NONE,
				new TransformMatrixConditionCheck<HomographyModel>(10000));
		this.matcher = new ConsistentLocalFeatureMatcher2d<Keypoint>(new FastBasicKeypointMatcher<Keypoint>(8));
		this.matcher.setFittingModel(fitter);

		data = IOUtils.read(new DataInputStream(ArtARDemo.class.getResourceAsStream("artARdemo.dat")));
	}

	@Override
	public void close() {
		isRunning = false;
		super.close();
	}

	@Override
	public JPanel getComponent(int width, int height) throws IOException {
		final JPanel panel = super.getComponent(width, height);

		labelField = new JEditorPane();
		labelField.setSize(640, 100);
		labelField.setPreferredSize(labelField.getSize());
		labelField.setContentType("text/html");

		final GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridy = 1;
		panel.add(labelField, gbc);

		vc.getDisplay().addVideoListener(this);

		isRunning = true;
		new Thread(this).start();

		return panel;
	}

	@Override
	public void afterUpdate(VideoDisplay<MBFImage> display) {
		// Do nothing
	}

	@Override
	public void beforeUpdate(MBFImage frame) {
		synchronized (this) {
			if (frame == null)
				currentFrame = null;
			else
				currentFrame = frame.clone();
		}
	}

	@Override
	public void run() {
		while (isRunning) {
			MBFImage frame;
			synchronized (this) {
				frame = currentFrame;
				currentFrame = null;
			}

			if (frame == null)
				continue;

			final LocalFeatureList<Keypoint> features = engine.findFeatures(frame.flatten());

			boolean found = false;
			for (final Entry<List<Keypoint>, String> e : data.entrySet()) {
				matcher.setModelFeatures(e.getKey());

				if (matcher.findMatches(features) && matcher.getMatches().size() > 35) {
					if (labelField.getText() != e.getValue())
						labelField.setText(e.getValue());

					found = true;
					break;
				}
			}

			if (!found)
				labelField.setText("");
		}
	}

	public static void main(String[] args) throws IOException {
		if (ArtARDemo.class.getResource("artARdemo.dat") == null) {
			final DoGSIFTEngine engine = new DoGSIFTEngine();
			engine.getOptions().setDoubleInitialImage(true);

			final Map<List<Keypoint>, String> data = new HashMap<List<Keypoint>, String>();

			data.put(engine.findFeatures(ImageUtilities.readF(ArtARDemo.class.getResource("images/N6574.PNG"))),
					"<h1>Lake Keitele</h1><h2>1905, Akseli Gallen-Kallela</h2>");
			data.put(engine.findFeatures(ImageUtilities.readF(ArtARDemo.class.getResource("images/N3908.PNG"))),
					"<h1>Bathers at Asnières</h1><h2>1884. Georges Seurat</h2>");
			data.put(engine.findFeatures(ImageUtilities.readF(ArtARDemo.class.getResource("images/N3268.PNG"))),
					"<h1>The Umbrellas</h1><h2>about 1881-6. Pierre-Auguste Renoir</h2>");
			data.put(engine.findFeatures(ImageUtilities.readF(ArtARDemo.class.getResource("images/N2514.PNG"))),
					"<h1>Venice: The Grand Canal facing Santa Croce</h1><h2>perhaps 1740s. Bernardo Bellotto</h2>");

			IOUtils.writeToFile(data, new File("src/main/resources/uk/ac/soton/ecs/summerschool/vision101/artARdemo.dat"));
		}

		new SlideshowApplication(new ArtARDemo(), 1024, 768, App.getBackground());
	}

}
