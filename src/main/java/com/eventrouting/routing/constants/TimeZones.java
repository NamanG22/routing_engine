package com.eventrouting.routing.constants;

import java.time.ZoneId;

public final class TimeZones {

    /** Application wall-clock zone — matches hibernate.jdbc.time_zone and payment_events. */
    public static final ZoneId APP = ZoneId.of("Asia/Kolkata");

    private TimeZones() {}
}
