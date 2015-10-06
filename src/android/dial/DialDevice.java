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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

import de.fhg.fokus.famium.hbbtv.ssdp.SsdpMessage;

/**
 * Created by lba on 22/04/15.
 */
public class DialDevice {
  public static final String TAG = "DialDevice";
  private SsdpMessage mSsdpMessage;
  private String mDescriptionUrl;
  private String mApplicationUrl;
  private String mUSN;
  private String mType;
  private String mFriendlyName;
  private String mManufacturer;
  private String mManufacturerUrl;
  private String mModelDescription;
  private String mModelName;
  private String mUDN;
  private String mPresentationUrl;

  public DialDevice(String descriptionUrl, SsdpMessage ssdpMessage){
    mDescriptionUrl = descriptionUrl;
    mSsdpMessage = ssdpMessage;
  }
  public DialDevice(String applicationUrl){
    mApplicationUrl = applicationUrl;
  }
  public SsdpMessage getSsdpMessage() {
    return mSsdpMessage;
  }

  public void setSsdpMessage(SsdpMessage mSsdpMessage) {
    this.mSsdpMessage = mSsdpMessage;
  }

  public String getType() {
    return mType;
  }

  public void setType(String mType) {
    this.mType = mType;
  }

  public String getDescriptionUrl() {
    return mDescriptionUrl;
  }

  public void setDescriptionUrl(String mDescriptionUrl) {
    this.mDescriptionUrl = mDescriptionUrl;
  }

  public String getApplicationUrl() {
    return mApplicationUrl;
  }

  public void setApplicationUrl(String mApplicationUrl) {
    if (mApplicationUrl != null && !mApplicationUrl.endsWith("/")){
      mApplicationUrl = mApplicationUrl+"/";
    }
    this.mApplicationUrl = mApplicationUrl;
  }

  public String getFriendlyName() {
    return mFriendlyName;
  }

  public void setFriendlyName(String mFriendlyName) {
    this.mFriendlyName = mFriendlyName;
  }

  public String getManufacturer() {
    return mManufacturer;
  }

  public void setManufacturer(String mManufacturer) {
    this.mManufacturer = mManufacturer;
  }

  public String getManufacturerUrl() {
    return mManufacturerUrl;
  }

  public void setManufacturerUrl(String mManufacturerUrl) {
    this.mManufacturerUrl = mManufacturerUrl;
  }

  public String getModelDescription() {
    return mModelDescription;
  }

  public void setModelDescription(String mModelDescription) {
    this.mModelDescription = mModelDescription;
  }

  public String getModelName() {
    return mModelName;
  }

  public void setModelName(String mModelName) {
    this.mModelName = mModelName;
  }

  public String getUDN() {
    return mUDN;
  }

  public void setUDN(String mUDN) {
    this.mUDN = mUDN;
  }

  public String getUSN() {
    return mUSN;
  }

  public void setUSN(String mUSN) {
    this.mUSN = mUSN;
  }

  public String getPresentationUrl() {
    return mPresentationUrl;
  }

  public void setPresentationUrl(String mPresentationUrl) {
    this.mPresentationUrl = mPresentationUrl;
  }

  public void getAppInfo(String appName, Dial.GetAppInfoCallback getAppInfoCallback){
    if(getApplicationUrl() != null){
      DialAppInfo appInfo = new DialAppInfo(this,appName);
      new DownloadAppInfoTask(getAppInfoCallback).execute(appInfo);
    }
  }

  public void launchApp(String appName, String launchData, String contentType, Dial.LaunchAppCallback launchAppCallback){
    if(getApplicationUrl() != null){
      String appUrl = getApplicationUrl()+appName;
      new LaunchAppTask(launchAppCallback).execute(appUrl, launchData, contentType);
    }
  }

  public void stopApp(String appName, String runId, Dial.StopAppCallback stopAppCallback){
    if(getApplicationUrl() != null){
      String stopUrl = getApplicationUrl()+appName+"/"+runId;
      new StopAppTask(stopAppCallback).execute(stopUrl);
    }
  }

