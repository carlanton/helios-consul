# Consul Service Registrar for Helios

Hi! This is a [Consul](https://consul.io) service registrar plugin for
[Helios](https://github.com/spotify/helios). It is designed to run together
with Helios agents and registers all deployed jobs to the Consul service
registry.

The code is based on the [SkyDNS
plugin](https://github.com/spotify/helios-skydns) made by Spotify.

## Installation

Requirements: Helios 0.8.298 or newer.

Just download the deb from the [release
page](https://github.com/SVT/helios-consul/releases/latest) and run: `dpkg -i
helios-consul_xxx_all.deb`

If you want you can build it and pack it yourself with maven: `mvn package`

You can also try out our Vagrantfile that will setup Helios, Consul and
helios-consul for you in a Vagrantbox: `vagrant up`

## Configuration

A Consul agent should be running locally on the agent. Start helios-agent with
the following parameters added to `HELIOS_AGENT_OPTS` in
`/etc/default/helios-agent`:

  * `--service-registrar-plugin` - The path to the helios-consul jar. This is
    usually `/usr/share/helios/helios-consul-XXX.jar`

  * `--service-registry` - Address to a Consul agent running on the agent. For
    example `http://localhost:8500`


It is possibly to configure more parts of helios-consul using Java system
properties. This can be done by adding some parameters to `JAVA_OPTS` in
`/etc/default/helios-agent`. The following properties are available:

  * `helios-consul.healthCheckInterval`: How often to run health checks (in
    seconds). Defaults to 10.

  * `helios-consul.syncInterval`: How often to sync the state between
    helios-consul and the Consul agent (also in seconds). Defaults to 30.

  * `helios-consul.deployTag`: Set the tag which all services deployed by
    Helios will get. It is used by helios-consul to keep track on which
    services that are deployed by the Helios agent. Therefore it is important
    that this tag is only used for such services. Defaults to
    `helios-deployed`.

## Usage

The value for `registration` in the Helios job definition will be used as
`ServiceID` in the registered Consul service. If the value follows the pattern
`(.+)-(v\\d+)`, for example `hello-world-service-v13`, the first group
(hello-world-service) will be used as `ServiceName` and the last group (v13)
will be added as a tag. All services will also be tagged with the deploy tag
and the endpoint protocol.

*Example:*

```shell
$ helios create \
    --port default=8080/tcp \
    --register my-shiny-webservice-v2/http=default \
    my-shiny-webservice:v2 \
    google/nodejs-hello:latest

$ helios deploy my-shiny-webservice:v2 some-helios-agent
```

Helios will then register the following service in Consul:
```
    ServiceId:   my-shiny-webservice-v2
    ServiceName: my-shiny-webservice
    Tags:        [v2, protocol-http, helios-deployed]
```

## Health checks

If you are using a [Helios health
checks](https://github.com/spotify/helios/blob/master/docs/user_manual.md#health-checks),
it will be converted into a [Consul health
check](https://www.consul.io/intro/getting-started/checks.html) on service
registration. This can be very useful: Helios will wait to do the service registration
until the check is up. After the service registration, Consul will continue to
check the same endpoint of your service.

Currently, only HTTP health checks are supported. Pull requests are welcome!
:-)


## More examples

Here are some more examples of how you can use Helios+helios-consul. Use the
Vagrantfile for a quick start.

### Job with health check

This example deploy and register a service with a health check on `/`.

```shell
$ helios create \
    --port default=8080/tcp \
    --register service-with-healthcheck-v1/http=default \
    --http-check default:/ \
    service-with-healthcheck:v1 \
    google/nodejs-hello:latest

$ helios deploy service-with-healthcheck ubuntu-14
```

### Custom service tags

It is also possible to add additional tags to services. This requires the job
definition to be in JSON since the feature is not exposed in the CLI. 

```shell
$ echo '{
  "image" : "google/nodejs-hello:latest",
  "ports" : {
    "default" : {
      "internalPort" : 8080,
      "protocol" : "tcp"
    }
  },
  "registration" : {
    "service-with-tags-v1/http" : {
      "ports" : {
        "default" : {
          "tags" : ["production", "really-important", "do-not-touch"]
        }
      }
    }
  },
  "healthCheck" : {
    "type" : "http",
    "path" : "/",
    "port" : "default",
    "type" : "http"
  }
}' > my-job.json

$ helios create -f my-job.json service-with-tags:v1
$ helios deploy service-with-tags:v1 ubuntu-14
```
