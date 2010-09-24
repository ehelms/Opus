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

from django.core.urlresolvers import reverse
from django.http import HttpRequest, HttpResponse, HttpResponseBadRequest, HttpResponseNotFound
from django.shortcuts import render_to_response
from django.core.exceptions import ObjectDoesNotExist
from django.views.decorators.csrf import csrf_exempt

from collections import namedtuple

import opus.lib.log
log = opus.lib.log.get_logger()
from opus.project.dcmux.models import Provider, Policy, AggregateImage, RealImage, Instance

HardwareProfile = namedtuple("HardwareProfile", "id")

def uri_lookup(request, resource):
    return request.build_absolute_uri(
        reverse("opus.project.dcmux.%s" % resource)
    )

def primary_entry_point(request):
    """The entry point which lists the urls for all resources."""

    resource_names = [
        "primary_entry_point",
        "hardware_profiles",
        "realms",
        "images",
        "instances",
    ]
    Link = namedtuple("Link", "resource uri")
    links = []
    for resource in resource_names:
        links.append(Link(resource, uri_lookup(request, resource)))

    return render_to_response(
        'dcmux/primary_entry.xml',
        {"links": links},
        mimetype="application/xml",
    )

def hardware_profile_list(request, name=None):
    """Lists all hardware profiles, or the given one if id!=None.

    In dcmux, there is one hardware profile.  The real hardware profile
    that will be used is specified in the RealImage.

    """

    if name == None or name == "generic":
        hardware_profiles = [HardwareProfile("generic")]
    else:
        hardware_profiles = []

    return render_to_response(
            'dcmux/hardware_profiles.xml',
            {
                "hardware_profiles_uri": uri_lookup(request, "hardware_profiles"),
                "hardware_profiles": hardware_profiles,
            },
            mimetype="application/xml",
    )

def realm_list(request, id=None):
    """List all realms, or the given realm if id!=None."""

    if id == None:
        realms = Policy.objects.all()
    else:
        try:
            realms = [Policy.objects.get(id=id)]
        except ValueError:
            # id wasn't an int, which it should be
            realms = []
        except ObjectDoesNotExist:
            # id not found in database
            realms = []

    return render_to_response(
        'dcmux/realms.xml',
        {
            "realms_uri": uri_lookup(request, "realms"),
            "realms": realms,
        },
        mimetype="application/xml",
    )

def image_list(request, id=None):
    """List all downstream images, or the given image if id!=None."""

    if id == None:
        images = AggregateImage.objects.all()
    else:
        try:
            images = [AggregateImage.objects.get(id=id)]
        except ValueError:
            # id wasn't an int, which it should be
            images = []
        except ObjectDoesNotExist:
            # id not found in database
            images = []

    return render_to_response(
        'dcmux/images.xml',
        {
            "images_uri": uri_lookup(request, "images"),
            "images": images,
        },
        mimetype="application/xml",
    )

@csrf_exempt
def instances_list(request):
    """Lists all instances (GET) or creates an instance (POST).

    See the instance_list and instance_create function for more information on
    this.

    """

    if request.method.startswith("POST"):
        return instance_create(request)
    else:
        return instance_list(request)

def instance_list(request, id=None):
    """List all instances, or the given instance if id!=None."""

    if id == None:
        instances = Instance.objects.all()
    else:
        try:
            instances = [Instance.objects.get(id=id)]
        except ValueError:
            # id wasn't an int, which it should be
            instances = []
        except ObjectDoesNotExist:
            # id not found in database
            instances = []

    return render_to_response(
        'dcmux/instances.xml',
        {
            "images_uri": uri_lookup(request, "images"),
            "instances_uri": uri_lookup(request, "instances"),
            "instances": instances,
        },
        mimetype="application/xml",
    )

def instance_create(request):
    """Creates an instance with the policy given by realm_id.

    Uses image_id and realm_id from the application/x-www-form-urlencoded
    format.  Both of these fields are required.

    """

    # Get multipart-form data: image_id, realm_id, hwp_name, name
    try:
        image_id = request.POST["image_id"]
        realm_id = request.POST["realm_id"]
    except KeyError:
        return HttpResponseBadRequest("Both an image_id and a realm_id must be specified.")

    try:
        policy = Policy.objects.get(id=realm_id)
    except ObjectDoesNotExist:
        return HttpResponseBadRequest("The requested realm_id was not found.")
    provider = policy.get_next_provider(image_id)
    driver = provider.get_client()
    try:
        downstream_image = AggregateImage.objects.get(id=image_id)
    except ObjectDoesNotExist:
        return HttpResponseBadRequest("The requested image_id was not found.")
    try:
        upstream_image = RealImage.objects.get(
            downstream_image=downstream_image,
            provider=provider,
        )
    except ObjectDoesNotExist:
        return HttpResponseBadRequest("There is no downstream image matching this provider.")
    if provider.realm:
        realm = provider.realm
    else:
        realm = None

    instance = driver.instance_create(upstream_image.image_id, realm)

    database_instance = Instance(
        image = downstream_image,
        owner_id = "",
        name = "",
        provider = provider,
        instance_id = instance.id,
        policy = policy,
    )
    database_instance.save()

    return render_to_response(
        'dcmux/instances.xml',
        {
            "instances_uri": uri_lookup(request, "instances"),
            "instances": [instance],
        },
        mimetype="application/xml",
    )
@csrf_exempt
def instance_action(request, id, action):
    """Perform an action on an instance.

    Valid actions are: "reboot", "start" or "stop".  The Deltacloud api says
    that these actions should be performed by using a POST request on this uri.
    We let any request type through here, assuming that it's POST.

    """

    instance = Instance.objects.get(id=id)

    if action.lower() == "reboot":
        instance_reboot(request, instance)
    elif action.lower() == "start":
        instance_start(request, instance)
    elif action.lower() == "stop":
        instance_stop(request, instance)
    elif action.lower() == "destroy":
        instance_destroy(request, instance)
    else:
        return HttpResponseNotFound()

    return render_to_response(
        'dcmux/instances.xml',
        {
            "instances_uri": uri_lookup(request, "instances"),
            "instances": [instance],
        },
        mimetype="application/xml",
    )

def instance_reboot(request, instance):
    if instance.state.lower != "running":
        pass #TODO: Error
    instance.driver_instance_object.reboot()
    instance.state = "PENDING"
    #TODO: catch errors

def instance_start(request, instance):
    if instance.state.lower != "stopped":
        pass #TODO: Error
    instance.state = "RUNNING"
    instance.driver_instance_object.start()
    #TODO: catch errors

def instance_stop(request, instance):
    if instance.state.lower != "running":
        pass #TODO: Error
    instance.state = "STOPPED"
    instance.driver_instance_object.stop()

def instance_destroy(request, instance):
    instance.driver_instance_object.destroy()
    instance.state = "PENDING"
    #TODO: catch errors
    #TODO: destroy local instance
