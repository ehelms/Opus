package opus.gwt.management.console.client.event;

import opus.gwt.management.console.client.overlays.Application;
import opus.gwt.management.console.client.overlays.Project;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.event.shared.GwtEvent;


public class UpdateProjectEvent extends GwtEvent<UpdateProjectEventHandler> {
	
	public static Type<UpdateProjectEventHandler> TYPE = new Type<UpdateProjectEventHandler>();
	private JsArray<Project> project;
	
	public UpdateProjectEvent(JavaScriptObject projectInfo) {
		this.project = ConvertProjectInfo(projectInfo);
	}
	
  	@Override
  	public Type<UpdateProjectEventHandler> getAssociatedType() {
	  return TYPE;
  	}

  	@Override
  	protected void dispatch(UpdateProjectEventHandler handler) {
	  handler.onUpdateProjectInfo(this);
  	}
  	
  	public final native JsArray<Project> ConvertProjectInfo(JavaScriptObject jso) /*-{
		return jso;
	}-*/;
}