  private class DownloadAppInfoTask extends AsyncTask<DialAppInfo, Void, DialAppInfo> {
    private String ns = null;
    private Dial.GetAppInfoCallback mGetAppInfoCallback;
    public DownloadAppInfoTask(Dial.GetAppInfoCallback getAppInfoCallback){
      mGetAppInfoCallback = getAppInfoCallback;
    }

    public Dial.GetAppInfoCallback getGetAppInfoCallback() {
      return mGetAppInfoCallback;
    }

    @Override
    protected DialAppInfo doInBackground(DialAppInfo... appInfos) {
      try {
        return loadAndParseAppInfo(appInfos[0]);
      } catch (Exception e) {
        Log.e(TAG, e.getMessage(), e);
        return null;
      }
    }

    @Override
    protected void onPostExecute(DialAppInfo appInfo) {
      if(getGetAppInfoCallback() != null){
        getGetAppInfoCallback().onReceiveAppInfo(appInfo);
      }
    }

    private DialAppInfo loadAndParseAppInfo(DialAppInfo appInfo) throws XmlPullParserException, IOException {
      InputStream is = null;
      try{
        is = downloadAppInfo(appInfo);
        parseAppInfo(is, appInfo);
      }
      finally {
        if (is != null){
          is.close();
        }
      }
      return appInfo;
    }

    private InputStream downloadAppInfo(DialAppInfo appInfo) throws IOException {
      URL url = new URL(appInfo.getDialDevice().getApplicationUrl()+appInfo.getName());
      HttpURLConnection conn = (HttpURLConnection) url.openConnection();
      conn.setReadTimeout(10000 /* milliseconds */);
      conn.setConnectTimeout(15000 /* milliseconds */);
      conn.setRequestMethod("GET");
      conn.setDoInput(true);
      // Starts the query
      conn.connect();
      return conn.getInputStream();
    }

    private void parseAppInfo(InputStream is, DialAppInfo appInfo) throws XmlPullParserException, IOException{
      XmlPullParser parser = Xml.newPullParser();
      //parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
      parser.setInput(is, null);
      parser.nextTag();
      readAppInfo(parser, appInfo);
    };

    private void readAppInfo(XmlPullParser parser, DialAppInfo appInfo) throws XmlPullParserException, IOException{
      parser.require(XmlPullParser.START_TAG, ns, "service");
      Log.d(TAG, "XML parser " + parser.getName());
      while (parser.next() != XmlPullParser.END_TAG) {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
          continue;
        }
        String name = parser.getName();
        Log.d(TAG, "XML parser current element"+name);
        if (name.equals("name")) {
          appInfo.setName(readText(parser, name));
        }
        else if(name.equals("options")){
          String allowStop = readAttribute(parser, "options", "allowStop");
          if (allowStop != null)
            appInfo.setAllowStop("true".equals(allowStop));
        }
        else if(name.equals("state")){
          appInfo.setState(readText(parser, name));
        }
        else if(name.equals("link")){
          String runId = readLink(parser,"run");
          if (runId != null)
            appInfo.setRunId(runId);
        }
        else if(name.equals("additionalData")){
          readAppInfoAdditionalData(parser,appInfo);
        }
        else {
          skip(parser);
        }
      }
    }

