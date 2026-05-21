package com.wkclz.iam.sdk.helper;

import com.alibaba.fastjson2.JSON;
import com.wkclz.core.base.R;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.io.PrintWriter;

public class ResponseHelper {

    private final static Logger logger = LoggerFactory.getLogger(ResponseHelper.class);

    public static void responseError(HttpServletResponse response, HttpStatus status, String msg) {
        try {
            if (status == null) {
                status = HttpStatus.UNAUTHORIZED;
            }
            R r = new R<>();
            r.setCode(status.value());
            r.setMsg(msg);
            String string = JSON.toJSONString(r);

            response.setStatus(status.value());
            response.setHeader("Content-Type", "application/json;charset=UTF-8");
            PrintWriter pw = response.getWriter();
            pw.write(string);
            pw.flush();
            pw.close();
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }

}
