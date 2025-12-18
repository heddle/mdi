package edu.cnu.mdi.view.demo;

public enum EDeviceSymbol {
    CAMERA("images/svg/camera.svg"),
    COMPUTER("images/svg/computer.svg"),
    LAPTOP("images/svg/laptop.svg"),
    MONITOR("images/svg/monitor.svg"),
    ROUTER("images/svg/router.svg"),
    TABLET("images/svg/tablet.svg"),
    VIDEOCAMERA("images/svg/videocamera.svg"),
    WEBCAM("images/svg/webcam.svg"),
    WORKSTATION("images/svg/workstation.svg");

    public final String iconPath;

    EDeviceSymbol(String iconPath) {
        this.iconPath = iconPath;
    }
}

