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

### Install NVIDIA drivers

The first step is **NVIDIA drivers** installation:
```bash
sudo add-apt-repository -y ppa:graphics-drivers
sudo apt update
sudo apt install -y nvidia-driver-410
```

In order to finalize the installation you need to **reboot your machine**.

### Install NVIDIA Docker
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

### Install NVIDIA drivers and CUDA Toolkit

First you need to install the **NVIDIA drivers**:
```bash
sudo add-apt-repository -y ppa:graphics-drivers
sudo apt update
sudo apt install -y nvidia-driver-410
```

In order to finalize the installation you need to **reboot your machine**.

Then you need to install the **CUDA Toolkit 10**, on Ubuntu 18.04 follow these steps:
```bash
# Download .deb package locally
wget -O cuda-repo-ubuntu1804-10-0-local-10.0.130-410.48_1.0-1_amd64.deb https://developer.nvidia.com/compute/cuda/10.0/Prod/local_installers/cuda-repo-ubuntu1804-10-0-local-10.0.130-410.48_1.0-1_amd64

# Install cuda
sudo dpkg -i cuda-repo-ubuntu1804-10-0-local-10.0.130-410.48_1.0-1_amd64.deb
sudo apt-key add /var/cuda-repo-10-0-local-10.0.130-410.48/7fa2af80.pub
sudo apt update
sudo apt install -y cuda
```

Next install the **NVIDIA cuDNN library** from: [NVIDIA cuDNN Download](https://developer.nvidia.com/rdp/cudnn-download). Select the option *"Download cuDNN v7.5.1 (April 22, 2019), for CUDA 10.0"* and then *"cuDNN Runtime Library for Ubuntu18.04 (Deb)"*. Finally simply run this command on the downloaded package:
```bash
sudo dpkg -i libcudnn7_7.5.1.10-1+cuda10.0_amd64.deb
```

### Install Java 8 and Python 3

ModernMT requires Java 8 and Python 3.6 (or higher). If not already installed on your system, you can run the following command:
```bash
sudo apt install -y openjdk-8-jdk python3 python3-pip
```

In order to check if the installation completed successfully you can run these two commands and check the output:
```bash
$ java -version
openjdk version "1.8.0_191"
[...]

$  python3 --version
Python 3.6.7
```

If your output is not the expected one, please go to the [Troubleshooting](#troubleshooting-and-support) section of this guide.

### Download ModernMT

Download the latest ModernMT binary file from [ModernMT releases page](https://github.com/modernmt/modernmt/releases) and extract the archive:
```bash
tar xvfz mmt-<version-number>-ubuntu.tar.gz
rm mmt-*.tar.gz
cd mmt
```

Finally install the python dependencies:
```bash
pip3 install -r requirements.txt
```

Done! Go to [README.md](README.md) to create your first engine.


# Install ModernMT from source

This option is most suitable for developers, contributors or enthusiasts willing to work with the bleeding-edge development code, before a stable release. Installing ModernMT from source in fact gives you the ability to keep your code continously up-to-date and modify the source code yourself.

### Common installation steps

Please, follow these installation steps from the previous option (binary installation):
- [Install NVIDIA drivers and CUDA Toolkit](#install-nvidia-drivers-and-cuda-toolkit)
- [Install Java 8 and Python 3](#install-java-8-and-python-3)

### Install development libraries and tools

Next install Git, Maven, CMake and Boost together with few more c++ libraries with the following command:
```bash
sudo apt install -y git maven cmake libboost-all-dev zlib1g-dev libbz2-dev
```

### Clone ModernMT repository from GitHub

We are now ready to clone the ModernMT repository from Github:
```bash
git clone https://github.com/modernmt/modernmt.git modernmt && cd modernmt
```

Next, run the installation script:
```bash
python3 setup.py
```

Finally you can compile the cource code with maven:
```bash
pushd src
mvn clean install
popd
```

You have now a working instance of ModernMT. Go to [README.md](README.md) to create your first engine.


# Troubleshooting and support

### "Too many open files" errors when runnning ModernMT

For performance reasons ModernMT does not limit the maximum number of open files. This could lead to errors reporting too many open files, or max open file limit reached.

In order to avoid this error, set the option `nofile` in `/etc/security/limits.conf` to a high limit and restart the machine, for example:
```
* soft nofile 1048576
* hard nofile 1048576
```

### Wrong version of Java

First, check your Java version with the following command:
```bash
java -version
```

If the first line report a version of Java different from 1.8, you need to **update default Java version**.
In order to do so, just run the command:
```bash
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

### Support

Our [GitHub issues page](https://github.com/ModernMT/MMT/issues) is the best option to search for solutions to your problems or open new issues regarding the code.
For customizations and enterprise support, please contact us at info@modernmt.eu .
