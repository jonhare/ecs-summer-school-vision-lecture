package uk.ac.soton.ecs.summerschool.vision101;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.openimaj.content.slideshow.PictureSlide;
import org.openimaj.content.slideshow.Slide;
import org.openimaj.content.slideshow.SlideshowApplication;
import org.openimaj.content.slideshow.VideoSlide;
import org.openimaj.video.VideoDisplay.EndAction;

/**
 * ECS Summer School Computer Vision Lecture
 */
public class App {
	private static int SLIDE_WIDTH = 1024;
	private static int SLIDE_HEIGHT = 768;
	private static BufferedImage background = null;

	/**
	 * Main method
	 *
	 * @param args
	 *            ignored
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		final List<Slide> slides = new ArrayList<Slide>();

		for (int i = 1; i <= 5; i++)
			slides.add(new PictureSlide(App.class.getResource(String.format("slides/slides.%03d.jpg", i))));

		slides.add(new VideoSlide(App.class.getResource("grader.mp4"), App.class.getResource("slides/slides.006.jpg"),
				EndAction.PAUSE_AT_END));

		slides.add(new PictureSlide(App.class.getResource("slides/slides.007.jpg")));

		slides.add(new VideoSlide(App.class.getResource("cars.mp4"), App.class.getResource("slides/slides.008.jpg"),
				EndAction.PAUSE_AT_END));

		slides.add(new InmoovDemo()); // 9

		slides.add(new PictureSlide(App.class.getResource("slides/slides.010.jpg")));

		slides.add(new ArtARDemo()); // 11

		for (int i = 12; i <= 26; i++)
			slides.add(new PictureSlide(App.class.getResource(String.format("slides/slides.%03d.jpg", i))));

		slides.add(new SimpleCameraDemo()); // 27

		for (int i = 28; i <= 29; i++)
			slides.add(new PictureSlide(App.class.getResource(String.format("slides/slides.%03d.jpg", i))));

		slides.add(new SimpleMeanColourFeatureDemo()); // 30
		slides.add(new StickyFeaturesDemo()); // 31
		slides.add(new PDMDemo()); // EXTRA
		slides.add(new CLMDemo()); // 32
		slides.add(new PuppeteerDemo()); // 33

		for (int i = 34; i <= 39; i++)
			slides.add(new PictureSlide(App.class.getResource(String.format("slides/slides.%03d.jpg", i))));

		slides.add(new LinearClassifierDemo()); // 40

		for (int i = 41; i <= 47; i++)
			slides.add(new PictureSlide(App.class.getResource(String.format("slides/slides.%03d.jpg", i))));

		new SlideshowApplication(slides, 1024, 768, getBackground());
	}

	/**
	 * @return the slide background
	 */
	public synchronized static BufferedImage getBackground() {
		if (background == null) {
			background = new BufferedImage(SLIDE_WIDTH, SLIDE_HEIGHT, BufferedImage.TYPE_3BYTE_BGR);
			final Graphics2D g = background.createGraphics();
			g.setColor(Color.WHITE);
			g.fillRect(0, 0, background.getWidth(), background.getHeight());
		}
		return background;
	}
}
