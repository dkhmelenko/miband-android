# Overview

[![GitHub Actions](https://github.com/dkhmelenko/miband-android/workflows/Android%20CI/badge.svg)](https://github.com/dkhmelenko/miband-android/actions)

This is unofficial SDK for Mi Band. This repository contains 2 modules:
* miband-sdk-kotlin -- SDK for interraction with the MiBand
* app -- Sample application demonstrating how to work with SDK.

The app was not tested with Mi Band 2 or newer bands. In case some of the method returns incorrect data or works wrong, pleare report an issue. 

The idea came from [this project](https://github.com/pangliang/miband-sdk-android). However, due to reactive vision of SDK it was reimplemented. Most of methods are implemented using [RxJava](https://github.com/ReactiveX/RxJava) library. Therefore the basic knowledge of reactive streams is required.

# Contribution
In case you have ideas or found an issue, don't hesitate to create pull request or an issue.

# How to use
**IMPORTANT: Use this SDK on your own risk, developer of this SDK is NOT responsible for any unpredictable results.** <br/> <br/>

### Discovery
In order to start sending and receiving commands from the MiBand, you have to connect and pair with it. Available devices for connection can be found using `startScan()` method.

```kotlin
miBand.startScan().subscribe { result ->
                val device = result.device
                // save found device
            }
```
Scanning can be interrupted any time using the method `stopScan()`.

### Connection
Next action is to establish connection to the device (MiBand). The fuction `connect(device)` is responsible for that. The fuction returns Observable which emits a boolean value indicating if connection was established or not.
```kotlin
miBand.connect(device).subscribe { connected ->
                if (connected) {
                    // connection established
                } else {
                    // resolve connection issue
                }
            }
```
After successfully established connection device needs to be paired. The function `pair()` will do pairing to the connected device.
```kotlin
miBand.pair().subscribe { 
                // device is ready for communication
            }
```
### Communication
When the band is connected and paired many different actions can be performed such as read battery information, start/stop vibration, read and change user information, read current steps in realtime, read heartrate and many other.

There are few examples how to perform those actions. 
*Read battery information*
```kotlin
miBand.batteryInfo.subscribe { batteryInfo ->
                ...
            }
```
*Start vibration*
```kotlin
miBand.startVibration(VibrationMode.VIBRATION_WITHOUT_LED).subscribe {
                ...
            }
```
*Realtime steps notification*
```kotlin
miBand.setRealtimeStepsNotifyListener(object: RealtimeStepsNotifyListener {
                override fun onNotify(steps: Int) {
                    ...
                }
            })
miBand.enableRealtimeStepsNotify().subscribe()
```
Reading heartrate can be done in a similar way: first setup the listener using `setHeartRateScanListener()` and then call the method `startHeartRateScan()`.


All available methods for reading band information are available in class [MiBand.kt](https://github.com/dkhmelenko/miband-android/blob/master/miband-sdk-kotlin/src/main/java/com/khmelenko/lab/miband/MiBand.kt). All methods have JavaDoc documentation explaining how the method works. They are:
* startScan()
* stopScan()
* connect(device)
* pair()
* readRssi()
* batteryInfo()
* startVibration() / stopVibration()
* enableRealtimeStepsNotify() / disableRealtimeStepsNotify()
* setUserInfo(userInfo)
* startHeartRateScan()
* setLedColor()

# License

[Apache Licence 2.0](http://www.apache.org/licenses/LICENSE-2.0)

Copyright 2020 Dmytro Khmelenko

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
