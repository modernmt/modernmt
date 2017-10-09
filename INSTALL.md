# HW Requirements

### Storage
At least 10 times the corpus size, min 10GB. If your unzipped training data is 10GB, make sure you have at least 100GB on drive.

### Platform
A x86_64 platform is required.

### CPU
No minimum required. 
* More cores generally will give you a faster training and translation request throughput. 
* More clock speed will generally give you a faster translation for the single request.

### GPU (only for neural engine)
At least one [CUDA-capable GPU](https://developer.nvidia.com/cuda-gpus). Current MMT version supports only single-GPU training, while you can increase the translation throughput using multiple GPUs at runtime.

### Memory
*  Min 5GB
*  1GB each 350MB of training data

# Support

You can report issues on [GitHub](https://github.com/ModernMT/MMT/issues)

For customizations and enterprise support: davide.caroselli@translated.net

# Option 1 - Using Docker

```
docker pull modernmt/master
```

To run your istance and publish the API on port 8045 of your host, execute

```
docker run -it --publish 8045:8045 modernmt/master bash
```

Done! go to [README.md](README.md) to create your first engine.

# Option 2 - Install Binaries on Your Server

This release was tested on a clean Ubuntu 14.04 server from Amazon AWS.
AMI: Ubuntu Server 14.04 LTS (HVM), SSD Volume Type -  ami-accff2b1

For training >100M words we suggest to use this instance: 
c3.4xlarge (30GB RAM, 16 core, circa $0.90/hour)

## Max open files limit
The current version of ModernMT does not limit the maximum number of open files for performance reasons. For this reason, if you plan to create an engine with a high number of different domains you could hit the OS limit and MMT will crash.

In order to avoid this error, in Ubuntu 14.04 and 16.04 you have to set the option `nofile` in `/etc/security/limits.conf` to a high limit and restart the machine, for example:
```
* soft nofile 1048576
* hard nofile 1048576
```

## CUDA drivers (only for neural engine)
In order to create and run MMT Neural MT engine, you have to install CUDA 8.0 drivers on your machine; you can find the procedure on the NVIDIA website: [NVIDIA CUDA Installation Guide for Linux](http://docs.nvidia.com/cuda/cuda-installation-guide-linux/#axzz4VZnqTJ2A)

Optionally, you can also install the **CUDNN 6.0** drivers in order to speed-up the deep-neural network computation. You can follow the steps described in this guide: [NVIDIA cuDNN](https://developer.nvidia.com/cudnn).

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

### `requests` for python
**Python 2.7** with module **Requests** is also required but it is pre-installed in Ubuntu.

Just in case *Requests* is not installed:
```
sudo apt-get install python-pip
sudo pip install -U requests
```

### PyTorch (for Neural engine)
Install *PyTorch 0.2* with `pip`:

```bash
pip install http://download.pytorch.org/whl/cu80/torch-0.2.0.post3-cp27-cp27mu-manylinux1_x86_64.whl 

# if the above command does not work, then you have python 2.7 UCS2, use this command 
pip install http://download.pytorch.org/whl/cu80/torch-0.2.0.post3-cp27-cp27m-manylinux1_x86_64.whl
```

## Install the MMT Binaries

Download from here: https://github.com/ModernMT/MMT/releases and then untar the files:

```
tar xvfz mmt-<version-number>.tar.gz
cd mmt
```

Done! go to [README.md](README.md)

# Option 3 - Installing from source

Build MMT from source ensures the best performance and it also resolves some issues with hardware compatibility.
The following procedure describes how to build MMT from source in an Ubuntu 14.04 or Ubuntu 16.04 environment.

## Installing third-party libraries

Open a bash shell and type:
```
sudo add-apt-repository ppa:george-edison55/cmake-3.x
sudo add-apt-repository ppa:openjdk-r/ppa
sudo apt-get update
```

If your OS is **Ubuntu 14.04**, then run the following command:
```
sudo apt-get install libsnappy-dev libbz2-dev libboost1.55-all-dev libsparsehash-dev cmake openjdk-8-jdk git maven
```

Alternatively, if your OS is **Ubuntu 16.04** then run the following commands:
```
sudo apt-get install python-pip
sudo pip install -U requests
sudo apt-get install g++
sudo apt-get install libsnappy-dev zlib1g-dev libbz2-dev libboost-all-dev libsparsehash-dev cmake openjdk-8-jdk git maven
```

## CUDA drivers (only for neural engine)
In order to create and run MMT Neural MT engine, you have to install CUDA 8.0 drivers on your machine; you can find the procedure on the NVIDIA website: [NVIDIA CUDA Installation Guide for Linux](http://docs.nvidia.com/cuda/cuda-installation-guide-linux/#axzz4VZnqTJ2A)

Optionally, you can also install the **CUDNN 6.0** drivers in order to speed-up the deep-neural network computation. You can follow the steps described in this guide: [NVIDIA cuDNN](https://developer.nvidia.com/cudnn).

## PyTorch (for Neural engine)
Install *PyTorch 0.2* with `pip`:

```bash
pip install http://download.pytorch.org/whl/cu80/torch-0.2.0.post3-cp27-cp27mu-manylinux1_x86_64.whl 

# if the above command does not work, then you have python 2.7 UCS2, use this command 
pip install http://download.pytorch.org/whl/cu80/torch-0.2.0.post3-cp27-cp27m-manylinux1_x86_64.whl
```

## Install MMT

First, clone ModernMT repository and initialize its submodules:

```
git clone https://github.com/ModernMT/MMT.git ModernMT

cd ModernMT

git submodule init
git submodule update
```

Download and compile MMT submodules:

```
cd vendor
./compile
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
