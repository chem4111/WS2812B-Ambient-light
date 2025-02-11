package com.example.colorled;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.graphics.Color;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    private SeekBar brightnessSeekBar;
    private SeekBar redSeekBar, greenSeekBar, blueSeekBar;
    private Button breathingLightButton, openSettingsButton;
    private TextView colorPreview;
    private int currentBrightness = 0;
    private boolean isBreathing = false;
    private boolean isColorAdjusting = true;  // 控制滑块是否可用

    // 定义七种颜色
    private final int[] colors = {
            Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW,
            Color.CYAN, Color.MAGENTA, Color.WHITE
    };

    private int currentColorIndex = 0; // 当前颜色的索引
    private int BREATHING_STEP = 3;      // 每次增减亮度的步长
    private int breathingDirection = 1;  // 用于控制亮度的增减

    private Handler handler = new Handler(Looper.getMainLooper());  // 用于更新 UI 线程

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 初始化控件
        brightnessSeekBar = findViewById(R.id.brightnessSeekBar);
        redSeekBar = findViewById(R.id.redSeekBar);
        greenSeekBar = findViewById(R.id.greenSeekBar);
        blueSeekBar = findViewById(R.id.blueSeekBar);
        breathingLightButton = findViewById(R.id.breathingLightButton);
        openSettingsButton = findViewById(R.id.openSettingsButton);
        colorPreview = findViewById(R.id.colorPreview);

        // 设置 SeekBar 值变化时更新色块预览并实时发送数据
        SeekBar.OnSeekBarChangeListener colorSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (isColorAdjusting) {
                    updateColorPreview();
                    int brightness = brightnessSeekBar.getProgress();
                    int red = redSeekBar.getProgress();
                    int green = greenSeekBar.getProgress();
                    int blue = blueSeekBar.getProgress();

                    sendData(brightness, red, green, blue); // 每次颜色变化时发送数据
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        };

        redSeekBar.setOnSeekBarChangeListener(colorSeekBarChangeListener);
        greenSeekBar.setOnSeekBarChangeListener(colorSeekBarChangeListener);
        blueSeekBar.setOnSeekBarChangeListener(colorSeekBarChangeListener);

        // 设置氛围灯按钮点击事件
        breathingLightButton.setOnClickListener(v -> toggleBreathingLight());

        // 设置 IP 和端口按钮点击事件
        openSettingsButton.setOnClickListener(view -> showSettingsDialog());
    }

    // 更新颜色预览框背景颜色
    private void updateColorPreview() {
        int red = redSeekBar.getProgress();
        int green = greenSeekBar.getProgress();
        int blue = blueSeekBar.getProgress();
        int brightness = brightnessSeekBar.getProgress();

        // 根据亮度调整颜色
        int adjustedRed = (int) (red * (brightness / 255.0f));
        int adjustedGreen = (int) (green * (brightness / 255.0f));
        int adjustedBlue = (int) (blue * (brightness / 255.0f));

        // 将颜色值限制在有效范围内
        adjustedRed = Math.min(Math.max(adjustedRed, 0), 255);
        adjustedGreen = Math.min(Math.max(adjustedGreen, 0), 255);
        adjustedBlue = Math.min(Math.max(adjustedBlue, 0), 255);

        // 更新预览框颜色
        colorPreview.setBackgroundColor(Color.rgb(adjustedRed, adjustedGreen, adjustedBlue));

        // 发送数据
        sendData(brightness, adjustedRed, adjustedGreen, adjustedBlue);
    }

    // 发送数据到目标设备
    private void sendData(int brightness, int red, int green, int blue) {
        // 从 SharedPreferences 获取存储的 IP 和端口
        SharedPreferences sharedPreferences = getSharedPreferences("UDP_Settings", Context.MODE_PRIVATE);
        String serverIp = sharedPreferences.getString("ip", "192.168.1.100"); // 默认值
        int serverPort = sharedPreferences.getInt("port", 12345); // 默认值

        // 生成 JSON 数据
        String message = generateJsonData(brightness, red, green, blue);

        // 通过 UDP 发送数据
        new UDPSenderTask().execute(serverIp, String.valueOf(serverPort), message);
    }

    // 生成 JSON 数据
    private String generateJsonData(int brightness, int r, int g, int b) {
        try {
            // 创建 JSON 对象
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("brightness", brightness);

            // 创建 LEDs 数组
            JSONArray ledsArray = new JSONArray();
            for (int i = 0; i < 6; i++) {
                ledsArray.put(new JSONArray().put(r).put(g).put(b));
            }

            // 将 LEDs 数组添加到 JSON 对象中
            jsonObject.put("leds", ledsArray);

            return jsonObject.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // 显示 IP 和端口设置的弹窗
    private void showSettingsDialog() {
        // 获取 SharedPreferences 数据
        SharedPreferences sharedPreferences = getSharedPreferences("UDP_Settings", Context.MODE_PRIVATE);
        String savedIp = sharedPreferences.getString("ip", "");
        int savedPort = sharedPreferences.getInt("port", 0);

        // 创建弹出框视图
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_settings, null);
        EditText ipEditText = dialogView.findViewById(R.id.ipEditText);
        EditText portEditText = dialogView.findViewById(R.id.portEditText);

        // 设置默认值
        ipEditText.setText(savedIp);
        portEditText.setText(savedPort == 0 ? "" : String.valueOf(savedPort));

        // 创建弹出框
        new AlertDialog.Builder(this)
                .setTitle("设置目标 IP 和端口")
                .setView(dialogView)
                .setPositiveButton("保存", (dialog, which) -> {
                    String ip = ipEditText.getText().toString();
                    int port = Integer.parseInt(portEditText.getText().toString());

                    // 保存到 SharedPreferences
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("ip", ip);
                    editor.putInt("port", port);
                    editor.apply();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    // 开关呼吸灯
    private void toggleBreathingLight() {
        if (isBreathing) {
            // 停止呼吸灯
            isBreathing = false;
            isColorAdjusting = true;  // 允许调整颜色
            breathingLightButton.setText("启动");
        } else {
            // 停止颜色调整
            isColorAdjusting = false;
            breathingLightButton.setText("停止");
            startBreathingLight();  // 启动呼吸灯
        }
    }

    // 启动呼吸灯效果
    private void startBreathingLight() {
        isBreathing = true;  // 开启呼吸灯

        // 使用 Handler 和 Runnable 实现每隔一段时间更新一次亮度
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isBreathing) {
                    // 随机选择一个颜色
                    int color = colors[currentColorIndex];

                    // 从颜色中提取 RGB 分量
                    int red = Color.red(color);
                    int green = Color.green(color);
                    int blue = Color.blue(color);

                    // 模拟呼吸灯效果：逐渐增加或减少亮度
                    if (currentBrightness >= 255) {
                        breathingDirection = -1;  // 反转亮度增减方向（减少亮度）
                    } else if (currentBrightness <= 0) {
                        breathingDirection = 1;   // 反转亮度增减方向（增加亮度）
                        currentColorIndex = (currentColorIndex + 1) % colors.length;  // 切换到下一个颜色
                    }

                    // 根据当前方向调整亮度
                    currentBrightness += breathingDirection * BREATHING_STEP;

                    // 防止亮度超出范围
                    currentBrightness = Math.max(0, Math.min(currentBrightness, 255));

                    // 发送数据
                    sendData(currentBrightness, red, green, blue);

                    // 继续执行呼吸灯效果
                    handler.postDelayed(this, 10);  // 每 10 毫秒更新一次亮度
                }
            }
        }, 20);  // 启动时立即调用
    }

}

