package opus.gwt.management.console.client.overlays;


import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;

public class VersionData extends JavaScriptObject {                              // [1]
  // Overlay types always have protected, zero argument constructors.
  protected VersionData() {}                                              // [2]

  // JSNI methods to get stock data.
  public final native String getVersion() /*-{ return this.version; }-*/; // [3]
  public final native JsArray<DependencyData> getDependencies() /*-{ return this.dependencies; }-*/;
  public final native String getPath() /*-{ return this.path }-*/;
  public final native String getType() /*-{ return this.type }-*/;
  public final native String getVersionPk() /*-{ return this.pk; }-*/;
  public final native String getTag() /*-{ return this.tag; }-*/;
  public final native String getAppPk() /*-{ return this.app_pk }-*/;

}
