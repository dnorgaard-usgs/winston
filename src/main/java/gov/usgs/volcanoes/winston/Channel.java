package gov.usgs.volcanoes.winston;

import gov.usgs.volcanoes.core.data.Scnl;
import gov.usgs.volcanoes.core.time.J2kSec;
import gov.usgs.volcanoes.core.time.TimeSpan;
import gov.usgs.volcanoes.core.util.UtilException;
import gov.usgs.volcanoes.winston.db.DbUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A class representing one row of the channels table.
 *
 * @author Dan Cervelli
 * @author Tom Parker
 */
public class Channel implements Comparable<Channel> {

  @SuppressWarnings("unused")
  private static final Logger LOGGER = LoggerFactory.getLogger(Channel.class);

  public final int sid;
  public final Instrument instrument;
  public final Scnl scnl;
  public final TimeSpan timeSpan;
  public final double linearA;
  public final double linearB;
  public final String alias;
  public final String unit;
  public final List<String> groups;
  public final Map<String, String> metadata;


  public static class Builder {
    private static final TimeSpan DEFAULT_TIME_SPAN = new TimeSpan(Long.MAX_VALUE, Long.MIN_VALUE);
    private Instrument instrument;
    private int sid = -1;
    private Scnl scnl;
    private TimeSpan timeSpan;

    private double linearA = Double.NaN;
    private double linearB = Double.NaN;
    private String alias;
    private String unit;
    private List<String> groups;
    private Map<String, String> metadata;

    public Builder() {
      timeSpan = DEFAULT_TIME_SPAN;
    }

    public Builder sid(int sid) {
      this.sid = sid;
      return this;
    }

    public Builder instrument(Instrument instrument) {
      this.instrument = instrument;
      return this;
    }

    public Builder scnl(Scnl scnl) {
      this.scnl = scnl;
      return this;
    }

    public Builder timeSpan(TimeSpan timeSpan) {
      this.timeSpan = timeSpan;
      return this;
    }

    public Builder linearA(double linearA) {
      this.linearA = linearA;
      return this;
    }

    public Builder linearB(double linearB) {
      this.linearB = linearB;
      return this;
    }

    public Builder alias(String alias) {
      this.alias = alias == null ? "" : alias;
      return this;
    }

    public Builder unit(String unit) {
      this.unit = unit == null ? "" : unit;
      return this;
    }

    public Builder group(String group) {
      if (groups == null)
        groups = new ArrayList<String>(2);

      groups.add(group);


      return this;
    }

    public Channel build() {
      if (instrument == null) {
        instrument = new Instrument.Builder().build();
      }

      if (groups == null) {
        groups = new ArrayList<String>();
      }

      if (metadata == null) {
        metadata = new HashMap<String, String>();
      }

      return new Channel(this);
    }

    public Builder parse(String s) throws UtilException {
//      2139:ANCK$BHE$AV:553935572.827000:559119563.488000:-999.000000:-999.000000
//      1:ABNG$SHE$VG$00:561970805.005000:571694285.010000:115.434767:-8.294367:Asia/Singapore:::NaN:NaN:Networks!^Indonesia:
        
      final String[] ss = s.split(":");
      sid = Integer.parseInt(ss[0]);
      scnl = Scnl.parse(ss[1]);
      double minTime = Double.parseDouble(ss[2]);
      double maxTime = Double.parseDouble(ss[3]);
      timeSpan = new TimeSpan(J2kSec.asEpoch(minTime), J2kSec.asEpoch(maxTime));

      Instrument.Builder builder = new Instrument.Builder();
      builder.longitude(Double.parseDouble(ss[4]));
      builder.latitude(Double.parseDouble(ss[5]));

      if (ss.length == 12) // metadata present
      {
        if (ss[6].length() >= 1){
          builder.timeZone(ss[6]);
        }
        if (ss[7].length() >= 1){
          alias = ss[7];
        }
        if (ss[8].length() >= 1){
          unit = ss[8];
        }
        linearA = Double.parseDouble(ss[9]);
        linearB = Double.parseDouble(ss[10]);
        if (!ss[11].equals("~")) {
          final String[] gs = ss[11].split("\\|");
          for (final String g : gs)
            group(g);
        }
      }
      instrument = builder.build();

      return this;
    }

    public Builder metadata(HashMap<String, String> metadata) {
      this.metadata = metadata;
      return this;
    }
  }



  /**
   * Default constructor
   */
  private Channel(Builder builder) {
    sid = builder.sid;
    scnl = builder.scnl;
    timeSpan = builder.timeSpan;
    instrument = builder.instrument;
    linearA = builder.linearA == 1e300 ? Double.NaN : builder.linearA;
    linearB = builder.linearB == 1e300 ? Double.NaN : builder.linearB;
    alias = builder.alias;
    unit = builder.unit;
    groups = Collections.unmodifiableList(builder.groups);
    metadata = Collections.unmodifiableMap(builder.metadata);
  }


  /**
   * Getter for groups as a |-separated string
   *
   * @return groups as a string
   */
  public String getGroupString() {
    if (groups.size() < 1) {
      return "~";
    }

    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < groups.size() - 1; i++) {
      sb.append(groups.get(i)).append("|");
    }
    sb.append(groups.get(groups.size() - 1));

    return sb.toString();
  }

  /**
   * Getter for metadata as a :-separated string
   * @return metadata as a string
   */
  public String toMetadataString() {
    return String.format("%s:%s:%s:%s:%f:%f:%s:", toPV2String(), instrument.timeZone,
        alias, unit, linearA, linearB, getGroupString());
  }

  /**
   * Getter for PV2 as a :-separated string
   *
   * @return PV2 as a string
   */
  public String toPV2String() {
    double min = J2kSec.fromEpoch(timeSpan.startTime);
    double max = J2kSec.fromEpoch(timeSpan.endTime);

    return String.format("%d:%s:%f:%f:%f:%f", sid, DbUtils.scnlAsWinstonCode(scnl), min, max,
        instrument.longitude, instrument.latitude);
  }
  

  /**
   * Getter for VDX as a :-separated string
   *
   * @return VDX as a string
   */
  public String toVDXString() {
    // this contains the new output for what VDX is expecting
    final String stripped = scnl.toString(" ");
    // return String.format("%s:%f:%f:%s:%s", code,
    // instrument.getLongitude(), instrument.getLatitude(), stripped,
    // stripped);
    return String.format("%d:%s:%s:%f:%f:%f:%s", sid, DbUtils.scnlAsWinstonCode(scnl), stripped,
        instrument.longitude,
        instrument.latitude, instrument.height, "0");
  }

  /**
   * Return channel code. 
   */
  public String toString() {
    return DbUtils.scnlAsWinstonCode(scnl);
  }

  /**
   * Sort channels alphabeticaly by n, s, c, l, with empty fields floating to
   * the top
   */
  public int compareTo(final Channel o) {
    return scnl.compareTo(o.scnl);
  }

  public boolean equals(Object other) {
    if (other instanceof Channel) {
      return scnl.equals(((Channel) other).scnl);
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return scnl.hashCode();
  }


}
