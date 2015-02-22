# WemoreJ [![Build Status](http://img.shields.io/travis/dhleong/wemorej.svg?style=flat)](https://travis-ci.org/dhleong/wemorej)

The WemoreJ project is two-fold:

1. A [Reactive](https://github.com/ReactiveX/RxJava) Java library for Wemo devices
2. A simple Android widget that uses it to toggle Wemo switches

## Why?

Also two-fold:

1. I wanted to play with RxJava.
2. The official Wemo app is sloooooow.

## Library Usage

```java
// find a switch called Lights and toggle it
new Wemore().search()
    .filter((d) -> d.hasFriendlyNameLike("Lights"))
    .toBlocking()
    .first()
    .toggleBinaryState();
```
