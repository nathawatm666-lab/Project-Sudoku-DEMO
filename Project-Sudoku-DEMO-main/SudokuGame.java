
// === นำเข้าไลบรารีที่จำเป็น ===
import java.awt.*; // สำหรับจัดการ GUI พื้นฐาน เช่น สี, ฟอนต์, เลย์เอาต์
import java.awt.event.*; // สำหรับจัดการเหตุการณ์ต่างๆ เช่น การคลิก, การพิมพ์
import java.util.ArrayList; // ใช้เก็บรายการตัวเลข 1-9 แบบสุ่มสำหรับ Backtracking
import java.util.Collections; // ใช้สุ่มสลับลำดับ (shuffle) ตัวเลขใน ArrayList
import java.util.Random; // ใช้สุ่มตำแหน่งช่องที่จะลบออกเพื่อสร้างโจทย์
import java.util.concurrent.ExecutionException; // ใช้จัดการข้อผิดพลาดจาก SwingWorker.get()
import javax.swing.*; // ใช้สำหรับ animation และ delay

public class SudokuGame extends JFrame {

    private static final int SIZE = 9;
    private static final int SUBGRID = 3;
    // กำหนดสีต่างๆ สำหรับ UI ==
    private static final Color BG_PRIMARY = new Color(15, 23, 42);
    private static final Color BG_CELL = new Color(30, 41, 59);
    private static final Color BG_CELL_SELECTED = new Color(56, 89, 160);
    private static final Color BG_CELL_HIGHLIGHT = new Color(39, 55, 85);
    private static final Color BG_CELL_FIXED = new Color(22, 33, 50);
    private static final Color BG_CELL_ERROR = new Color(120, 30, 30);
    private static final Color TEXT_FIXED = new Color(148, 163, 184);
    private static final Color TEXT_USER = new Color(96, 165, 250);
    private static final Color TEXT_WHITE = new Color(226, 232, 240);
    private static final Color ACCENT = new Color(59, 130, 246);
    private static final Color ACCENT_GREEN = new Color(34, 197, 94);
    private static final Color BORDER_THICK = new Color(100, 116, 139);
    private static final Color BORDER_THIN = new Color(51, 65, 85);

    // ฟีเจอร์หลักของเกม ==
    private int[][] solution = new int[SIZE][SIZE]; // เก็บ "เฉลย" ที่สมบูรณ์แบบของกระดานนั้นๆ
    private final int[][] puzzle = new int[SIZE][SIZE]; // เก็บโจทย์เริ่มต้น (ที่เจาะรู/ลบตัวเลขออกไปแล้วบางส่วน)
    private final int[][] playerBoard = new int[SIZE][SIZE]; // เก็บตัวเลขปัจจุบันที่อยู่บนหน้าจอ (รวมที่โปรแกรมให้มา
    // และที่ผู้เล่นพิมพ์ลงไป)
    private final boolean[][] fixed = new boolean[SIZE][SIZE]; // อาเรย์บอกสถานะว่าช่องไหนเป็น "โจทย์ที่ห้ามแก้" (true =
    // แก้ไม่ได้, false = ช่องว่างที่ผู้เล่นเติมได้)
    private final JTextField[][] cells = new JTextField[SIZE][SIZE]; // เก็บออบเจกต์ JTextField (กล่องข้อความ) ทั้ง 81
    // ช่องบนหน้าจอ เพื่อให้เราสั่งเปลี่ยนสีหรือดึงข้อความได้
    private int selectedRow = -1, selectedCol = -1; // เก็บตำแหน่งของช่องที่ผู้เล่นกำลังเลือกอยู่ (เริ่มต้นเป็น -1
                                                    // คือยังไม่เลือกอะไรเลย)
    private JLabel timerLabel, statusLabel; // ป้ายข้อความ (Label) สำหรับแสดง เวลา, จำนวนที่ผิด,
                                            // และสถานะ/ระดับความยาก ด้านบนของจอ
    private Timer gameTimer; // ตัวจับเวลา
    private int secondsElapsed = 0; // เวลารวมที่เล่นไป

    private int difficulty = 40; // จำนวนช่องที่จะถูกลบออกเพื่อสร้างโจทย์ (ค่าเริ่มต้นลบ 40 ช่อง = ระดับ Medium)
    private String difficultyName = "Medium"; // ค่าเริ่มต้น ระดับ Medium
    private boolean isCustomMode = false; // โหมด Custom = ผู้ใช้กรอกโจทย์เอง (ไม่สร้างเฉลยอัตโนมัติ)

    // สำหรับจัดการหน้าต่าง (Start Menu & Game)
    // ตัวจัดการหน้าจอ (Layout) ที่ช่วยให้เราสลับไปมาระหว่างหน้า "Menu" กับหน้า
    // "Game" ได้โดยไม่ต้องเปิดหน้าต่างโปรแกรมใหม่
    private final CardLayout cardLayout;
    private final JPanel mainContentPanel;

    // ทำหน้าที่ตั้งค่าหน้าต่างโปรแกรม (JFrame), เรียกใช้ CardLayout, และนำหน้า Menu
    // กับหน้า Game มาแปะรวมกัน
    public SudokuGame() {
        setTitle("🧩 Sudoku Game");
        setDefaultCloseOperation(EXIT_ON_CLOSE); // ตั้งให้โปรแกรมปิดเมื่อกดปุ่มกากบาท
        setResizable(false); // ห้ามแก้ขนาดหน้าต่างเพื่อให้ UI ไม่เสียรูป

        // ใช้ CardLayout เพื่อสลับหน้าระหว่าง Menu กับ Game
        cardLayout = new CardLayout();
        mainContentPanel = new JPanel(cardLayout);

        // 1. สร้างหน้า Main Menu
        mainContentPanel.add(createMenuPanel(), "MENU");

        // 2. สร้างหน้า Game Play
        JPanel gamePanel = new JPanel(new BorderLayout(0, 0));
        gamePanel.setBackground(BG_PRIMARY);
        gamePanel.add(createTopPanel(), BorderLayout.NORTH);
        gamePanel.add(createBoardPanel(), BorderLayout.CENTER);
        gamePanel.add(createBottomPanel(), BorderLayout.SOUTH);
        mainContentPanel.add(gamePanel, "GAME");

        add(mainContentPanel);

        pack();
        setLocationRelativeTo(null);
        setVisible(true);

        // เริ่มต้นที่หน้า Menu ==
        cardLayout.show(mainContentPanel, "MENU");
    }

