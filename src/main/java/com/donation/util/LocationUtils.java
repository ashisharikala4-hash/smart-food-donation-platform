package com.donation.util;

public class LocationUtils {

    private static final double EARTH_RADIUS_KM = 6371.0;

    /**
     * Calculates the distance between two points in kilometers using the Haversine formula.
     * @param loc1 "lat,lng" string
     * @param loc2 "lat,lng" string
     * @return distance in km
     */
    public static double calculateDistanceKm(String loc1, String loc2) {
        double[] c1 = parseCoords(loc1);
        double[] c2 = parseCoords(loc2);

        double lat1 = Math.toRadians(c1[0]);
        double lng1 = Math.toRadians(c1[1]);
        double lat2 = Math.toRadians(c2[0]);
        double lng2 = Math.toRadians(c2[1]);

        double dLat = lat2 - lat1;
        double dLng = lng2 - lng1;

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(lat1) * Math.cos(lat2) *
                   Math.sin(dLng / 2) * Math.sin(dLng / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }

    private static double[] parseCoords(String location) {
        try {
            if (location == null || location.isBlank()) return new double[]{0.0, 0.0};
            String[] parts = location.split(",");
            return new double[]{
                Double.parseDouble(parts[0].trim()), 
                Double.parseDouble(parts[1].trim())
            };
        } catch (Exception e) {
            return new double[]{0.0, 0.0};
        }
    }
}
