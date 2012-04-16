/**
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
 
package jogamp.opengl.util.av.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.media.opengl.GLProfile;

import com.jogamp.common.os.DynamicLibraryBundle;
import com.jogamp.common.os.DynamicLibraryBundleInfo;
import com.jogamp.common.util.RunnableExecutor;

class FFMPEGDynamicLibraryBundleInfo implements DynamicLibraryBundleInfo  {
    private static List<String> glueLibNames = new ArrayList<String>(); // none
    
    private static final int symbolCount = 29;
    private static String[] symbolNames = {
         "avcodec_version",
         "avformat_version",
/* 3 */  "avutil_version",
        
         // libavcodec
         "avcodec_close", 
         "avcodec_string", 
         "avcodec_find_decoder", 
         "avcodec_open2",             // 53.6.0    (opt) 
         "avcodec_open", 
         "avcodec_alloc_frame", 
         "avcodec_default_get_buffer", 
         "avcodec_default_release_buffer", 
         "av_free_packet", 
         "avcodec_decode_audio4",     // 53.25.0   (opt)
         "avcodec_decode_audio3",     // 52.23.0
/* 15 */ "avcodec_decode_video2",     // 52.23.0
        
         // libavutil
         "av_pix_fmt_descriptors", 
         "av_free", 
/* 18 */ "av_get_bits_per_pixel",
        
         // libavformat
         "avformat_close_input",      // 53.17.0   (opt)
         "av_close_input_file",
         "av_register_all", 
         "avformat_open_input", 
         "av_dump_format", 
         "av_read_frame",
         "av_seek_frame",
         "avformat_network_init",     // 53.13.0   (opt)
         "avformat_network_deinit",   // 53.13.0   (opt)
         "avformat_find_stream_info", // 53.3.0    (opt)
/* 29 */ "av_find_stream_info"
    };
    
    private static String[] optionalSymbolNames = {
         "avcodec_open2",             // 53.6.0    (opt) 
         "avcodec_decode_audio4",     // 53.25.0   (opt)
         "avformat_close_input",      // 53.17.0   (opt)
         "avformat_network_init",     // 53.13.0   (opt)
         "avformat_network_deinit",   // 53.13.0   (opt)
         "avformat_find_stream_info"  // 53.3.0    (opt)        
    };
    
    private static long[] symbolAddr;
    private static final boolean ready;
    
    static {
        // native ffmpeg media player implementation is included in jogl_desktop and jogl_mobile     
        GLProfile.initSingleton();
        boolean _ready = false;
        try {
            _ready = initSymbols();
        } catch (Throwable t) {
            t.printStackTrace();
        }
        ready = _ready;
        if(!ready) {
            System.err.println("FFMPEG: Not Available");
        }
    }
    
    static boolean initSingleton() { return ready; }
    
    private static boolean initSymbols() {
        final DynamicLibraryBundle dl = new DynamicLibraryBundle(new FFMPEGDynamicLibraryBundleInfo());
        final boolean avutilLoaded = dl.isToolLibLoaded(0); 
        final boolean avformatLoaded = dl.isToolLibLoaded(1);
        final boolean avcodecLoaded = dl.isToolLibLoaded(2);
        if(!avutilLoaded || !avformatLoaded || !avcodecLoaded) {
            throw new RuntimeException("FFMPEG Tool library incomplete: [ avutil "+avutilLoaded+", avformat "+avformatLoaded+", avcodec "+avcodecLoaded+"]");
        }        
        if(!dl.isToolLibComplete()) {
            throw new RuntimeException("FFMPEG Tool libraries incomplete");
        }        
        symbolAddr = new long[symbolCount];
        
        final Set<String> optionalSet = new HashSet<String>();
        optionalSet.addAll(Arrays.asList(optionalSymbolNames));
        
        if(!lookupSymbols(dl, symbolNames, optionalSet, symbolAddr, symbolCount)) {
            return false;
        }        
        return initSymbols0(symbolAddr, symbolAddr.length);
    }
    
    private static boolean lookupSymbols(DynamicLibraryBundle dl, 
                                         String[] symbols, Set<String> optionalSymbols, 
                                         long[] addresses, int symbolCount) {
        for(int i = 0; i<symbolCount; i++) {
            final long addr = dl.dynamicLookupFunction(symbols[i]);
            if( 0 == addr ) {
                if(!optionalSymbols.contains(symbols[i])) {
                    System.err.println("Could not resolve mandatory symbol <"+symbols[i]+">");
                    return false;
                } else if(true || DEBUG ) { // keep it verbose per default for now ..
                    System.err.println("Could not resolve optional symbol <"+symbols[i]+">");
                }
            }
            addresses[i] = addr;
        }
        return true;
    }
    
    protected FFMPEGDynamicLibraryBundleInfo() {
    }

    @Override
    public boolean shallLinkGlobal() { return true; }

    @Override
    public boolean shallLookupGlobal() { return true; }
    
    @Override
    public final List<String> getGlueLibNames() {
        return glueLibNames;
    }

    @Override
    public List<List<String>> getToolLibNames() {
        List<List<String>> libsList = new ArrayList<List<String>>();

        final List<String> avutil = new ArrayList<String>();
        avutil.add("avutil");        // default
        avutil.add("avutil-52");     // dummy future proof
        avutil.add("avutil-51");     // 0.8
        avutil.add("avutil-50");     // 0.7
        libsList.add(avutil);
        
        final List<String> avformat = new ArrayList<String>();
        avformat.add("avformat");    // default
        avformat.add("avformat-55"); // dummy future proof
        avformat.add("avformat-54"); // 0.?
        avformat.add("avformat-53"); // 0.8
        avformat.add("avformat-52"); // 0.7
        libsList.add(avformat);
        
        final List<String> avcodec = new ArrayList<String>();
        avcodec.add("avcodec");      // default
        avcodec.add("avcodec-55");   // dummy future proof
        avcodec.add("avcodec-54");   // 0.?
        avcodec.add("avcodec-53");   // 0.8
        avcodec.add("avcodec-52");   // 0.7
        libsList.add(avcodec);
                
        return libsList;
    }

    @Override
    public final List<String> getToolGetProcAddressFuncNameList() {
        return null;
    }

    @Override
    public final long toolGetProcAddress(long toolGetProcAddressHandle, String funcName) {
        return 0;
    }

    @Override
    public boolean useToolGetProcAdressFirst(String funcName) {
        return false;
    }

    @Override
    public RunnableExecutor getLibLoaderExecutor() {
        return DynamicLibraryBundle.getDefaultRunnableExecutor();
    }    
    
    private static native boolean initSymbols0(long[] symbols, int count);
}
