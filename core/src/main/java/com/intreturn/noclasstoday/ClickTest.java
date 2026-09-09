package com.intreturn.noclasstoday;

import java.awt.Robot;
import java.awt.event.InputEvent;

/** 模拟鼠标点击 (验证选边流程用): 参数 x y. */
public class ClickTest {
    public static void main(String[] args) throws Exception {
        int x = Integer.parseInt(args[0]);
        int y = Integer.parseInt(args[1]);
        Robot robot = new Robot();
        robot.mouseMove(x, y);
        Thread.sleep(200);
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        Thread.sleep(80);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        System.out.println("clicked " + x + "," + y);
    }
}
