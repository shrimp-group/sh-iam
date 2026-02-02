package com.wkclz.iam.sdk.filter;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StreamUtils;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.*;

/**
 * @author shrimp
 */
public class EagerContentCachingRequestWrapper extends ContentCachingRequestWrapper {

    private byte[] cachedBody = null;

    public EagerContentCachingRequestWrapper(HttpServletRequest request) {
        super(request, 0);
    }
    public EagerContentCachingRequestWrapper(HttpServletRequest request, int contentCacheLimit) {
        super(request, contentCacheLimit);
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        if (cachedBody == null) {
            return super.getInputStream();
        }
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(cachedBody);
        return new ServletInputStream() {
            @Override
            public boolean isFinished() {
                return byteArrayInputStream.available() == 0;
            }
            @Override
            public boolean isReady() {
                return true;
            }
            @Override
            public void setReadListener(ReadListener readListener) {
                throw new UnsupportedOperationException();
            }
            @Override
            public int read() {
                return byteArrayInputStream.read();
            }
        };
    }

    @Override
    public BufferedReader getReader() throws IOException {
        ServletInputStream inputStream = getInputStream();
        InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
        return new BufferedReader(inputStreamReader);
    }

    @Override
    public byte[] getContentAsByteArray() {
        return cachedBody == null ? super.getContentAsByteArray() : cachedBody;
    }


    public void makeBodyCache() throws IOException {
        ServletRequest request = super.getRequest();
        InputStream inputStream = request.getInputStream();
        this.cachedBody = StreamUtils.copyToByteArray(inputStream);
    }

}
