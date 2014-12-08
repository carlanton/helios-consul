# Consul Service Registrar for Helios

Hi! This is a [Consul](https://consul.io) service registrar plugin for
[Helios](https://github.com/spotify/helios). It is designed to run together
with helios-agent and registers all deployed jobs to the Consul service
registry.

The code is based on the [SkyDNS
plugin](https://github.com/spotify/helios-skydns) made by Spotify.

## Usage

A Consul agent should be running locally on the agent. Start helios-agent with
the following parameters:

  * `--service-registrar-plugin /path/to/helios-consul-xxx.jar`
  * `--service-registry http://localhost:8500` 

## From a Helios job to a Consul service

The value for `registration` in the Helios job definition will be used as
`ServiceID` in the registered Consul service. If the string follows the pattern
`(.+)-(v\\d+)`, for example `hello-world-service-v13`, the first group
(hello-world) will be used as `ServiceName` and the last group (v13) will be
added as a tag. All services will also be tagged with `helios-deployed` and the
endpoint protocol.

*Example:*

    helios create \
        --port default=8080/tcp \
        --register my-shiny-webservice-v2/http=default \
        google/python-hello:2.7 \
        my-shiny-webservice:v2

    helios deploy my-shiny-webservice:v2 some-helios-agent

The Helios agent will then register the following service in Consul:

    ServiceId:   my-shiny-webservice-v2
    ServiceName: my-shiny-webservice
    Tags:        [v2, protocol-http, helios-deployed]

## Consul health checks

If you want to register a 
[health check](http://www.consul.io/docs/agent/checks.html) for deployed
services, you can set the location of the script and the check interval in the
connect string:

    --service-registry http://localhost:8500,/path/to/your/script.sh,10s

Consul will then execute your script every 10 second with the parameters host,
port, and protocol. See `simple-service-check.sh` for an example. The check will
be added to all services deployed by the agent.

