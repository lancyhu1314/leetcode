import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Random;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;

public class TimerApp extends JFrame {

    private JButton startButton;
    private JLabel timeLabel;
    private JLabel statusLabel;

    private SwingWorker<Void, String> timerWorker;
    private final int TOTAL_MINUTES = 90;
    private final long TOTAL_DURATION_SECONDS = TOTAL_MINUTES * 60L;

    // 音频文件名常量
    private final String INTERVAL_START_SOUND_FILE = "interval_start_sound.wav"; // 间隔开始提示音
    private final String INTERVAL_END_SOUND_FILE = "interval_end_sound.wav";   // 间隔结束提示音 (10秒后)
    private final String FINAL_END_SOUND_FILE = "end_sound.wav";               // 90分钟结束提示音

    // 间隔开始和结束音之间的延迟 (单位：秒)
    // The code is designed for this to be 10 seconds.
    // If you experience a different delay, please verify this constant in your running code.
    private final int INTERVAL_END_SOUND_DELAY_SECONDS = 10;

    private Random random = new Random();
    private long nextIntervalStartTriggerSeconds; // 下一个“间隔开始提示音”的触发时间（总用时秒数）
    private long currentIntervalEndTriggerSeconds; // 当前“间隔结束提示音”的触发时间, -1表示没有计划中的

    public TimerApp() {
        setTitle("90分钟计时器 (增强版)");
        setSize(450, 220); // 稍微增大窗口以容纳更长的文件名信息
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        initComponents();
        layoutComponents();
    }

