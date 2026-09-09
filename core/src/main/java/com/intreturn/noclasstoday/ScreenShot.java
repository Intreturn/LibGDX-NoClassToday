package com.intreturn.noclasstoday;

import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** 屏幕区域截图 (验证窗口实际渲染内容用): 参数 x y w h out. */
public class ScreenShot {
    public static void main(String[] args) throws Exception {
        int x = Integer.parseInt(args[0]);
        int y = Integer.parseInt(args[1]);
        int w = Integer.parseInt(args[2]);
        int h = Integer.parseInt(args[3]);
        String out = args[4];
        Robot robot = new Robot();
        BufferedImage img = robot.createScreenCapture(new Rectangle(x, y, w, h));
        ImageIO.write(img, "png", new File(out));
        System.out.println("saved " + out + " " + w + "x" + h);
    }
}
