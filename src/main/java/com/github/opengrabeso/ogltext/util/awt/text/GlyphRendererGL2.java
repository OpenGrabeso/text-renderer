/*
 * Copyright 2012 JogAmp Community. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 *
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY JogAmp Community ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL JogAmp Community OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * or implied, of JogAmp Community.
 */
package com.github.opengrabeso.ogltext.util.awt.text;

import com.github.opengrabeso.jaagl.GL2;
import com.github.opengrabeso.jaagl.GL2GL3;


/**
 * {@link GlyphRenderer} for use with OpenGL 2.
 */
/*@VisibleForTesting*/
/*@NotThreadSafe*/
public final class GlyphRendererGL2 extends AbstractGlyphRenderer {

    /**
     * True if using vertex arrays.
     */
    private boolean useVertexArrays = true;

    /**
     * Constructs a {@link GlyphRendererGL2}.
     */
    /*@VisibleForTesting*/
    public GlyphRendererGL2() {
        // empty
    }

    @Override
    protected void doBeginRendering(/*@Nonnull*/ final GL2GL3 gl,
                                    final boolean ortho,
                                    /*@Nonnegative*/ final int width,
                                    /*@Nonnegative*/ final int height,
                                    final boolean disableDepthTest) {

        Check.notNull(gl, "GL cannot be null");
        Check.argument(width >= 0, "Width cannot be negative");
        Check.argument(height >= 0, "Height cannot be negative");

        final GL2 gl2 = gl.getGL2();

        // Change general settings
        gl2.glPushAttrib(getAttribMask(gl2, ortho));
        gl2.glDisable(gl2.GL_LIGHTING());
        gl2.glEnable(gl2.GL_BLEND());
        gl2.glDisable(gl2.GL_SCISSOR_TEST());
        gl2.glBlendFunc(gl2.GL_SRC_ALPHA(), gl2.GL_ONE_MINUS_SRC_ALPHA());
        gl2.glEnable(gl2.GL_TEXTURE_2D());
        gl2.glTexEnvi(gl2.GL_TEXTURE_ENV(), gl2.GL_TEXTURE_ENV_MODE(), gl2.GL_MODULATE());

        // Set up transformations
        if (ortho) {
            if (disableDepthTest) {
                gl2.glDisable(gl2.GL_DEPTH_TEST());
            }
            gl2.glDisable(gl2.GL_CULL_FACE());
            gl2.glMatrixMode(gl2.GL_PROJECTION());
            gl2.glPushMatrix();
            gl2.glLoadIdentity();
            gl2.glOrtho(0, width, 0, height, -1, +1);
            gl2.glMatrixMode(gl2.GL_MODELVIEW());
            gl2.glPushMatrix();
            gl2.glLoadIdentity();
            gl2.glMatrixMode(gl2.GL_TEXTURE());
            gl2.glPushMatrix();
            gl2.glLoadIdentity();
        }
    }

    /*@Nonnull*/
    protected QuadPipeline doCreateQuadPipeline(/*@Nonnull*/ final GL2GL3 gl) {

        Check.notNull(gl, "GL cannot be null");

        final GL2 gl2 = gl.getGL2();

        return new QuadPipelineGL15(gl2);
    }

    protected void doDispose(/*@Nonnull*/ final GL2GL3 gl) {
        Check.notNull(gl, "GL cannot be null");
    }

    @Override
    protected void doEndRendering(/*@Nonnull*/ final GL2GL3 gl) {

        Check.notNull(gl, "GL cannot be null");

        final GL2 gl2 = gl.getGL2();

        // Reset transformations
        if (isOrthoMode()) {
            gl2.glMatrixMode(gl2.GL_PROJECTION());
            gl2.glPopMatrix();
            gl2.glMatrixMode(gl2.GL_MODELVIEW());
            gl2.glPopMatrix();
            gl2.glMatrixMode(gl2.GL_TEXTURE());
            gl2.glPopMatrix();
        }

        // Reset general settings
        gl2.glPopAttrib();
    }

    @Override
    protected void doSetColor(/*@Nonnull*/ final GL2GL3 gl,
                              final float r,
                              final float g,
                              final float b,
                              final float a) {

        Check.notNull(gl, "GL cannot be null");

        final GL2 gl2 = gl.getGL2();

        gl2.glColor4f(r, g, b, a);
    }

    @Override
    protected void doSetTransform3d(/*@Nonnull*/ final GL2GL3 gl,
                                    /*@Nonnull*/ final float[] value,
                                    final boolean transpose) {

        Check.notNull(gl, "GL cannot be null");
        Check.notNull(value, "Value cannot be null");

        // FIXME: Could implement this...
        throw new UnsupportedOperationException("Use standard GL instead");
    }

    @Override
    protected void doSetTransformOrtho(/*@Nonnull*/ final GL2GL3 gl,
                                       /*@Nonnegative*/ final int width,
                                       /*@Nonnegative*/ final int height) {

        Check.notNull(gl, "GL cannot be null");
        Check.argument(width >= 0, "Width cannot be negative");
        Check.argument(height >= 0, "Height cannot be negative");

        final GL2 gl2 = gl.getGL2();

        gl2.glMatrixMode(gl2.GL_PROJECTION());
        gl2.glPushMatrix();
        gl2.glLoadIdentity();
        gl2.glOrtho(0, width, 0, height, -1, +1);
        gl2.glMatrixMode(gl2.GL_MODELVIEW());
        gl2.glPushMatrix();
        gl2.glLoadIdentity();
    }

    /**
     * Returns attribute bits for {@code glPushAttrib} calls.
     *
     * @param ortho True if using orthographic projection
     * @return Attribute bits for {@code glPushAttrib} calls
     */
    private static int getAttribMask(final GL2 gl2, final boolean ortho) {
        return gl2.GL_ENABLE_BIT() |
               gl2.GL_TEXTURE_BIT() |
               gl2.GL_COLOR_BUFFER_BIT() |
               gl2.GL_SCISSOR_BIT() |
               (ortho ? (gl2.GL_DEPTH_BUFFER_BIT() | gl2.GL_TRANSFORM_BIT()) : 0);
    }

    @Override
    public boolean getUseVertexArrays() {
        return useVertexArrays;
    }

    @Override
    public void setUseVertexArrays(final boolean useVertexArrays) {
        if (useVertexArrays != this.useVertexArrays) {
            dirtyPipeline();
            this.useVertexArrays = useVertexArrays;
        }
    }
}
