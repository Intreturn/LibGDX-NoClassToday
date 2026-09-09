package com.intreturn.noclasstoday;

/**
 * 棋钟 (FIDE 计时) 回归测试: 计时流程 / 增量切换 / 超时裁决 / 优先级 / 悔棋还原.
 * 运行: java -cp core\build\classes\java\main com.intreturn.noclasstoday.TestClock
 */
public class TestClock {
    static int passed = 0, failed = 0;

    static void check(String name, boolean cond) {
        if (cond) {
            passed++;
        } else {
            failed++;
            System.out.println("FAIL: " + name);
        }
    }

    static String[][] empty() {
        String[][] b = new String[8][8];
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) b[r][c] = null;
        }
        return b;
    }

    /** 浮点容差比较. */
    static boolean close(double a, double b) {
        return Math.abs(a - b) < 1e-9;
    }

    /** 基础计时: 启动时双方满时, 仅当前行棋方扣减. */
    static void testBasicTick() {
        ChessGame g = new ChessGame();
        g.setTimeControl(900, 10);
        check("启动白方900", close(g.getRemainWhite(), 900));
        check("启动黑方900", close(g.getRemainBlack(), 900));
        g.updateClock(5f);
        check("白方走时扣白", close(g.getRemainWhite(), 895));
        check("黑方不动", close(g.getRemainBlack(), 900));
        g.updateClock(0f);
        check("零增量无变化", close(g.getRemainWhite(), 895));
    }

    /** 落子切换棋钟 + 增量; 思考阶段不预加增量. */
    static void testSwitchAndIncrement() {
        ChessGame g = new ChessGame();
        g.setTimeControl(900, 10);
        g.updateClock(5f);                 // 白 895
        check("思考阶段无增量", close(g.getRemainWhite(), 895));
        g.makeMove(1, 4, 3, 4, (char) 0);  // e4: 白 += 10 -> 905, 换黑
        check("切换后黑方走时", g.isBlackToMove());
        check("落子后白加增量905", close(g.getRemainWhite(), 905));
        g.updateClock(3f);                 // 黑 897
        check("黑方走时扣黑", close(g.getRemainBlack(), 897));
        check("白方停止不动", close(g.getRemainWhite(), 905));
        g.makeMove(6, 4, 4, 4, (char) 0);  // e5: 黑 907
        check("黑加增量907", close(g.getRemainBlack(), 907));
        g.updateClock(1f);                 // 白 904
        check("白方继续扣减904", close(g.getRemainWhite(), 904));
    }

    /** 超时判负: 对手有后 (初始局面全子). */
    static void testTimeoutLossWithQueen() {
        ChessGame g = new ChessGame();
        g.updateClock(1000f);              // 白方超时, 黑方有后
        check("白超时黑有后判白负", "0-1".equals(g.getResult()));
        check("原因超时", "超时".equals(g.getResultReason()));
        check("remain下限为0", close(g.getRemainWhite(), 0));
    }

    /** 超时判负: 对手有车. */
    static void testTimeoutLossWithRook() {
        ChessGame g = new ChessGame();
        String[][] b = empty();
        b[0][4] = "k";
        b[7][4] = "K";
        b[7][0] = "R";
        g.loadPosition(b, false, false, false, false, false, -1, -1, 0);
        g.updateClock(1000f);
        check("白超时黑有车判白负", "0-1".equals(g.getResult()));
    }

    /** 超时判负: 对手有双象. */
    static void testTimeoutLossWithBishopPair() {
        ChessGame g = new ChessGame();
        String[][] b = empty();
        b[0][4] = "k";
        b[7][4] = "K";
        b[7][2] = "B";
        b[7][5] = "B";
        g.loadPosition(b, false, false, false, false, false, -1, -1, 0);
        g.updateClock(1000f);
        check("白超时黑双象判白负", "0-1".equals(g.getResult()));
    }

    /** 超时判负: 对手有象+马. */
    static void testTimeoutLossWithBishopKnight() {
        ChessGame g = new ChessGame();
        String[][] b = empty();
        b[0][4] = "k";
        b[7][4] = "K";
        b[7][2] = "B";
        b[7][5] = "N";
        g.loadPosition(b, false, false, false, false, false, -1, -1, 0);
        g.updateClock(1000f);
        check("白超时黑象马判白负", "0-1".equals(g.getResult()));
    }

    /** 超时判负: 对手有至少1个未升变兵. */
    static void testTimeoutLossWithPawn() {
        ChessGame g = new ChessGame();
        String[][] b = empty();
        b[0][4] = "k";
        b[7][4] = "K";
        b[6][3] = "P";
        g.loadPosition(b, false, false, false, false, false, -1, -1, 0);
        g.updateClock(1000f);
        check("白超时黑有兵判白负", "0-1".equals(g.getResult()));
    }

    /** 超时判和: 对手仅单王. */
    static void testTimeoutDrawBareKing() {
        ChessGame g = new ChessGame();
        String[][] b = empty();
        b[0][4] = "k";
        b[7][4] = "K";
        g.loadPosition(b, false, false, false, false, false, -1, -1, 0);
        g.updateClock(1000f);
        check("白超时黑单王判和", "1/2-1/2".equals(g.getResult()));
        check("原因超时判和", "超时判和".equals(g.getResultReason()));
    }

    /** 超时判和: 对手王+单象. */
    static void testTimeoutDrawSingleBishop() {
        ChessGame g = new ChessGame();
        String[][] b = empty();
        b[0][4] = "k";
        b[7][4] = "K";
        b[7][2] = "B";
        g.loadPosition(b, false, false, false, false, false, -1, -1, 0);
        g.updateClock(1000f);
        check("白超时黑单象判和", "1/2-1/2".equals(g.getResult()));
    }

    /** 超时判和: 对手王+双马 (无强制将死). */
    static void testTimeoutDrawTwoKnights() {
        ChessGame g = new ChessGame();
        String[][] b = empty();
        b[0][4] = "k";
        b[7][4] = "K";
        b[7][1] = "N";
        b[7][6] = "N";
        g.loadPosition(b, false, false, false, false, false, -1, -1, 0);
        g.updateClock(1000f);
        check("白超时黑双马判和", "1/2-1/2".equals(g.getResult()));
    }

    /** 黑方超时判白胜 (方向校验). */
    static void testTimeoutBlackSide() {
        ChessGame g = new ChessGame();
        String[][] b = empty();
        b[0][4] = "k";
        b[7][4] = "K";
        b[0][0] = "q";                    // 白方有后
        g.loadPosition(b, true, false, false, false, false, -1, -1, 0); // 黑方走时
        g.updateClock(1000f);              // 黑超时
        check("黑超时白有后判黑负", "1-0".equals(g.getResult()));
    }

    /** 局面判定优先于超时: 将死后不再触发超时裁决. */
    static void testCheckmatePriority() {
        ChessGame g = new ChessGame();
        g.setTimeControl(300, 0);
        // 学者杀: e4 e5 Qh5 Nc6 Bc4 Nf6 Qxf7#
        g.makeMove(1, 4, 3, 4, (char) 0);
        g.makeMove(6, 4, 4, 4, (char) 0);
        g.makeMove(0, 3, 4, 7, (char) 0);
        g.makeMove(7, 1, 5, 2, (char) 0);
        g.makeMove(0, 5, 3, 2, (char) 0);
        g.makeMove(7, 6, 5, 5, (char) 0);
        g.makeMove(4, 7, 6, 5, (char) 0);
        check("学者杀将死", "1-0".equals(g.getResult()) && "将死".equals(g.getResultReason()));
        double wb = g.getRemainWhite();
        double bb = g.getRemainBlack();
        g.updateClock(9999f);              // 将死已终局, 棋钟停止
        check("将死不被超时覆盖", "1-0".equals(g.getResult()) && "将死".equals(g.getResultReason()));
        check("终局后时间不再扣减", close(g.getRemainWhite(), wb) && close(g.getRemainBlack(), bb));
    }

    /** 悔棋还原时间: 撤销落子后双方时间回到走子前. */
    static void testUndoRestoresTime() {
        ChessGame g = new ChessGame();
        g.setTimeControl(900, 10);
        g.updateClock(5f);                 // 白 895
        g.makeMove(1, 4, 3, 4, (char) 0);  // 白 905
        check("走子后白905", close(g.getRemainWhite(), 905));
        check("悔棋成功", g.undo());
        check("悔棋后白回895", close(g.getRemainWhite(), 895));
        check("悔棋后白方走时", !g.isBlackToMove());
        g.updateClock(1f);
        check("悔棋后继续扣减894", close(g.getRemainWhite(), 894));
    }

    /** 重新开始: 时间回到时限初始值, 对局可继续. */
    static void testResetRestoresTime() {
        ChessGame g = new ChessGame();
        g.setTimeControl(300, 0);
        g.updateClock(10f);
        g.makeMove(1, 4, 3, 4, (char) 0);
        g.updateClock(10f);
        g.reset();
        check("reset后白回300", close(g.getRemainWhite(), 300));
        check("reset后黑回300", close(g.getRemainBlack(), 300));
        check("reset后白方回合", !g.isBlackToMove());
        g.updateClock(1f);
        check("reset后正常计时299", close(g.getRemainWhite(), 299));
    }

    /** 超时瞬间终止: 不再接收任何走子. */
    static void testNoMoveAfterTimeout() {
        ChessGame g = new ChessGame();
        g.updateClock(1000f);              // 白超时判负
        g.makeMove(1, 4, 3, 4, (char) 0);  // 应被拒绝
        check("超时后走子被拒", g.getPiece(3, 4) == null);
        check("超时后仍白方回合", !g.isBlackToMove());
        check("超时后历史为空", g.getMoveCount() == 0);
    }

    public static void main(String[] args) {
        testBasicTick();
        testSwitchAndIncrement();
        testTimeoutLossWithQueen();
        testTimeoutLossWithRook();
        testTimeoutLossWithBishopPair();
        testTimeoutLossWithBishopKnight();
        testTimeoutLossWithPawn();
        testTimeoutDrawBareKing();
        testTimeoutDrawSingleBishop();
        testTimeoutDrawTwoKnights();
        testTimeoutBlackSide();
        testCheckmatePriority();
        testUndoRestoresTime();
        testResetRestoresTime();
        testNoMoveAfterTimeout();
        System.out.println("TestClock: " + passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }
}
