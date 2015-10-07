<!---
/*
 *
 * Copyright (c) 2015 Fraunhofer FOKUS, All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library. If not, see <http://www.gnu.org/licenses/>.
 *
 * AUTHORS: Louay Bassbouss (louay.bassbouss@fokus.fraunhofer.de)
 *
 */
-->

# cordova-plugin-hbbtv

This plugin provides an API that implements the client features of the HbbTV 2.0 CS Spec:
 * It allows Cordova applications to discover HbbTV Terminals
 * It allows Cordova applications to launch HbbTV Apps on discovered terminals
 * It allows to create App2App communication channel between the Cordova App (Companion App) and the HbbTV App using WebSockets

Please refer to the [HbbTV 2.0 Spec document -> section 14 about Companion Screen][hbbtv20spec] for more details.

## Installation

use the following cordova command to add the plugin to your Cordova App from npm:

```
cordova plugin add cordova-plugin-hbbtv
```

or add the following line to `config.xml` of your Cordova App to install the plugin automatically after adding a platform e.g. `cordova platform add android`:

```xml
<plugin name="cordova-plugin-hbbtv" version="0.0.2"/>
```

NOTE: the plugin can be also installed directly from this GitHub repository using:

```
cordova plugin add https://github.com/fraunhoferfokus/cordova-plugin-hbbtv.git
```

to remove the plugin use the following command:

```
cordova plugin remove de.fhg.fokus.famium.hbbtv
```

## Supported Platforms

* Android
* iOS (coming soon)

### Android Quirks

Currently no known.

### iOS Quirks

Currently no known.

## API

The API features are described in the following subsections.

Note: After installation the plugin is available to the Cordova application when the `deviceready` event has been fired.
Before this event has not been fired, there is not guarantee on the availability of this plugin. The namespace of the plugin
is `hbbtv`.

### Create `HbbTVTerminalManager`

The `HbbTVTerminalManager` offers all the APIs of the plugin to discover HbbTV terminals and launch HbbTV Apps.
The following example can be used to create a `HbbTVTerminalManager` instance:

```javascript
// hbbtv is the namespace of the HbbTV Cordova plugin.
// It is available after "deviceready" event is fired.
var hbbtvTerminalManager = hbbtv.createTerminalManager();
```

### Discover HbbTV Terminals

The `HbbTVTerminalManager.discoverTerminals(onTerminalDiscovery)` function can be used to Discover HbbTV terminals.
The `onTerminalDiscovery` is the success callback and will be triggered when discovery is done. The array of discovered
Terminals is passed as input when the callback is triggered. Each item in the array is an object from type `DiscoveredTerminal`
that offers the following properties:
* `enum_id`: the identifier of the Terminal. It can be used to launch a HbbTV App on the corresponding Terminal.
* `friendly_name`: the friendly name of the Terminal.
* `X_HbbTV_App2AppURL`: the App2App WebSocket endpoint.
* `X_HbbTV_InterDevSyncURL`: The Inter-Device Synchronization endpoint.
* `X_HbbTV_UserAgent`: The user agent of the HbbTV Engine.

For more information about Discovering HbbTV Terminals, please refer to the [HbbTV 2.0 Spec, Section 14.7][hbbtv20spec].

The following example demonstrates the API calls needed to discover HbbTV terminals and get details about each terminal:

```javascript
    hbbtvTerminalManager.discoverTerminals(function (discoveredTerminals) {
      var len = discoveredTerminals && discoveredTerminals.length || 0;
      console.log(len>0?len+" terminal(s) found": "no terminals found");
      for(var i in discoveredTerminals){
        var terminal = discoveredTerminals[i];
        console.log('Terminal #',i);
        console.log('   enum_id = ',terminal.enum_id);
        console.log('   friendly_name = ',terminal.friendly_name);
        console.log('   X_HbbTV_App2AppURL = ',terminal.X_HbbTV_App2AppURL);
        console.log('   X_HbbTV_InterDevSyncURL = ',terminal.X_HbbTV_InterDevSyncURL);
        console.log('   X_HbbTV_UserAgent = ',terminal.X_HbbTV_UserAgent);
      }
    });
```

### Launch HbbTV App

