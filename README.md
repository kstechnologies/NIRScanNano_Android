# NIRScan Nano Project Description

The purpose of this project is to provide the simplest possible template for developers to create their own native Android app for communicating with the Texas Instruments' NIRScan Nano Product.

This project allows the user to scan for and connect to the NanoScan using Bluetooth Low Energy (BLE).  It is feature poor, and the intent is for you to take the KSTNanoSDK.java files, add it to your own product, and use this SDK as an easy means of communicating with the NanoScan. 

Please consider searching on the TODO pragma marks throughout this code to see where we are taking it next. Our understanding of the NIRScan Nano, the firmware, the hardware, and the basic business logic of interacting with the device has all been under development and very dynamic.  There are "less than ideal" things in this source, but we believe it's in a great state to let you build great things!  Tell us what you're doing with this source via [email](mailto://sensing@kstechnologies.com)  or [Twitter](http://www.twitter.com/kstechnologies) .

# Compatibility

* Android 5.0+ (Lollipop / SDK 21)
* Requires Bluetooth Low Energy (BLE) Radio
* Requires TI NIRScanNano EVM running Firmware v2.0+

# Build Requirements

* nirscannanolibrary.aar (1)
* NanoBLEService.java
* MPAndroidChart (2)

(1) In order to get the source code for the Spectrum C Librarym which is built into the nirscannanolibrary.aar, you must execute a Non-Disclosure Agreement directly with Texas Instruments.
(2) This is an awesome open source project created by Philipp Jahoda and is designed for building beautiful graphcs and charts for Android.  Please see [Phil's github repo](https://github.com/PhilJay/MPAndroidChart) for more information.

If you just want to test out the app and get data as fast as possible, consider just downloading the compiled version of this app, [available for free on the Google Play Store](https://play.google.com/store/apps/details?id=com.kstechnologies.NanoScan) .

# Version

*  Version 1.0, Build 8
*  Opencsv 3.6
*  MPAndroidChart
*  Android Studio 1.5.1

# First Steps

*  After executing a Non-Disclosure Agreement directly with Texas Instruments and obtaining the Spectrum C Library source files, use the NDK to compile libdlpspectrum according to the Android.mk and Application.mk files. The interface.c file is used to provide a JNI wrapper around the Spectrum C Library. Add JNI functions here to extend the app's usage of the Spectrum C library.

*  Import libdlpspectrum.so into the project and include the KSTNanoSDK and NanoBLEService files.

* Use the NanoBLEService as a template for interacting with the Nano over BLE, and the KSTNanoSDK as a way to issue specific commands to the Nano.

# Contact Information

Please [contact KST](mailto://sensing@kstechnologies.com) for any questions you may have regarding this SDK or for requesting custom hardware, firmware, app, or cloud development work based on TI's DLP Technology.  You can also [visit the KS Technologies website](http://www.kstechnologies.com) for more information about our company.

# FAQ

tbd

# License

Software License Agreement (BSD License)

Copyright (c) 2015, KS Technologies, LLC
All rights reserved.

Redistribution and use of this software in source and binary forms,
with or without modification, are permitted provided that the following conditions are met:

* Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.

* Neither the name of KS Technologies, LLC nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission of KS Technologies, LLC.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
