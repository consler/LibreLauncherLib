module librelauncherlib {
    requires transitive com.google.gson;
    requires java.desktop;
    requires java.net.http;
    requires java.xml;
    requires javafx.web;

    exports net.consler.librelauncherlib.auth;
    exports net.consler.librelauncherlib.exception;
    exports net.consler.librelauncherlib.install;
    exports net.consler.librelauncherlib.instance;
    exports net.consler.librelauncherlib.launch;
    exports net.consler.librelauncherlib.modloader;
    exports net.consler.librelauncherlib.nbt;
    exports net.consler.librelauncherlib.utill;
    exports net.consler.librelauncherlib.versions;
}
