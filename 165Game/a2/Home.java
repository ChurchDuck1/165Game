package a2;

import tage.*;
import tage.shapes.*;
import org.joml.*;

public class Home extends ManualObject
{
    private Vector3f[] v = new Vector3f[24];
    private Vector2f[] uv = new Vector2f[24];
    private Vector3f[] n = new Vector3f[24];

    private int[] idx = new int[] {
        // back face
        4,  5,  6,   4,  6,  7,
        // left face
        8,  9, 10,   8, 10, 11,
        // right face
        12, 13, 14,  12, 14, 15,
        // top face
        16, 17, 18,  16, 18, 19,
        // bottom face
        20, 21, 22,  20, 22, 23
    };

    public Home()
    {
        super();

        float hx = 6.5f;
        float hy = 6.0f;
        float hz = 6.0f;

        // ---- front face (doorway) ----
        v[0] = new Vector3f(-hx, -hy,  hz);
        v[1] = new Vector3f( hx, -hy,  hz);
        v[2] = new Vector3f( hx,  hy,  hz);
        v[3] = new Vector3f(-hx,  hy,  hz);
        uv[0] = new Vector2f(0f, 0f);
        uv[1] = new Vector2f(1f, 0f);
        uv[2] = new Vector2f(1f, 1f);
        uv[3] = new Vector2f(0f, 1f);
        for (int i = 0; i < 4; i++) n[i] = new Vector3f(0f, 0f, 1f);

        // ---- back face ----
        v[4] = new Vector3f( hx, -hy, -hz);
        v[5] = new Vector3f(-hx, -hy, -hz);
        v[6] = new Vector3f(-hx,  hy, -hz);
        v[7] = new Vector3f( hx,  hy, -hz);
        uv[4] = new Vector2f(0f, 0f);
        uv[5] = new Vector2f(1f, 0f);
        uv[6] = new Vector2f(1f, 1f);
        uv[7] = new Vector2f(0f, 1f);
        for (int i = 4; i < 8; i++) n[i] = new Vector3f(0f, 0f, -1f);

        // ---- left face ----
        v[8]  = new Vector3f(-hx, -hy, -hz);
        v[9]  = new Vector3f(-hx, -hy,  hz);
        v[10] = new Vector3f(-hx,  hy,  hz);
        v[11] = new Vector3f(-hx,  hy, -hz);
        uv[8]  = new Vector2f(0f, 0f);
        uv[9]  = new Vector2f(1f, 0f);
        uv[10] = new Vector2f(1f, 1f);
        uv[11] = new Vector2f(0f, 1f);
        for (int i = 8; i < 12; i++) n[i] = new Vector3f(-1f, 0f, 0f);

        // ---- right face ----
        v[12] = new Vector3f( hx, -hy,  hz);
        v[13] = new Vector3f( hx, -hy, -hz);
        v[14] = new Vector3f( hx,  hy, -hz);
        v[15] = new Vector3f( hx,  hy,  hz);
        uv[12] = new Vector2f(0f, 0f);
        uv[13] = new Vector2f(1f, 0f);
        uv[14] = new Vector2f(1f, 1f);
        uv[15] = new Vector2f(0f, 1f);
        for (int i = 12; i < 16; i++) n[i] = new Vector3f(1f, 0f, 0f);

        // ---- top face ----
        v[16] = new Vector3f(-hx,  hy,  hz);
        v[17] = new Vector3f( hx,  hy,  hz);
        v[18] = new Vector3f( hx,  hy, -hz);
        v[19] = new Vector3f(-hx,  hy, -hz);
        uv[16] = new Vector2f(0f, 0f);
        uv[17] = new Vector2f(1f, 0f);
        uv[18] = new Vector2f(1f, 1f);
        uv[19] = new Vector2f(0f, 1f);
        for (int i = 16; i < 20; i++) n[i] = new Vector3f(0f, 1f, 0f);

        // ---- bottom face ----
        v[20] = new Vector3f(-hx, -hy, -hz);
        v[21] = new Vector3f( hx, -hy, -hz);
        v[22] = new Vector3f( hx, -hy,  hz);
        v[23] = new Vector3f(-hx, -hy,  hz);
        uv[20] = new Vector2f(0f, 0f);
        uv[21] = new Vector2f(1f, 0f);
        uv[22] = new Vector2f(1f, 1f);
        uv[23] = new Vector2f(0f, 1f);
        for (int i = 20; i < 24; i++) n[i] = new Vector3f(0f, -1f, 0f);

        setNumVertices(30);
        setVerticesIndexed(idx, v);
        setTexCoordsIndexed(idx, uv);
        setNormalsIndexed(idx, n);

        setMatAmb(Utils.silverAmbient());
        setMatDif(Utils.silverDiffuse());
        setMatSpe(Utils.silverSpecular());
        setMatShi(Utils.silverShininess());
    }
}