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

import opus.gwt.management.console.client.ClientFactory;
import opus.gwt.management.console.client.event.BreadCrumbEvent;
import opus.gwt.management.console.client.event.PanelTransitionEvent;
import opus.gwt.management.console.client.event.PanelTransitionEventHandler;
import opus.gwt.management.console.client.resources.ProjectManagerCss.ProjectManagerStyle;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DeckPanel;
import com.google.gwt.user.client.ui.Widget;

public class ProjectManagerController extends Composite {

	private static ProjectDashboardUiBinder uiBinder = GWT.create(ProjectDashboardUiBinder.class);
	interface ProjectDashboardUiBinder extends UiBinder<Widget, ProjectManagerController> {}

	private DashboardPanel dashboardPanel;
	private ProjectSettingsPanel projectSettingsPanel;
	private AppSettingsPanel appSettingsPanel;
	private EventBus eventBus;
	private String projectName;
	private ClientFactory clientFactory;

	@UiField ProjectManagerStyle style;
	@UiField DeckPanel managerDeckPanel;
	
	public ProjectManagerController(ClientFactory clientFactory){
		initWidget(uiBinder.createAndBindUi(this));
		this.clientFactory = clientFactory;
		this.eventBus = clientFactory.getEventBus();
		this.dashboardPanel = new DashboardPanel(clientFactory);
		this.appSettingsPanel = new AppSettingsPanel(clientFactory);
		this.projectSettingsPanel = new ProjectSettingsPanel(clientFactory);
		setupBreadCrumbs();
		setupmanagerDeckPanel();
		registerHandlers();
	}
	
	private void setupBreadCrumbs(){
		String[] crumbs = {"Projects", projectName, dashboardPanel.getTitle()};
		eventBus.fireEvent(new BreadCrumbEvent(BreadCrumbEvent.Action.SET_CRUMBS, crumbs));
		eventBus.fireEvent(new BreadCrumbEvent(BreadCrumbEvent.Action.SET_ACTIVE, projectName));
	}
	
	private void registerHandlers(){
		eventBus.addHandler(PanelTransitionEvent.TYPE, 
				new PanelTransitionEventHandler(){
					public void onPanelTransition(PanelTransitionEvent event){
						if( event.getTransitionType() == PanelTransitionEvent.TransitionTypes.SETTINGS ){
							managerDeckPanel.showWidget(managerDeckPanel.getWidgetIndex(appSettingsPanel));
						} else if( event.getTransitionType() == PanelTransitionEvent.TransitionTypes.DASHBOARD ){
							projectName = event.name;
							managerDeckPanel.showWidget(0);
						} else if(event.getTransitionType() == PanelTransitionEvent.TransitionTypes.PROJECTSETTINGS) {
							managerDeckPanel.showWidget(managerDeckPanel.getWidgetIndex(projectSettingsPanel));
						}
					}
			});
	}
	
	private void setupmanagerDeckPanel(){
		managerDeckPanel.add(dashboardPanel);
		dashboardPanel.setTitle("Dashboard");
		managerDeckPanel.add(appSettingsPanel);
		appSettingsPanel.setTitle("Application Settings");
		managerDeckPanel.add(projectSettingsPanel);
		projectSettingsPanel.setTitle("Project Settings");
		managerDeckPanel.showWidget(0);
	}
}
