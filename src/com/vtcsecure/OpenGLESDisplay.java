package com.vtcsecure;

public class OpenGLESDisplay {
	public static native void init(int ptr, int width, int height);
    public static native void render(int ptr);
}
