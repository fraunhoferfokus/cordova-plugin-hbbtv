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

import android.os.AsyncTask;
import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import de.fhg.fokus.famium.hbbtv.ssdp.Ssdp;
import de.fhg.fokus.famium.hbbtv.ssdp.SsdpMessage;

/**
 * Created by lba on 22/04/15.
 */
public class Dial {
  public static final String TAG = "Dial";
  public static final String DIAL_SERVICE_TYPE = "urn:dial-multiscreen-org:service:dial:1";
  public static final int DEFAULT_SEARCH_TIMEOUT = 5000;
  public static final int MIN_SEARCH_TIMEOUT = 1000;
  public static final int MAX_SEARCH_TIMEOUT = 50000;

  private DeviceFoundCallback mDeviceFoundCallback;
  private Ssdp.SsdpCallback mSsdpCallback;
  private Ssdp mSsdp;

  public Dial(DeviceFoundCallback deviceFoundCallback){
    mDeviceFoundCallback = deviceFoundCallback;
  }

  public DeviceFoundCallback getDialCallback() {
    return mDeviceFoundCallback;
  }

  public synchronized Ssdp.SsdpCallback getSsdpCallback() {
    if (mSsdpCallback == null){
      mSsdpCallback = new Ssdp.SsdpCallback() {
        @Override
        public void onSsdpMessageReceived(SsdpMessage ssdpMessage) {
          if (DIAL_SERVICE_TYPE.equals(ssdpMessage.get("ST"))){
            String deviceDescriptionUrl = ssdpMessage.get("LOCATION");
            if (deviceDescriptionUrl != null){
              getDialDevice(deviceDescriptionUrl, ssdpMessage);
            }
          }
          Log.d(TAG,"DIAL Device found: "+ssdpMessage);
        }
      };
    }
    return mSsdpCallback;
  }

  public void search() throws IOException{
    search(DEFAULT_SEARCH_TIMEOUT);
  }

  public synchronized void search(int timeoutInSeconds) throws IOException{
    int timeout = Math.max(MIN_SEARCH_TIMEOUT,Math.min(timeoutInSeconds,MAX_SEARCH_TIMEOUT));
    if (getSsdp().start(timeout)){
      getSsdp().search(DIAL_SERVICE_TYPE);
    }
  }

  public synchronized void cancel() throws IOException{
    getSsdp().stop();
  }

  public synchronized Ssdp getSsdp() throws IOException{
    if (mSsdp == null){
      mSsdp = new Ssdp(getSsdpCallback());
    }
    return mSsdp;
  }

  private void getDialDevice(String deviceDescriptionUrl, SsdpMessage ssdpMessage){
    DialDevice device = new DialDevice(deviceDescriptionUrl, ssdpMessage);
    device.setUSN(ssdpMessage.get("USN"));
    new DownloadDeviceDescriptionTask().execute(device);
  }

  public interface DeviceFoundCallback {
    public void onDialDeviceFound(DialDevice dialDevice);
  }

  public interface GetAppInfoCallback {
    public void onReceiveAppInfo(DialAppInfo appInfo);
  }

  public interface LaunchAppCallback {
    public void onLaunchApp(Integer statusCode);
  }

  public interface StopAppCallback {
    public void onStopApp(Integer statusCode);
  }

  private class DownloadDeviceDescriptionTask extends AsyncTask<DialDevice, Void, DialDevice> {
    private String ns = null;
    @Override
    protected DialDevice doInBackground(DialDevice... devices) {
      try {
        return loadAndParseDeviceDescription(devices[0]);
      } catch (Exception e) {
        Log.e(TAG, e.getMessage(), e);
        return null;
      }
    }

    @Override
    protected void onPostExecute(DialDevice device) {
      if(device != null && getDialCallback() != null){
        getDialCallback().onDialDeviceFound(device);
      }
    }

    private DialDevice loadAndParseDeviceDescription(DialDevice device) throws XmlPullParserException, IOException {
      InputStream is = null;
      try{
        is = downloadDeviceDescription(device);
        parseDeviceDescription(is, device);
      }
      finally {
        if (is != null){
          is.close();
        }
      }
      return device;
    }

    private InputStream downloadDeviceDescription(DialDevice device) throws IOException {
      URL url = new URL(device.getDescriptionUrl());
      HttpURLConnection conn = (HttpURLConnection) url.openConnection();
      conn.setReadTimeout(10000 /* milliseconds */);
      conn.setConnectTimeout(15000 /* milliseconds */);
      conn.setRequestMethod("GET");
      conn.setDoInput(true);
      // Starts the query
      conn.connect();
      device.setApplicationUrl(conn.getHeaderField("Application-URL"));
      return conn.getInputStream();
    }

    private void parseDeviceDescription(InputStream is, DialDevice device) throws XmlPullParserException, IOException{
      XmlPullParser parser = Xml.newPullParser();
      //parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
      parser.setInput(is, null);
      parser.nextTag();
      readDeviceDescription(parser, device);
    };

    private void readDeviceDescription(XmlPullParser parser, DialDevice device) throws XmlPullParserException, IOException{
      parser.require(XmlPullParser.START_TAG, ns, "root");
      Log.d(TAG, "XML parser "+parser.getName());
      while (parser.next() != XmlPullParser.END_TAG) {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
          continue;
        }
        String name = parser.getName();
        if (name.equals("deviceType")) {
          device.setType(readText(parser, name));
        }
        else if(name.equals("friendlyName")){
          device.setFriendlyName(readText(parser, name));
        }
        else if(name.equals("manufacturer")){
          device.setManufacturer(readText(parser, name));
        }
        else if(name.equals("manufacturerURL")){
          device.setManufacturerUrl(readText(parser, name));
        }
        else if(name.equals("modelDescription")){
          device.setModelDescription(readText(parser, name));
        }
        else if(name.equals("modelName")){
          device.setModelName(readText(parser, name));
        }
        else if(name.equals("UDN")){
          device.setUDN(readText(parser, name));
        }
        else if(!name.equals("device")){
          skip(parser);
        }
      }
    }

    private void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
      if (parser.getEventType() != XmlPullParser.START_TAG) {
        throw new IllegalStateException();
      }
      int depth = 1;
      while (depth != 0) {
        switch (parser.next()) {
          case XmlPullParser.END_TAG:
            depth--;
            break;
          case XmlPullParser.START_TAG:
            depth++;
            break;
        }
      }
    }

    private String readText(XmlPullParser parser, String tag) throws IOException, XmlPullParserException {
      String result = "";
      parser.require(XmlPullParser.START_TAG, ns, tag);
      if (parser.next() == XmlPullParser.TEXT) {
        result = parser.getText();
        parser.nextTag();
      }
      parser.require(XmlPullParser.END_TAG, ns, tag);
      return result;
    }
  }
}