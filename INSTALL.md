# HW Requirements

**Storage**: at least 6 times the corpus size, min 10GB. If your training data is 10GB, make sure you have at least 60GB on drive.


**CPU**: No minimum required. 
  - More cores generally will give you a faster training and translation request throughput. 
  - More clock speed will generally give you a faster translation for the single request.

**Memory**: 
  - Min 5GB
  - 3GB each 100M words (soruce+target) of training.

# Support

You can report issues on [GitHub](https://github.com/ModernMT/MMT/issues)

For customizations and enterprise support: davide.caroselli@translated.net

# Setup up the server

**MMT 0.11 only works on Ubuntu 14.04**

This release was tested on a clean Ubuntu 14.04 server from Amazon AWS.
AMI: Ubuntu Server 14.04 LTS (HVM), SSD Volume Type -  ami-accff2b1

For training >100M words we suggest to use this instance: 
c3.4xlarge (30GB RAM, 16 core, circa $0.90/hour)

## Libraries that MMT requires:

Install **Java 8** if not present
```bash
sudo add-apt-repository ppa:openjdk-r/ppa && sudo apt-get update && sudo apt-get install openjdk-8-jdk
```

**Python 2.7** with module **Requests** is also required but it is pre-installed in Ubuntu.

Just in case *Requests* in not installed:
```
sudo apt-get install python-pip
sudo pip install -U requests
```


# Install the MMT Binaries

Download from here: https://github.com/ModernMT/MMT/releases and then untar the files:

```
tar xvfz mmt-0.11-ubuntu14_04.tar.gz
cd mmt
```

Done! go to [README.md](README.md)
