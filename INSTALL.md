# HW Requirements

**Storage**: at least 10 times the corpus size, min 10GB. If your unzipped training data is 10GB, make sure you have at least 100GB on drive.


**CPU**: No minimum required. 
  - More cores generally will give you a faster training and translation request throughput. 
  - More clock speed will generally give you a faster translation for the single request.

**Memory**: 
  - Min 5GB
  - 1GB each 350MB of training data

# Support

You can report issues on [GitHub](https://github.com/ModernMT/MMT/issues)

For customizations and enterprise support: davide.caroselli@translated.net

# Setup up the server

**MMT currently only works on Ubuntu 14.04**

This release was tested on a clean Ubuntu 14.04 server from Amazon AWS.
AMI: Ubuntu Server 14.04 LTS (HVM), SSD Volume Type -  ami-accff2b1

For training >100M words we suggest to use this instance: 
c3.4xlarge (30GB RAM, 16 core, circa $0.90/hour)

## Libraries that MMT requires:

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

**Python 2.7** with module **Requests** is also required but it is pre-installed in Ubuntu.

Just in case *Requests* is not installed:
```
sudo apt-get install python-pip
sudo pip install -U requests
```


# Install the MMT Binaries

Download from here: https://github.com/ModernMT/MMT/releases and then untar the files:

```
tar xvfz mmt-0.13-ubuntu14_04.tar.gz
cd mmt
```

Done! go to [README.md](README.md)



# Installing from source (for contributors)

Build MMT from source allows you to contribute to this repository. Please note that currently this procedure has been tested only on Ubuntu 14.04, it is highly recommended to use this OS as your development environment.

## Installing third-party libraries

Open a bash shell and type:

```
sudo add-apt-repository ppa:george-edison55/cmake-3.x
sudo add-apt-repository ppa:openjdk-r/ppa
sudo apt-get update

sudo apt-get install libbz2-dev libboost1.55-all-dev libgoogle-perftools-dev libsparsehash-dev cmake openjdk-8-jdk git maven
```

## Install MMT

First, clone ModernMT repository and initialize its submodules:

```
git clone https://github.com/ModernMT/MMT.git ModernMT

cd ModernMT

git submodule init
git submodule update
```

Compile MMT submodules:

```
cd vendor
make res
make
cd ..
```

Finally compile your MMT distribution:

```
cd src
mvn clean install
```

You have now a working instance of MMT. Go to [README.md](README.md) to find how to test your installation.
