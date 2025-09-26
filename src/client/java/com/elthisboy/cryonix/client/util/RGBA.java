package com.elthisboy.cryonix.client.util;

public class RGBA {
    public final int r,g,b,a;
    public RGBA(int r, int g, int b, int a){ this.r=r; this.g=g; this.b=b; this.a=a; }
    public float rf(){ return r/255f; }
    public float gf(){ return g/255f; }
    public float bf(){ return b/255f; }
    public float af(){ return a/255f; }
}