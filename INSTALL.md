# Hardware requirements

<table>
  <tr>
    <td valign="top"><b>STORAGE</b></td>
    <td>At least 10 times the corpus size, min 10GB. For example, if your unzipped training data is 10GB, make sure you have at least 100GB on drive.</td>
  </tr>
  <tr>
    <td valign="top"><b>PLATFORM</b></td>
    <td>ModernMT is developed and tested on a Linux, x86_64 platform.</td>
  </tr>
  <tr>
    <td valign="top"><b>CPU</b></td>
    <td>No minimum hardware specifications are required. Thus, we suggest a least a 8-cores CPU for training and decoding.</td>
  </tr>
  <tr>
    <td valign="top"><b>GPU</b></td>
    <td>At least one <a href="https://developer.nvidia.com/cuda-gpus">CUDA-capable GPU</a> with minimum 8GB of internal memory. Multiple GPUs are recommended in order to speedup both training and decoding.</td>
  </tr>
  <tr>
    <td valign="top"><b>RAM</b></td>
    <td>Minimum 16GB, highly depending on parallel data during training</td>
  </tr>
</table>

# Install ModernMT via Docker

If you are familiar with Docker, this is usually the easiest option to use ModernMT. This section assumes you have already a running instance of Docker, if this is not the case please [follow these instructions](https://docs.docker.com/install/linux/docker-ce/ubuntu/) in order to properly install Docker.

### NVIDIA drivers

The first step is **NVIDIA drivers** installation:
```bash
sudo add-apt-repository -y ppa:graphics-drivers
sudo apt update
sudo apt install -y nvidia-driver-410
```

In order to finalize the installation you need to **reboot your machine**.

### NVIDIA Docker
Next step is to install [nvidia-docker2](https://github.com/NVIDIA/nvidia-docker) package that allow docker images to directly access the underlying GPU hardware with the CUDA library:
```bash
# Add the package repositories
curl -s -L https://nvidia.github.io/nvidia-docker/gpgkey | \
  sudo apt-key add -
distribution=$(. /etc/os-release;echo $ID$VERSION_ID)
curl -s -L https://nvidia.github.io/nvidia-docker/$distribution/nvidia-docker.list | \
  sudo tee /etc/apt/sources.list.d/nvidia-docker.list
sudo apt-get update

# Install nvidia-docker2 and reload the Docker daemon configuration
sudo apt-get install -y nvidia-docker2
sudo pkill -SIGHUP dockerd
```

### Run latest ModernMT image
Finally, you are able to run the latest ModernMT image with Docker:
```bash
sudo docker run --runtime=nvidia --rm -it --publish 8045:8045 modernmt/master bash
```

Done! Go to [README.md](README.md) to create your first engine.


# Install ModernMT from binaries

With every ModernMT release in Github we also include a binary version of the package that can be used directly without the need to compile the source code.

### NVIDIA drivers and CUDA Toolkit

First you need to install the **NVIDIA drivers**:
```bash
sudo add-apt-repository -y ppa:graphics-drivers
sudo apt update
sudo apt install -y nvidia-driver-410
```

In order to finalize the installation you need to **reboot your machine**.

Then you need to install the **CUDA Toolkit 10**, on Ubuntu 18.04 follow this steps:
```bash
# Download .deb package locally
wget -O cuda-repo-ubuntu1804-10-1-local-10.1.105-418.39_1.0-1_amd64.deb https://developer.nvidia.com/compute/cuda/10.1/Prod/local_installers/cuda-repo-ubuntu1804-10-1-local-10.1.105-418.39_1.0-1_amd64.deb

# Install cuda
sudo dpkg -i cuda-repo-ubuntu1804-10-1-local-10.1.105-418.39_1.0-1_amd64.deb
sudo apt-key add /var/cuda-repo-<version>/7fa2af80.pub
sudo apt update
sudo apt install cuda
```


<br>
<br>
<br>
<br>
<br>
<br>
<br>
<br>
<br>
<br>
<br>
<br>
<br>
<br>
<br>
<br>
<br>
<br>
<br>
<br>
<br>
<br>
<br>
<br>
<br>
<br>
<br>
<br>
<br>
<br>
<br>
<br>
<br>
<br>
<br>

## CUDA/cuDNN libraries and software
### CUDA library
In order to run ModernMT Enterprise Edition, [CUDA 9.0 library](https://developer.nvidia.com/cuda-90-download-archive?target_os=Linux) are required.

```bash
# IMPORTANT: Tensorflow **won't work** with CUDA > 9.0
wget -O cuda-repo-ubuntu1604-9-0-local_9.0.176-1_amd64.deb https://developer.nvidia.com/compute/cuda/9.0/Prod/local_installers/cuda-repo-ubuntu1604-9-0-local_9.0.176-1_amd64-deb

sudo dpkg -i cuda-repo-ubuntu1604-9-0-local_9.0.176-1_amd64.deb
sudo apt-key add /var/cuda-repo-<version>/7fa2af80.pub
sudo apt-get update
sudo apt-get install cuda

# Install Patch 1
wget -O cuda-repo-ubuntu1604-9-0-local-cublas-performance-update_1.0-1_amd64.deb https://developer.nvidia.com/compute/cuda/9.0/Prod/patches/1/cuda-repo-ubuntu1604-9-0-local-cublas-performance-update_1.0-1_amd64-deb

sudo dpkg -i cuda-repo-ubuntu1604-9-0-local-cublas-performance-update_1.0-1_amd64.deb
sudo apt-get update
sudo apt-get upgrade cuda

# Install Patch 2
wget -O cuda-repo-ubuntu1604-9-0-local-cublas-performance-update-2_1.0-1_amd64.deb https://developer.nvidia.com/compute/cuda/9.0/Prod/patches/2/cuda-repo-ubuntu1604-9-0-local-cublas-performance-update-2_1.0-1_amd64-deb

sudo dpkg -i cuda-repo-ubuntu1604-9-0-local-cublas-performance-update-2_1.0-1_amd64.deb 
sudo apt-get update
sudo apt-get upgrade cuda
```

### cuDNN library

Install cuDNN 7.1 library from: [NVIDIA cuDNN Download](https://developer.nvidia.com/rdp/cudnn-download)

Select option `Download cuDNN v7.1.4 (May 16, 2018), for CUDA 9.0` and version `cuDNN v7.1.4 Runtime Library for Ubuntu16.04 (Deb)`

```
curl [...] --output libcudnn7_7.1.4.18-1+cuda9.0_amd64.deb
sudo dpkg -i libcudnn7_7.1.4.18-1+cuda9.0_amd64.deb
```

## Max open files limit
The current version of ModernMT does not limit the maximum number of open files for performance reasons. For this reason, if you plan to create an engine with a high number of different domains you could hit the OS limit and MMT will crash.

In order to avoid this error, in Ubuntu 16.04 you have to set the option `nofile` in `/etc/security/limits.conf` to a high limit and restart the machine, for example:
```
* soft nofile 1048576
* hard nofile 1048576
```

# Option 2 - Install Binaries on Your Server

**Important**: follow [pre-installation steps](#pre-installation-actions) before continuing with this installation.

This release was tested on a clean Ubuntu 16.04 server.

## Libraries that MMT requires:

### Java 8
Install **Java 8** if not present
```bash
sudo add-apt-repository ppa:openjdk-r/ppa && sudo apt-get update && sudo apt-get install openjdk-8-jdk
```

Check Java version with command:

```
java -version
```

If the first line report a version of Java prior 1.8, you need to **update default Java version**. Run command:

```
sudo update-alternatives --config java
```

and type the number of the option that contains **java-8-openjdk**, then press ENTER. Here's an example:

```
$ sudo update-alternatives --config java
There are 2 choices for the alternative java (providing /usr/bin/java).

  Selection    Path                                            Priority   Status
------------------------------------------------------------
* 0            /usr/lib/jvm/java-7-openjdk-amd64/jre/bin/java   1071      auto mode
  1            /usr/lib/jvm/java-7-openjdk-amd64/jre/bin/java   1071      manual mode
  2            /usr/lib/jvm/java-8-openjdk-amd64/jre/bin/java   1069      manual mode

Press enter to keep the current choice[*], or type selection number: 2
```

### Python module `requests`
You can install `requests` module with the following commands:
```bash
sudo apt-get install python-pip
pip install -U requests
```

### Tensorflow and Tensor2Tensor

In order to install Tensorflow and Tensor2Tensor just type:

```bash
pip install -U requests
pip install numpy==1.14.5
pip install tensorflow-gpu==1.8.0
pip install tensor2tensor==1.6.3

pip install --upgrade oauth2client
```

## Install the MMT Binaries

Download from here: https://github.com/ModernMT/MMT/releases and then untar the files:

```
tar xvfz mmt-<version-number>.tar.gz
rm mmt-*.tar.gz
cd mmt
```

Done! go to [README.md](README.md)

# Option 3 - Installing from source

**Important**: follow [pre-installation steps](#pre-installation-actions) before continuing with this installation.

The following procedure describes how to build MMT from source in an Ubuntu 16.04 environment.

## Installing third-party libraries

Open a bash shell and type:
```
sudo add-apt-repository ppa:george-edison55/cmake-3.x
sudo add-apt-repository ppa:openjdk-r/ppa
sudo apt-get update

sudo apt-get install python-pip
pip install -U requests

sudo apt-get install openjdk-8-jdk zlib1g-dev libbz2-dev libboost-all-dev cmake git maven
```

### Tensorflow and Tensor2Tensor

In order to install Tensorflow and Tensor2Tensor just type:

```bash
pip install -U requests
pip install numpy==1.14.5
pip install tensorflow-gpu==1.8.0
pip install tensor2tensor==1.6.3

pip install --upgrade oauth2client
```

## Install MMT

First, clone ModernMT repository:

```
git clone https://github.com/ModernMT/MMT.git ModernMT

cd ModernMT
```

Download MMT dependencies:

```
cd vendor
python download_dependencies.py
cd ..
```

Check your Java version and if necessary update it and select the latest jdk, as described in the Option 2 paragraph.

You can now build your MMT distribution:

```
cd src
mvn clean install
cd ..
```

You have now a working instance of MMT. Go to [README.md](README.md) to find how to test your installation.

# Support

You can report issues on [GitHub](https://github.com/ModernMT/MMT/issues).
For customizations and enterprise support: info@modernmt.eu
