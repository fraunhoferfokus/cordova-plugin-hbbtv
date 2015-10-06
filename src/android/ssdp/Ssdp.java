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
package de.fhg.fokus.famium.hbbtv.ssdp;

import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;

/**
 * This class implements SSDP protocol.
 * It supports discovery of SSDP devices in
 * the local network using SSDP M-SEARCH search requests
 */

public class Ssdp implements Runnable {
  public static final String TAG = "Ssdp";
  public static final String SSDP_ADDRESS = "239.255.255.250";
  public static final int SSDP_PORT = 1900;
  public static final String SSDP_HOST = SSDP_ADDRESS + ":" + SSDP_PORT;
  public static final String MAX_AGE = "max-age=1800";
  public static final int TTL = 128;
  public static final int MX = 3;
  public static final String ALIVE = "ssdp:alive";
  public static final String BYEBYE = "ssdp:byebye";
  public static final String UPDATE = "ssdp:update";
  public static final String DISCOVER = "\"ssdp:discover\"";
  public static final String TYPE_M_SEARCH = "M-SEARCH";
  public static final String TYPE_NOTIFY = "NOTIFY";
  public static final String TYPE_200_OK = "200 OK";

  private SsdpCallback mSsdpCallback;
  private SocketAddress mMulticastGroup;
  private DatagramSocket mDatagramSocket;
  private Thread mThread;

  public Ssdp(SsdpCallback ssdpCallback){
    mSsdpCallback = ssdpCallback;
  }

  public synchronized boolean start(Integer timeout) throws IOException {
    if (mThread == null) {
      // create a DatagramSocket without binding to any address
      mDatagramSocket = new DatagramSocket(null);
      mDatagramSocket.setReuseAddress(true);
      // bind to any free port
      mDatagramSocket.bind(null);
      if (timeout != null && timeout > 0) {
        mDatagramSocket.setSoTimeout(timeout);
      }
      mThread = new Thread(this);
      mThread.start();
      return true;
    }
    return false;
  }

  public synchronized boolean stop() throws IOException {
    if(mThread != null) {
      mThread.interrupt();
      mDatagramSocket.close();
      mThread = null;
      mDatagramSocket = null;
      return true;
    }
    return false;
  }

  public synchronized void search(SsdpMessage msg) throws IOException {
    if(mDatagramSocket != null){
      byte bytes[] = msg.toString().getBytes();
      DatagramPacket dp = new DatagramPacket(bytes, bytes.length, getMulticastGroup());
      mDatagramSocket.send(dp);
    }
  }

  public SsdpMessage search(String ST) throws IOException {
    SsdpMessage msg = new SsdpMessage(SsdpMessage.TYPE_SEARCH);
    msg.getHeaders().put("ST", ST);
    msg.getHeaders().put("HOST", SSDP_HOST);
    msg.getHeaders().put("MAN", DISCOVER);
    msg.getHeaders().put("MX" , MX + "");
    search(msg);
    return msg;
  }

  @Override
  public void run() {
    Thread currentThread = Thread.currentThread();
    byte[] buf = new byte[1024];
    Log.d(TAG, "SSDP scan started");
    while (!currentThread.isInterrupted() && mDatagramSocket != null) {
      try {
        DatagramPacket dp = new DatagramPacket(buf, buf.length);
        mDatagramSocket.receive(dp);
        String txt = new String(dp.getData());
        SsdpMessage msg = new SsdpMessage(txt);
        mSsdpCallback.onSsdpMessageReceived(msg);
      }
      catch (SocketTimeoutException e) {
        Log.d(TAG, e.getMessage() != null?e.getMessage():"Socket Timeout");
        break;
      }
      catch (IOException e) {
        Log.e(TAG, e.getMessage());
      }
    }
    // cleanup if needed
    synchronized (this) {
      if (mThread == currentThread) {
        mThread = null;
        if (mDatagramSocket != null) {
          mDatagramSocket.close();
          mDatagramSocket = null;
        }
      }
    }
    Log.d(TAG, "SSDP scan terminated");
  }

  public SocketAddress getMulticastGroup() {
    if (mMulticastGroup == null){
      mMulticastGroup = new InetSocketAddress(SSDP_ADDRESS, SSDP_PORT);
    }
    return mMulticastGroup;
  }

  public interface SsdpCallback {
    public void onSsdpMessageReceived(SsdpMessage ssdpMessage);
  }
}