    // ============== สร้างหน้า Start Menu ==============
    private JPanel createMenuPanel() {
        JPanel menu = new JPanel();
        menu.setBackground(BG_PRIMARY);
        menu.setLayout(new BoxLayout(menu, BoxLayout.Y_AXIS));

        menu.add(Box.createVerticalGlue());

        // --- เริ่มต้นส่วนของข้อความ Title แบบคลื่น ---
        JPanel waveTitlePanel = new JPanel() {
            private int angle = 0;
            private final String text = "SUDOKU";

            { // Block เริ่มต้นการทำงานของ Timer
                setOpaque(false);
                setPreferredSize(new Dimension(400, 80));
                setMaximumSize(new Dimension(400, 80));

                // Timer สั่งให้วาดใหม่ทุกๆ 50 มิลลิวินาที
                Timer waveTimer = new Timer(50, e -> {
                    angle = (angle + 10) % 360; // เพิ่มมุมไปเรื่อยๆ เพื่อสร้างคลื่น
                    repaint();
                });
                waveTimer.start();
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();

                // ตั้งค่าให้ตัวอักษรคมชัด
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setFont(new Font("SansSerif", Font.BOLD, 48));
                g2.setColor(TEXT_WHITE);

                FontMetrics fm = g2.getFontMetrics();
                int startX = (getWidth() - fm.stringWidth(text)) / 2;
                int baseY = getHeight() / 2 + fm.getAscent() / 4;

                int currentX = startX;
                // วาดตัวอักษรทีละตัว
                for (int i = 0; i < text.length(); i++) {
                    char c = text.charAt(i);
                    // คำนวณความสูงของคลื่นด้วย Sine (คูณ 8 คือความสูงคลื่น, i * 30
                    // คือระยะห่างคลื่นแต่ละตัว)
                    int yOffset = (int) (Math.sin(Math.toRadians(angle + (i * 30))) * 8);

                    g2.drawString(String.valueOf(c), currentX, baseY + yOffset);
                    currentX += fm.charWidth(c);
                }
                g2.dispose();
            }
        };
        waveTitlePanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        menu.add(waveTitlePanel);
        // --- จบส่วนของข้อความ Title แบบคลื่น ---

        menu.add(Box.createRigidArea(new Dimension(0, 50)));

        JButton startBtn = createStyledButton("▶ Start Game");
        startBtn.setFont(new Font("SansSerif", Font.BOLD, 20));
        startBtn.setMaximumSize(new Dimension(200, 50));
        startBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        // เมื่อกด Start ให้เลือกความยากและเข้าสู่เกม
        startBtn.addActionListener(e -> showDifficultyDialog(true));
        menu.add(startBtn);

        menu.add(Box.createRigidArea(new Dimension(0, 20)));

        JButton exitBtn = createStyledButton("🚪 Exit");
        exitBtn.setFont(new Font("SansSerif", Font.BOLD, 20));
        exitBtn.setMaximumSize(new Dimension(200, 50));
        exitBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        // เมื่อกด Exit ให้ปิดโปรแกรม
        exitBtn.addActionListener(e -> System.exit(0));
        menu.add(exitBtn);

        menu.add(Box.createVerticalGlue());

        return menu;
    }

    // ============== ส่วน UI ของหน้าเกม ==============

