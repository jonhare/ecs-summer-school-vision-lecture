package uk.ac.soton.ecs.summerschool.vision101.inmoov;

import java.io.Closeable;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import jssc.SerialPort;

import org.openimaj.hardware.serial.SerialDevice;

public class ServoController implements Runnable, Closeable {
	public static final int FREQUENCY = 100; // aim for 100Hz
	private transient Map<Servo, Integer> servos = new HashMap<Servo, Integer>();
	protected transient boolean isRunning = true;
	private SerialDevice device;
	private Thread thread;

	public ServoController() throws Exception {
		this(findSerialPort());
	}

	private static String findSerialPort() {
		final String[] files = new File("/dev").list(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.startsWith("tty.usbmodem");
			}
		});

		return "/dev/" + files[0];
	}

	public ServoController(String port) throws Exception {
		device = new SerialDevice(port,
				115200,
				SerialPort.DATABITS_8,
				SerialPort.STOPBITS_1,
				SerialPort.PARITY_NONE);

		thread = new Thread(this);
		thread.setDaemon(true);
		thread.start();
	}

	public void registerServo(Servo servo) {
		final int init = servo.getInitialPW();
		servos.put(servo, init);
		setServo(servo.getAddress(), init);
	}

	private void setServo(int address, int pw) {
		try {
			// System.out.println(String.format("%d %d\n", address, pw));
			final byte[] cmd = String.format("%d %d\n", address, pw).getBytes("US-ASCII");
			device.getOutputStream().write(cmd);
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		final int allowedTime = 1000 / FREQUENCY;

		while (isRunning) {
			final long t1 = System.currentTimeMillis();
			for (final Entry<Servo, Integer> entry : servos.entrySet()) {
				final Servo servo = entry.getKey();
				final int lastPW = entry.getValue();
				final int newPW = servo.nextPW();
				if (lastPW != newPW) {
					entry.setValue(newPW);
					setServo(servo.getAddress(), newPW);
				}
			}
			final long t2 = System.currentTimeMillis();

			final long sleepTime = allowedTime - (t2 - t1);

			if (sleepTime > 0) {
				try {
					Thread.sleep(sleepTime);
				} catch (final InterruptedException e) {
					e.printStackTrace();
				}
			} else {
				System.err.println("Sleeptime = " + sleepTime);
			}
		}

		// close the device on stop
		try {
			device.close();
		} catch (final IOException e) {
		}
	}

	@Override
	public void close() {
		isRunning = false;
	}
}
