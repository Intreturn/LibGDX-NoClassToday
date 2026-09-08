package com.intreturn.noclasstoday;

import java.util.List;

/** 规则引擎回归测试: java -cp core/build/classes/java/main com.intreturn.noclasstoday.TestChess */
public class TestChess {
    static int passed = 0, failed = 0;

    static void check(String name, boolean cond) {
        if (cond) { passed++; System.out.println("[PASS] " + name); }
        else { failed++; System.out.println("[FAIL] " + name); }
    }

    static boolean hasMove(List<int[]> moves, int tr, int tc) {
        for (int[] m : moves) if (m[2] == tr && m[3] == tc) return true;
        return false;
    }

    static boolean hasMoveFlag(List<int[]> moves, int tr, int tc, int flag) {
        for (int[] m : moves) if (m[2] == tr && m[3] == tc && m[4] == flag) return true;
        return false;
    }

    static String[][] empty() {
        String[][] b = new String[8][8];
        for (int r = 0; r < 8; r++) for (int c = 0; c < 8; c++) b[r][c] = null;
        return b;
    }

    public static void main(String[] args) {
        testInitialPosition();
        testKnightJump();
        testEnPassant();
        testCastling();
        testFoolsMate();
        testStalemate();
        testPromotion();
        System.out.println("===== passed=" + passed + " failed=" + failed + " =====");
        if (failed > 0) System.exit(1);
    }

