package uk.ac.soton.ecs.summerschool.vision101.inmoov;

import org.openimaj.content.animation.animator.ValueAnimator;

public class Servo {
	private int address;
	private int minPW = 544;
	private int maxPW = 2400;
	private int initialPW;
	private int pw;
	transient ValueAnimator<Integer> animator;

	public Servo(int address, int initialPW) {
		super();
		this.address = address;
		this.initialPW = initialPW;
	}

	public int getInitialPW() {
		return initialPW;
	}

	public int getAddress() {
		return address;
	}

	int nextPW() {
		if (animator != null)
			pw = animator.nextValue();
		else
			pw = initialPW;

		return pw;
	}

	public void setPW(int newPW) {
		if (newPW < minPW)
			newPW = minPW;
		if (newPW > maxPW)
			newPW = maxPW;

		animator = new ConstantIntegerAnimator(newPW);
	}

	public void setOff() {
		animator = new ConstantIntegerAnimator(0);
	}

	public void changePWRelative(int r) {
		final int newPW = pw + r;
		setPW(newPW);
	}

	public void increment() {
		increment(1);
	}

	public void increment(int i) {
		setPW(pw + i);
	}

	public void decrement() {
		decrement(1);
	}

	public void decrement(int i) {
		setPW(pw - i);
	}

	public int getPW() {
		return pw;
	}

	protected int randomAbsoluteValue() {
		return (int) ((Math.random() * (maxPW - minPW)) + minPW);
	}

	public void moveToRandom() {
		setPW(randomAbsoluteValue());
	}
}
