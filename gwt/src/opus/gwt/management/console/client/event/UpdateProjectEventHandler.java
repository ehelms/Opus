package opus.gwt.management.console.client.event;

import com.google.gwt.event.shared.EventHandler;

public interface UpdateProjectEventHandler extends EventHandler {
	void onUpdateProjectInfo(UpdateProjectEvent event);
}
