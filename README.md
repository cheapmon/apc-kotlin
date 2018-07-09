`apc` is a simple application for extracting privacy policy texts from 
different Android apps.

## Installation

* Install [`adb`](https://developer.android.com/studio/#command-tools) and 
[`gradle`](https://gradle.org/install/)
* Run `gradle clean build`

Before you run, attach any Android (virtual) device and enable Android debugging.

## Usage
```
usage: java MainKt [-h] [-i <arg>] [-f <arg>] [-d <arg>] [-a <arg>] [-m]
 -h,--help              this help message
 -i,--ids <arg>         app ids
 -f,--file <arg>        file containing app ids
 -d,--device <arg>      device to run extraction on
 -a,--algorithm <arg>   search algorithm
 -m,--model             extract model of app
```