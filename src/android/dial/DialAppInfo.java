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
package de.fhg.fokus.famium.hbbtv.dial;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by lba on 22/04/15.
 */
public class DialAppInfo {
  public static final String TAG = "DialAppInfo";
  private DialDevice mDialDevice;
  private String mName;
  private Boolean mAllowStop;
  private String mState;
  private String runId;
  private Map<String,String> mAdditionalData;
  public DialAppInfo(DialDevice dialDevice, String name){
    mDialDevice = dialDevice;
    mName = name;
  }

  public DialDevice getDialDevice() {
    return mDialDevice;
  }

  public void setDialDevice(DialDevice mDialDevice) {
    this.mDialDevice = mDialDevice;
  }

  public String getName() {
    return mName;
  }

  public void setName(String mName) {
    this.mName = mName;
  }

  public Boolean getAllowStop() {
    return mAllowStop;
  }

  public void setAllowStop(Boolean mAllowStop) {
    this.mAllowStop = mAllowStop;
  }

  public String getState() {
    return mState;
  }

  public void setState(String mState) {
    this.mState = mState;
  }

  public String getRunId() {
    return runId;
  }

  public void setRunId(String runId) {
    this.runId = runId;
  }

  public synchronized Map<String, String> getAdditionalData() {
    if(mAdditionalData == null){
      mAdditionalData = new HashMap<String, String>();
    }
    return mAdditionalData;
  }

  public String getAdditionalData(String name){
    return getAdditionalData().get(name);
  }
}
