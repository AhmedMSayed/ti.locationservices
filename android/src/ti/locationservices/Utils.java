package ti.locationservices;

import android.location.Location;
import android.os.Build;

import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.Priority;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.titanium.util.TiConvert;

public class Utils {
    public static KrollDict createLocationPayload(Location location) {
        KrollDict coords = new KrollDict();
        coords.put("latitude", location.getLatitude());
        coords.put("longitude", location.getLongitude());

        KrollDict locationData = new KrollDict();
        locationData.put("success", true);
        locationData.put("coords", coords);
        locationData.put("accuracy", location.getAccuracy());
        locationData.put("altitude", location.getAltitude());
        locationData.put("provider", location.getProvider());
        locationData.put("speed", location.getSpeed());
        locationData.put("bearing", location.getBearing());
        locationData.put("time", location.getTime());
        locationData.put("isFromMockProvider", Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && location.isMock());

        return locationData;
    }

    public static LocationRequest createLocationRequest(KrollDict dict) {
        LocationRequest.Builder locationBuilder = new LocationRequest
                .Builder(10000)
                .setMinUpdateIntervalMillis(5000)
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY);

        if (dict == null) {
            return locationBuilder.build();
        }

        if (dict.containsKey("interval")) {
            locationBuilder.setIntervalMillis(TiConvert.toInt(dict.getInt("interval")));
        }

        if (dict.containsKey("fastestInterval")) {
            locationBuilder.setMinUpdateIntervalMillis(TiConvert.toInt(dict.getInt("fastestInterval")));
        }

        if (dict.containsKey("priority")) {
            int priority = TiConvert.toInt(dict, "priority");
            switch (priority) {
                case 1:
                    locationBuilder.setPriority(Priority.PRIORITY_BALANCED_POWER_ACCURACY);
                    break;
                case 2:
                    locationBuilder.setPriority(Priority.PRIORITY_HIGH_ACCURACY);
                    break;
                case 3:
                    locationBuilder.setPriority(Priority.PRIORITY_LOW_POWER);
                    break;
                default:
                    locationBuilder.setPriority(Priority.PRIORITY_PASSIVE);
                    break;
            }
        }

        return locationBuilder.build();
    }
}