    private void initComponents() {
        startButton = new JButton("开始计时 (90分钟)");
        timeLabel = new JLabel(formatTime(TOTAL_DURATION_SECONDS), SwingConstants.CENTER);
        timeLabel.setFont(new Font("Arial", Font.BOLD, 48));
        statusLabel = new JLabel("请点击开始。提示音: " + INTERVAL_START_SOUND_FILE + ", " + INTERVAL_END_SOUND_FILE + ", " + FINAL_END_SOUND_FILE, SwingConstants.CENTER);
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 12)); // 调整字体大小以适应更多文本

        startButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (timerWorker == null || timerWorker.isDone()) {
                    startTimer();
                }
            }
        });
    }

    private void layoutComponents() {
        setLayout(new BorderLayout(10, 10));
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(timeLabel, BorderLayout.CENTER);
        topPanel.add(statusLabel, BorderLayout.SOUTH);

        add(topPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.add(startButton);
        add(buttonPanel, BorderLayout.SOUTH);

        ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    }

    private void startTimer() {
        startButton.setEnabled(false);
        statusLabel.setText("timing...");
        timeLabel.setText(formatTime(TOTAL_DURATION_SECONDS));

        // 初始化第一个间隔开始提示音的时间
        scheduleNextIntervalStartSound(0);
        currentIntervalEndTriggerSeconds = -1; // 没有计划中的间隔结束音

        timerWorker = new SwingWorker<Void, String>() {
            @Override
            protected Void doInBackground() throws Exception {
                long remainingSecondsInTotal; // 总剩余秒数

                for (long elapsedSeconds = 0; elapsedSeconds < TOTAL_DURATION_SECONDS; elapsedSeconds++) {
                    if (isCancelled()) {
                        break;
                    }

                    remainingSecondsInTotal = TOTAL_DURATION_SECONDS - elapsedSeconds;
                    publish(formatTime(remainingSecondsInTotal));

                    // 检查是否到达播放“间隔开始提示音”的时间
                    if (elapsedSeconds == nextIntervalStartTriggerSeconds) {
                        playSound(INTERVAL_START_SOUND_FILE, "间隔开始提示音");
                        // 安排10秒后播放“间隔结束提示音”
                        currentIntervalEndTriggerSeconds = elapsedSeconds + INTERVAL_END_SOUND_DELAY_SECONDS;
                        // 确保“间隔结束提示音”不超出总时长
                        if (currentIntervalEndTriggerSeconds >= TOTAL_DURATION_SECONDS) {
                            currentIntervalEndTriggerSeconds = -1; // 取消，如果它会超出总时间
                        }
                        // 安排下一个“间隔开始提示音”
                        scheduleNextIntervalStartSound(elapsedSeconds);
                    }

                    // 检查是否到达播放“间隔结束提示音”的时间
                    if (currentIntervalEndTriggerSeconds != -1 && elapsedSeconds == currentIntervalEndTriggerSeconds) {
                        playSound(INTERVAL_END_SOUND_FILE, "间隔结束提示音");
                        currentIntervalEndTriggerSeconds = -1; // 重置，因为它已经播放了
                    }

                    Thread.sleep(1000);
                }
                return null;
            }

            @Override
            protected void process(java.util.List<String> chunks) {
                if (!chunks.isEmpty()) {
                    timeLabel.setText(chunks.get(chunks.size() - 1));
                }
            }

            @Override
            protected void done() {
                try {
                    if (!isCancelled()) {
                        timeLabel.setText("00:00:00");
                        statusLabel.setText("时间到！");
                        playSound(FINAL_END_SOUND_FILE, "结束提示音");
                        JOptionPane.showMessageDialog(TimerApp.this,
                                "90分钟计时结束！",
                                "提示",
                                JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        statusLabel.setText("计时已取消");
                    }
                } finally {
                    startButton.setEnabled(true);
                }
            }
        };
        timerWorker.execute();
    }

    private void scheduleNextIntervalStartSound(long currentTotalElapsedSeconds) {
        // 5到8分钟，转换为秒 (300到480秒)
        int minIntervalSeconds = 5 * 60; // Changed from 3 to 5 minutes
        int maxIntervalSeconds = 8 * 60; // Changed from 5 to 8 minutes
        long randomDelaySeconds = minIntervalSeconds + random.nextInt(maxIntervalSeconds - minIntervalSeconds + 1);

        nextIntervalStartTriggerSeconds = currentTotalElapsedSeconds + randomDelaySeconds;

        // 确保下一次提示音不会超过总时长 (严格来说，是确保计划的时间点有意义)
        // 如果计划的下一个开始时间已经等于或超过总时长，那么它实际上不会再响了
        // 因为循环条件是 elapsedSeconds < TOTAL_DURATION_SECONDS
        if (nextIntervalStartTriggerSeconds >= TOTAL_DURATION_SECONDS) {
            nextIntervalStartTriggerSeconds = TOTAL_DURATION_SECONDS + 1; // 设置一个永远不会达到的值，有效禁用后续间隔音
            System.out.println("后续间隔提示音已超出总时长 ("+ formatTime(nextIntervalStartTriggerSeconds) +")，将不再安排。");
        } else {
            System.out.println("下一次 [间隔开始提示音] 计划在总用时: " + formatTime(nextIntervalStartTriggerSeconds) + " (即 " + nextIntervalStartTriggerSeconds + " 秒处)");
        }
    }

    private String formatTime(long totalSeconds) {
        if (totalSeconds < 0) totalSeconds = 0; // 避免负数显示
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    private void playSound(String soundFileName, String soundType) {
        try {
            File soundFile = new File(soundFileName);
            if (!soundFile.exists()) {
                System.err.println(soundType + " 文件未找到: " + soundFileName);
                // 更新状态标签，让用户知道问题
                String currentStatus = statusLabel.getText();
                if (!currentStatus.contains("错误")) { // 避免重复错误信息
                    statusLabel.setText("错误: " + soundType + " 文件 (" + soundFileName + ") 未找到!");
                }
                return;
            }

            AudioInputStream audioIn = AudioSystem.getAudioInputStream(soundFile.toURI().toURL());
            Clip clip = AudioSystem.getClip();
            clip.open(audioIn);
            clip.start();
            System.out.println("正在播放: " + soundType + " (" + soundFileName + ")");
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            System.err.println("播放 " + soundType + " (" + soundFileName + ") 时出错: " + e.getMessage());
            String currentStatus = statusLabel.getText();
            if (!currentStatus.contains("错误")) {
                statusLabel.setText("错误: 播放 " + soundType + " 失败");
            }
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new TimerApp().setVisible(true);
            }
        });
    }
}
