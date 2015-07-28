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
