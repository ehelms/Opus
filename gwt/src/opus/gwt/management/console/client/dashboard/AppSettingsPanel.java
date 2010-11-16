package opus.gwt.management.console.client.dashboard;

import java.util.HashMap;

import opus.gwt.management.console.client.ClientFactory;
import opus.gwt.management.console.client.JSVariableHandler;
import opus.gwt.management.console.client.deployer.ErrorPanel;
import opus.gwt.management.console.client.event.AsyncRequestEvent;
import opus.gwt.management.console.client.event.PanelTransitionEvent;
import opus.gwt.management.console.client.event.PanelTransitionEventHandler;
import opus.gwt.management.console.client.overlays.Project;
import opus.gwt.management.console.client.overlays.ProjectSettingsData;
import opus.gwt.management.console.client.resources.FormsCss.FormsStyle;
import opus.gwt.management.console.client.tools.TooltipPanel;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.http.client.URL;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

public class AppSettingsPanel extends Composite {

	interface AppSettingsUiBinder extends UiBinder<Widget, AppSettingsPanel> {}
	private static AppSettingsUiBinder uiBinder = GWT.create(AppSettingsUiBinder.class);
	
	private final String optionsUrl = "/deployments/projectName/confapps/";
	
	private String projectName;
	private ClientFactory clientFactory;
	private JSVariableHandler jsVarHandler;
	private EventBus eventBus;
	private boolean active;
	private boolean hasSettings;
	private Project project;
	private TooltipPanel tooltip;
	private String appName;
	private HashMap<String, String> formData;

	@UiField Button saveButton;
	@UiField Button activateButton;
	@UiField Label projectLabel;
	@UiField FormsStyle form;
	@UiField FlowPanel content;


	public AppSettingsPanel(ClientFactory clientFactory) {
		initWidget(uiBinder.createAndBindUi(this));
		this.clientFactory = clientFactory;
		this.jsVarHandler = clientFactory.getJSVariableHandler();
		this.eventBus = clientFactory.getEventBus();
		this.formData = new HashMap<String, String>();
		this.projectName = projectName;
		registerHandlers();
		tooltip = new TooltipPanel();
		setTooltipInitialState();
	}
	
