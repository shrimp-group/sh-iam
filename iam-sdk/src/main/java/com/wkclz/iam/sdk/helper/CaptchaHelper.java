package com.wkclz.iam.sdk.helper;

import com.wkclz.core.exception.SystemException;
import com.wkclz.iam.sdk.model.PictureCaptchaResponse;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Random;
import java.util.UUID;

public class CaptchaHelper {

    // 验证码有效时间：5
    private static final long CAPTCHA_EXPIRATION = 5;
    // 验证码长度
    private static final int CAPTCHA_LENGTH = 4;
    // 验证码宽度
    private static final int WIDTH = 120;
    // 验证码高度
    private static final int HEIGHT = 40;
    // 字体大小
    private static final int FONT_SIZE = 20;
    // 可选字符集，避开容易混淆的字符：0, O, 1, I, l
    private static final String CHARACTERS = "23456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";


    public static PictureCaptchaResponse getCaptcha() {
        // 生成验证码内容
        String captchaCode = generateCaptchaCode();
        // 生成验证码唯一ID
        String captchaId = UUID.randomUUID().toString().replace("-", "");
        // 生成验证码图片
        String base64Image = generateCaptchaImage(captchaCode);

        // 构建返回结果
        PictureCaptchaResponse response = new PictureCaptchaResponse();
        response.setCaptchaId(captchaId);
        response.setCaptchaCode(captchaCode);
        response.setCaptchaImage(base64Image);
        response.setExpireTime(System.currentTimeMillis() + CAPTCHA_EXPIRATION * 60 * 1000);

        return response;
    }


    public static String getCaptchaRedisKey(String captchaId) {
        return "iam:captcha:" + captchaId;
    }


    /**
     * 生成验证码内容
     * @return 验证码字符串
     */
    private static String generateCaptchaCode() {
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < CAPTCHA_LENGTH; i++) {
            int index = random.nextInt(CHARACTERS.length());
            sb.append(CHARACTERS.charAt(index));
        }
        return sb.toString();
    }

    /**
     * 生成验证码图片
     * @param captchaCode 验证码内容
     * @return base64编码的图片
     */
    private static String generateCaptchaImage(String captchaCode) {
        // 创建图片缓冲区
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        // 设置背景色
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, WIDTH, HEIGHT);
        // 设置字体
        Font font = new Font("Arial", Font.BOLD, FONT_SIZE);
        g.setFont(font);
        // 绘制干扰线
        drawInterferenceLines(g);
        // 绘制噪点
        drawNoisePoints(g);
        // 绘制验证码字符
        drawCaptchaCharacters(g, captchaCode);
        // 添加边框
        // g.setColor(Color.GRAY);
        // g.drawRect(0, 0, WIDTH - 1, HEIGHT - 1);
        // 释放资源
        g.dispose();
        // 将图片转换为base64
        return "data:image/png;base64,"+ imageToBase64(image);
    }

    /**
     * 绘制干扰线
     * @param g 图形上下文
     */
    private static void drawInterferenceLines(Graphics2D g) {
        Random random = new Random();
        g.setColor(Color.LIGHT_GRAY);
        for (int i = 0; i < 5; i++) {
            int x1 = random.nextInt(WIDTH);
            int y1 = random.nextInt(HEIGHT);
            int x2 = random.nextInt(WIDTH);
            int y2 = random.nextInt(HEIGHT);
            g.drawLine(x1, y1, x2, y2);
        }
    }

    /**
     * 绘制噪点
     * @param g 图形上下文
     */
    private static void drawNoisePoints(Graphics2D g) {
        Random random = new Random();
        for (int i = 0; i < 50; i++) {
            int x = random.nextInt(WIDTH);
            int y = random.nextInt(HEIGHT);
            int radius = random.nextInt(6);
            g.setColor(new Color(random.nextInt(256), random.nextInt(256), random.nextInt(256)));
            g.fillOval(x, y, radius, radius);
        }
    }

    /**
     * 绘制验证码字符
     * @param g 图形上下文
     * @param captchaCode 验证码内容
     */
    private static void drawCaptchaCharacters(Graphics2D g, String captchaCode) {
        Random random = new Random();
        int charWidth = WIDTH / captchaCode.length();
        for (int i = 0; i < captchaCode.length(); i++) {
            // 随机颜色
            g.setColor(new Color(random.nextInt(100), random.nextInt(100), random.nextInt(100)));
            // 随机旋转角度, -10° 到 10°
            double angle = random.nextDouble() * 0.4 - 0.2;
            g.rotate(angle, i * charWidth + (double) charWidth / 2, (double) HEIGHT / 2);
            // 绘制字符
            g.drawString(String.valueOf(captchaCode.charAt(i)), i * charWidth + 10, HEIGHT / 2 + 8);
            // 恢复旋转
            g.rotate(-angle, i * charWidth + (double) charWidth / 2, (double) HEIGHT / 2);
        }
    }

    /**
     * 将图片转换为base64编码
     * @param image 图片对象
     * @return base64编码的图片
     */
    private static String imageToBase64(BufferedImage image) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", baos);
            byte[] imageBytes = baos.toByteArray();
            return Base64.getEncoder().encodeToString(imageBytes);
        } catch (IOException e) {
            throw SystemException.of("验证码图片生成失败");
        }
    }


}
