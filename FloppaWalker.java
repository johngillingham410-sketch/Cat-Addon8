package com.example.floppawalker.modules;

import com.example.floppawalker.FloppaAddon;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

import java.util.Random;

/**
 * Draws a Floppa cube that waddles around the screen and meows at random.
 *
 * Texture: assets/floppa_walker/textures/gui/floppa.png  (276x390)
 * Sound:   assets/floppa_walker/sounds/floppa.ogg, declared in assets/floppa_walker/sounds.json
 */
public class FloppaWalker extends Module {
    // Must match the real pixel size of floppa.png
    private static final int TEX_W = 276;
    private static final int TEX_H = 390;

    private static final Identifier TEXTURE = Identifier.of(FloppaAddon.ID, "textures/gui/floppa.png");
    // Not registered in the sound registry on purpose: SoundManager resolves it through sounds.json.
    private static final SoundEvent MEOW = SoundEvent.of(Identifier.of(FloppaAddon.ID, "floppa"));

    // ---------------------------------------------------------------- settings

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgSound = settings.createGroup("Sound");

    private final Setting<Integer> size = sgGeneral.add(new IntSetting.Builder()
        .name("size")
        .description("Height of Floppa in GUI pixels.")
        .defaultValue(70)
        .min(16)
        .sliderMin(16)
        .sliderMax(300)
        .build());

    private final Setting<Double> speed = sgGeneral.add(new DoubleSetting.Builder()
        .name("speed")
        .description("How fast Floppa walks, in GUI pixels per second.")
        .defaultValue(90)
        .min(5)
        .sliderMin(10)
        .sliderMax(500)
        .build());

    private final Setting<Boolean> waddle = sgGeneral.add(new BoolSetting.Builder()
        .name("waddle")
        .description("Tilts and bounces Floppa while walking.")
        .defaultValue(true)
        .build());

    private final Setting<Boolean> pauses = sgGeneral.add(new BoolSetting.Builder()
        .name("pauses")
        .description("Floppa stops for a moment after reaching each spot.")
        .defaultValue(true)
        .build());

    private final Setting<Boolean> bottomOnly = sgGeneral.add(new BoolSetting.Builder()
        .name("bottom-only")
        .description("Only walk back and forth along the bottom of the screen.")
        .defaultValue(false)
        .build());

    private final Setting<Boolean> hideWithHud = sgGeneral.add(new BoolSetting.Builder()
        .name("hide-with-hud")
        .description("Hide Floppa when the HUD is hidden (F1).")
        .defaultValue(true)
        .build());

    private final Setting<Boolean> playSound = sgSound.add(new BoolSetting.Builder()
        .name("play-sound")
        .description("Make Floppa let out a noise.")
        .defaultValue(true)
        .build());

    private final Setting<Boolean> soundOnEnable = sgSound.add(new BoolSetting.Builder()
        .name("sound-on-enable")
        .description("Play the noise right when the module is turned on.")
        .defaultValue(true)
        .visible(playSound::get)
        .build());

    private final Setting<Double> minInterval = sgSound.add(new DoubleSetting.Builder()
        .name("min-interval")
        .description("Shortest time between noises, in seconds.")
        .defaultValue(3)
        .min(0.5)
        .sliderMin(0.5)
        .sliderMax(60)
        .visible(playSound::get)
        .build());

    private final Setting<Double> maxInterval = sgSound.add(new DoubleSetting.Builder()
        .name("max-interval")
        .description("Longest time between noises, in seconds.")
        .defaultValue(8)
        .min(0.5)
        .sliderMin(0.5)
        .sliderMax(60)
        .visible(playSound::get)
        .build());

    private final Setting<Double> volume = sgSound.add(new DoubleSetting.Builder()
        .name("volume")
        .description("Volume of the noise.")
        .defaultValue(1.0)
        .min(0)
        .max(2)
        .sliderMin(0)
        .sliderMax(2)
        .visible(playSound::get)
        .build());

    private final Setting<Double> pitch = sgSound.add(new DoubleSetting.Builder()
        .name("pitch")
        .description("Pitch of the noise.")
        .defaultValue(1.0)
        .min(0.5)
        .max(2)
        .sliderMin(0.5)
        .sliderMax(2)
        .visible(playSound::get)
        .build());

    // ------------------------------------------------------------------- state

    private final Random random = new Random();

