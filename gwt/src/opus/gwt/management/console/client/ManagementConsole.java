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

package opus.gwt.management.console.client;

import opus.gwt.management.console.client.dashboard.IconPanel;
import opus.gwt.management.console.client.dashboard.ProjectManagerController;
import opus.gwt.management.console.client.deployer.ProjectDeployerController;
import opus.gwt.management.console.client.event.BreadCrumbEvent;
import opus.gwt.management.console.client.event.PanelTransitionEvent;
import opus.gwt.management.console.client.event.PanelTransitionEventHandler;
import opus.gwt.management.console.client.navigation.BreadCrumbsPanel;
import opus.gwt.management.console.client.navigation.NavigationPanel;
import opus.gwt.management.console.client.resources.ManagementConsoleControllerResources.ManagementConsoleControllerStyle;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.user.client.ui.Widget;


public class ManagementConsole extends Composite {	

	private static ManagementConsoleUiBinder uiBinder = GWT.create(ManagementConsoleUiBinder.class);
	interface ManagementConsoleUiBinder extends UiBinder<Widget, ManagementConsole> {}
	
	private EventBus eventBus;
	private ClientFactory clientFactory;
	private JSVariableHandler jsVarHandler;
	private ProjectDeployerController projectDeployerController; 
	private ProjectManagerController projectManagerController;
	private IconPanel iconPanel;
	
	@UiField LayoutPanel contentLayoutPanel;
	@UiField(provided = true) NavigationPanel navigationPanel;
	@UiField(provided = true) BreadCrumbsPanel breadCrumbsPanel;
	@UiField ManagementConsoleControllerStyle style;
	
	public ManagementConsole(ClientFactory clientFactory) {
		this.clientFactory = clientFactory;
		this.jsVarHandler = clientFactory.getJSVariableHandler();
		this.eventBus = clientFactory.getEventBus();
		navigationPanel = new NavigationPanel(clientFactory);
		breadCrumbsPanel = new BreadCrumbsPanel(clientFactory);
		initWidget(uiBinder.createAndBindUi(this));
		RootLayoutPanel.get().add(this);
		RootLayoutPanel.get().setStyleName(style.rootLayoutPanel());
		projectDeployerController = new ProjectDeployerController(clientFactory);
		projectManagerController = new ProjectManagerController(clientFactory);
		iconPanel = new IconPanel(clientFactory);
		registerHandlers();
		startConsole();
	}
	
	private void registerHandlers(){
		eventBus.addHandler(PanelTransitionEvent.TYPE, 
			new PanelTransitionEventHandler(){
				public void onPanelTransition(PanelTransitionEvent event){
					if( event.getTransitionType() == PanelTransitionEvent.TransitionTypes.DEPLOY ){
						showDeployer();
					} else if( event.getTransitionType() == PanelTransitionEvent.TransitionTypes.PROJECTS ){
						showIconPanel();
					} else if( event.getTransitionType() == PanelTransitionEvent.TransitionTypes.DASHBOARD ){
						manageProjects(event.getName());
					}
				}
		});
	}
	
	private void startConsole(){
		if( clientFactory.getProjects().size() > 0 ){
			if( jsVarHandler.getProjectToken() != null ){
				showDeployer();
			} else {
				showIconPanel();
				eventBus.fireEvent(new PanelTransitionEvent(PanelTransitionEvent.TransitionTypes.PROJECTS));
			}
		} else {
			showDeployer();
		}
	}
	
	private void showDeployer(){
		contentLayoutPanel.clear();
		contentLayoutPanel.add(projectDeployerController);
		contentLayoutPanel.setVisible(true);
	}
	
	private void manageProjects(String projectName){
		contentLayoutPanel.clear();
		contentLayoutPanel.add(projectManagerController);
		contentLayoutPanel.setVisible(true);
	}
	
	private void showIconPanel(){
		contentLayoutPanel.clear();
		contentLayoutPanel.add(iconPanel);
		contentLayoutPanel.setVisible(true);
	}
}
