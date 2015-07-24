package uk.ac.soton.ecs.summerschool.vision101;

import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.io.IOException;

import javax.swing.JPanel;

import org.openimaj.content.slideshow.Slide;
import org.openimaj.content.slideshow.SlideshowApplication;

import uk.ac.soton.ecs.summerschool.vision101.utils.VideoCaptureComponent;

/**
 * Slide showing simple video capture and display
 *
 * @author Jonathon Hare (jsh2@ecs.soton.ac.uk)
 *
 */
public class SimpleCameraDemo implements Slide {
	protected VideoCaptureComponent vc;
	private String devName;

	public SimpleCameraDemo(String devName) {
		this.devName = devName;
	}

	@Override
	public JPanel getComponent(int width, int height) throws IOException {
		// the main panel
		final JPanel base = new JPanel();
		base.setOpaque(false);
		base.setPreferredSize(new Dimension(width, height));
		base.setLayout(new GridBagLayout());
		vc = new VideoCaptureComponent(640, 480, devName);
		base.add(vc);

		return base;
	}

	@Override
	public void close() {
		vc.close();
	}

	public static void main(String[] args) throws IOException {
		new SlideshowApplication(new SimpleCameraDemo("FaceTime"), 1024, 768, App.getBackground());
	}
}
