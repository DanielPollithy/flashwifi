package com.flashwifi.wifip2p;

/**
 * Created by daniel on 23.01.18.
 */

public class Constants {
    public interface ACTION {
        public static String MAIN_ACTION = "com.flashwifi.wifip2p.service.main";
        public static String PREV_ACTION = "com.flashwifi.wifip2p.service.prev";
        public static String PLAY_ACTION = "com.flashwifi.wifip2p.service.play";
        public static String NEXT_ACTION = "com.flashwifi.wifip2p.service.next";
        public static String STARTFOREGROUND_ACTION = "com.flashwifi.wifip2p.start_foreground_service";
        public static String STOPFOREGROUND_ACTION = "com.flashwifi.wifip2p.sop_foreground_service";
    }

    public interface NOTIFICATION_ID {
        public static int FOREGROUND_SERVICE = 101;
    }
}
