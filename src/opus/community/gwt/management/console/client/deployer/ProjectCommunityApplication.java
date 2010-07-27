package opus.community.gwt.management.console.client.deployer;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;

public class ProjectCommunityApplication extends JavaScriptObject{
	protected ProjectCommunityApplication() {}
	
	public final native String getPath() /*-{ return this.path }-*/;
	public final native String getName() /*-{ return this.name + " " + this.version_number }-*/;
	public final native String getType() /*-{ return this.type }-*/;
}
