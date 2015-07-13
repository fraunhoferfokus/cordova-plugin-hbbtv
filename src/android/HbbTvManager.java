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
package de.fhg.fokus.famium.hbbtv;

import android.util.Log;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import de.fhg.fokus.famium.hbbtv.dial.Dial;
import de.fhg.fokus.famium.hbbtv.dial.DialAppInfo;
import de.fhg.fokus.famium.hbbtv.dial.DialDevice;

/**
 * Created by lba on 23/04/15.
 */
public class HbbTvManager{
  public static final String TAG = "HbbTvManager";
  public static final int TIMEOUT = 5000;
  private Dial mDial;
  private Map<String, DialDevice> mHbbTvDialDevices;
  private Map<String, DialDevice> mTmpHbbTvDialDevices;
  private DiscoverTerminalsCallback mDiscoverTerminalsCallback;
  private boolean searching = false;
  public HbbTvManager(){

  };

  public HbbTvManager(DiscoverTerminalsCallback discoverTerminalsCallback){
    mDiscoverTerminalsCallback = discoverTerminalsCallback;
  };

  public DiscoverTerminalsCallback getDiscoverTerminalsCallback() {
    return mDiscoverTerminalsCallback;
  }

  public void setDiscoverTerminalsCallback(DiscoverTerminalsCallback discoverTerminalsCallback) {
    this.mDiscoverTerminalsCallback = discoverTerminalsCallback;
  }

  public synchronized void discoverTerminals(){
    if(!searching){
      searching = true;
      try {
        getTmpHbbTvDialDevices().clear();
        getDial().search(TIMEOUT);
        wait(TIMEOUT);
        getHbbTvDialDevicesInternal().clear();
        getHbbTvDialDevicesInternal().putAll(getTmpHbbTvDialDevices());
      }
      catch (IOException e){
        Log.e(TAG,e.getMessage(),e);
      }
      catch (InterruptedException e){
        Log.e(TAG,e.getMessage(),e);
      }
      finally {
        getTmpHbbTvDialDevices().clear();
        if (getDiscoverTerminalsCallback() != null){
          getDiscoverTerminalsCallback().onDiscoverTerminals(getLastFoundTerminals());
        }
        searching = false;
      }
    }
  }

  public synchronized Map<String, DialDevice> getLastFoundTerminals() {
    return new HashMap<String, DialDevice>(getHbbTvDialDevicesInternal());
  }

  private synchronized Map<String, DialDevice> getHbbTvDialDevicesInternal() {
    if (mHbbTvDialDevices == null){
      mHbbTvDialDevices = new HashMap<String, DialDevice>();
    }
    return mHbbTvDialDevices;
  }

  private synchronized Map<String, DialDevice> getTmpHbbTvDialDevices() {
    if(mTmpHbbTvDialDevices == null ){
      mTmpHbbTvDialDevices = new HashMap<String, DialDevice>();
    }
    return mTmpHbbTvDialDevices;
  }

  private synchronized  Dial getDial(){
    if(mDial == null){
      mDial = new Dial(new Dial.DeviceFoundCallback() {
        @Override
        public void onDialDeviceFound(final DialDevice dialDevice) {
          dialDevice.getAppInfo("HbbTV",new Dial.GetAppInfoCallback() {
            @Override
            public void onReceiveAppInfo(DialAppInfo appInfo) {
              if(appInfo != null && dialDevice.getApp2AppUrl() != null){
                getTmpHbbTvDialDevices().put(dialDevice.getUDN(),dialDevice);
              }
            }
          });
        }
      });
    }
    return mDial;
  }

  public interface DiscoverTerminalsCallback {
    public void onDiscoverTerminals(Map<String,DialDevice> terminals);
  }
}
