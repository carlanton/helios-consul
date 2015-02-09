# -*- mode: ruby -*-
# vi: set ft=ruby :

BOX_NAME = ENV['BOX_NAME'] || "phusion/ubuntu-14.04-amd64"
FORWARD_DOCKER_PORTS = ENV['FORWARD_DOCKER_PORTS']

Vagrant.require_version ">= 1.6.2"

Vagrant.configure("2") do |config|
  # Setup virtual machine box. This VM configuration code is always executed.
  config.vm.box = BOX_NAME

  config.ssh.forward_agent = true
  config.vm.network :forwarded_port, guest: 5801, host: 5801
  config.vm.network :forwarded_port, guest: 8500, host: 8500
  config.vm.network :private_network, ip: "192.168.33.10"

  pkg_cmd = "export DEBIAN_FRONTEND=noninteractive; "
  pkg_cmd << "set -x; "

  # install other helios dependencies and development tools
  pkg_cmd << "apt-get update && apt-get install -y openjdk-7-jre zookeeperd software-properties-common; "

  # make sure zk is running
  pkg_cmd << "initctl start zookeeper ;"

  # Install and enable Consul
  pkg_cmd << "apt-add-repository ppa:bcandrea/consul --yes && apt-get update && apt-get install -y consul consul-web-ui ;"
  pkg_cmd << <<-END.gsub(/^ {4}/, '')
    echo '{
      "disable_update_check": true,
      "server": true,
      "datacenter": "vagrant",
      "bootstrap_expect" : 1
    }' > /etc/consul.d/30-single-master.json ;
    END
  pkg_cmd << "service consul restart ; "

  # install docker
  pkg_cmd << "curl -s https://get.docker.io/gpg | apt-key add -; "
  pkg_cmd << "echo deb http://get.docker.io/ubuntu docker main > /etc/apt/sources.list.d/docker.list; "
  pkg_cmd << "apt-get update && apt-get -y install lxc-docker; "

  # Set up docker to listen on 127.0.0.1:2375
  pkg_cmd << "echo 'DOCKER_OPTS=\"--restart=false -D=true -H=tcp://127.0.0.1:2375 -H=unix:///var/run/docker.sock --dns=192.168.33.10\"' > /etc/default/docker; "
  # make docker usable by vagrant user w/o sudo
  pkg_cmd << "groupadd docker; gpasswd -a vagrant docker; service docker restart;"
  # create the helios user (needed to be able to set up permissions to docker properly)
  # this is copied from the docker-agent postinst script
  pkg_cmd << "adduser --system --quiet --home /var/lib/helios-agent --no-create-home --shell /bin/bash --group --gecos 'Helios' helios;"
  # give the helios user access to docker
  pkg_cmd << "gpasswd -a helios docker;"

  # install helios conf files
  pkg_cmd << <<-END.gsub(/^ {4}/, '')
    echo '
    ENABLED=true

    HELIOS_AGENT_OPTS="--state-dir=/var/lib/helios-agent --docker tcp://127.0.0.1:2375 --name=ubuntu-14.consul --zk localhost:2181 --service-registry http://127.0.0.1:8500 --service-registrar-plugin /usr/share/helios/lib/plugins/helios-consul-0.24.jar"
    ' > /etc/default/helios-agent ;
    END
  pkg_cmd << <<-END.gsub(/^ {4}/, '')
    echo '
    ENABLED=true

    HELIOS_MASTER_OPTS="--zk localhost:2181"
    ' > /etc/default/helios-master ;
    END

  pkg_cmd << "mkdir -p /home/vagrant/.helios;"
  pkg_cmd << <<-END.gsub(/^ {4}/, '')
    echo '{"masterEndpoints":["http://localhost:5801"]}' > /home/vagrant/.helios/config;
    END

  # Install helios and helios-consul
  pkg_cmd << <<-END.gsub(/^ {4}/, '')
    wget -q -O /tmp/helios-deb.tar.gz 'https://github.com/spotify/helios/releases/download/0.8.163/helios-debs.tar.gz' && \
    wget -q -O /tmp/helios-consul.deb 'https://github.com/SVT/helios-consul/releases/download/0.24/helios-consul_0.24_all.deb' && \
    cd /tmp && tar xf helios-deb.tar.gz && dpkg --force-confdef --force-confold -i *.deb ;
    END

  config.vm.provision :shell, :inline => pkg_cmd
end


# Providers were added on Vagrant >= 1.1.0
Vagrant::VERSION >= "1.1.0" and Vagrant.configure("2") do |config|
  config.vm.provider :virtualbox do |vb, override|
    config.vm.box = BOX_NAME
    vb.customize ["modifyvm", :id, "--natdnshostresolver1", "on"]
    vb.customize ["modifyvm", :id, "--natdnsproxy1", "on"]
  end
end

if !FORWARD_DOCKER_PORTS.nil?
  Vagrant::VERSION < "1.1.0" and Vagrant::Config.run do |config|
    (49000..49900).each do |port|
      config.vm.forward_port port, port
    end
  end

  Vagrant::VERSION >= "1.1.0" and Vagrant.configure("2") do |config|
    (49000..49900).each do |port|
      config.vm.network :forwarded_port, :host => port, :guest => port
    end
  end
end
