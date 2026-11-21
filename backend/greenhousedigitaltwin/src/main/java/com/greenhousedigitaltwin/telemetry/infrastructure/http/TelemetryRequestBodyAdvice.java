package com.greenhousedigitaltwin.telemetry.infrastructure.http;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;

@RestControllerAdvice(assignableTypes = TelemetryController.class)
class TelemetryRequestBodyAdvice extends RequestBodyAdviceAdapter {

    static final int MAX_BODY_BYTES = 4096;

    @Override
    public boolean supports(MethodParameter parameter, Type targetType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        return targetType == TelemetryRequest.class;
    }

    @Override
    public HttpInputMessage beforeBodyRead(HttpInputMessage inputMessage, MethodParameter parameter,
                                           Type targetType,
                                           Class<? extends HttpMessageConverter<?>> converterType) throws IOException {
        if (inputMessage.getHeaders().getContentLength() > MAX_BODY_BYTES) {
            throw new PayloadTooLargeException();
        }
        byte[] body = inputMessage.getBody().readNBytes(MAX_BODY_BYTES + 1);
        if (body.length > MAX_BODY_BYTES) {
            throw new PayloadTooLargeException();
        }
        return new HttpInputMessage() {
            private final InputStream bodyStream = new ByteArrayInputStream(body);

            @Override
            public InputStream getBody() {
                return bodyStream;
            }

            @Override
            public HttpHeaders getHeaders() {
                return inputMessage.getHeaders();
            }
        };
    }

    static final class PayloadTooLargeException extends RuntimeException {
    }
}
