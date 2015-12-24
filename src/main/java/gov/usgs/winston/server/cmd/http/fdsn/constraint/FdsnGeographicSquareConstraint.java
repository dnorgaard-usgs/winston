package gov.usgs.winston.server.cmd.http.fdsn.constraint;

import gov.usgs.earthworm.message.TraceBuf;
import gov.usgs.util.Util;
import gov.usgs.winston.Channel;
import gov.usgs.winston.Instrument;
import gov.usgs.winston.server.cmd.http.fdsn.FdsnException;

/**
 * 
 * @author Tom Parker
 * 
 */
public class FdsnGeographicSquareConstraint implements FdsnConstraint {

    private static final double DEFAULT_MINLATITUDE = -90;
    private static final double DEFAULT_MAXLATITUDE = 90;
    private static final double DEFAULT_MINLONGITUDE = -180;
    private static final double DEFAULT_MAXLONGITUDE = 180;

    public final double minlatitude;
    public final double maxlatitude;
    public final double minlongitude;
    public final double maxlongitude;

    public FdsnGeographicSquareConstraint(String minlatitude, String maxlatitude, String minlongitude,
            String maxlongitude) throws FdsnException {

        this.minlatitude = Util.stringToDouble(minlatitude, DEFAULT_MINLATITUDE);
        this.maxlatitude = Util.stringToDouble(maxlatitude, DEFAULT_MAXLATITUDE);
        this.minlongitude = Util.stringToDouble(minlongitude, DEFAULT_MINLONGITUDE);
        this.maxlongitude = Util.stringToDouble(maxlongitude, DEFAULT_MAXLONGITUDE);

    }

    public boolean matches(Channel chan) {
        Instrument i = chan.getInstrument();

        if (i.getLatitude() < minlatitude)
            return false;

        if (i.getLatitude() > maxlatitude)
            return false;

        if (i.getLongitude() < minlongitude)
            return false;

        if (i.getLongitude() > maxlongitude)
            return false;

        return true;
    }
    
    public String toString() {
        return "FdsnGeographcSquareConstraint" + minlatitude + ", " + minlongitude + " -> " + maxlatitude + ", " + maxlongitude;
    }
}