    private void readAppInfoAdditionalData(XmlPullParser parser, DialAppInfo appInfo) throws XmlPullParserException, IOException{
      parser.require(XmlPullParser.START_TAG, ns, "additionalData");
      Log.d(TAG, "XML parser "+parser.getName());
      while (parser.next() != XmlPullParser.END_TAG) {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
          continue;
        }
        try {
          String name = parser.getName();
          Log.d(TAG, "XML parser current element"+name);
          String value = readText(parser, name);
          appInfo.getAdditionalData().put(name,value);
        }
        catch (Exception e){
          Log.e(TAG,e.getMessage(),e);
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

    private String readLink(XmlPullParser parser, String rel) throws IOException, XmlPullParserException {
      String link = null;
      parser.require(XmlPullParser.START_TAG, ns, "link");
      String tag = parser.getName();
      String relType = parser.getAttributeValue(null, "rel");
      if (tag.equals("link")) {
        if (relType.equals(rel)){
          link = parser.getAttributeValue(null, "href");
          parser.nextTag();
        }
      }
      parser.require(XmlPullParser.END_TAG, ns, "link");
      return link;
    }

    private String readAttribute(XmlPullParser parser, String tagName, String attrName) throws IOException, XmlPullParserException {
      String value = null;
      parser.require(XmlPullParser.START_TAG, ns, tagName);
      String tag = parser.getName();
      String relType = parser.getAttributeValue(null, attrName);
      if (tag.equals(tagName)) {
        value = parser.getAttributeValue(null, attrName);
        parser.nextTag();
      }
      parser.require(XmlPullParser.END_TAG, ns, tagName);
      return value;
    }
  }

  private class LaunchAppTask extends AsyncTask<String, Void, Integer> {
    private String ns = null;
    private Dial.LaunchAppCallback mLaunchAppCallback;

    public LaunchAppTask(Dial.LaunchAppCallback launchAppCallback) {
      mLaunchAppCallback = launchAppCallback;
    }

    public Dial.LaunchAppCallback getLaunchAppCallback() {
      return mLaunchAppCallback;
    }

    @Override
    protected Integer doInBackground(String... params) {
      try {
        String appUrl = params[0];
        String launchData = params[1];
        String contentType = params[2];
        return postLaunchRequest(appUrl, launchData, contentType);
      } catch (Exception e) {
        Log.e(TAG, e.getMessage(), e);
        return null;
      }
    }

    @Override
    protected void onPostExecute(Integer status) {
      if (getLaunchAppCallback() != null) {
        getLaunchAppCallback().onLaunchApp(status);
      }
    }

    private Integer postLaunchRequest(String appUrl, String launchData, String contentType) throws IOException {
      URL url = new URL(appUrl);
      HttpURLConnection conn = (HttpURLConnection) url.openConnection();
      conn.setReadTimeout(10000 /* milliseconds */);
      conn.setConnectTimeout(15000 /* milliseconds */);
      conn.setRequestMethod("POST");
      conn.setDoInput(true);
      conn.setDoOutput(true);
      if (contentType != null){
        conn.setRequestProperty("Content-Type", contentType);
      }
      if(launchData != null){
        OutputStream os = conn.getOutputStream();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
        writer.write(launchData);
        writer.flush();
        writer.close();
        os.close();
      }
      conn.connect();
      return conn.getResponseCode();
    }
  }

  private class StopAppTask extends AsyncTask<String, Void, Integer> {
    private Dial.StopAppCallback mStopAppCallback;

    public StopAppTask(Dial.StopAppCallback stopAppCallback) {
      mStopAppCallback = stopAppCallback;
    }

    public Dial.StopAppCallback getStopAppCallback() {
      return mStopAppCallback;
    }

    @Override
    protected Integer doInBackground(String... params) {
      try {
        String stopUrl = params[0];
        return sendStopRequest(stopUrl);
      } catch (Exception e) {
        Log.e(TAG, e.getMessage(), e);
        return null;
      }
    }

    @Override
    protected void onPostExecute(Integer status) {
      if (getStopAppCallback() != null) {
        getStopAppCallback().onStopApp(status);
      }
    }

    private Integer sendStopRequest(String stopUrl) throws IOException {
      URL url = new URL(stopUrl);
      HttpURLConnection conn = (HttpURLConnection) url.openConnection();
      conn.setReadTimeout(10000 /* milliseconds */);
      conn.setConnectTimeout(15000 /* milliseconds */);
      conn.setRequestMethod("DELETE");
      conn.setDoInput(true);
      conn.connect();
      return conn.getResponseCode();
    }
  }
}