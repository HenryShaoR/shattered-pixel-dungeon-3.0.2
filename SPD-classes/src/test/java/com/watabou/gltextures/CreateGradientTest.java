package com.watabou.gltextures;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.utils.GdxNativesLoader;
import com.watabou.glwrap.Texture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

class TextureCacheTest {

    @BeforeAll
    static void setUpGlStub() {
        GdxNativesLoader.load();
        GL20 gl = (GL20) Proxy.newProxyInstance(
                TextureCacheTest.class.getClassLoader(),
                new Class[]{GL20.class},
                new DefaultGlInvocationHandler()
        );
        Gdx.gl = gl;
        Gdx.gl20 = gl;
    }

    @AfterEach
    void clearTextureCache() {
        TextureCache.clear();
    }

    @Test
    void testCreateGradient_sameColors_returnsSameTexture() {
        // Arrange and Act
        SmartTexture first = TextureCache.createGradient(0, 1, 2);
        SmartTexture second = TextureCache.createGradient(0, 1, 2);

        // Assert
        assertSame(first, second);
    }

    @Test
    void testCreateGradient_differentColors_returnsDifferentTexture() {
        // Arrange and Act
        SmartTexture first = TextureCache.createGradient(0, 1);
        SmartTexture second = TextureCache.createGradient(0, 2);

        // Assert
        assertNotSame(first, second);
    }

    @Test
    void testCreateGradient_noColours_noPixels() {
        // Arrange and Act
        SmartTexture texture = TextureCache.createGradient();

        // Assert
        assertEquals(0, texture.width);
        assertEquals(1, texture.height);
        assertEquals(Texture.LINEAR, texture.fModeMin);
        assertEquals(Texture.LINEAR, texture.fModeMax);
        assertEquals(Texture.CLAMP, texture.wModeH);
        assertEquals(Texture.CLAMP, texture.wModeV);
    }

    @Test
    void testCreateGradient_oneColour_colourMatches() {
        // Arrange and Act
        SmartTexture texture = TextureCache.createGradient(0);

        // Assert
        assertEquals(1, texture.width);
        assertEquals(1, texture.height);
        assertEquals(Texture.LINEAR, texture.fModeMin);
        assertEquals(Texture.LINEAR, texture.fModeMax);
        assertEquals(Texture.CLAMP, texture.wModeH);
        assertEquals(Texture.CLAMP, texture.wModeV);
        assertEquals(0, texture.getPixel(0, 0));
    }

    @Test
    void testCreateGradient_multipleColours_coloursMatch() {
        // Arrange and Act
        SmartTexture texture = TextureCache.createGradient(0xFF123456, 0xFFABCDEF, 0xFF987654);

        // Assert
        assertEquals(3, texture.width);
        assertEquals(1, texture.height);
        assertEquals(Texture.LINEAR, texture.fModeMin);
        assertEquals(Texture.LINEAR, texture.fModeMax);
        assertEquals(Texture.CLAMP, texture.wModeH);
        assertEquals(Texture.CLAMP, texture.wModeV);
        assertEquals(0xFF123456, texture.getPixel(0, 0));
        assertEquals(0xFFABCDEF, texture.getPixel(1, 0));
        assertEquals(0xFF987654, texture.getPixel(2, 0));
    }

    // Fake InvocationHandler
    private static final class DefaultGlInvocationHandler implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            Class<?> returnType = method.getReturnType();
            if (returnType == Void.TYPE) {
                return null;
            } else if (returnType == Integer.TYPE) {
                return 0;
            } else if (returnType == Boolean.TYPE) {
                return false;
            } else if (returnType == Float.TYPE) {
                return 0f;
            } else if (returnType == Long.TYPE) {
                return 0L;
            } else if (returnType == Double.TYPE) {
                return 0d;
            }
            return null;
        }
    }
}