    /** 初始局面: 白方 20 个合法走法; 白兵初始可走一格或两格. */
    static void testInitialPosition() {
        ChessGame g = new ChessGame();
        int total = 0;
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                String p = g.getPiece(r, c);
                if (p != null && !ChessGame.isBlackPiece(p)) total += g.getLegalMoves(r, c).size();
            }
        }
        check("初始局面白方共20个合法走法", total == 20);
        check("白兵e2可走一格e3", hasMove(g.getLegalMoves(1, 4), 2, 4));
        check("白兵e2可走两格e4", hasMove(g.getLegalMoves(1, 4), 3, 4));
        g.makeMove(1, 4, 3, 4, (char) 0); // e4
        check("走子后轮到黑方", g.isBlackToMove());
        check("黑方回合白兵e4无走法", g.getLegalMoves(3, 4).isEmpty());
    }

    /** 马是唯一可跳子的棋子: b1 马可跳过己方兵到 a3/c3. */
    static void testKnightJump() {
        ChessGame g = new ChessGame();
        List<int[]> moves = g.getLegalMoves(0, 1);
        check("马b1可跳至a3", hasMove(moves, 2, 0));
        check("马b1可跳至c3", hasMove(moves, 2, 2));
        check("马b1不可走d2(被己方兵占)", !hasMove(moves, 1, 3));
    }

    /** 吃过路兵: 白兵e5, 黑兵d7-d5 两格后, 白兵可斜吃至d6并移除黑兵. */
    static void testEnPassant() {
        ChessGame g = new ChessGame();
        g.makeMove(1, 4, 3, 4, (char) 0); // 白 e2-e4
        g.makeMove(6, 0, 5, 0, (char) 0); // 黑 a7-a6
        g.makeMove(3, 4, 4, 4, (char) 0); // 白 e4-e5
        g.makeMove(6, 3, 4, 3, (char) 0); // 黑 d7-d5 (两格, 触发过路兵)
        List<int[]> moves = g.getLegalMoves(4, 4);
        check("白兵e5可吃过路兵至d6", hasMoveFlag(moves, 5, 3, 2));
        check("白兵e5不可横吃同行黑兵d5", !hasMove(moves, 4, 3));
        g.makeMove(4, 4, 5, 3, (char) 0); // 吃过路兵
        check("吃后白兵位于d6", "p".equals(g.getPiece(5, 3)));
        check("被吃的黑兵d5已移除", g.getPiece(4, 3) == null);
        check("原位置e5已清空", g.getPiece(4, 4) == null);
    }

    /** 王车易位: 短易位 O-O 与长易位 O-O-O. */
    static void testCastling() {
        ChessGame g = new ChessGame();
        String[][] b = empty();
        b[0][0] = "r"; b[0][4] = "k"; b[0][7] = "r";
        b[7][4] = "K";
        g.loadPosition(b, false, true, true, true, true, -1, -1, 0);
        List<int[]> moves = g.getLegalMoves(0, 4);
        check("白王可短易位至g1", hasMoveFlag(moves, 0, 6, 3));
        check("白王可长易位至c1", hasMoveFlag(moves, 0, 2, 3));
        g.makeMove(0, 4, 0, 6, (char) 0); // O-O
        check("短易位后王在g1", "k".equals(g.getPiece(0, 6)));
        check("短易位后车在f1", "r".equals(g.getPiece(0, 5)));
        check("短易位后h1已空", g.getPiece(0, 7) == null);

        ChessGame g2 = new ChessGame();
        g2.loadPosition(b, false, true, true, true, true, -1, -1, 0);
        g2.makeMove(0, 4, 0, 2, (char) 0); // O-O-O
        check("长易位后王在c1", "k".equals(g2.getPiece(0, 2)));
        check("长易位后车在d1", "r".equals(g2.getPiece(0, 3)));
        check("长易位后a1已空", g2.getPiece(0, 0) == null);
    }

    /** Fool's mate: 1.g4 e5 2.f3 Qh4# 黑方四步将死白方. */
    static void testFoolsMate() {
        ChessGame g = new ChessGame();
        check("1.g4 合法", hasMove(g.getLegalMoves(1, 6), 3, 6));
        g.makeMove(1, 6, 3, 6, (char) 0);
        check("1...e5 合法", hasMove(g.getLegalMoves(6, 4), 4, 4));
        g.makeMove(6, 4, 4, 4, (char) 0);
        check("2.f3 合法", hasMove(g.getLegalMoves(1, 5), 2, 5));
        g.makeMove(1, 5, 2, 5, (char) 0);
        check("2...Qh4 合法", hasMove(g.getLegalMoves(7, 3), 3, 7));
        g.makeMove(7, 3, 3, 7, (char) 0);
        check("Fool's mate 黑方将死获胜", "0-1".equals(g.getResult()));
        check("将死原因正确", "将死".equals(g.getResultReason()));
    }

    /** 困毙: 黑王h8, 白王f7, 白后g5->g6 后黑方无子可动且未被将军. */
    static void testStalemate() {
        ChessGame g = new ChessGame();
        String[][] b = empty();
        b[7][7] = "K"; // 黑王 h8
        b[6][5] = "k"; // 白王 f7
        b[4][6] = "q"; // 白后 g5
        g.loadPosition(b, false, false, false, false, false, -1, -1, 0);
        check("后g5->g6 合法", hasMove(g.getLegalMoves(4, 6), 5, 6));
        g.makeMove(4, 6, 5, 6, (char) 0);
        check("困毙和棋", "1/2-1/2".equals(g.getResult()));
        check("困毙原因正确", "困毙".equals(g.getResultReason()));
    }

    /** 升变: 白兵至第8横线, 4种升变选择, 选后. */
    static void testPromotion() {
        ChessGame g = new ChessGame();
        String[][] b = empty();
        b[6][7] = "p"; // 白兵 h7
        b[7][0] = "K"; // 黑王 a8
        b[0][0] = "k"; // 白王 a1
        g.loadPosition(b, false, false, false, false, false, -1, -1, 0);
        List<int[]> moves = g.getLegalMoves(6, 7);
        check("h7兵升变走法4个", moves.size() == 4);
        check("含升变后选项", hasMoveFlag(moves, 7, 7, 'q'));
        check("含升变车选项", hasMoveFlag(moves, 7, 7, 'r'));
        g.makeMove(6, 7, 7, 7, 'q');
        check("升变为白后", "q".equals(g.getPiece(7, 7)));
    }
}