    private boolean placed;
    private float x, y;                 // top-left of the sprite, GUI pixels
    private float targetX, targetY;
    private float pauseTimer;
    private float walkPhase;            // advances while walking, drives the waddle
    private float waddleAmount;         // 0..1, eases in and out
    private float soundTimer;
    private long lastNanos;

    public FloppaWalker() {
        super(FloppaAddon.CATEGORY, "floppa-walker", "A Floppa cube waddles around your screen and meows.");
    }

    @Override
    public void onActivate() {
        placed = false;
        lastNanos = 0;
        pauseTimer = 0;
        walkPhase = 0;
        waddleAmount = 0;
        resetSoundTimer();

        if (playSound.get() && soundOnEnable.get()) playNoise();
    }

    // --------------------------------------------------------------- rendering

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        if (hideWithHud.get() && mc.options.hudHidden) {
            lastNanos = 0; // avoid a big time jump when the HUD comes back
            return;
        }

        // Frame delta, measured ourselves so we don't depend on event fields.
        long now = System.nanoTime();
        float dt = lastNanos == 0 ? 0f : (now - lastNanos) / 1_000_000_000f;
        lastNanos = now;
        dt = Math.min(dt, 0.1f);

        int screenW = mc.getWindow().getScaledWidth();
        int screenH = mc.getWindow().getScaledHeight();
        int h = size.get();
        int w = Math.max(1, Math.round(h * (float) TEX_W / TEX_H));
        float maxX = Math.max(0, screenW - w);
        float maxY = Math.max(0, screenH - h);

        if (!placed) {
            x = random.nextFloat() * maxX;
            y = bottomOnly.get() ? maxY : random.nextFloat() * maxY;
            pickTarget(maxX, maxY);
            placed = true;
        }

        boolean walking = update(dt, maxX, maxY);
        updateSound(dt);

        // Keep on screen if the window or size setting changed.
        x = clamp(x, 0, maxX);
        y = clamp(y, 0, maxY);
        if (bottomOnly.get()) y = maxY;

        // Waddle: tilt + small hop, easing in/out so it doesn't snap.
        waddleAmount += ((walking && waddle.get() ? 1f : 0f) - waddleAmount) * Math.min(1f, dt * 10f);
        float angle = (float) Math.sin(walkPhase) * 0.12f * waddleAmount;            // ~7 degrees
        float hop = Math.abs((float) Math.sin(walkPhase)) * h * 0.04f * waddleAmount;

        DrawContext ctx = event.drawContext;
        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate(x + w / 2f, y + h / 2f - hop);
        ctx.getMatrices().rotate(angle);
        ctx.drawTexture(RenderPipelines.GUI_TEXTURED, TEXTURE, -w / 2, -h / 2, 0f, 0f, w, h, TEX_W, TEX_H, TEX_W, TEX_H);
        ctx.getMatrices().popMatrix();
    }

    // ------------------------------------------------------------------- logic

    /** Moves Floppa toward the current target. Returns true if he moved this frame. */
    private boolean update(float dt, float maxX, float maxY) {
        if (pauseTimer > 0) {
            pauseTimer -= dt;
            if (pauseTimer <= 0) pickTarget(maxX, maxY);
            return false;
        }

        float dx = targetX - x;
        float dy = targetY - y;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        float step = speed.get().floatValue() * dt;

        if (dist <= Math.max(step, 0.5f)) {
            x = targetX;
            y = targetY;
            if (pauses.get()) pauseTimer = 0.4f + random.nextFloat() * 1.6f;
            else pickTarget(maxX, maxY);
            return false;
        }

        x += dx / dist * step;
        y += dy / dist * step;
        walkPhase += dt * 14f; // ~2.2 waddles per second
        return true;
    }

    private void pickTarget(float maxX, float maxY) {
        targetX = random.nextFloat() * maxX;
        targetY = bottomOnly.get() ? maxY : random.nextFloat() * maxY;
    }

    // ------------------------------------------------------------------- sound

    private void updateSound(float dt) {
        if (!playSound.get()) return;

        soundTimer -= dt;
        if (soundTimer <= 0) {
            playNoise();
            resetSoundTimer();
        }
    }

    private void resetSoundTimer() {
        float lo = (float) Math.min(minInterval.get(), maxInterval.get());
        float hi = (float) Math.max(minInterval.get(), maxInterval.get());
        soundTimer = lo + random.nextFloat() * (hi - lo);
    }

    private void playNoise() {
        mc.getSoundManager().play(PositionedSoundInstance.master(MEOW, pitch.get().floatValue(), volume.get().floatValue()));
    }

    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }
}
