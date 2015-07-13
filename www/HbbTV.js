
/*******************************************************************************
 *
 * Copyright (c) 2015 Louay Bassbouss, Fraunhofer FOKUS, All rights reserved.
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
        for(var appUrl in terminals){
            var oldTerminal = discoveredTerminals[appUrl];
            var terminal = terminals[appUrl];
            terminal.id = appUrl;
            var enumId = oldTerminal && oldTerminal.enumId || terminalCounter++;
            var newTerminal = new DiscoveredTerminal(enumId, terminal.friendlyName, terminal.app2AppURL, terminal.interDevSyncURL, terminal.userAgent);
            discoveredTerminals[appUrl] = newTerminal;
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
    var success = function (code) {
        onHbbTVLaunch && onHbbTVLaunch.call(null,enumId,code);
    };
    var error = function (code) {
        onHbbTVLaunch && onHbbTVLaunch.call(null,enumId,code);
    };
    exec(success, error, "HbbTV", "launchHbbTVApp", [terminal.id, options]);
    return true;
};

exports.createTerminalManager = function(){
    return new HbbTVTerminalManager();
};