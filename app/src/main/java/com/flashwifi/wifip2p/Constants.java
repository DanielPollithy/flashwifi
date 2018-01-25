package com.flashwifi.wifip2p;

/**
 * Created by daniel on 23.01.18.
 */

public class Constants {
    public interface ACTION {
        public static String MAIN_ACTION = "com.flashwifi.wifip2p.service.main";
        public static String HOTSPOT = "com.flashwifi.wifip2p.service.hotspot";
        public static String SEARCH = "com.flashwifi.wifip2p.service.search";
        public static String NEGOTIATION = "com.flashwifi.wifip2p.service.negotiation";
        public static String ROAMING = "com.flashwifi.wifip2p.service.negotiation";
        public static String STARTFOREGROUND_ACTION = "com.flashwifi.wifip2p.start_foreground_service";
        public static String STOPFOREGROUND_ACTION = "com.flashwifi.wifip2p.sop_foreground_service";
    }

    public interface NOTIFICATION_ID {
        public static int FOREGROUND_SERVICE = 101;
    }
}
