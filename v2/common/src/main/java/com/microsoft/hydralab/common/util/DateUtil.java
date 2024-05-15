// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.util;

import cn.hutool.core.date.BetweenFormatter;
import cn.hutool.core.date.DateUnit;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class DateUtil {

    public static final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    public static final SimpleDateFormat kustoServiceToDateFormat = new SimpleDateFormat("yyyyMMddHHmm");
    public static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    public static final SimpleDateFormat dateFormatMd = new SimpleDateFormat("MM/dd");
    public static final SimpleDateFormat formatDateFile = new SimpleDateFormat("yyyy_MM_dd_hh_mm_ss");
    public static final SimpleDateFormat ymdFormat = new SimpleDateFormat("yyyy/MM/dd");
    public static final DateFormat appCenterFormat1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    public static final DateFormat appCenterFormat2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    public static final SimpleDateFormat mmssFormat = new SimpleDateFormat("mm:ss");
    public static final SimpleDateFormat fileNameDateFormat = new SimpleDateFormat("MMddHHmmss");
    public static final SimpleDateFormat fileNameDateDashFormat = new SimpleDateFormat("yyyyMMdd_HHmm");
    public static final SimpleDateFormat nowDirFormat = new SimpleDateFormat("yyyy" + File.separator + "MM" + File.separator + "dd" + File.separator + "HHmmss");

    public static Date localToUTC(Date localDate) {
        long localTimeInMillis = localDate.getTime();
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(localTimeInMillis);
        int zoneOffset = calendar.get(Calendar.ZONE_OFFSET);
        int dstOffset = calendar.get(Calendar.DST_OFFSET);
        calendar.add(Calendar.MILLISECOND, -(zoneOffset + dstOffset));
        return new Date(calendar.getTimeInMillis());
    }

    public static long betweenDay(Date date1, Date date2) {
        return cn.hutool.core.date.DateUtil.between(date1, date2, DateUnit.DAY);
    }

    public static Date stringToDate(String year, String month, String date) {
        String dateStr = year + "-" + month + "-" + date;
        return cn.hutool.core.date.DateUtil.parse(dateStr);
    }

    public static String getISO8601TimeString(Date date) {
        return appCenterFormat1.format(date);
    }

    /**
     * unparseable:
     * "2021-01-01T17:31:39Z"
     *
     * @param timeString
     * @return
     */
    public static Date iSO8601TimeStringToDate(String timeString) {
        try {
            return appCenterFormat1.parse(timeString);
        } catch (ParseException e) {
            try {
                return appCenterFormat2.parse(timeString);
            } catch (ParseException parseException) {
                parseException.printStackTrace();
            }
        }
        return null;
    }

    /**
     * Format time intervals
     */
    public static String formatBetween(Date date1, Date date2) {
        long between = Math.abs(date1.getTime() - date2.getTime());
        String formatString = cn.hutool.core.date.DateUtil.formatBetween(between, BetweenFormatter.Level.SECOND);
        return formatString.replace("天", "d ").replace("小时", "h ")
                .replace("分", "m ").replace("秒", "s");
    }

    public static Date beginOfDay(Date date) {
        return cn.hutool.core.date.DateUtil.beginOfDay(date);
    }

    public static Date endOfDay(Date date) {
        return cn.hutool.core.date.DateUtil.endOfDay(date);
    }

    @NotNull
    /**
     * provide 2021-02-04 08:16:27
     * return 2021-02-04 08:17:30
     */
    public static Date get5MinutesTimeMidDate(String date) throws ParseException {
        Date fiveMinuteSpanDate = DateUtil.kustoServiceToDateFormat.parse(date.trim());
        return new Date(fiveMinuteSpanDate.getTime() - (fiveMinuteSpanDate.getTime() % TimeUnit.MINUTES.toMillis(5)) + TimeUnit.SECONDS.toMillis(150));
    }
}
