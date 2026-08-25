package me.imflowow.tritium.core.modules;

import me.imflowow.tritium.core.modules.components.informations.PerformanceEntity;
import tritium.api.module.Module;
import tritium.api.module.value.impl.BooleanValue;
import tritium.api.module.value.impl.ColorValue;
import tritium.api.module.value.impl.PositionValue;
import tritium.api.module.value.utils.HSBColor;
import tritium.api.module.value.utils.Position;
import tritium.api.module.value.utils.Position.Direction;

public class PerformanceMonitor extends Module {

	public ColorValue textColor = new ColorValue("TextColor", new HSBColor(255, 255, 255, 255));
	public ColorValue backgroundColor = new ColorValue("BackgroundColor", new HSBColor(0, 0, 0, 180));

	public PositionValue position = new PositionValue("PerformanceMonitor",
			new Position(10, 10, Direction.Left, Direction.Top));

	public BooleanValue showFPS = new BooleanValue("ShowFPS", true);
	public BooleanValue showMemory = new BooleanValue("ShowMemory", true);
	public BooleanValue showFrameTime = new BooleanValue("ShowFrameTime", true);

	private PerformanceEntity entity;

	public PerformanceMonitor() {
		super("PerformanceMonitor", "Display FPS / memory / frame time on your screen.");
		this.entity = new PerformanceEntity(this.position);
		super.addValues(textColor, backgroundColor, showFPS, showMemory, showFrameTime);
		super.addGuiEntities(this.entity);
	}

}
