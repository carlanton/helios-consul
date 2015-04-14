# -*- mode: ruby -*-
# vi: set ft=ruby :

BOX_NAME = ENV['BOX_NAME'] || "phusion/ubuntu-14.04-amd64"
FORWARD_DOCKER_PORTS = ENV['FORWARD_DOCKER_PORTS']

Vagrant.require_version ">= 1.6.2"

$script = <<SCRIPT
export DEBIAN_FRONTEND=noninteractive
set -x

# Setup keys and repos
apt-get update && apt-get install -y apt-transport-https
echo deb http://get.docker.io/ubuntu docker main > /etc/apt/sources.list.d/docker.list
echo deb https://spotify.github.io/helios-apt helios main > /etc/apt/sources.list.d/helios.list
curl -s https://get.docker.io/gpg | apt-key add -
apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys FB0ACEBA8887F477
apt-get update

# Zookeeper and Docker
apt-get install -y zookeeperd lxc-docker unzip

# Install Consul
wget -q -O /tmp/consul.zip https://dl.bintray.com/mitchellh/consul/0.5.0_linux_amd64.zip
wget -q -O /tmp/consul-web-ui.zip https://dl.bintray.com/mitchellh/consul/0.5.0_web_ui.zip
yes | unzip /tmp/consul.zip -d /usr/bin/
yes | unzip /tmp/consul-web-ui.zip -d /usr/share/consul-web-ui

cat <<EOF > /etc/init/consul.conf
description "Consul agent"
start on runlevel [2345]
stop on runlevel [!2345]
respawn
script
  export GOMAXPROCS=`nproc`
  exec /usr/bin/consul agent -server -bootstrap-expect 1 -client=0.0.0.0 -bind=0.0.0.0 -data-dir /var/lib/consul -ui-dir /usr/share/consul-web-ui/dist
end script
EOF

start consul

# Install Helios
apt-get install -y helios helios-agent helios-master

# Install helios-consul
wget -q -O /tmp/helios-consul.deb 'https://github.com/SVT/helios-consul/releases/download/0.24/helios-consul_0.24_all.deb' && \
dpkg --force-confdef --force-confold -i /tmp/helios-consul.deb ;

# Setup helios agent to use helios-consul
echo 'ENABLED=true \
HELIOS_AGENT_OPTS="--service-registry http://127.0.0.1:8500 --service-registrar-plugin /usr/share/helios/lib/plugins/helios-consul-0.24.jar"' > /etc/default/helios-agent
restart helios-agent

# Good stuff for the vagrant user
mkdir -p /home/vagrant/.helios
echo '{"masterEndpoints":["http://localhost:5801"]}' > /home/vagrant/.helios/config
usermod -aG docker vagrant
SCRIPT

Vagrant.configure("2") do |config|
  # Setup virtual machine box. This VM configuration code is always executed.
  config.vm.box = BOX_NAME
  config.ssh.forward_agent = true
  config.vm.network :forwarded_port, guest: 5801, host: 5801
  config.vm.network :forwarded_port, guest: 8500, host: 8500
  config.vm.network :private_network, ip: "192.168.33.10"
  config.vm.provision "shell", inline: $script
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
