
/*******************************************************************************
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
 ******************************************************************************/
var exec = require('cordova/exec');

var terminalCounter = 1;
var discoveredTerminals = {};

/**
 * A DiscoveredTerminal object shall have the following properties:
 *  - readonly Number enum_id: A unique ID for a discovered HbbTV terminal
 *  - readonly String friendly_name: A discovered terminal may provide a friendly name, e.g. “Muttleys TV”, for an HbbTV application to make use of.
 * 	- readonly String X_HbbTV_App2AppURL: The remote service endpoint on the discovered HbbTV terminal for application to application communication
 * 	- readonly String X_HbbTV_InterDevSyncURL: The remote service endpoint on the discovered HbbTV terminal for inter-device synchronisation
 * 	- readonly String X_HbbTV_UserAgent: The User Agent string of the discovered HbbTV terminal
 */
var DiscoveredTerminal = function(enum_id, friendly_name, X_HbbTV_App2AppURL, X_HbbTV_InterDevSyncURL, X_HbbTV_UserAgent){
    Object.defineProperty(this, "enum_id", {
        get: function () {
            return enum_id;
        }
    });
    Object.defineProperty(this, "friendly_name", {
        get: function () {
            return friendly_name;
        }
    });
    Object.defineProperty(this, "X_HbbTV_App2AppURL", {
        get: function () {
            return X_HbbTV_App2AppURL;
        }
    });
    Object.defineProperty(this, "X_HbbTV_InterDevSyncURL", {
        get: function () {
            return X_HbbTV_InterDevSyncURL;
        }
    });
    Object.defineProperty(this, "X_HbbTV_UserAgent", {
        get: function () {
            return X_HbbTV_UserAgent;
        }
    });
};

/**
 * @constructor
 */
var HbbTVTerminalManager = function(){
    Object.defineProperty(this, "discoverTerminals", {
        get: function () {
            return discoverTerminals;
        }
    });

    Object.defineProperty(this, "launchHbbTVApp", {
        get: function () {
            return launchHbbTVApp;
        }
    });
};

/**
 * Boolean discoverTerminals(function onTerminalDiscovery)
 * callback onTerminalDiscovery (DiscoveredTerminal[])
 */
var discoverTerminals = function(onTerminalDiscovery){
    var success = function (terminals) {
        var res = [];
        for(var i=0;i<terminals.length; i++){
            var terminal = terminals[i];
            var launchUrl = terminal.launchUrl;
            var oldTerminal = discoveredTerminals[launchUrl];
            var enumId = oldTerminal && oldTerminal.enum_id || terminalCounter++;
            var newTerminal = new DiscoveredTerminal(enumId, terminal.friendlyName, terminal.X_HbbTV_App2AppURL, terminal.X_HbbTV_InterDevSyncURL, terminal.X_HbbTV_UserAgent);
            discoveredTerminals[launchUrl] = newTerminal;
            discoveredTerminals[enumId] = terminal;
            res.push(newTerminal);
        }
        onTerminalDiscovery && onTerminalDiscovery.call(null,res);
    };
    var error = function (code) {
        var res = [];
        onTerminalDiscovery && onTerminalDiscovery.call(null,res);
    };
    exec(success, error, "HbbTV", "discoverTerminals", []);
    return true;
};

/**
 * Boolean launchHbbTVApp(Integer enum_id, Object options, function onCSLaunch)
 * callback onCSLaunch(int enum_id, int error_code)
 * Error Codes Values:
 *	0: op_rejected
 *  2: op_not_guaranteed
 *  3: invalid_id
 *  4: general_error
 */
var launchHbbTVApp = function(enumId,options,onHbbTVLaunch){
    var terminal = discoveredTerminals[enumId];
    var code = null;
    if(!terminal){
        code = 3;
        onHbbTVLaunch && onHbbTVLaunch.call(null,enumId,code);
        return false;
    }
    var success = function (statusCode) {
        onHbbTVLaunch && onHbbTVLaunch.call(null,enumId);
    };
    var error = function (statusCode) {
        code = 4;
        onHbbTVLaunch && onHbbTVLaunch.call(null,enumId,code);
    };
    var payload = createXmlLaunchRequest(options);
    exec(success, error, "HbbTV", "launchHbbTVApp", [terminal.applicationUrl, payload]);
    return true;
};

var createXmlLaunchRequest = function(options){
    var xml = '<?xml version="1.0" encoding="UTF-8"?> ' +
        '<mhp:ServiceDiscovery xmlns:mhp="urn:dvb:mhp:2009" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:hbb="urn:hbbtv:application_descriptor:2014" > ' +
        '<mhp:ApplicationDiscovery DomainName="'+(options.domainName || "")+'"> ' +
        '<mhp:ApplicationList> ' +
        '<mhp:Application> ' +
        '<mhp:appName Language="eng">'+(options.appName || "")+'</mhp:appName> ' +
        '<mhp:applicationIdentifier> ' +
        '<mhp:orgId>'+(options.orgId || "")+'</mhp:orgId> ' +
        '<mhp:appId>'+(options.appId || "")+'</mhp:appId> ' +
        '</mhp:applicationIdentifier> ' +
        '<mhp:applicationDescriptor xsi:type="hbb:HbbTVApplicationDescriptor"> ' +
        '<mhp:type> ' +
        '<mhp:OtherApp>application/vnd.hbbtv.xhtml+xml</mhp:OtherApp> ' +
        '</mhp:type> ' +
        '<mhp:controlCode>AUTOSTART</mhp:controlCode> ' +
        '<mhp:visibility>VISIBLE_ALL</mhp:visibility> ' +
        '<mhp:serviceBound>false</mhp:serviceBound> ' +
        '<mhp:priority>1</mhp:priority> ' +
        '<mhp:version>01</mhp:version> ' +
        '<mhp:mhpVersion> ' +
        '<mhp:profile>0</mhp:profile> ' +
        '<mhp:versionMajor>1</mhp:versionMajor> ' +
        '<mhp:versionMinor>3</mhp:versionMinor> ' +
        '<mhp:versionMicro>1</mhp:versionMicro> ' +
        '</mhp:mhpVersion> ' +
        (options.parentalRating && ('<hbb:ParentalRating Scheme="dvb-si" Region="'+(options.region || "")+'">'+options.parentalRating+'</hbb:ParentalRating> ') || ' ')+
        '</mhp:applicationDescriptor> ' +
        '<mhp:applicationTransport xsi:type="mhp:HTTPTransportType"> ' +
        '<mhp:URLBase>'+(options.appUrlBase || "")+'</mhp:URLBase> ' +
        '</mhp:applicationTransport> ' +
        '<mhp:applicationLocation>'+(options.appLocation || "")+'</mhp:applicationLocation> ' +
        '</mhp:Application> ' +
        '</mhp:ApplicationList> ' +
        '</mhp:ApplicationDiscovery> ' +
    '</mhp:ServiceDiscovery>';
    return xml;
};

exports.createTerminalManager = function(){
    return new HbbTVTerminalManager();
};