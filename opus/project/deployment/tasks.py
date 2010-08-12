##############################################################################
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
##############################################################################

from celery.decorators import task

from opus.project.deployment import models
from opus.lib.log import get_logger
log = get_logger()

@task(task_name='opus.project.tasks.destroy_project')
def destroy_project(projectid):
    project = models.DeployedProject.objects.get(pk=projectid)

    log.info("Running task to destroy project %s", project)
    project.destroy()
