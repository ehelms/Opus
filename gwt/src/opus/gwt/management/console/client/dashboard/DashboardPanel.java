/*############################################################################
# Copyright 2010 North Carolina State University                             #
#                                                                            #
#   Licensed under the Apache License, Version 2.0 (the "License");          #
#   you may not use this file except in compliance with the License.         #
#   You may obtain a copy of the License at                                  #
#                                                                            #
#       http://www.apache.org/licenses/LICENSE-2.0                           #
#                                                                            #
#   Unless required by applicable law or agreed to in writing, software      #
#   distributed under the License is distributed on an "AS IS" BASIS,        #
#   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. #
#   See the License for the specific language governing permissions and      #
#   limitations under the License.                                           #
############################################################################*/

package opus.gwt.management.console.client.dashboard;

import java.util.HashMap;

import opus.gwt.management.console.client.ClientFactory;
import opus.gwt.management.console.client.JSVariableHandler;
import opus.gwt.management.console.client.deployer.ErrorPanel;
import opus.gwt.management.console.client.event.AsyncRequestEvent;
import opus.gwt.management.console.client.event.DeleteProjectEvent;
import opus.gwt.management.console.client.event.PanelTransitionEvent;
import opus.gwt.management.console.client.event.PanelTransitionEventHandler;
import opus.gwt.management.console.client.overlays.Application;
import opus.gwt.management.console.client.overlays.Project;
import opus.gwt.management.console.client.resources.ManagementConsoleControllerResources.ManagementConsoleControllerStyle;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
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
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.FormPanel.SubmitCompleteEvent;
import com.google.gwt.user.client.ui.FormPanel.SubmitEvent;
import com.google.gwt.user.client.ui.Hidden;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.Widget;

public class DashboardPanel extends Composite {

	private static DashboardUiBinder uiBinder = GWT.create(DashboardUiBinder.class);
	interface DashboardUiBinder extends UiBinder<Widget, DashboardPanel> {}
	
	private final String deleteProjectURL = "/deployments/projectName/destroy";
	
	private EventBus eventBus;
	private ClientFactory clientFactory;
	private HashMap<String, Application> applications;
	private JSVariableHandler JSVarHandler;
	private String projectName;
	private boolean active;
	private FormPanel deleteForm;
	
	@UiField FlowPanel applicationsFlowPanel;
	@UiField ManagementConsoleControllerStyle manager;
	@UiField Label projectLabel;
	@UiField Button activeButton;
	@UiField Button deleteButton;
	@UiField FlexTable formContainer;
	@UiField FormPanel optionsForm;
	@UiField PopupPanel deletePopupPanel;
	@UiField Button destroyButton;
	@UiField Button noThanksButton;
	@UiField FlowPanel deleteTitlePanel;
	
	public DashboardPanel(ClientFactory clientFactory) {
		initWidget(uiBinder.createAndBindUi(this));
		this.eventBus = clientFactory.getEventBus();
		this.clientFactory = clientFactory;
		this.JSVarHandler = clientFactory.getJSVariableHandler();
		this.applications = clientFactory.getApplications();
		activeButton.setText("");
		deleteForm = new FormPanel();
		registerHandlers();
		setDeletePopupPanelInitialState();
	}
	
	private void registerHandlers() {
		eventBus.addHandler(PanelTransitionEvent.TYPE, 
				new PanelTransitionEventHandler(){
					public void onPanelTransition(PanelTransitionEvent event){
						if(event.getTransitionType() == PanelTransitionEvent.TransitionTypes.DASHBOARD){
							projectName = event.name;
							projectLabel.setText(projectName);
							active = clientFactory.getProjects().get(projectName).isActive();
							handleProjectInformation(projectName);
						}
					}
			});
	}
	
	@UiHandler("activeButton")
	void onDeactivateButtonClick(ClickEvent event) {
		setProjectStatus(!active);
	}
	
	@UiHandler("deleteButton")
	void onDeleteButtonClick(ClickEvent event) {
		deletePopupPanel.setPopupPosition(Window.getClientWidth()/2 - 210, Window.getClientHeight()/2 - 150);
		deletePopupPanel.setGlassEnabled(true);
		deletePopupPanel.setGlassStyleName(manager.glassOverlay());
		deletePopupPanel.setAutoHideEnabled(true);
		deletePopupPanel.show();
	}
	
	@UiHandler("noThanksButton")
	void onNoThanksButtonClick(ClickEvent event) {
		deletePopupPanel.hide();
	}
	
	@UiHandler("destroyButton")
	void onDestroyButtonClick(ClickEvent event) {
		StringBuffer formBuilder = new StringBuffer();
		formBuilder.append("csrfmiddlewaretoken=");
		formBuilder.append( URL.encodeQueryString(JSVarHandler.getCSRFTokenURL()));
		
		RequestBuilder builder = new RequestBuilder(RequestBuilder.POST, "/deployments/" + projectName + "/destroy");
	    builder.setHeader("Content-type", "application/x-www-form-urlencoded");
	    
	    try {
		      Request request = builder.sendRequest(formBuilder.toString(), new RequestCallback() {
		        public void onError(Request request, Throwable exception) {
		        	ErrorPanel ep = new ErrorPanel(clientFactory);
		    		ep.errorHTML.setHTML("<p>Error Occured</p>");
		    		applicationsFlowPanel.add(ep);
		        }

		        public void onResponseReceived(Request request, Response response) {
			    	if(response.getText().contains("scheduled for destruction")){
			    		clientFactory.getProjects().remove(projectName);
			    		eventBus.fireEvent(new DeleteProjectEvent(projectName));
			    		deletePopupPanel.hide();
			    	} else {
			    	 	ErrorPanel ep = new ErrorPanel(clientFactory);
			    		ep.errorHTML.setHTML(response.getText());
			    		applicationsFlowPanel.add(ep);
			    	}
		        }});
		    } catch (RequestException e) {
		    	
		    }
	}

