package com.intreturn.noclasstoday;

import java.awt.Robot;
import java.awt.event.KeyEvent;

/** 模拟键盘按键 (验证悔棋/记录面板用): 参数 键名 (如 U, H). */
public class KeyPressTest {
    public static void main(String[] args) throws Exception {
        String key = args[0].toUpperCase();
        Robot robot = new Robot();
        robot.keyPress(KeyEvent.class.getField("VK_" + key).getInt(null));
        Thread.sleep(60);
        robot.keyRelease(KeyEvent.class.getField("VK_" + key).getInt(null));
        System.out.println("pressed " + key);
    }
}
