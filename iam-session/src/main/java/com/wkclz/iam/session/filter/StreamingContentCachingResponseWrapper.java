package com.wkclz.iam.session.filter;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.util.Locale;

/**
 * 支持流式响应直写的 ContentCachingResponseWrapper 改造版（本包同名类，优先于 Spring 原类）。
 *
 * <p>Spring 的 {@link org.springframework.web.util.ContentCachingResponseWrapper} 会把所有写入内容先缓存到内存，
 * 再由过滤器 finally 中的 {@code copyBodyToResponse()} 一次性写到底层响应；而 SSE 等异步流式响应的数据是在
 * {@code chain.doFilter} 返回之后由异步线程持续写入的，copy 只执行一次，导致后续数据全部滞留缓存、客户端永远收不到。
 * 此处仅覆写 {@link #getOutputStream()}：application/json 响应仍走父类缓存（用于请求日志记录），
 * text/event-stream 等流式/二进制响应直写底层 response，其余父类能力（copyBodyToResponse/getContentAsByteArray 等）复用。</p>
 */
public class StreamingContentCachingResponseWrapper extends ContentCachingResponseWrapper {

    private ServletOutputStream wrapper;

    public StreamingContentCachingResponseWrapper(HttpServletResponse response) {
        super(response);
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        if (this.wrapper == null) {
            ServletOutputStream cachingStream = super.getOutputStream();
            ServletOutputStream directStream = getResponse().getOutputStream();
            this.wrapper = new ServletOutputStream() {
                @Override
                public void write(int b) throws IOException {
                    if (isStreaming()) {
                        directStream.write(b);
                    } else {
                        cachingStream.write(b);
                    }
                }

                @Override
                public void write(byte[] b, int off, int len) throws IOException {
                    if (isStreaming()) {
                        directStream.write(b, off, len);
                    } else {
                        cachingStream.write(b, off, len);
                    }
                }

                @Override
                public void flush() throws IOException {
                    cachingStream.flush();
                    directStream.flush();
                }

                @Override
                public boolean isReady() {
                    return cachingStream.isReady();
                }

                @Override
                public void setWriteListener(WriteListener writeListener) {
                    cachingStream.setWriteListener(writeListener);
                }
            };
        }
        return this.wrapper;
    }

    /**
     * 是否流式/非 JSON 响应（需要直写底层；仅 application/json 走父类缓存用于日志记录）
     */
    private boolean isStreaming() {
        String contentType = getContentType();
        return contentType != null && !contentType.toLowerCase(Locale.ROOT).contains("application/json");
    }
}
