package me.imflowow.tritium.core.modules;

import tritium.api.module.Module;
import tritium.api.module.value.impl.BooleanValue;
import tritium.api.utils.event.api.EventTarget;
import tritium.api.utils.event.events.RenderNametagEvent;

public class EntityCulling extends Module {

	public BooleanValue hideNametags = new BooleanValue("HideNametags", true);

	public EntityCulling() {
		super("EntityCulling", "Skip rendering of entity nametags to reduce overdraw.");
		super.addValues(hideNametags);
	}

	@EventTarget
	public void onNametag(RenderNametagEvent event) {
		if (hideNametags.isEnabled()) {
			event.setCancelled(true);
		}
	}
}
