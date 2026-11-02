package io.github.eliasnau.mc.client.core.screen;

import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/** A slider over {@code [min, max]} that reports every change and labels itself {@code "Name: value"}. */
public final class ConfigSlider extends AbstractSliderButton {
	private final Component name;
	private final float min;
	private final float max;
	private final Function<Float, Component> formatter;
	private final Consumer<Float> onChange;

	public ConfigSlider(
		final int x,
		final int y,
		final int width,
		final Component name,
		final float min,
		final float max,
		final float current,
		final Function<Float, Component> formatter,
		final Consumer<Float> onChange
	) {
		super(x, y, width, 20, Component.empty(), (current - min) / (max - min));
		this.name = name;
		this.min = min;
		this.max = max;
		this.formatter = formatter;
		this.onChange = onChange;
		this.updateMessage();
	}

	private float current() {
		return this.min + (this.max - this.min) * (float)this.value;
	}

	@Override
	protected void updateMessage() {
		this.setMessage(CommonComponents.optionNameValue(this.name, this.formatter.apply(this.current())));
	}

	@Override
	protected void applyValue() {
		this.onChange.accept(this.current());
	}
}
