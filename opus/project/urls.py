from django.conf.urls.defaults import *
from django.views.generic.simple import direct_to_template

import opus.project.projectbuilder.views
import opus.project.projectdeployer.views

# Uncomment the next two lines to enable the admin:
# from django.contrib import admin
# admin.autodiscover()

urlpatterns = patterns('opus.project',
    # Example:
    # (r'^project/', include('project.foo.urls')),

    # Uncomment the admin/doc line below and add 'django.contrib.admindocs' 
    # to INSTALLED_APPS to enable admin documentation:
    # (r'^admin/doc/', include('django.contrib.admindocs.urls')),

    # Uncomment the next line to enable the admin:
    # (r'^admin/', include(admin.site.urls)),

    (r'^create/', opus.project.projectbuilder.views.createproject),
    (r'^deploy/$', opus.project.projectdeployer.views.choose_project),
    (r'^deploy/step2', opus.project.projectdeployer.views.deploy_project),
)