	private void registerHandlers() {
		eventBus.addHandler(PanelTransitionEvent.TYPE, 
				new PanelTransitionEventHandler(){
					public void onPanelTransition(PanelTransitionEvent event){
						if(event.getTransitionType() == PanelTransitionEvent.TransitionTypes.SETTINGS) {
							appName = event.name;
							projectLabel.setText(projectName + " settings: " + appName);
							importProjectSettings(project.getAppSettings(), appName);
						}
					}
			});
	}
	
	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}
	
	public void setProject(Project project) {
		this.project = project;
	}
	
	public void importProjectSettings(ProjectSettingsData settings, String application) {
		content.clear();
		content.setStyleName(form.content());
		
		JsArray<JavaScriptObject> appSettings = settings.getAppSettings(application);
		
		FlowPanel formWrapper = new FlowPanel();
		formWrapper.setStyleName(form.formWrapper());

		for(int j = 0; j < appSettings.length(); j++) {
			FlowPanel fields = new FlowPanel();
			FlowPanel fieldWrapper = new FlowPanel();
			fieldWrapper.setStyleName(form.fieldWrapper());
			fields.setStyleName(form.field());
			
			JsArray<JavaScriptObject> settingsArray = settings.getSettingsArray(appSettings.get(j));
			String choiceSettings = settings.getChoiceSettingsArray(appSettings.get(j));
			
			String[] settingsContent = settingsArray.join(";;").split(";;\\s*");
			//String[] choiceSettingsContent = choiceSettingsArray.join(";;").split(";;\\s*");
			
			Label appName = new Label(application);
			
			final Label description = new Label(settingsContent[0]);
			description.setTitle(settingsContent[0]);
			description.setStyleName(form.settingsFieldLabel());
			fields.add(description);
			
			if(settingsContent[2].equals("string") || settingsContent[2].equals("int")) {
				final TextBox setting = new TextBox();
				setting.setName(settingsContent[1]);
				setting.setStyleName(form.greyBorder());
				
				if(settingsContent.length > 3) {
					setting.setText(settingsContent[3]);
				}
				
				setting.addFocusHandler(new FocusHandler() {
					public void onFocus(FocusEvent event) {
						tooltip.hide();
						tooltip.setVisible(true);
						
						int x = getTooltipPosition(setting)[0];
						int y = getTooltipPosition(setting)[1];
							
						tooltip.setGray();
						setTooltipPosition(x, y);
						tooltip.show();
						setTooltipText(setting.getName());
					}
				});
				
				setting.addChangeHandler(new ChangeHandler() {
					public void onChange(ChangeEvent event) {
						formData.remove(description.getText());
						formData.put(description.getText(), setting.getText());
					}
				});
				
				formData.put(description.getText(), setting.getValue());
				fields.add(setting);
			} else if(settingsContent[2].equals("choice")) {
				final ListBox setting = new ListBox();
				setting.setName(settingsContent[1]);
				setting.setStyleName(form.greyBorder());
				setting.getElement().setInnerHTML(choiceSettings);
				
				setting.addChangeHandler(new ChangeHandler() {
					public void onChange(ChangeEvent event) {
						formData.remove(description.getText());
						formData.put(description.getText(), setting.getValue(setting.getSelectedIndex()));
					}
				});
				
				formData.put(description.getText(), setting.getValue(setting.getSelectedIndex()));
				fields.add(setting);
			} else if(settingsContent[2].equals("bool")) {
				final CheckBox setting = new CheckBox();
				setting.setName(settingsContent[1]);
				
				if (settingsContent.length > 3) {
					setting.setValue(Boolean.valueOf(settingsContent[3]));
				}
				
				setting.addClickHandler(new ClickHandler() {
					public void onClick(ClickEvent event) {
						formData.remove(description.getText());
						formData.put(description.getText(), setting.getValue().toString());
					}
				});
				
				formData.put(description.getText(), setting.getValue().toString());
				fields.add(setting);
			}

			fieldWrapper.add(fields);
			formWrapper.add(fieldWrapper);
		}
		
		content.add(formWrapper);
	}
	
	public void setHasSettings(boolean state) {
		this.hasSettings = state;
	}
	
	@UiHandler("saveButton")
	void handleSaveButton(ClickEvent event) {
		saveSettings();
	}
	
	@UiHandler("activateButton")
	void handleActivateButton(ClickEvent event){
		saveSettings();
	}
	
	private void saveSettings(){
		StringBuffer formBuilder = new StringBuffer();
	    formBuilder.append("csrfmiddlewaretoken=");
		formBuilder.append(URL.encodeQueryString(jsVarHandler.getCSRFTokenURL()));
		
		for(String key : formData.keySet()) {
			formBuilder.append("&" + projectName + "-" + key + "=" + formData.get(key));
		}
		
		RequestBuilder builder = new RequestBuilder(RequestBuilder.POST, "/deployments/" + projectName + "/confapps");
	    builder.setHeader("Content-type", "application/x-www-form-urlencoded");
		
	    try {
	    	Request request = builder.sendRequest(formBuilder.toString(), new RequestCallback() {
		        public void onError(Request request, Throwable exception) {
		        	Window.alert("ERORR SENDING FORM WITH REQUEST BUILDER");
		        }

		        public void onResponseReceived(Request request, Response response) {
			    	if(response.getText().contains("Settings saved")) {
			    		formData = new HashMap<String, String>();
			    	    eventBus.fireEvent(new AsyncRequestEvent("updateProject", projectName));
			    		eventBus.fireEvent(new PanelTransitionEvent(PanelTransitionEvent.TransitionTypes.DASHBOARD, projectName));
			    	} else {
			    		ErrorPanel ep = new ErrorPanel(clientFactory);
			    		ep.errorHTML.setHTML(response.getText());
			    		content.clear();
			    		content.add(ep);
			    	}
		        }
	    	});
	    } catch (RequestException e) {
	    	
	    }
	}
	
	/**
	 * Set the tooltips initial state on page load
	 */
	private void setTooltipInitialState() {
		tooltip.setVisible(false);
	}
	
	/**
	 * Set the position of a tooltip relative to the browser window
	 * @param x the x coordinate
	 * @param y the y coordinate
	 */
	private void setTooltipPosition(int x, int y) {
		tooltip.setPopupPosition(x, y);
	}
	
	/**
	 * Set the text of a tooltip
	 * @param text the text to set
	 */
	private void setTooltipText(String text) {
		tooltip.hide();
		tooltip.setText(text);
		tooltip.show();
	}
	
	/**
	 * Return the tooltip position as an array in for them [x, y]
	 * @param textbox the textbox to get the position of
	 * @return tooltip position
	 */
	private int[] getTooltipPosition(TextBox textbox) {
		int[] pos = new int[2];
		
		pos[0] = textbox.getAbsoluteLeft() + textbox.getOffsetWidth() + 5;
		pos[1] = textbox.getAbsoluteTop() + 2;
		
		return pos;
	}
	
	/**
	 * Return the tooltip position as an array in for them [x, y]
	 * @param textbox the textbox to get the position of
	 * @return tooltip position
	 */
	private int[] getTooltipPosition(PasswordTextBox textbox) {
		int[] pos = new int[2];
		
		pos[0] = textbox.getAbsoluteLeft() + textbox.getOffsetWidth() + 5;
		pos[1] = textbox.getAbsoluteTop() + 2;
	
		return pos;
	}
}
