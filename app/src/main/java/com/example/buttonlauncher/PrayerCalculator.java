package com.example.buttonlauncher;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * Lightweight astronomical prayer time calculator for Egypt and the Islamic world.
 * Calculates Fajr (19.5° Egyptian Survey), Dhuhr, Asr (Shafi'i / Standard), Maghrib, Isha (17.5°).
 * Uses GPS Latitude and Longitude with zero external dependencies.
 */
public class PrayerCalculator {

    public static class PrayerResult {
        public String fajr;
        public String dhuhr;
        public String asr;
        public String maghrib;
        public String isha;
        public String nextPrayerName;
        public int remainingMinutes;
    }

    public static PrayerResult calculate(double lat, double lng, double timezone) {
        Calendar cal = Calendar.getInstance();
        int dayOfYear = cal.get(Calendar.DAY_OF_YEAR);
        int currentHour = cal.get(Calendar.HOUR_OF_DAY);
        int currentMinute = cal.get(Calendar.MINUTE);
        int nowMinutes = currentHour * 60 + currentMinute;

        // Approximate Solar Equation of Time & Declination
        double b = 2 * Math.PI * (dayOfYear - 81) / 365.0;
        double eot = 9.87 * Math.sin(2 * b) - 7.53 * Math.cos(b) - 1.5 * Math.sin(b); // minutes
        double declination = 23.45 * Math.sin(Math.toRadians((360.0 / 365.0) * (dayOfYear - 81))); // degrees

        // Solar Noon (Dhuhr)
        double solarNoonMinutes = 720 - (4 * lng) - eot + (timezone * 60);

        // Hour angle helper
        double latRad = Math.toRadians(lat);
        double decRad = Math.toRadians(declination);

        // Fajr (19.5 degrees below horizon)
        double fajrHourAngle = getHourAngle(-19.5, latRad, decRad);
        double fajrMinutes = solarNoonMinutes - (fajrHourAngle * 4);

        // Dhuhr
        double dhuhrMinutes = solarNoonMinutes;

        // Asr (Shadow length = 1 + shadow at noon)
        double noonZenith = Math.abs(lat - declination);
        double asrAltitude = Math.toDegrees(Math.atan(1.0 / (1.0 + Math.tan(Math.toRadians(noonZenith)))));
        double asrHourAngle = getHourAngle(asrAltitude, latRad, decRad);
        double asrMinutes = solarNoonMinutes + (asrHourAngle * 4);

        // Maghrib (0.833 degrees refraction)
        double sunsetHourAngle = getHourAngle(-0.833, latRad, decRad);
        double maghribMinutes = solarNoonMinutes + (sunsetHourAngle * 4);

        // Isha (17.5 degrees below horizon)
        double ishaHourAngle = getHourAngle(-17.5, latRad, decRad);
        double ishaMinutes = solarNoonMinutes + (ishaHourAngle * 4);

        PrayerResult res = new PrayerResult();
        res.fajr = formatTime(fajrMinutes);
        res.dhuhr = formatTime(dhuhrMinutes);
        res.asr = formatTime(asrMinutes);
        res.maghrib = formatTime(maghribMinutes);
        res.isha = formatTime(ishaMinutes);

        // Determine next prayer and remaining time
        int fM = (int) fajrMinutes;
        int dM = (int) dhuhrMinutes;
        int aM = (int) asrMinutes;
        int mM = (int) maghribMinutes;
        int iM = (int) ishaMinutes;

        if (nowMinutes < fM) {
            res.nextPrayerName = "الفجر (" + res.fajr + ")";
            res.remainingMinutes = fM - nowMinutes;
        } else if (nowMinutes < dM) {
            res.nextPrayerName = "الظهر (" + res.dhuhr + ")";
            res.remainingMinutes = dM - nowMinutes;
        } else if (nowMinutes < aM) {
            res.nextPrayerName = "العصر (" + res.asr + ")";
            res.remainingMinutes = aM - nowMinutes;
        } else if (nowMinutes < mM) {
            res.nextPrayerName = "المغرب (" + res.maghrib + ")";
            res.remainingMinutes = mM - nowMinutes;
        } else if (nowMinutes < iM) {
            res.nextPrayerName = "العشاء (" + res.isha + ")";
            res.remainingMinutes = iM - nowMinutes;
        } else {
            res.nextPrayerName = "الفجر غداً (" + res.fajr + ")";
            res.remainingMinutes = (1440 - nowMinutes) + fM;
        }

        return res;
    }

    private static double getHourAngle(double targetAlt, double latRad, double decRad) {
        double sinAlt = Math.sin(Math.toRadians(targetAlt));
        double cosHour = (sinAlt - (Math.sin(latRad) * Math.sin(decRad))) / (Math.cos(latRad) * Math.cos(decRad));
        if (cosHour > 1) cosHour = 1;
        if (cosHour < -1) cosHour = -1;
        return Math.toDegrees(Math.acos(cosHour));
    }

    private static String formatTime(double minutes) {
        int m = (int) Math.round(minutes);
        if (m < 0) m += 1440;
        if (m >= 1440) m -= 1440;
        int hr = m / 60;
        int min = m % 60;
        return String.format("%02d:%02d", hr, min);
    }
}
