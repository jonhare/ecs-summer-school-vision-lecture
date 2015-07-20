package uk.ac.soton.ecs.summerschool.vision101;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.openimaj.content.slideshow.PictureSlide;
import org.openimaj.content.slideshow.Slide;
import org.openimaj.content.slideshow.SlideshowApplication;

/**
 * ECS Summer School Computer Vision Lecture
 */
public class App {
	public static void main(String[] args) throws IOException {
		final List<Slide> slides = new ArrayList<Slide>();

		for (int i = 1; i <= 44; i++)
			slides.add(new PictureSlide(App.class.getResource(String.format("slides.%03d.jpg", i))));

		new SlideshowApplication(slides, 1024, 768, null);
	}
}
