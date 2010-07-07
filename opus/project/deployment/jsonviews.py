"""Views for the json interface to the data"""

import json

from django.http import HttpResponse
from django.contrib.auth.decorators import login_required
from django.core.urlresolvers import reverse
from django.conf import settings

from opus.project.deployment import models
from opus.project.deployment.views import get_project_object, debug_view

def render(struct, request):
    response = HttpResponse(mimetype="application/json")
    callback = request.GET.get("callback", None)
    if callback:
        response.write(callback+"(")
        json.dump(struct, response)
        response.write(")")
    else:
        json.dump(struct, response)
    return response

#@login_required
@debug_view
def projectlist(request):
    deployments = models.DeployedProject.objects.all()
    #if not request.user.is_superuser:
    #    deployments = deployments.filter(owner=request.user)

    ret = []
    for d in deployments:
        info = {}

        info['name'] = d.name
        info['owner'] = d.owner.username
        info['uri'] = reverse('opus.project.deployment.jsonviews.projectinfo',
                kwargs=dict(projectname=d.name))
        info['href'] = reverse('opus.project.deployment.views.edit_or_create',
                kwargs=dict(projectname=d.name))
        info['urls'] = d.get_urls()

        ret.append(info)

    return render(ret, request)

#@login_required
@get_project_object
@debug_view
def projectinfo(request, project):
    
    info = {}

    info['name'] = project.name
    info['owner'] = project.owner.username
    info['uri'] = reverse('opus.project.deployment.jsonviews.projectinfo',
            kwargs=dict(projectname=project.name))
    info['href'] = reverse('opus.project.deployment.views.edit_or_create',
            kwargs=dict(projectname=project.name))
    info['urls'] = project.get_urls()

    info['apps'] = []
    for app in project.config['INSTALLED_APPS']:
        if app.startswith(project.name + "."):
            info['apps'].append(app)

    database = project.config['DATABASES']['default']
    info['dbname'] = database['NAME']
    info['dbengine'] = database['ENGINE'].rsplit(".",1)[1]
    info['dbpassword'] = "12345" # Nobody would use this password
    info['dbhost'] = database['HOST']
    info['dbport'] = database['PORT']
    info['active'] = project.active

    return render(info, request)