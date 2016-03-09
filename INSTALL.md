# HW Requirements

**Storage**: at least 10 times the corpus size, min 10GB. If your unzipped training data is 10GB, make sure you have at least 80GB on drive.


**CPU**: No minimum required. 
  - More cores generally will give you a faster training and translation request throughput. 
  - More clock speed will generally give you a faster translation for the single request.

**Memory**: 
  - Min 5GB
  - 3GB each 100M words (source+target) of training.

# Support

You can report issues on [GitHub](https://github.com/ModernMT/MMT/issues)

For customizations and enterprise support: davide.caroselli@translated.net

# Setup up the server

**MMT 0.11.x only works on Ubuntu 14.04**

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
tar xvfz mmt-0.11.1-ubuntu14_04.tar.gz
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

sudo apt-get install libbz2-dev
sudo apt-get install libboost1.55-all-dev
sudo apt-get install libgoogle-perftools-dev
sudo apt-get install cmake
sudo apt-get install openjdk-8-jdk
sudo apt-get install git
sudo apt-get install maven
```

Next, you need to compile few libraries from source.

### CMPH 2.0

Get the release 2.0 of CMPH from [sourceforge.net](http://sourceforge.net/projects/cmph/) or you can directly execute:

```
wget "http://skylineservers.dl.sourceforge.net/project/cmph/cmph/cmph-2.0.tar.gz"
```

Extract downloaded archive:

```
tar xvf cmph-2.0.tar.gz
cd cmph-2.0
```

Then compile and install it:

```
./configure
make
sudo make install
```

You can safely remove downloaded files now:

```
cd ..
rm cmph-2.0.tar.gz
rm -rf cmph-2.0
```

### XMLRPC-C

Get the release 1.39.07 of XMLRPC-C from [sourceforge.net](http://sourceforge.net/projects/xmlrpc-c/files/Xmlrpc-c%20Super%20Stable/1.39.07/) or you can directly execute:

```
wget "http://skylineservers.dl.sourceforge.net/project/xmlrpc-c/Xmlrpc-c%20Super%20Stable/1.39.07/xmlrpc-c-1.39.07.tgz"
```

Extract downloaded archive:

```
tar xvf xmlrpc-c-1.39.07.tgz
cd xmlrpc-c-1.39.07
```

Then compile and install it:

```
./configure
make
sudo make install
```

You can safely remove downloaded files now:

```
cd ..
rm xmlrpc-c-1.39.07.tgz
rm -rf xmlrpc-c-1.39.07
```

## Install IRSTLM

Clone IRSTLM repository and move to cd-correction-model branch:

```
git clone https://github.com/irstlm-team/irstlm.git irstlm
cd irstlm
git checkout cd-correction-model
```


Compile IRSTLM:

```
mkdir build
cd build
cmake .. -DCMAKE_INSTALL_PREFIX=$(pwd) -DCXX0:BOOL=OFF -DBUILD_SHARED_LIBS=ON -DBUILD_STATIC_LIBS=OFF
make
make install
cd ../../
```

## Install Moses

Clone Moses repository and move to mmt-dev branch:

```
git clone https://github.com/ModernMT/mosesdecoder.git mosesdecoder
cd mosesdecoder
git checkout mmt-dev
```

Finally compile Moses:

```
/usr/bin/bjam -j$(nproc) -q --with-mm          \
         --with-irstlm=$(pwd)/../irstlm/build  \
         --with-cmph=/usr/local                \
         --with-xmlrpc-c=/usr/local            \
         link=shared                           \
         --prefix=$(pwd)/build
cd ..
```

## Install MMT

Move to the same folder where `irstlm`, `moses` and `mmt-submodule` are, then export these Env Variables:

```
export MOSESDECODER_HOME=$(pwd)/mosesdecoder
export IRSTLM_HOME=$(pwd)/irstlm
```

add these definitions also in your `.profile` bash file:

```
echo "export MOSESDECODER_HOME=$(pwd)/mosesdecoder" >> ~/.profile
echo "export IRSTLM_HOME=$(pwd)/irstlm" >> ~/.profile
```

Now clone ModernMT repository:

```
git clone https://github.com/ModernMT/MMT.git ModernMT
cd ModernMT
```

Download `opt` resources for Ubuntu 14.04:

```
wget "http://labs.mmt.rocks/builds/mmt-opt-0.11-ubuntu14_04.tar.gz"
tar xvf mmt-opt-0.11-ubuntu14_04.tar.gz
rm mmt-opt-0.11-ubuntu14_04.tar.gz
```

Install custom maven dependencies:

```
mvn install:install-file -Dfile=opt/maven/paoding-analysis.jar -DpomFile=opt/maven/paoding-analysis.pom
```

Finally compile your MMT distribution:

```
cd src
mvn clean install
```

You have now a working instance of MMT. Go to [README.md](README.md) to find how to test your installation.
