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

package opus.gwt.management.console.client.deployer;

import java.util.ArrayList;

import opus.gwt.management.console.client.ClientFactory;
import opus.gwt.management.console.client.JSVariableHandler;
import opus.gwt.management.console.client.event.AsyncRequestEvent;
import opus.gwt.management.console.client.event.BreadCrumbEvent;
import opus.gwt.management.console.client.event.DeployProjectEvent;
import opus.gwt.management.console.client.event.DeployProjectEventHandler;
import opus.gwt.management.console.client.event.PanelTransitionEvent;
import opus.gwt.management.console.client.event.PanelTransitionEventHandler;
import opus.gwt.management.console.client.resources.ProjectDeployerCss.ProjectDeployerStyle;
import opus.gwt.management.console.client.resources.images.OpusImages;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.http.client.URL;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DeckPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.Widget;


public class ProjectDeployerController extends Composite {
	
	private static applicationDeployerUiBinder uiBinder = GWT.create(applicationDeployerUiBinder.class);
	interface applicationDeployerUiBinder extends UiBinder<Widget, ProjectDeployerController> {}
		
	private final String deploymentURL =  "/deployments/projectName/";
		
	private ProjectOptionsPanel projectOptionsPanel;
	private DatabaseOptionsPanel databaseOptionsPanel;
	private DeploymentOptionsPanel deploymentOptionsPanel;
	private AppBrowserPanel appBrowserPanel;
	private EventBus eventBus;
	private String createdProjectName;
	private PopupPanel loadingPopup;
	private JSVariableHandler jsVarHandler;
	private ClientFactory clientFactory;
	private int	currentPanelIndex;
		
	@UiField DeckPanel deployerDeckPanel;
	@UiField ProjectDeployerStyle style;
	
	public ProjectDeployerController(ClientFactory clientFactory) {
		initWidget(uiBinder.createAndBindUi(this));
		this.createdProjectName = "";
		this.currentPanelIndex = 0;
		this.eventBus = clientFactory.getEventBus();
		this.clientFactory = clientFactory;
		this.jsVarHandler = clientFactory.getJSVariableHandler();
		this.loadingPopup = new PopupPanel(false, true);
		this.appBrowserPanel = new AppBrowserPanel(clientFactory);
		this.projectOptionsPanel = new ProjectOptionsPanel(clientFactory);
		this.databaseOptionsPanel = new DatabaseOptionsPanel(clientFactory);
		this.deploymentOptionsPanel = new DeploymentOptionsPanel(clientFactory);
		setupLoadingPopup();
		setupDeployerDeckPanel();
		registerHandlers();
		setupBreadCrumbs();
		String token = jsVarHandler.getProjectToken();
		if (token != null) {
			eventBus.fireEvent(new AsyncRequestEvent("handleImportAppList", token));
		}
	}
	
	private void setupDeployerDeckPanel(){
		deployerDeckPanel.add(appBrowserPanel);
		deployerDeckPanel.add(projectOptionsPanel);
		deployerDeckPanel.add(databaseOptionsPanel);
		deployerDeckPanel.add(deploymentOptionsPanel);
		appBrowserPanel.setTitle("Application Browser");
		projectOptionsPanel.setTitle("Project Options");
		databaseOptionsPanel.setTitle("Database Options");
		deploymentOptionsPanel.setTitle("Deployment Options");
		deployerDeckPanel.showWidget(0);
		appBrowserPanel.setHeight("");
		appBrowserPanel.setWidth("");
	}
	
	private void setupBreadCrumbs(){
		String[] crumbs = {appBrowserPanel.getTitle(), projectOptionsPanel.getTitle(), databaseOptionsPanel.getTitle(), deploymentOptionsPanel.getTitle()};
		eventBus.fireEvent(new BreadCrumbEvent(BreadCrumbEvent.Action.SET_CRUMBS, crumbs));
	}
	
	private void registerHandlers(){
		eventBus.addHandler(PanelTransitionEvent.TYPE, 
			new PanelTransitionEventHandler(){
				public void onPanelTransition(PanelTransitionEvent event){
					if( event.getTransitionType() == PanelTransitionEvent.TransitionTypes.NEXT ){
						currentPanelIndex++;
						Widget panel =  event.getPanel();
						deployerDeckPanel.showWidget(currentPanelIndex);
						eventBus.fireEvent(new BreadCrumbEvent(BreadCrumbEvent.Action.SET_ACTIVE, deployerDeckPanel.getWidget(deployerDeckPanel.getVisibleWidget()).getTitle()));
						setFocus(panel);
					} else if( event.getTransitionType() == PanelTransitionEvent.TransitionTypes.PREVIOUS ){
						currentPanelIndex--;
						deployerDeckPanel.showWidget(currentPanelIndex);
						eventBus.fireEvent(new BreadCrumbEvent(BreadCrumbEvent.Action.SET_ACTIVE, deployerDeckPanel.getWidget(deployerDeckPanel.getVisibleWidget()).getTitle()));
					}
					if( event.getTransitionType() == PanelTransitionEvent.TransitionTypes.DEPLOY ){
						reset();
					}
		}});
		eventBus.addHandler(DeployProjectEvent.TYPE, 
				new DeployProjectEventHandler(){
					public void onDeployProject(DeployProjectEvent event){
						deployProject();
					}
		});
	}
	
