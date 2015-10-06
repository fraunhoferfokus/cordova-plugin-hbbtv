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
package de.fhg.fokus.famium.hbbtv;

import android.util.Log;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import de.fhg.fokus.famium.hbbtv.dial.Dial;
import de.fhg.fokus.famium.hbbtv.dial.DialAppInfo;
import de.fhg.fokus.famium.hbbtv.dial.DialDevice;

/**
 * HbbTV Cordova Plugin Class.
 */
public class HbbTV extends CordovaPlugin {
  public static final String TAG = "HbbTV";
  private HbbTvManager hbbTvManager;
  private ArrayList<CallbackContext> mPendingDiscoveryRequests;
  @Override
  public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
    if (action.equals("discoverTerminals")) {
      return this.discoverTerminals(args,callbackContext);
    }
    else if (action.equals("launchHbbTVApp")) {
      return this.launchHbbTVApp(args, callbackContext);
    }
    return false;
  }

  private synchronized boolean discoverTerminals(JSONArray args, CallbackContext callbackContext) {
    getPendingDiscoveryRequests().add(callbackContext);
    getHbbTvManager().discoverTerminals();
    return true;
  }

  private boolean launchHbbTVApp(JSONArray args, final CallbackContext callbackContext) {
    try{
      String applicationUrl = args.getString(0);
      String payload = args.getString(1);
      DialDevice dialDevice = new DialDevice(applicationUrl);
      dialDevice.launchApp("HbbTV", payload, "text/plain", new Dial.LaunchAppCallback() {
        @Override
        public void onLaunchApp(Integer statusCode) {
          if(statusCode != null && statusCode>=200 && statusCode<300){
            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK,statusCode));
          }
          else {
            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR,statusCode));
          }
        }
      });
    }
    catch (Exception e){
      Log.e(TAG,e.getMessage(),e);
      callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR,500));
    }
    return true;
  }

  private HbbTvManager getHbbTvManager() {
    if (hbbTvManager == null){
      hbbTvManager = new HbbTvManager(new HbbTvManager.DiscoverTerminalsCallback() {
        @Override
        public void onDiscoverTerminals(Map<String, DialAppInfo> terminals) {
          synchronized (HbbTV.this){
            JSONArray arr = new JSONArray();
            for (DialAppInfo terminal: terminals.values()){
              DialDevice device = terminal.getDialDevice();
              HashMap<String,Object> copy = new HashMap<String,Object>();
              copy.put("descriptionUrl",device.getDescriptionUrl());
              copy.put("launchUrl",device.getApplicationUrl()+"/HbbTV");
              copy.put("applicationUrl",device.getApplicationUrl());
              copy.put("usn",device.getUSN());
              copy.put("type",device.getType());
              copy.put("friendlyName",device.getFriendlyName());
              copy.put("manufacturer",device.getManufacturer());
              copy.put("manufacturerUrl",device.getManufacturerUrl());
              copy.put("modelDescription",device.getModelDescription());
              copy.put("modelName",device.getModelName());
              copy.put("udn",device.getUDN());
              copy.put("state",terminal.getState());
              copy.putAll(terminal.getAdditionalData());
              arr.put(new JSONObject(copy));
            }
            for (CallbackContext callbackContext: getPendingDiscoveryRequests()){
              if (callbackContext != null){
                PluginResult result = new PluginResult(PluginResult.Status.OK, arr);
                callbackContext.sendPluginResult(result);
              }
            }
            getPendingDiscoveryRequests().clear();
          }
        }
      });
    }
    return hbbTvManager;
  }

  public ArrayList<CallbackContext> getPendingDiscoveryRequests() {
    if(mPendingDiscoveryRequests == null){
      mPendingDiscoveryRequests = new ArrayList<CallbackContext>();
    }
    return mPendingDiscoveryRequests;
  }
}
