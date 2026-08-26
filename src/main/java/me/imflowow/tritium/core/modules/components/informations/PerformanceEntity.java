package me.imflowow.tritium.core.modules.components.informations;

import java.util.ArrayList;
import java.util.List;

import me.imflowow.tritium.core.Tritium;
import me.imflowow.tritium.core.modules.PerformanceMonitor;
import tritium.api.module.gui.GuiEntity;
import tritium.api.module.value.impl.PositionValue;
import tritium.api.utils.StringUtils;
import tritium.api.utils.render.Rect;
import me.imflowow.tritium.utils.language.LangUtils.SizeType;

public class PerformanceEntity extends GuiEntity {

	private final List<String> lines = new ArrayList<>();

	public PerformanceEntity(PositionValue position) {
		super(position);
	}

	@Override
	public void init() {
	}

	@Override
	public void draw(double x, double y) {
		final PerformanceMonitor module = (PerformanceMonitor) Tritium.instance.getModuleManager()
				.getModule(PerformanceMonitor.class);
		if (module == null) {
			return;
		}
		lines.clear();
		final int fps = mc.getDebugFPS();
		if (module.showFPS.isEnabled()) {
			lines.add(fps + " FPS");
		}
		if (module.showFrameTime.isEnabled()) {
			lines.add(String.format("%.1f ms", 1000.0 / Math.max(1, fps)));
		}
		if (module.showMemory.isEnabled()) {
			final Runtime rt = Runtime.getRuntime();
			final long used = (rt.totalMemory() - rt.freeMemory()) / 1048576L;
			final long total = rt.totalMemory() / 1048576L;
			lines.add(used + " / " + total + " MB");
		}
		if (lines.isEmpty()) {
			return;
		}
		int maxWidth = 0;
		for (final String line : lines) {
			maxWidth = Math.max(maxWidth, StringUtils.getWidth(line, SizeType.Size16));
		}
		final int w = maxWidth + 3;
		final int h = lines.size() * 13;
		new Rect(x, y, w, h, module.backgroundColor.getValue().getColor().getRGB(), Rect.RenderType.Expand).draw();
		double cy = y + 5;
		for (final String line : lines) {
			StringUtils.drawStringWithShadow(line, x + 1.5, cy, module.textColor.getValue().getColor().getRGB(),
					SizeType.Size16);
			cy += 13;
		}
	}

	@Override
	public int getHeight() {
		return Math.max(13, lines.size() * 13);
	}

	@Override
	public int getWidth() {
		int maxWidth = 0;
		for (final String line : lines) {
			maxWidth = Math.max(maxWidth, StringUtils.getWidth(line, SizeType.Size16));
		}
		return maxWidth + 3;
	}

	public PerformanceMonitor getModule() {
		return (PerformanceMonitor) Tritium.instance.getModuleManager().getModule(PerformanceMonitor.class);
	}
}
