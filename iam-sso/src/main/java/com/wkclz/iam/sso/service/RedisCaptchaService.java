package com.wkclz.iam.sso.service;

import com.wkclz.auth.bean.Captcha;
import com.wkclz.auth.contract.auth.CaptchaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class RedisCaptchaService implements CaptchaService {

    private static final long CAPTCHA_TTL_MINUTES = 5;
    private static final int CAPTCHA_LENGTH = 4;
    private static final int WIDTH = 120;
    private static final int HEIGHT = 40;
    private static final int FONT_SIZE = 20;
    private static final String CHARACTERS = "23456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";
    private static final String REDIS_KEY_PREFIX = "iam:captcha:";

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Override
    public Captcha generate() {
        String captchaCode = generateCaptchaCode();
        String captchaId = UUID.randomUUID().toString().replace("-", "");
        String base64Image = generateCaptchaImage(captchaCode);

        String redisKey = REDIS_KEY_PREFIX + captchaId;
        redisTemplate.opsForValue().set(redisKey, captchaCode, CAPTCHA_TTL_MINUTES, TimeUnit.MINUTES);

        Captcha captcha = new Captcha();
        captcha.setCaptchaId(captchaId);
        captcha.setCaptchaImage(base64Image);
        captcha.setExpireTime(LocalDateTime.now().plusMinutes(CAPTCHA_TTL_MINUTES));
        return captcha;
    }

    @Override
    public boolean verify(String captchaId, String captchaCode) {
        if (captchaId == null || captchaCode == null) {
            return false;
        }
        String redisKey = REDIS_KEY_PREFIX + captchaId;
        String stored = redisTemplate.opsForValue().getAndDelete(redisKey);
        if (stored == null) {
            return false;
        }
        return stored.equalsIgnoreCase(captchaCode);
    }

    // ===== 以下图形生成逻辑提取自 CaptchaHelper =====

    private String generateCaptchaCode() {
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < CAPTCHA_LENGTH; i++) {
            sb.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }
        return sb.toString();
    }

    private String generateCaptchaImage(String captchaCode) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, WIDTH, HEIGHT);

        Font font = new Font("Arial", Font.BOLD, FONT_SIZE);
        g.setFont(font);

        drawInterferenceLines(g);
        drawNoisePoints(g);
        drawCharacters(g, captchaCode);
        g.dispose();

        return "data:image/png;base64," + imageToBase64(image);
    }

    private void drawInterferenceLines(Graphics2D g) {
        Random random = new Random();
        g.setColor(Color.LIGHT_GRAY);
        for (int i = 0; i < 5; i++) {
            g.drawLine(random.nextInt(WIDTH), random.nextInt(HEIGHT),
                    random.nextInt(WIDTH), random.nextInt(HEIGHT));
        }
    }

    private void drawNoisePoints(Graphics2D g) {
        Random random = new Random();
        for (int i = 0; i < 50; i++) {
            int x = random.nextInt(WIDTH);
            int y = random.nextInt(HEIGHT);
            g.setColor(new Color(random.nextInt(256), random.nextInt(256), random.nextInt(256)));
            g.fillOval(x, y, random.nextInt(6), random.nextInt(6));
        }
    }

    private void drawCharacters(Graphics2D g, String captchaCode) {
        Random random = new Random();
        int charWidth = WIDTH / captchaCode.length();
        for (int i = 0; i < captchaCode.length(); i++) {
            g.setColor(new Color(random.nextInt(100), random.nextInt(100), random.nextInt(100)));
            double angle = random.nextDouble() * 0.4 - 0.2;
            g.rotate(angle, i * charWidth + (double) charWidth / 2, (double) HEIGHT / 2);
            g.drawString(String.valueOf(captchaCode.charAt(i)), i * charWidth + 10, HEIGHT / 2 + 8);
            g.rotate(-angle, i * charWidth + (double) charWidth / 2, (double) HEIGHT / 2);
        }
    }

    private String imageToBase64(BufferedImage image) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("验证码图片生成失败", e);
        }
    }
}