	private void reset(){
		projectOptionsPanel.reset();
		appBrowserPanel.reset();
		databaseOptionsPanel.reset();
		deploymentOptionsPanel.reset();
		setupBreadCrumbs();
		deployerDeckPanel.showWidget(0);
		currentPanelIndex = 0;
	}
	
	private void setupLoadingPopup(){
		FlowPanel fp = new FlowPanel();
		Label label = new Label("Deploying Project...");
		Image loadingImage = new Image(OpusImages.INSTANCE.loadingSpinner());
		loadingImage.setStyleName(style.loadingImage());
		label.setStyleName(style.loadingLabel());
		fp.add(loadingImage);
		fp.add(label);
		loadingPopup.add(fp);	
	}

	public void setFocus(Widget panel){
		if( panel.getClass().equals(databaseOptionsPanel.getClass()) ){
			deploymentOptionsPanel.setFocus();
		} else if( panel.getClass().equals(appBrowserPanel.getClass()) ){
			projectOptionsPanel.setFocus();
		} else if( panel.getClass().equals(projectOptionsPanel.getClass()) ){
			databaseOptionsPanel.setFocus();
		}
	}
	 
	private void deployProject(){
		createdProjectName = deploymentOptionsPanel.getProjectName();
		
		ArrayList<String> paths = appBrowserPanel.getAppPaths();
		ArrayList<String> apptypes = appBrowserPanel.getAppTypes();
		ArrayList<String> appNames = appBrowserPanel.getAppNames();
		
		StringBuffer formBuilder = new StringBuffer();
		formBuilder.append("csrfmiddlewaretoken=");
		formBuilder.append( URL.encodeQueryString(jsVarHandler.getCSRFTokenURL()));
		
		formBuilder.append("&form-TOTAL_FORMS=");
		formBuilder.append( URL.encodeQueryString(String.valueOf(paths.size())));
		formBuilder.append("&form-INITIAL_FORMS=");
		formBuilder.append( URL.encodeQueryString(String.valueOf(0)));
		formBuilder.append("&form-MAX_NUM_FORMS=");
		
		for(int i=0; i < paths.size(); i++) {
			formBuilder.append("&form-" + i + "-apptype=");
			formBuilder.append(apptypes.get(i));

			formBuilder.append("&form-" + i + "-apppath=");
			formBuilder.append(paths.get(i));

			formBuilder.append("&form-" + i + "-appname=");
			formBuilder.append(appNames.get(i));
		}
		
		formBuilder.append(deploymentOptionsPanel.getPostData());
		formBuilder.append(projectOptionsPanel.getPostData());
		formBuilder.append(databaseOptionsPanel.getPostData());
		
	    RequestBuilder builder = new RequestBuilder(RequestBuilder.POST, "/deployments/" + createdProjectName + "/");
	    builder.setHeader("Content-type", "application/x-www-form-urlencoded");
	    //builder.setHeader(header, value);
	    
	    try {
	      Request request = builder.sendRequest(formBuilder.toString(), new RequestCallback() {
	        public void onError(Request request, Throwable exception) {
	        	ErrorPanel ep = new ErrorPanel(clientFactory);
	    		ep.errorHTML.setHTML("<p>Error Occured</p>");
	    		deployerDeckPanel.add(ep);
	    		deployerDeckPanel.showWidget(deployerDeckPanel.getWidgetIndex(ep));
	        }

	        public void onResponseReceived(Request request, Response response) {
		    	if( response.getText().contains("Back to") ){
		    		loadingPopup.hide();
		    		eventBus.fireEvent(new AsyncRequestEvent("addProject", createdProjectName));
		    	} else {
		    		loadingPopup.hide();
		    	 	ErrorPanel ep = new ErrorPanel(clientFactory);
		    		ep.errorHTML.setHTML(response.getText());
		    		deployerDeckPanel.add(ep);
		    		deployerDeckPanel.showWidget(deployerDeckPanel.getWidgetIndex(ep));
		    	}
	        }});
	    } catch (RequestException e) {
	    	
	    }
		
		loadingPopup.setGlassEnabled(true);
		loadingPopup.setGlassStyleName(style.loadingGlass());
		loadingPopup.show();
		int left = ( Window.getClientWidth() / 2 ) - 150;
		int top = ( Window.getClientHeight() / 2) - 10;
		loadingPopup.setPopupPosition(left, top);
		loadingPopup.show();
	}
}