    // สร้างแถบด้านบน: แสดงชื่อเกม, ระดับความยาก, จำนวนที่ผิด, และเวลา
    private JPanel createTopPanel() {
        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(BG_PRIMARY);
        top.setBorder(BorderFactory.createEmptyBorder(15, 20, 10, 20));

        JLabel title = new JLabel("SUDOKU", SwingConstants.LEFT);
        title.setFont(new Font("SansSerif", Font.BOLD, 28));
        title.setForeground(TEXT_WHITE);

        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 0));
        infoPanel.setBackground(BG_PRIMARY);

        timerLabel = new JLabel("⏱ 00:00");
        timerLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        timerLabel.setForeground(TEXT_FIXED);

        statusLabel = new JLabel(difficultyName);
        statusLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        statusLabel.setForeground(ACCENT);

        infoPanel.add(statusLabel);

        infoPanel.add(timerLabel);

        top.add(title, BorderLayout.WEST);
        top.add(infoPanel, BorderLayout.EAST);
        return top;
    }

    // สร้างกระดาน Sudoku 9x9 โดยแบ่งเป็น 9 กล่องย่อย (3x3) แต่ละกล่องมี 9 ช่อง
    // ใช้ลูปซ้อน 4 ชั้น: กล่องแถว -> กล่องคอลัมน์ -> แถวในกล่อง -> คอลัมน์ในกล่อง
    private JPanel createBoardPanel() {
        JPanel boardWrapper = new JPanel(new GridLayout(SUBGRID, SUBGRID, 3, 3));
        boardWrapper.setBackground(BORDER_THICK);
        boardWrapper.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(5, 20, 5, 20),
                BorderFactory.createLineBorder(BORDER_THICK, 3)));

        for (int boxRow = 0; boxRow < SUBGRID; boxRow++) {
            for (int boxCol = 0; boxCol < SUBGRID; boxCol++) {
                JPanel subGrid = new JPanel(new GridLayout(SUBGRID, SUBGRID, 1, 1));
                subGrid.setBackground(BORDER_THIN);

                for (int r = 0; r < SUBGRID; r++) {
                    for (int c = 0; c < SUBGRID; c++) {
                        int row = boxRow * SUBGRID + r;
                        int col = boxCol * SUBGRID + c;

                        JTextField cell = new JTextField();
                        cell.setHorizontalAlignment(JTextField.CENTER);
                        cell.setFont(new Font("SansSerif", Font.BOLD, 22));
                        cell.setPreferredSize(new Dimension(55, 55));
                        cell.setBackground(BG_CELL);
                        cell.setForeground(TEXT_USER);
                        cell.setCaretColor(TEXT_USER);
                        cell.setBorder(BorderFactory.createLineBorder(BORDER_THIN, 1));

                        final int fr = row, fc = col; // เก็บค่าตำแหน่งไว้ใน final เพื่อใช้ใน inner class

                        // เมื่อผู้เล่นคลิกเลือกช่องนี้ → จำตำแหน่งไว้ แล้วไฮไลท์ช่องที่เกี่ยวข้อง
                        cell.addFocusListener(new FocusAdapter() {
                            @Override
                            public void focusGained(FocusEvent e) {
                                selectedRow = fr;
                                selectedCol = fc;
                                highlightCells();
                            }
                        });

                        // จัดการเมื่อผู้เล่นกดปุ่มบนแป้นพิมพ์ในช่องนี้
                        cell.addKeyListener(new KeyAdapter() {
                            @Override
                            public void keyTyped(KeyEvent e) {
                                char ch = e.getKeyChar();
                                // ถ้าเป็นช่องโจทย์ (fixed) → ไม่ให้พิมพ์อะไรเลย
                                if (fixed[fr][fc]) {
                                    e.consume();
                                    return;
                                }
                                // ถ้าผู้เล่นกดเลข 1-9 → บันทึกลง playerBoard แล้วแสดงบนหน้าจอ
                                if (ch >= '1' && ch <= '9') {
                                    e.consume(); // กัน JTextField ไม่ให้ใส่ตัวอักษรซ้ำเอง
                                    int num = ch - '0'; // แปลง char '1'-'9' เป็น int 1-9
                                    playerBoard[fr][fc] = num;
                                    cell.setText(String.valueOf(num));
                                    cell.setForeground(TEXT_USER);

                                    // โหมด Custom: เช็คเลขซ้ำตามกฎ Sudoku → กะพริบแดงเตือน
                                    if (isCustomMode && hasDuplicate(fr, fc)) {
                                        flashError(cell);
                                    }
                                    highlightCells(); // อัปเดตสีไฮไลท์
                                    if (!isCustomMode)
                                        checkWin(); // เช็คว่าชนะหรือยัง (เฉพาะโหมดปกติ)
                                    // ถ้ากดเลข 0 หรือปุ่ม Backspace/Delete → ลบตัวเลขออกจากช่อง
                                } else if (ch == '0' || ch == KeyEvent.VK_BACK_SPACE || ch == KeyEvent.VK_DELETE) {
                                    e.consume();
                                    playerBoard[fr][fc] = 0;
                                    cell.setText("");
                                    highlightCells();
                                } else {
                                    e.consume(); // กดปุ่มอื่น → ไม่ทำอะไร (ป้องกันตัวอักษรอื่นเข้ามา)
                                }
                            }

                            // จัดการปุ่มพิเศษ: ลบตัวเลข และเลื่อนช่องด้วยปุ่มลูกศร
                            @Override
                            public void keyPressed(KeyEvent e) {
                                int code = e.getKeyCode();
                                // กด Delete/Backspace → ลบตัวเลขออก (ถ้าไม่ใช่ช่องโจทย์)
                                if (code == KeyEvent.VK_DELETE || code == KeyEvent.VK_BACK_SPACE) {
                                    if (!fixed[fr][fc]) {
                                        playerBoard[fr][fc] = 0;
                                        cell.setText("");
                                        highlightCells();
                                    }
                                    e.consume();
                                }
                                // เลื่อนช่องด้วยปุ่มลูกศร ↑↓←→ (ไม่ต้องคลิกเมาส์)
                                int nr = fr, nc = fc;
                                switch (code) {
                                    case KeyEvent.VK_UP -> nr = Math.max(0, fr - 1);
                                    case KeyEvent.VK_DOWN -> nr = Math.min(8, fr + 1);
                                    case KeyEvent.VK_LEFT -> nc = Math.max(0, fc - 1);
                                    case KeyEvent.VK_RIGHT -> nc = Math.min(8, fc + 1);
                                    default -> {
                                    }
                                }
                                if (nr != fr || nc != fc) {
                                    cells[nr][nc].requestFocusInWindow(); // ย้าย focus ไปช่องใหม่
                                }
                            }
                        });

                        cells[row][col] = cell;
                        subGrid.add(cell);
                    }
                }
                boardWrapper.add(subGrid);
            }
        }
        return boardWrapper;
    }

    // สร้างแผงปุ่มด้านล่าง: ปุ่มตัวเลข 1-9 และปุ่มควบคุม (Menu, New, Hint, Erase,
    // Solve)
    private JPanel createBottomPanel() {

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBackground(BG_PRIMARY);
        bottom.setBorder(BorderFactory.createEmptyBorder(10, 20, 15, 20));

        // สร้างปุ่มควบคุมเกม (Menu, New Game, Hint, Erase, Solve)
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        actionPanel.setBackground(BG_PRIMARY);

        JButton menuBtn = createStyledButton("🏠 Menu");
        menuBtn.addActionListener(e -> {
            if (gameTimer != null)
                gameTimer.stop();
            cardLayout.show(mainContentPanel, "MENU"); // กลับหน้าหลัก
        });

        JButton newGameBtn = createStyledButton("🎲 New Game");
        newGameBtn.addActionListener(e -> showDifficultyDialog(false)); // สุ่มใหม่ในหน้าเกม

        JButton hintBtn = createStyledButton("💡 Hint");
        hintBtn.addActionListener(e -> giveHint());

        // ปุ่มดูเฉลย: แสดงคำตอบทั้งหมด (ถามยืนยันก่อน)
        JButton solveBtn = createStyledButton("👁 Solve");
        solveBtn.addActionListener(e -> revealSolution());

        actionPanel.add(menuBtn);
        actionPanel.add(newGameBtn);
        actionPanel.add(hintBtn);
        actionPanel.add(solveBtn);

        bottom.add(actionPanel, BorderLayout.SOUTH);
        return bottom;
    }

    // สร้างปุ่มแบบ Custom สวยๆ: มีมุมโค้ง, เปลี่ยนสีเมื่อ hover/กด
    // ใช้ paintComponent วาดพื้นหลังปุ่มเอง แทนที่จะใช้ปุ่มมาตรฐานของ Swing
    private JButton createStyledButton(String text) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isPressed()) {
                    g2.setColor(ACCENT.darker()); // กำลังกดอยู่ → สีน้ำเงินเข้ม
                } else if (getModel().isRollover()) {
                    g2.setColor(BG_CELL_HIGHLIGHT); // เมาส์ชี้อยู่ → สีไฮไลท์
                } else {
                    g2.setColor(BG_CELL); // ปกติ → สีพื้นหลังช่อง
                }
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10); // วาดสี่เหลี่ยมมุมโค้ง
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("SansSerif", Font.PLAIN, 13));
        btn.setForeground(TEXT_WHITE);
        btn.setContentAreaFilled(false); // ปิดพื้นหลังปุ่มมาตรฐาน (เราวาดเอง)
        btn.setBorderPainted(false); // ปิดขอบปุ่มมาตรฐาน
        btn.setFocusPainted(false); // ปิดเส้นประรอบปุ่มเมื่อ focus
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR)); // เปลี่ยนเคอร์เซอร์เป็นมือเมื่อชี้
        return btn;
    }

    // ไฮไลท์ช่องทั้ง 81 ช่องตามเงื่อนไข (เรียกทุกครั้งที่มีการเปลี่ยนแปลงบนกระดาน)
    // ลำดับความสำคัญ: ช่องที่เลือก > แถว/คอลัมน์/กล่องเดียวกัน > เลขเดียวกัน >
    // ช่องผิด
    private void highlightCells() {
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                Color bg;
                if (r == selectedRow && c == selectedCol) {
                    bg = BG_CELL_SELECTED; // ช่องที่กำลังเลือกอยู่ → สีน้ำเงินเข้ม
                } else if (r == selectedRow || c == selectedCol ||
                        (r / SUBGRID == selectedRow / SUBGRID && c / SUBGRID == selectedCol / SUBGRID)) {
                    bg = BG_CELL_HIGHLIGHT; // อยู่แถว/คอลัมน์/กล่อง 3x3 เดียวกัน → สีไฮไลท์
                } else if (fixed[r][c]) {
                    bg = BG_CELL_FIXED; // ช่องโจทย์ → สีเข้มกว่าปกติ
                } else {
                    bg = BG_CELL; // ช่องว่างทั่วไป → สีปกติ
                }

                // ไฮไลท์ช่องที่มีตัวเลขเดียวกันกับช่องที่เลือก (ช่วยให้เห็นตัวเลขซ้ำ)
                if (selectedRow >= 0 && selectedCol >= 0 && playerBoard[selectedRow][selectedCol] != 0
                        && playerBoard[r][c] == playerBoard[selectedRow][selectedCol]
                        && !(r == selectedRow && c == selectedCol)) {
                    bg = BG_CELL_HIGHLIGHT.brighter();
                }

                // ถ้าช่องนี้ใส่เลขซ้ำผิดกฎ Sudoku → แสดงสีแดง
                // เช็คเลขซ้ำตามกฎ Sudoku (แถว/คอลัมน์/กล่อง 3x3) ทั้งโหมดปกติและ Custom
                if (playerBoard[r][c] != 0 && hasDuplicate(r, c)) {
                    bg = BG_CELL_ERROR;
                }

                cells[r][c].setBackground(bg);
            }
        }
    }

    // กะพริบสีแดงที่ช่องที่ตอบผิด เป็นเวลา 500ms แล้วกลับสู่สีปกติ
    private void flashError(JTextField cell) {
        cell.setBackground(BG_CELL_ERROR);
        Timer t = new Timer(500, e -> highlightCells()); // หลัง 500ms → เรียก highlightCells() คืนสีปกติ
        t.setRepeats(false); // ทำแค่ครั้งเดียว (ไม่ทำซ้ำ)
        t.start();
    }

    // ============== ส่วนสร้างโจทย์ Sudoku (Generation) ==============

    // สร้างเฉลย Sudoku ที่สมบูรณ์ (เติมเลขครบทุกช่อง)
    // เริ่มจากกระดานว่าง แล้วใช้ Backtracking เติมทีละช่อง
    private void generateSolution() {
        solution = new int[SIZE][SIZE]; // สร้างกระดานว่างขนาด 9x9
        fillBoard(solution, 0, 0); // เริ่มเติมจากช่อง (0,0) มุมซ้ายบน
    }

    // === Backtracking Algorithm (หัวใจหลัก) ===
    // เติมเลขลงกระดานทีละช่อง จากซ้ายไปขวา บนลงล่าง
    // ถ้าเติมไม่ได้ → ถอยกลับ (backtrack) แล้วลองเลขอื่น
    private boolean fillBoard(int[][] board, int row, int col) {
        if (row == SIZE) // Base Case: ผ่านแถวสุดท้ายแล้ว = เติมครบ!
            return true;
        int nextRow = (col == SIZE - 1) ? row + 1 : row; // คำนวณช่องถัดไป
        int nextCol = (col + 1) % SIZE; // ถ้าสุดคอลัมน์ → ขึ้นแถวใหม่

        // สร้างรายการเลข 1-9 แล้วสุ่มลำดับ (ทำให้ได้โจทย์ไม่ซ้ำกันทุกครั้ง)
        ArrayList<Integer> nums = new ArrayList<>();
        for (int i = 1; i <= 9; i++)
            nums.add(i);
        Collections.shuffle(nums); // สุ่มสลับลำดับเลข (Randomized Backtracking)

        // ลองใส่เลขแต่ละตัวที่สุ่มมา
        for (int num : nums) {
            if (isValid(board, row, col, num)) { // ตรวจว่าใส่ได้ไหม
                board[row][col] = num; // ลองวางเลขลง
                if (fillBoard(board, nextRow, nextCol)) // เรียกตัวเอง (Recursion) ไปช่องถัดไป
                    return true; // สำเร็จทั้งหมด!
                board[row][col] = 0; // ⏪ Backtrack: ถอยกลับ ลบเลขออก
            }
        }
        return false; // ลองหมดทุกเลขแล้วไม่ได้ → ต้องถอยกลับไปช่องก่อนหน้า
    }

    // ตรวจสอบว่าเลข num ใส่ที่ตำแหน่ง (row, col) ได้หรือไม่
    // ตรวจ 3 เงื่อนไข: แถวเดียวกัน, คอลัมน์เดียวกัน, กล่อง 3x3 เดียวกัน
    private boolean isValid(int[][] board, int row, int col, int num) {
        // เช็คแถวและคอลัมน์พร้อมกัน (ลูปเดียว เพราะ Sudoku มี 9 แถว = 9 คอลัมน์)
        for (int i = 0; i < SIZE; i++) {
            if (board[row][i] == num || board[i][col] == num)
                return false; // ซ้ำ! → ใส่ไม่ได้
        }
        // เช็คกล่อง 3x3: หาจุดเริ่มต้นมุมซ้ายบนของกล่อง
        int sr = (row / SUBGRID) * SUBGRID, sc = (col / SUBGRID) * SUBGRID;
        for (int r = sr; r < sr + SUBGRID; r++)
            for (int c = sc; c < sc + SUBGRID; c++)
                if (board[r][c] == num)
                    return false; // ซ้ำในกล่อง! → ใส่ไม่ได้
        return true; // ผ่านทั้ง 3 เงื่อนไข → ใส่ได้ ✅
    }

    // === Solver สำหรับโหมด Custom ===
    // ใช้ Backtracking แก้โจทย์ที่ผู้ใช้กรอกมา (เติมเฉพาะช่องที่เป็น 0)
    // ต่างจาก fillBoard() ตรงที่ไม่ shuffle เลข (เพื่อให้ได้คำตอบที่แน่นอน)
    private boolean solveBoard(int[][] board, int row, int col) {
        if (row == SIZE) // Base Case: แก้ครบทุกช่องแล้ว!
            return true;
        int nextRow = (col == SIZE - 1) ? row + 1 : row;
        int nextCol = (col + 1) % SIZE;

        // ถ้าช่องนี้มีเลขอยู่แล้ว (ผู้ใช้กรอกมา) → ข้ามไปช่องถัดไป
        if (board[row][col] != 0) {
            return solveBoard(board, nextRow, nextCol);
        }

        // ลองใส่เลข 1-9 ตามลำดับ (ไม่สุ่ม)
        for (int num = 1; num <= 9; num++) {
            if (isValid(board, row, col, num)) {
                board[row][col] = num;
                if (solveBoard(board, nextRow, nextCol))
                    return true; // แก้ได้!
                board[row][col] = 0; // ⏪ Backtrack
            }
        }
        return false; // แก้ไม่ได้ → โจทย์ผิดหรือไม่มีคำตอบ
    }

    // ตรวจสอบว่า playerBoard ถูกกฎ Sudoku หรือไม่ (ไม่มีเลขซ้ำในแถว/คอลัมน์/กล่อง
    // 3x3)
    // ใช้ก่อน solve เพื่อจับ error เร็ว (ไม่ต้องรอ backtracking นาน)
    private boolean validateBoard() {
        for (int i = 0; i < SIZE; i++) {
            boolean[] rowCheck = new boolean[SIZE + 1]; // เช็คแถว
            boolean[] colCheck = new boolean[SIZE + 1]; // เช็คคอลัมน์
            for (int j = 0; j < SIZE; j++) {
                // เช็คแถว i
                int rv = playerBoard[i][j];
                if (rv != 0) {
                    if (rowCheck[rv])
                        return false; // ซ้ำในแถว!
                    rowCheck[rv] = true;
                }
                // เช็คคอลัมน์ i
                int cv = playerBoard[j][i];
                if (cv != 0) {
                    if (colCheck[cv])
                        return false; // ซ้ำในคอลัมน์!
                    colCheck[cv] = true;
                }
            }
        }
        // เช็คกล่อง 3x3 ทั้ง 9 กล่อง
        for (int br = 0; br < SIZE; br += SUBGRID) {
            for (int bc = 0; bc < SIZE; bc += SUBGRID) {
                boolean[] boxCheck = new boolean[SIZE + 1];
                for (int r = br; r < br + SUBGRID; r++) {
                    for (int c = bc; c < bc + SUBGRID; c++) {
                        int v = playerBoard[r][c];
                        if (v != 0) {
                            if (boxCheck[v])
                                return false; // ซ้ำในกล่อง!
                            boxCheck[v] = true;
                        }
                    }
                }
            }
        }
        return true; // ผ่าน ✅ ไม่มีเลขซ้ำ
    }

    // เช็คว่าช่อง (row, col) มีเลขซ้ำกับช่องอื่นในแถว/คอลัมน์/กล่อง 3x3 หรือไม่
    // ใช้สำหรับโหมด Custom เพื่อแจ้งเตือนทันทีขณะกรอก
    private boolean hasDuplicate(int row, int col) {
        int num = playerBoard[row][col];
        if (num == 0)
            return false; // ช่องว่างไม่มีทางซ้ำ

        // เช็คแถวและคอลัมน์
        for (int i = 0; i < SIZE; i++) {
            if (i != col && playerBoard[row][i] == num)
                return true; // ซ้ำในแถว
            if (i != row && playerBoard[i][col] == num)
                return true; // ซ้ำในคอลัมน์
        }
        // เช็คกล่อง 3x3
        int sr = (row / SUBGRID) * SUBGRID, sc = (col / SUBGRID) * SUBGRID;
        for (int r = sr; r < sr + SUBGRID; r++)
            for (int c = sc; c < sc + SUBGRID; c++)
                if (!(r == row && c == col) && playerBoard[r][c] == num)
                    return true; // ซ้ำในกล่อง
        return false; // ไม่ซ้ำ ✅
    }

    // สร้างโจทย์โดยการ "เจาะรู" (ลบตัวเลขออก) จากเฉลย
    // จำนวนช่องที่ลบ = ค่า difficulty (เช่น 40 ช่อง สำหรับ Medium)
    private void createPuzzle() {
        // คัดลอกเฉลยมาเป็นโจทย์ก่อน (ยังครบทุกช่อง)
        for (int r = 0; r < SIZE; r++)
            System.arraycopy(solution[r], 0, puzzle[r], 0, SIZE);

        // สุ่มลบตัวเลขออกจนครบจำนวนตาม difficulty
        Random rand = new Random();
        int removed = 0;
        while (removed < difficulty) {
            int r = rand.nextInt(SIZE); // สุ่มแถว 0-8
            int c = rand.nextInt(SIZE); // สุ่มคอลัมน์ 0-8
            if (puzzle[r][c] != 0) { // ถ้าช่องนี้ยังมีเลขอยู่
                puzzle[r][c] = 0; // ลบออก (เป็นช่องว่าง)
                removed++;
            }
            // ถ้าช่องนี้เคยลบไปแล้ว → ข้าม แล้วสุ่มใหม่
        }
    }

    // เริ่มเกมใหม่: สร้างเฉลย → สร้างโจทย์ → แสดงบนหน้าจอ → รีเซ็ตสถิติ
    private void newGame() {
        generateSolution(); // ขั้นตอน 1: สร้างเฉลยที่ถูกต้อง
        createPuzzle(); // ขั้นตอน 2: เจาะรูสร้างโจทย์

        // ขั้นตอน 3: นำโจทย์มาแสดงบน UI ทุกช่อง
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                playerBoard[r][c] = puzzle[r][c]; // คัดลอกโจทย์มาเป็นกระดานผู้เล่น
                fixed[r][c] = puzzle[r][c] != 0; // ถ้ามีเลข → ล็อคไม่ให้แก้
                if (fixed[r][c]) {
                    // ช่องโจทย์: แสดงเลข, สีเทา, ห้ามแก้ไข
                    cells[r][c].setText(String.valueOf(puzzle[r][c]));
                    cells[r][c].setForeground(TEXT_FIXED);
                    cells[r][c].setEditable(false);
                    cells[r][c].setBackground(BG_CELL_FIXED);
                } else {
                    // ช่องว่าง: ไม่แสดงเลข, สีน้ำเงิน, แก้ไขได้
                    cells[r][c].setText("");
                    cells[r][c].setForeground(TEXT_USER);
                    cells[r][c].setEditable(true);
                    cells[r][c].setBackground(BG_CELL);
                }
            }
        }

        // ขั้นตอน 4: รีเซ็ตสถิติทั้งหมด

        secondsElapsed = 0;
        timerLabel.setText("⏱ 00:00");
        statusLabel.setText(difficultyName);
        selectedRow = -1; // ยังไม่ได้เลือกช่องไหน
        selectedCol = -1;
    }

    // เริ่มเกมโหมด Custom: กระดานเปล่า ผู้ใช้กรอกโจทย์เอง แล้วกด Solve เพื่อดูคำตอบ
    private void newCustomGame() {
        // ล้างกระดานทั้งหมด — ทุกช่องเป็น 0 (ว่าง) และแก้ไขได้
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                solution[r][c] = 0;
                puzzle[r][c] = 0;
                playerBoard[r][c] = 0;
                fixed[r][c] = false; // ทุกช่องแก้ไขได้
                cells[r][c].setText("");
                cells[r][c].setForeground(TEXT_USER);
                cells[r][c].setEditable(true);
                cells[r][c].setBackground(BG_CELL);
            }
        }

        secondsElapsed = 0;

        timerLabel.setText(""); // ซ่อนเวลา
        statusLabel.setText("Custom - Input puzzle, press Solve");
        statusLabel.setForeground(ACCENT);
        selectedRow = -1;
        selectedCol = -1;
    }

    // เริ่มนาฬิกาจับเวลา: นับเพิ่มทุก 1 วินาที แล้วอัปเดตแสดงผลบนหน้าจอ
    private void startTimer() {
        if (gameTimer != null)
            gameTimer.stop(); // หยุดตัวจับเวลาเก่า (ถ้ามี)
        gameTimer = new Timer(1000, e -> { // ทุกๆ 1000ms (1 วินาที)
            secondsElapsed++;
            int min = secondsElapsed / 60; // คำนวณนาที
            int sec = secondsElapsed % 60; // คำนวณวินาที
            timerLabel.setText(String.format("⏱ %02d:%02d", min, sec)); // แสดง เช่น 02:35
        });
        gameTimer.start();
    }

    // ระบบคำใบ้: เฉลยช่องที่เลือก หรือสุ่มเฉลยช่องว่างอัตโนมัติ
    // ปิดใช้งานในโหมด Custom (เพราะยังไม่มี solution จนกว่าจะกด Solve)
    private void giveHint() {
        // โหมด Custom → ไม่มีคำใบ้ (ต้องกด Solve แทน)
        if (isCustomMode) {
            JOptionPane.showMessageDialog(this,
                    "Hint is not available in Custom mode.\nPlease press Solve to see the answer.",
                    "Hint", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        // กรณี 1: ผู้เล่นเลือกช่องว่างอยู่ → เฉลยช่องนั้นทันที
        if (selectedRow >= 0 && selectedCol >= 0 && !fixed[selectedRow][selectedCol]) {
            int answer = solution[selectedRow][selectedCol];
            playerBoard[selectedRow][selectedCol] = answer;
            cells[selectedRow][selectedCol].setText(String.valueOf(answer));
            cells[selectedRow][selectedCol].setForeground(ACCENT_GREEN); // แสดงเป็นสีเขียว (แยกจากของผู้เล่น)
            highlightCells();
            checkWin();
        } else {
            // กรณี 2: ไม่ได้เลือกช่องว่าง → สุ่มหาช่องที่ยังไม่ถูกหรือยังว่างมาเฉลยให้
            ArrayList<int[]> emptyCells = new ArrayList<>();
            for (int r = 0; r < SIZE; r++)
                for (int c = 0; c < SIZE; c++)
                    if (playerBoard[r][c] == 0 || playerBoard[r][c] != solution[r][c])
                        emptyCells.add(new int[] { r, c }); // เก็บช่องที่ยังว่างหรือตอบผิด

            if (!emptyCells.isEmpty()) {
                int[] pick = emptyCells.get(new Random().nextInt(emptyCells.size())); // สุ่มเลือก 1 ช่อง
                int r = pick[0], c = pick[1];
                playerBoard[r][c] = solution[r][c]; // ใส่คำตอบที่ถูก
                cells[r][c].setText(String.valueOf(solution[r][c]));
                cells[r][c].setForeground(ACCENT_GREEN); // สีเขียว = คำใบ้
                cells[r][c].requestFocusInWindow(); // ย้าย focus ไปช่องที่เฉลย
                highlightCells();
                checkWin();
            }
        }
    }

    // เอฟเฟกต์การกระพริบสีเขียวเมื่อแสดงเฉลย
    private void startRevealAnimation() {
        Timer reveal = new Timer(80, new ActionListener() {
            int count = 0;
            Random rand = new Random();

            @Override
            public void actionPerformed(ActionEvent e) {
                // สุ่มสีเขียวหลายเฉดให้เฉพาะช่องที่ผู้เล่นกรอก (ไม่ใช่โจทย์)
                for (int r = 0; r < SIZE; r++) {
                    for (int c = 0; c < SIZE; c++) {
                        if (!fixed[r][c]) {
                            cells[r][c].setBackground(new Color(
                                    10 + rand.nextInt(30), // R: 10-39 (น้อย)
                                    100 + rand.nextInt(130), // G: 100-229 (เขียวเด่น)
                                    50 + rand.nextInt(100))); // B: 50-149
                        }
                    }
                }
                count++;
                if (count > 12) { // กระพริบ 12 รอบ → หยุด
                    ((Timer) e.getSource()).stop();
                    // เปลี่ยนเป็นสีเขียวเข้มสม่ำเสมอ
                    for (int r = 0; r < SIZE; r++) {
                        for (int c = 0; c < SIZE; c++) {
                            if (!fixed[r][c]) {
                                cells[r][c].setBackground(BG_CELL);
                            }
                        }
                    }
                }
            }
        });
        reveal.start();
    }

    // เปิดเฉลยทั้งหมด: ถามยืนยันก่อน แล้วแสดงคำตอบเต็มกระดาน
    // ในโหมด Custom → ใช้ solveBoard() แก้จาก playerBoard แทน
    private void revealSolution() {
        // เช็คว่ามีช่องที่ผิดกฎ Sudoku (เลขซ้ำ) อยู่หรือไม่ → ต้องลบออกก่อนถึงจะ Solve
        // ได้
        if (!isCustomMode) {
            for (int r = 0; r < SIZE; r++) {
                for (int c = 0; c < SIZE; c++) {
                    if (!fixed[r][c] && playerBoard[r][c] != 0 && hasDuplicate(r, c)) {
                        JOptionPane.showMessageDialog(this,
                                "Please erase the incorrect answers (red cells) first\nbefore revealing the solution.",
                                "Incorrect Answers Found", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                }
            }
        }

        // ถามยืนยันก่อนเปิดเฉลย
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to reveal the solution?",
                "Reveal Solution", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            if (gameTimer != null)
                gameTimer.stop(); // หยุดจับเวลา

            // === โหมด Custom: ตรวจสอบโจทย์ก่อน แล้วแก้ด้วย Backtracking ===
            if (isCustomMode) {
                // ขั้นตอน 1: ตรวจว่าโจทย์ที่กรอก ถูกกฎ Sudoku หรือไม่ (ไม่มีเลขซ้ำ)
                if (!validateBoard()) {
                    JOptionPane.showMessageDialog(this,
                            "Invalid puzzle! Duplicate numbers found.\nPlease check your input.",
                            "Error", JOptionPane.ERROR_MESSAGE);
                    statusLabel.setText("Invalid Puzzle");
                    statusLabel.setForeground(new Color(239, 68, 68));
                    return;
                }

                // ขั้นตอน 2: แก้โจทย์ใน background thread (ไม่ให้ UI ค้าง)
                statusLabel.setText("Solving...");
                statusLabel.setForeground(ACCENT);
                // ล็อคทุกช่องระหว่างแก้
                for (int r = 0; r < SIZE; r++)
                    for (int c = 0; c < SIZE; c++)
                        cells[r][c].setEditable(false);

                SwingWorker<boolean[], Void> worker = new SwingWorker<>() {
                    int[][] boardToSolve = new int[SIZE][SIZE];

                    @Override
                    protected boolean[] doInBackground() {
                        // คัดลอก playerBoard ไปแก้
                        for (int r = 0; r < SIZE; r++)
                            System.arraycopy(playerBoard[r], 0, boardToSolve[r], 0, SIZE);
                        boolean solved = solveBoard(boardToSolve, 0, 0);
                        return new boolean[] { solved };
                    }

                    @Override
                    protected void done() {
                        try {
                            boolean solved = get()[0];
                            if (solved) {
                                // แก้ได้! → แสดงคำตอบ
                                for (int r = 0; r < SIZE; r++) {
                                    for (int c = 0; c < SIZE; c++) {
                                        solution[r][c] = boardToSolve[r][c];
                                        if (playerBoard[r][c] == 0) {
                                            playerBoard[r][c] = boardToSolve[r][c];
                                            cells[r][c].setText(String.valueOf(boardToSolve[r][c]));
                                            cells[r][c].setForeground(ACCENT_GREEN);
                                        }
                                        cells[r][c].setBackground(BG_CELL); // ลบสีแดงออก
                                        cells[r][c].setEditable(false);
                                    }
                                }
                                statusLabel.setText("Solution Found!");
                                statusLabel.setForeground(ACCENT_GREEN);
                                startRevealAnimation(); // เอฟเฟกต์กระพริบสีเขียว
                            } else {
                                // แก้ไม่ได้! → เปิดให้แก้ใหม่
                                for (int r = 0; r < SIZE; r++)
                                    for (int c = 0; c < SIZE; c++)
                                        cells[r][c].setEditable(true);
                                JOptionPane.showMessageDialog(SudokuGame.this,
                                        "No solution exists for this puzzle!\nPlease check your input.",
                                        "Error", JOptionPane.ERROR_MESSAGE);
                                statusLabel.setText("No Solution Found");
                                statusLabel.setForeground(new Color(239, 68, 68));
                            }
                        } catch (InterruptedException | ExecutionException ex) {
                            statusLabel.setText("Error");
                        }
                    }
                };
                worker.execute();
                return;
            }

            // === โหมดปกติ: แสดง solution ที่สร้างไว้ล่วงหน้า ===
            for (int r = 0; r < SIZE; r++) {
                for (int c = 0; c < SIZE; c++) {
                    playerBoard[r][c] = solution[r][c];
                    cells[r][c].setText(String.valueOf(solution[r][c]));
                    if (!fixed[r][c]) {
                        cells[r][c].setForeground(ACCENT_GREEN); // ช่องที่เฉลย = สีเขียว
                        cells[r][c].setBackground(BG_CELL); // ลบสีแดงออก
                    } else {
                        cells[r][c].setBackground(BG_CELL_FIXED); // คืนสีช่องโจทย์
                    }
                    cells[r][c].setEditable(false); // ล็อคทุกช่อง
                }
            }
            startRevealAnimation(); // เอฟเฟกต์กระพริบสีเขียว
            statusLabel.setText("Solution Revealed"); // เปลี่ยนสถานะ
            statusLabel.setForeground(new Color(251, 191, 36)); // สีเหลืองทอง
        }
    }

    // เช็คว่าผู้เล่นชนะหรือยัง: กรอกครบทุกช่อง + ไม่มีเลขซ้ำผิดกฎ Sudoku
    private void checkWin() {
        // ถ้ามีช่องไหนยังว่าง หรือมีเลขซ้ำ → ยังไม่ชนะ กลับออกไป
        for (int r = 0; r < SIZE; r++)
            for (int c = 0; c < SIZE; c++)
                if (playerBoard[r][c] == 0 || hasDuplicate(r, c))
                    return;

        // === ถ้ามาถึงตรงนี้ = เติมถูกครบทุกช่อง = ชนะ! ===
        gameTimer.stop();
        statusLabel.setText("🎉 YOU WIN!");
        statusLabel.setForeground(ACCENT_GREEN);

        // แสดง Dialog สรุปผลการเล่น
        int min = secondsElapsed / 60;
        int sec = secondsElapsed % 60;
        JOptionPane.showMessageDialog(this,
                String.format("🎉 Congratulations!\nTime: %02d:%02d\nDifficulty: %s",
                        min, sec, difficultyName),
                "You Won!", JOptionPane.INFORMATION_MESSAGE);
    }

    // แสดง Dialog ให้ผู้เล่นเลือกระดับความยาก แล้วเริ่มเกมใหม่
    // fromMenu = true ถ้ากดมาจากหน้า Menu (ต้องสลับ Card ไปหน้า GAME)
    // fromMenu = false ถ้ากดมาจากในเกม (อยู่หน้า GAME อยู่แล้ว)
    private void showDifficultyDialog(boolean fromMenu) {
        // เพิ่มตัวเลือก Custom สำหรับกรอกโจทย์เอง
        String[] options = { "Easy", "Medium", "Hard", "Expert", "Custom" };
        int choice = JOptionPane.showOptionDialog(this,
                "Select difficulty:", "New Game",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                null, options, "Medium");

        // ถ้าผู้ใช้กดปิดหน้าต่าง (กากบาท) ให้กลับไปโดยไม่ต้องทำอะไร
        if (choice == JOptionPane.CLOSED_OPTION)
            return;

        // ถ้าเรียกจากหน้าเมนู ให้สลับ Card ไปหน้า GAME
        if (fromMenu) {
            cardLayout.show(mainContentPanel, "GAME");
        }

        // หยุด timer เก่า
        secondsElapsed = 0;
        if (gameTimer != null)
            gameTimer.stop();

        // === โหมด Custom: กระดานเปล่า ให้ผู้ใช้กรอกโจทย์เอง ===
        if (choice == 4) {
            isCustomMode = true;
            difficultyName = "Custom";
            newCustomGame();
            return; // ไม่ต้อง startTimer ในโหมด Custom
        }

        // === โหมดปกติ: สร้างโจทย์อัตโนมัติ ===
        isCustomMode = false;
        // ตั้งค่าจำนวนช่องที่จะลบตามระดับที่เลือก
        switch (choice) {
            case 0 -> {
                difficulty = 30; // Easy: ลบ 30 ช่อง (เหลือ 51)
                difficultyName = "Easy";
            }
            case 1 -> {
                difficulty = 40; // Medium: ลบ 40 ช่อง (เหลือ 41)
                difficultyName = "Medium";
            }
            case 2 -> {
                difficulty = 50; // Hard: ลบ 50 ช่อง (เหลือ 31)
                difficultyName = "Hard";
            }
            case 3 -> {
                difficulty = 58; // Expert: ลบ 58 ช่อง (เหลือ 23)
                difficultyName = "Expert";
            }
            default -> {
            }
        }

        newGame(); // สร้างเฉลย + โจทย์ + แสดงบน UI
        startTimer(); // เริ่มจับเวลา
        statusLabel.setForeground(ACCENT); // สีน้ำเงินเริ่มต้น
    }

    // === จุดเริ่มต้นของโปรแกรม ===
    public static void main(String[] args) {
        try {
            // ตั้งค่า UI ให้ดูเหมือนโปรแกรมบน OS (Windows/Mac)
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException
                | UnsupportedLookAndFeelException ignored) {
        }

        // สร้างหน้าต่างเกมใน Event Dispatch Thread (กฎของ Swing)
        SwingUtilities.invokeLater(SudokuGame::new);
    }
}