	public void handleProjectInformation(String projectName){
		applicationsFlowPanel.clear();
		
		final Project project = clientFactory.getProjects().get(projectName);
		HashMap<String, Application> applicationsMap = clientFactory.getApplications();
		JsArrayString applicationsArray = project.getApps();
		
		if(active) {
			activeButton.setText("Deactivate project");
		} else {
			activeButton.setText("Activate project");
		}
		
		for(int i = 0; i < applicationsArray.length() - 1; i++) {
			
			final Application app = applicationsMap.get(applicationsArray.get(i));
			final FlowPanel application = new FlowPanel();
			final FocusPanel applicationLabel = new FocusPanel();
			
			final Label appName = new Label(app.getName());
			final Label httpLabel = new Label("HTTP");
			final Label httpsLabel = new Label("HTTPS");
			final Label settingsLabel = new Label("Settings");
			
			Image appIcon = new Image();
			
			if(app.getIconURL().split("//").length < 2) {
				appIcon = new Image(JSVarHandler.getCommunityBaseURL() + app.getIconURL());
			} else {
				appIcon = new Image(app.getIconURL());
			}
			
			appIcon.setSize("64px", "64px");
			
			application.add(appIcon);
			application.add(appName);
			application.add(httpLabel);
			application.add(httpsLabel);
			application.add(settingsLabel);
			application.setStyleName(manager.appIcon());
			
			applicationLabel.add(application);

			applicationLabel.addMouseOverHandler(new MouseOverHandler() {
				public void onMouseOver(MouseOverEvent event){
					if(active) {
						applicationLabel.setStyleName(manager.appIconActive());
						appName.addStyleName(manager.text());
						httpLabel.addStyleName(manager.link());
						httpsLabel.addStyleName(manager.link());
						settingsLabel.addStyleName(manager.link());
					}
				}
			});
			applicationLabel.addMouseOutHandler(new MouseOutHandler() {
				public void onMouseOut(MouseOutEvent event){
					applicationLabel.setStyleName(manager.appIcon());
					appName.removeStyleName(manager.text());
					httpLabel.removeStyleName(manager.link());
					httpsLabel.removeStyleName(manager.link());
					settingsLabel.removeStyleName(manager.link());
				}
			});
			
			httpLabel.addClickHandler(new ClickHandler() {
				public void onClick(ClickEvent event) {
					if(active) {
						Window.Location.assign(project.getURLS().get(0) + app.getAppName());
					}
				}
			});
			
			httpsLabel.addClickHandler(new ClickHandler() {
				public void onClick(ClickEvent event) {
					if(active) {
						Window.Location.assign(project.getURLS().get(1) + app.getAppName());
					}
				}
			});
			
			settingsLabel.addClickHandler(new ClickHandler() {
				public void onClick(ClickEvent event) {
					if(active) {
						eventBus.fireEvent(new PanelTransitionEvent(PanelTransitionEvent.TransitionTypes.SETTINGS, app.getAppName()));
					}
				}
			});
			
			applicationsFlowPanel.add(applicationLabel);
		}

	}
	
	private void setProjectStatus(boolean active) {
		StringBuffer formBuilder = new StringBuffer();
		formBuilder.append("csrfmiddlewaretoken=");
		formBuilder.append( URL.encodeQueryString(JSVarHandler.getCSRFTokenURL()));
		formBuilder.append("&active=" + active);
		
	    RequestBuilder builder = new RequestBuilder(RequestBuilder.POST, "/deployments/" + projectName + "/");
		builder.setHeader("Content-type", "application/x-www-form-urlencoded");
		
		try {
			Request request = builder.sendRequest(formBuilder.toString(), new RequestCallback() {
				public void onError(Request request, Throwable exception) {
		        	ErrorPanel ep = new ErrorPanel(clientFactory);
		    		ep.errorHTML.setHTML("<p>Error Occured</p>");
		    		applicationsFlowPanel.clear();
		    		applicationsFlowPanel.add(ep);
		        }
				
				@Override
				public void onResponseReceived(Request request, Response response) {
					if(response.getText().contains("Project deactivated")) {
						setActive(false);
					} else if(response.getText().contains("Project activated")) {
						setActive(true);
					} else if(response.getText().contains("You asked me to activate the project")) {
						eventBus.fireEvent(new PanelTransitionEvent(PanelTransitionEvent.TransitionTypes.PROJECTSETTINGS, projectName));
					} else {
						ErrorPanel ep = new ErrorPanel(clientFactory);
			    		ep.errorHTML.setHTML(response.getText());
			    		applicationsFlowPanel.clear();
			    		applicationsFlowPanel.add(ep);
					}
				}
			});
		} catch (RequestException e) {
			e.printStackTrace();
		}
	}
	
	private void setActive(boolean active) {
		setActive(active, this.projectName);
	}
	
	private void setActive(boolean active, String projectName) {
		if(active) {
			activeButton.setText("Deactivate project");
		} else {
			activeButton.setText("Activate project");
		}
		
		this.active = active;
		handleProjectInformation(projectName);
	}
	
	private void setDeletePopupPanelInitialState() {
		deletePopupPanel.setVisible(false);
		deletePopupPanel.show();
		deletePopupPanel.setPopupPosition(-100, -100);
		deletePopupPanel.hide();
		deletePopupPanel.setVisible(true);
	}
}
