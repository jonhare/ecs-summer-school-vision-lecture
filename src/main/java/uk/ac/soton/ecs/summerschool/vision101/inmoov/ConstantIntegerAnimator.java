package uk.ac.soton.ecs.summerschool.vision101.inmoov;

import org.openimaj.content.animation.animator.AbstractValueAnimator;

public class ConstantIntegerAnimator extends AbstractValueAnimator<Integer> {
	private int duration = -1;
	private int count = 0;
	private int value;

	public ConstantIntegerAnimator(int initial) {
		super(initial, 0, 0);
		this.value = initial;
	}

	public ConstantIntegerAnimator(int initial, int duration) {
		super(initial, 0, 0);
		this.duration = duration;
		this.value = initial;
	}

	public ConstantIntegerAnimator(int initial, int startWait, int stopWait) {
		super(initial, startWait, stopWait);
		this.value = initial;
	}

	public ConstantIntegerAnimator(int initial, int startWait, int stopWait, int duration) {
		super(initial, startWait, stopWait);
		this.duration = duration;
		this.value = initial;
	}

	@Override
	protected Integer makeNextValue() {
		count++;
		return value;
	}

	@Override
	protected void resetToInitial() {
		// Do nothing
	}

	@Override
	protected boolean complete() {
		if (duration > 0 && count >= duration)
			return true;
		return false;
	}
}
