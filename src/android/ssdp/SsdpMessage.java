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

import java.util.HashMap;
import java.util.Map;

/**
 * This class is the Java representation of SSDP messages.
 * It creates SsdpMessage instances from a text representation
 * of a SSDP message or converts a SsdpMessage instance to string
 */

public class SsdpMessage {
  public static final int TYPE_SEARCH = 0;
  public static final int TYPE_NOTIFY = 1;
  public static final int TYPE_FOUND = 2;
  private static final String FIRST_LINE[] = {Ssdp.TYPE_M_SEARCH + " * HTTP/1.1", Ssdp.TYPE_NOTIFY + " * HTTP/1.1","HTTP/1.1 " + Ssdp.TYPE_200_OK};
  private int mType;
  private Map<String, String> mHeaders;

  public SsdpMessage(int type) {
    this.mType = type;
  }
  
  public SsdpMessage(String txt) {
    String lines[] = txt.split("\r\n");
    String line = lines[0].trim();
    if(line.startsWith(Ssdp.TYPE_M_SEARCH)) {
      this.mType = TYPE_SEARCH;
    }
    else if (line.startsWith(Ssdp.TYPE_NOTIFY)) {
      this.mType = TYPE_NOTIFY;
    }
    else {
      this.mType = TYPE_FOUND;
    }
    for (int i = 1; i < lines.length; i++) {
      line = lines[i].trim();
      int index = line.indexOf(":");
      if (index>0) {
        String key = line.substring(0, index).trim();
        String value = line.substring(index+1).trim();
        getHeaders().put(key, value);
      }
    }
  }

  public Map<String, String> getHeaders() {
    if(mHeaders == null) {
      mHeaders = new HashMap<String, String>();
    }
    return mHeaders;
  }

  public int getType() {
    return mType;
  }

  public String get(String key) {
    return getHeaders().get(key);
  }

  public String put(String key, String value) {
    return getHeaders().put(key, value);
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append(FIRST_LINE[this.mType]).append("\r\n");
    for (Map.Entry<String,String> entry: getHeaders().entrySet()) {
      builder.append(entry.getKey())
          .append(": ")
          .append(entry.getValue())
          .append("\r\n");
    }
    builder.append("\r\n");
    return builder.toString();
  }
}