The `HbbTVTerminalManager.launchHbbTVApp(enumId,options,onHbbTVLaunch)` function can be used to launch a HbbTV app on a
`DiscoveredTerminal`. The input `enumId` is the `enum_id` of a discovered HbbTV Terminal. The second input `options` is a JSON
object that contains key/value-pairs that are needed to build the XML MHP Launch Request `<mhp:ServiceDiscovery>...</mhp:ServiceDiscovery>`
(see example for a list of supported properties). The last input `onHbbTVLaunch` is the success/error callback. For more information about
launching HbbTV Apps, please refer to the [HbbTV 2.0 Spec, Section 14.6][hbbtv20spec].

The following example demonstrates the API calls needed to launch a HbbTV App on a discovered Terminal:

```javascript
var terminal = ...; // terminal is set after discovery is completed
var enumId = terminal.enum_id; // terminal can be set after discovery is completed.
var APP_URL_BASE = "http://fraunhoferfokus.github.io/node-hbbtv/www/hbbtv-app.html";
var APP_LOCATION = "?channel=myChannel";
var options = {
  domainName: $DOMAIN_NAME$, // used for <mhp:ApplicationDiscovery DomainName="$DOMAIN_NAME$">. Default is an empty String
  appName: $APP_NAME$, // used for <mhp:appName Language="eng">$APP_NAME$</mhp:appName>. Default is an empty String
  orgId: $ORG_ID$, // used for <mhp:orgId>$ORG_ID$</mhp:orgId>. Default is an empty String
  appId: $APP_ID$, // used for <mhp:appId>$APP_ID$</mhp:appId>. Default is an empty String
  parentalRating: $PARENTAL_RATING$, // used for <hbb:ParentalRating>$PARENTAL_RATING$</hbb:ParentalRating>. <hbb:ParentalRating> element is created only if region is defined and not empty.
  region: $REGION$, // used for  <hbb:ParentalRating Region="$REGION$">. Default is an empty String
  appUrlBase: APP_URL_BASE, // used for <mhp:URLBase>$APP_URL_BASE$</mhp:URLBase>. Default is an empty String
  appLocation: APP_LOCATION, // used for <mhp:applicationLocation>$APP_LOCATION$</mhp:applicationLocation>. Default is an empty String
};
hbbtvTerminalManager.launchHbbTVApp(enumId,options,function (enumId, errorCode) {
  if(errorCode){
    console.error("An error is occurred while launching the HbbTV App. HbbTV Error Code = ", errorCode);
  }
  else {
    console.log("HbbTV App Launched successfully");
  }
});
```

### App2App Communication

The Cordova App can use the W3C WebSocket API already available in the Browser to create an App2App communication channel to the launched HbbTV App. The Endpoint of the
WebSocket Server on the discovered Terminal can be retrieved using `DiscoveredTerminal.X_HbbTV_App2AppURL`. For more information about HbbTV App2App communication
please refer to the [HbbTV 2.0 Spec, Section 14.5][hbbtv20spec].

The following example demonstrates the App2App:

```javascript
var terminal = ...; // terminal is set after discovery is completed
var app2AppEndpoint = terminal.X_HbbTV_App2AppURL;
var channel = "myChannel";
var ws = new WebSocket(app2AppEndpoint + channel);
  ws.binaryType = "arraybuffer";
  ws.onopen = function(evt) {
    console.log("Connection waiting ...");
  };
  ws.onclose = function(evt) {
    console.log("Connection closed");
  };
  ws.onerror = function (evt) {
    console.log("Connection error");
  };
  ws.onmessage = function(evt) {
    console.log(evt.data);
    if (evt.data == "pairingcompleted") {
      console.log("connection paired");
      ws.onmessage = function(evt) {
        console.log( "Message Received : " + evt.data);
      };
      var msg = "Hello from Companion Screen";
      ws.send(msg);
      if(typeof Int8Array != "undefined"){
        var array = [0,1,2,3,4,5,6,7,8,9];
        var binary = new Int8Array(array).buffer;
        ws.send(binary);
      }
    } else {
      log("Unexpected message received from terminal.");
      ws.close();
    }
  };
```

[hbbtv20spec]: https://www.hbbtv.org/pages/about_hbbtv/HbbTV_specification_2_0.pdf