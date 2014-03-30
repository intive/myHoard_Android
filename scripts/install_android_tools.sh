#!/bin/sh
#author AWA

# Install base Android SDK
sudo apt-get update -qq
if [ `uname -m` = x86_64 ]; then sudo apt-get install -qq --force-yes libgd2-xpm ia32-libs ia32-libs-multiarch > /dev/null; fi
wget http://dl.google.com/android/android-sdk_r22.0.5-linux.tgz
tar xzf android-sdk_r22.0.5-linux.tgz

# install android build tools
wget https://dl-ssl.google.com/android/repository/build-tools_r19.0.1-linux.zip
unzip build-tools_r19.0.1-linux.zip -d $ANDROID_HOME
mkdir -p $ANDROID_HOME/build-tools/
mv $ANDROID_HOME/android-4.4.2 $ANDROID_HOME/build-tools/19.0.1
# Install required components.
# For a full list, run `android list sdk -a --extended`
# Note that sysimg-18 downloads the ARM, x86 and MIPS images (we should optimize this).
# Other relevant API's
echo yes | android update sdk --filter platform-tools --no-ui --force
echo yes | android update sdk --filter android-19 --no-ui --force
echo yes | android update sdk --filter sysimg-19 --no-ui --force
echo yes | android update sdk --filter extra-android-support --no-ui --force
echo yes | android update sdk --filter extra-android-m2repository --no-ui --force